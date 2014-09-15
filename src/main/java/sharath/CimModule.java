package sharath;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.log4j.Logger;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* Created by sgururaj on 9/6/14.
*/
class CimModule {
    final Path src;
    final Path dest;
    final Path srcTest;
    final Path destTest;
    final Path srcResource;
    final Path destResource;
    final Path srcTestResource;
    final Path destTestResource;
    final String srcRuntimeOptions;
    final String[] testRuntimeOptions;
    final List<String> javacSrcOptions;
    final List<String> javacTestOptions;
    final StandardJavaFileManager fm;
    public CimModule(Path src, Path dest, Path srcTest, Path destTest, Path srcResource, Path destResource, Path srcTestResource, Path destTestResource, String srcRuntimeOptions, String[] testRuntimeOptions, List<String> javacSrcOptions, List<String> javacTestOptions, StandardJavaFileManager fm) {

        this.src = src;
        this.dest = dest;
        this.srcTest = srcTest;
        this.destTest = destTest;
        this.srcResource = srcResource;
        this.destResource = destResource;
        this.srcTestResource = srcTestResource;
        this.destTestResource = destTestResource;
        this.srcRuntimeOptions = srcRuntimeOptions;
        this.testRuntimeOptions = testRuntimeOptions;
        this.javacSrcOptions = javacSrcOptions;
        this.javacTestOptions = javacTestOptions;
        this.fm = fm;
    }

    static class Factory {
        private Connection conn;
        private JavaCompiler jc;
        private static final Logger log = Logger.getLogger(Factory.class);


        @Inject
        public Factory(Connection conn, JavaCompiler jc) {
            this.conn = conn;
            this.jc = jc;
        }
        public CimModule create(String moduleName) throws SQLException {

            Path src=null, dest=null, srcTest=null, destTest=null, srcResource=null, destResource=null, srcTestResource=null, destTestResource=null;
            String path=null;
            String srcRuntimeOptions;
            String[] testRuntimeOptions;
            List<String> javacSrcOptions, javacTestOptions;

            String sql = "select * from module where name = ? limit 1";

            PreparedStatement ps=null;
            ResultSet rs=null;
            try {
                //log.info(ps);
                ps = conn.prepareStatement(sql);
                ps.setString(1, moduleName);
                rs = ps.executeQuery();

                while (rs.next()) {
                    path = rs.getString("path");
                    src = Paths.get(rs.getString("path"), "src", "main", "java");
                    dest = Paths.get(rs.getString("path"), "target", "classes");
                    srcTest = Paths.get(rs.getString("path"), "src", "test", "java");
                    destTest = Paths.get(rs.getString("path"), "target", "test-classes");
                    srcResource = Paths.get(rs.getString("path"), "src", "main", "resources");
                    destResource = Paths.get(rs.getString("path"), "target", "classes");
                    srcTestResource = Paths.get(rs.getString("path"), "src", "test", "resources");
                    destTestResource = Paths.get(rs.getString("path"), "tatrge", "test-classes");
                }
            } finally {
                if(ps!=null) ps.close();
                if(rs!=null) rs.close();
            }

            //for compile and test
            String compile = null, test = null, runtime = null;

            sql = "select * from module where name = ? and (phase = 'compile' or phase = 'test' or phase='runtime')";
            try {
                ps = conn.prepareStatement(sql);
                ps.setString(1, moduleName);
                rs = ps.executeQuery();
                while (rs.next()) {
                    if (rs.getString("phase").equals("compile")) {
                        compile = rs.getString("classpath");
                    } else if (rs.getString("phase").equals("test")) {
                        test = rs.getString("classpath");
                    } else if (rs.getString("phase").equals("runtime")) {
                        runtime = rs.getString("classpath");
                    }
                }
            } finally {
                if(ps!=null) ps.close();
                if(rs!=null) rs.close();
            }

            javacSrcOptions = ImmutableList.of("-d", dest.toString(), "-classpath", dest.toString()+":"+compile, "-sourcepath", src.toString(), "-s", Paths.get(path, "target", "generated-sources", "annotations").toString(), "-g", "-nowarn", "-target 1.7", "-source", "1.7", "-encoding", "UTF-8");

            javacTestOptions = ImmutableList.of("-d", destTest.toString(), "-classpath", destTest.toString()+":"+dest.toString()+":"+compile+":"+test, "-sourcepath", srcTest.toString(), "-s", Paths.get(path, "target", "generated-test-sources", "annotations").toString(), "-g", "-nowarn", "-target 1.7", "-source", "1.7", "-encoding", "UTF-8");

            testRuntimeOptions = new String[]{"java","-Xdebug", "-Xrunjdwp:server=y,transport=dt_socket,address=4005,suspend=n", "-classpath", destTest.toString()+":"+dest.toString()+":"+compile+":"+test+":"+runtime, "com.coverity.TestRunner", "PLACEHOLDER_FOR_TEST"};

            srcRuntimeOptions = dest.toString()+":"+compile+":"+runtime;



            return new CimModule(src, dest, srcTest, destTest, srcResource, destResource, srcTestResource, destTestResource, srcRuntimeOptions, testRuntimeOptions, javacSrcOptions, javacTestOptions, jc.getStandardFileManager(null, null, null));
        }
    }
    @Singleton
    static class AllModules {
        Map<String, CimModule> all = new HashMap<>();
        private Utils.Config cfg;
        private Factory factory;

        @Inject
        public AllModules(Utils.Config cfg, Factory factory) {
            this.cfg = cfg;
            this.factory = factory;
        }
        public CimModule forName(String name) throws SQLException {
            if(!all.containsKey(name)) {
                all.put(name, factory.create(name));
            }
            return all.get(name);
        }
    }
}
