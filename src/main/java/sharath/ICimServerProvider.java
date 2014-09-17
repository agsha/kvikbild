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
        System.setProperty("org.eclipse.jetty.LEVEL", "WARN");
        System.setProperty("org.eclipse.jetty.websocket.LEVEL", "WARN");
        System.setProperty("java.io.tmpdir", "/tmp");

        String[]paths;
        try {
            paths = allModules.forName("web").srcRuntimeOptions.split(":");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        Set<String> targetClasses = new LinkedHashSet<>(paths.length);
        final Set<URL> urlClasses = new LinkedHashSet<>();
        for(String p:paths) {
            p = p.trim();
            //if(!p.contains(Paths.get("target", "classes").toString())) continue;
            if(!Files.isDirectory(Paths.get(p))) continue;
            if(!p.endsWith("/")) p = p+"/";
            targetClasses.add("file://"+p);
            try {
                urlClasses.add(new URL("file://"+p));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        //targetClasses.add("file://"+cfg.cwd+"/app/web/src/main/webapp/");
        //targetClasses.add("file://"+cfg.cwd+"/app/web/src/main/webapp/WEB-INF/");

        for (URL url : cimClassLoader.getURLs()) {
            //targetClasses.add(url.toString());
        }

        //final String extraClasspath = Joiner.on(",").join(cimClassLoader.getURLs());
        final String extraClasspath = Joiner.on(",").join(targetClasses);
        //final String extraClasspath = "";


        log.info(extraClasspath);
        class CimStarter implements Runnable{
            Object uncastedServer;

            @Override
            public void run() {
                Thread.currentThread().setContextClassLoader(cimClassLoader);
                try {
                    uncastedServer = cimClassLoader.loadClass("sharath.CimServer").getDeclaredConstructor(new Class[]{int.class, String.class, String.class, URL[].class}).newInstance(8005, cfg.cwd, extraClasspath, urlClasses.toArray(new URL[]{}));
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

