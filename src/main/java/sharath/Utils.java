package sharath;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.log4j.Logger;

import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sgururaj on 8/22/14.
 */
@Singleton
public class Utils {

    private static final Logger log = Logger.getLogger(Utils.class);
    private CimModule module;

    //cwd/app/core/target/classes(test-classes)
    //cwd/app/core/src/main(test)/java

    public Utils(CimModule module) {
        this.module = module;

    }

    public String toClass(Path file) {
        return file.toString()
                .replaceAll(module.src.toString(), module.dest.toString())
                .replaceAll(module.srcTest.toString(), module.destTest.toString())
                .replaceAll("java$", "class");
    }

    public String toJava(Path file) {
        return file.toString()
                .replaceAll(module.dest.toString(), module.src.toString())
                .replaceAll(module.destTest.toString(), module.srcTest.toString())
                .replaceAll("class$", "java");

    }

    public String toTargetResource(Path resource) {
        return resource.toString()
                .replaceAll(module.srcResource.toString(), module.destResource.toString())
                .replaceAll(module.srcTestResource.toString(), module.destTestResource.toString());
    }

    public String toSourceResource(Path resource) {
        return resource.toString()
                .replaceAll(module.destResource.toString(), module.srcResource.toString())
                .replaceAll(module.destTestResource.toString(), module.srcTestResource.toString());
    }

    static class CimModuleFactory {
        String cwd;

        CimModuleFactory(Config config) {
            this.cwd = config.cwd;
        }
    }

    static class Factory {

        private CimModule.AllModules allModules;

        @Inject
        Factory(CimModule.AllModules allModules) {

            this.allModules = allModules;
        }

        public Utils createCoreUtils() throws SQLException {return new Utils(allModules.forName("core"));}
        public Utils create(CimModule module) {
            return new Utils(module);
        }
    }

    @Singleton
    static class StandardJavaFileManagerFactory {
        HashMap<String, StandardJavaFileManager> map = new HashMap<>();
        public StandardJavaFileManager forCoreSrc() {
            return getOrCreate("coreSrc");
        }

        public StandardJavaFileManager forCoreTest() {
            return getOrCreate("coreTest");
        }

        private StandardJavaFileManager getOrCreate(String key) {
            if(!map.containsKey(key)) {
                map.put(key, ToolProvider
                                .getSystemJavaCompiler()
                                .getStandardFileManager(null, null, null));
            }
            return map.get(key);

        }
    }

    @Singleton
    static class Config {
        String cwd;
        int port;
        int cimPort;
        String jettyClasspath;
        Map<String, String> moduleToPath;

        Config(String cwd, int port, int cimPort, String jettyClasspath, Map<String, String> moduleToPath) {
            this.cwd = cwd;
            this.port = port;
            this.cimPort = cimPort;
            this.jettyClasspath = jettyClasspath;
            this.moduleToPath = moduleToPath;
        }
        static class Factory {
            public Config create(String cwd, int port, int cimPort, String jettyClasspath) {
                Map<String, String> moduleToPath = new HashMap<>();

                List<String> forbidden = ImmutableList.of(
                        "ces", "",
                        "base", "base",
                        "license", "base/license",
                        "cimlistener", "base/cimlistener",
                        "structext", "base/structext",
                        "app", "app",
                        "core", "app/core",
                        "ws", "app/ws",
                        "web", "app/web",
                        "findbugs-checkers", "app/findbugs-checkers",
                        "ces-tools", "ces-tools",
                        "tomcat", "tomcat");
                for(int i=0; i<forbidden.size(); i+=2) {
                    moduleToPath.put(forbidden.get(i), Paths.get(cwd, forbidden.get(i + 1)).toString());
                }
                return new Config(cwd, port, cimPort, jettyClasspath, moduleToPath);

            }
        }

    }
}
