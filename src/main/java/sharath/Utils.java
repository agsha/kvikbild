package sharath;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.log4j.Logger;

import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

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

    static class CimModule {
        Path src;
        Path dest;
        Path srcTest;
        Path destTest;
        Path srcResource;
        Path destResource;
        Path srcTestResource;
        Path destTestResource;
        List<String> javacSrcOptions;
        List<String> javacTestOptions;

        CimModule(Path src, Path dest, Path srcTest, Path destTest, Path srcResource, Path destResource, Path srcTestResource, Path destTestResource, List<String> javacSrcOptions, List<String> javacTestOptions) {
            this.src = src;
            this.dest = dest;
            this.srcTest = srcTest;
            this.destTest = destTest;
            this.srcResource = srcResource;
            this.destResource = destResource;
            this.srcTestResource = srcTestResource;
            this.destTestResource = destTestResource;
            this.javacSrcOptions = javacSrcOptions;
            this.javacTestOptions = javacTestOptions;
        }


    }

    static class CimModuleFactory {
        String cwd;

        CimModuleFactory(Config config) {
            this.cwd = config.cwd;
        }
    }

    static class Factory {
        CimModule core;

        @Inject
        Factory(@Named("core")CimModule core) {
            this.core = core;
        }

        public Utils createCoreUtils() {return new Utils(core);}
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

        Config(String cwd, int port, int cimPort, String jettyClasspath) {
            this.cwd = cwd;
            this.port = port;
            this.cimPort = cimPort;
            this.jettyClasspath = jettyClasspath;
        }

    }
}
