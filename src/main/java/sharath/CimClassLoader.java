package sharath;

import com.google.inject.Inject;
import org.apache.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author sgururaj
 */
class CimClassLoader extends URLClassLoader {
    public CimClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }
    private static final Logger log = Logger.getLogger(CimClassLoader.class);

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if(name==null) return null;
        if(name.contains("CIMResourceBundleMessageSource")) {
            log.info("hiiiiiiiiiiiii");
        }
        //log.info(name);
        Class c = null;
        if(name.startsWith("java.")
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
        boolean fresh = false;
        if(c==null) {
            try {
                c = findClass(name);
                fresh = true;
            } catch (ClassNotFoundException e) {
                if(!name.startsWith("com")&&!name.startsWith("org")) {
                    //log.info("couldnt find "+name);
                    //log.info(e);
                }
                throw e;
            }

        }
        if(c!=null&&fresh) {
            //log.info("found class: "+name);
        }

        /*if(resolve) {
            resolveClass(c);
        }*/
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
                if(path.contains(Paths.get("target", "classes").toString())&& Files.isDirectory(Paths.get(path)) && path.contains("core")) continue;
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
                urlSet.add(new URL("file:///data00/projects/kvikbild/target/classes/"));
            } catch (MalformedURLException e) {
            }
            log.info(urlSet);
            return new CimClassLoader(urlSet.toArray(new URL[]{}), Provider.class.getClassLoader());
        }
    }
}
