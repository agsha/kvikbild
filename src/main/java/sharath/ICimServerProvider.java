package sharath;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

/**
 * @author sgururaj
 */
public class ICimServerProvider implements Provider<ICimServer> {
    CimClassLoader cimClassLoader;
    private Utils.Config cfg;
    private CimModule.AllModules allModules;
    private static final Logger log = Logger.getLogger(ICimServerProvider.class);

    @Inject
    public ICimServerProvider(CimClassLoader cimClassLoader, Utils.Config cfg, CimModule.AllModules allModules) {
        this.cimClassLoader = cimClassLoader;
        this.cfg = cfg;
        this.allModules = allModules;
    }

    @Override
    public ICimServer get() {
        ICimServer server = null;
        Properties prop = new Properties();
        try {
            prop.load(Files.newInputStream(Paths.get(cfg.cwd, "app/core/src/test/resources/properties/build.properties")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            prop.load(Files.newInputStream(Paths.get(cfg.cwd, "app/core/src/test/resources/properties", "build_"+System.getProperty("user.name")+".properties")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //log.info(prop);

        System.setProperty("env.ALT_DB_SUFFIX", "true");
        System.setProperty("commitPort", "9010");
        System.setProperty("maindb.name", (String)prop.get("maindb.name"));
        System.setProperty("testdb.name", (String)prop.get("testdb.name"));
        System.setProperty("dir.log", Paths.get(cfg.cwd, "logs").toString());
        System.setProperty("log4j.rootLogger.level", "ALL");
        System.setProperty("ces.home", cfg.cwd);
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StrErrLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "ALL");
        System.setProperty("org.eclipse.jetty.websocket.LEVEL", "ALL");
        System.setProperty("java.io.tmpdir", "/tmp");

        String[]paths;
        try {
            paths = allModules.forName("web").srcRuntimeOptions.split(":");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        Set<String> targetClasses = new LinkedHashSet<>(paths.length);
        for(String p:paths) {
            p = p.trim();
            if(!p.contains(Paths.get("target", "classes").toString())) continue;
            if(!Files.isDirectory(Paths.get(p))) continue;
            if(!p.endsWith("/")) p = p+"/";
            targetClasses.add(p);
        }

        final String extraClasspath = Joiner.on(",").join(targetClasses);

        log.info(extraClasspath);
        class CimStarter implements Runnable{
            Object uncastedServer;

            @Override
            public void run() {
                Thread.currentThread().setContextClassLoader(cimClassLoader);
                try {
                    uncastedServer = cimClassLoader.loadClass("sharath.CimServer").getDeclaredConstructor(new Class[]{int.class, String.class, String.class}).newInstance(8005, cfg.cwd, extraClasspath);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }

            }
        }
        CimStarter cimStarter = new CimStarter();
        Thread t = new Thread(cimStarter);
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            log.info(e);
        }
        server = (ICimServer)cimStarter.uncastedServer;
        return server;
    }
}

class CimClassLoader extends URLClassLoader {
    public CimClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }
    private static final Logger log = Logger.getLogger(CimClassLoader.class);

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if(name==null) return null;
        //log.info(name);
        Class c = null;
        if(c==null && name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("sun.")
                || name.startsWith("com.sun.")
                || name.startsWith("org.xml.")
                || name.startsWith("org.w3c.")
                || name.startsWith("sharath.ICimServer")) {
            try {
                c = super.loadClass(name, resolve);
            } catch (ClassNotFoundException e) {

            }
        }
        if(c==null) {
            c = findLoadedClass(name);
        }
        if(c==null) {
            c = findClass(name);
            if(name.startsWith("com.coverity.shared")) log.info(name + (c!=null?c.toString():"no name"));

        }

        if(resolve) {
            resolveClass(c);
        }
        return c;
    }
    static class Provider implements com.google.inject.Provider<CimClassLoader>{

        private CimModule.AllModules allModules;
        private Utils.Config cfg;
        private static final Logger log = Logger.getLogger(Provider.class);

        @Inject
        Provider(CimModule.AllModules allModules, Utils.Config cfg) {
            this.allModules = allModules;
            this.cfg = cfg;
        }

        @Override
        public CimClassLoader get() {
            String[] paths;
            try {
                paths = allModules.forName("web").srcRuntimeOptions.split(":");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            Set<URL> urlSet = new LinkedHashSet<>(paths.length);
            for(String p:paths) {
                String path = p.length()>0&&(p.endsWith(".jar")||p.endsWith("/"))?p:p+"/";
                try {
                    urlSet.add(new URL("file://"+path));
                } catch (MalformedURLException e) {
                    log.info("Invalid path: " + path);
                }
            }
            for(String path:cfg.jettyClasspath.split(":")) {
                try {
                    urlSet.add(new URL("file://"+path));
                } catch (MalformedURLException e) {
                    log.info("Invalid path: " + path);
                }
            }

            try {
                //urlSet.remove(new URL("file:///Users/sgururaj/projects/cim/app/core/target/test-classes/"));
                //urlSet.remove(new URL("file:///Users/sgururaj/projects/cim/app/core/target/classes/"));
                urlSet.add(new URL("file:///Users/sgururaj/projects/kvikbild/target/classes/"));
            } catch (MalformedURLException e) {
            }
            //log.info(urlSet);
            return new CimClassLoader(urlSet.toArray(new URL[]{}), Provider.class.getClassLoader());
        }
    }
}
