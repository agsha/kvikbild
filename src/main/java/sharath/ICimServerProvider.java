package sharath;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * @author sgururaj
 */
public class ICimServerProvider implements Provider<ICimServer> {
    CimClassLoader cimClassLoader;
    private Utils.Config cfg;

    @Inject
    public ICimServerProvider(CimClassLoader cimClassLoader, Utils.Config cfg) {
        this.cimClassLoader = cimClassLoader;
        this.cfg = cfg;
    }

    @Override
    public ICimServer get() {
        ICimServer server = null;
        ClassLoader oldThreadContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cimClassLoader);
            server = (ICimServer)cimClassLoader.loadClass("sharath.CimServer").getDeclaredConstructor(new Class[]{int.class, String.class}).newInstance(8005, cfg.cwd);
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
        } finally {
            Thread.currentThread().setContextClassLoader(oldThreadContextClassLoader);
        }
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

        Class c = null;
        if(c==null && name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("sun.")
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
        }

        if(resolve) {
            resolveClass(c);
        }
        return c;
    }
    static class Provider implements com.google.inject.Provider<CimClassLoader>{

        Utils.CimModule webmodule;
        private Utils.Config cfg;
        private static final Logger log = Logger.getLogger(Provider.class);

        @Inject
        Provider(@Named("web")Utils.CimModule webModule, Utils.Config cfg) {
            this.webmodule = webModule;
            this.cfg = cfg;
        }

        @Override
        public CimClassLoader get() {
            String[] paths = webmodule.javacSrcOptions.get(3).split(":");
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
                urlSet.remove(new URL("file:///Users/sgururaj/projects/cim/app/core/target/test-classes"));
                urlSet.remove(new URL("file:///Users/sgururaj/projects/cim/app/core/target/classes"));
                urlSet.add(new URL("file:///Users/sgururaj/projects/kvikbild/target/classes/"));


            } catch (MalformedURLException e) {
            }
            log.info(urlSet);
            return new CimClassLoader(urlSet.toArray(new URL[]{}), Provider.class.getClassLoader());
        }
    }
}
