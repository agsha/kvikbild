package sharath;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;
import org.junit.Ignore;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Unit test for simple App.
 */
public class AppTest
    extends TestCase
{
    private static final Logger log = Logger.getLogger(AppTest.class.getName());
    private static final FileSystem fs = FileSystems.getDefault();
    private final Utils utils;
    private final Utils.Config cfg;
    Injector injector = Guice.createInjector(new MyModule());
    Utils.CimModule cim;
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
        cfg = injector.getInstance(Utils.Config.class);
        cim = injector.getInstance(Key.get(Utils.CimModule.class, Names.named("core")));
        utils = injector.getInstance(Utils.Factory.class).createCoreUtils();
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    @Ignore
    public void testApp() throws Exception {
        App app = injector.getInstance(App.Factory.class).create(new String[0]);
        app.start();
        Builder builder = injector.getInstance(Builder.class);
    }

    public void testPb() throws IOException, InterruptedException {
//        ProcessBuilder pb = new ProcessBuilder( "echo", "yoyo");
//        log.info(pb.command());
//        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
//        Process process = pb.start();
//        process.waitFor();
        String[] command = {"/bin/bash", "echo", "Argument1"};
        ProcessBuilder p = new ProcessBuilder(command);
        Process p2 = p.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(p2.getInputStream()));
        String line;

        System.out.println("Output of running " + command + " is: ");
        while ((line = br.readLine()) != null) {
            log.error(line);
        }
        log.error("fini");
    }


    public void testBuilder() throws IOException, SQLException {
        injector.getInstance(GraphFlusher.class).load(injector.getInstance(Graph.Factory.class).getForName("core"), "core_graph");
        CompileTask task = injector.getInstance(CompileTask.Factory.class).createCoreCompileTask();
        task.doCompile(true);
        task.runNailgun();
    }

    @org.junit.Test
    public void testUrl() throws MalformedURLException {
        URL url = new URL("file:/asdfa");
        log.error(url);
    }


    public void testExtractLine() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(Paths.get(cfg.cwd, "build.log").toString()));
        String line;
        int count = 1;
        while ((line = br.readLine()) != null) {
            log.info(line);
            if(count==5073) {
                log.info("hiiiiiiiiiiiiiiiii");
                Files.write(Paths.get("/Users/sgururaj/projects/kvikbild/javac_core_src_options.mac"), line.getBytes());
                return;
            }
            count++;
        }
        br.close();

    }

    public void testMavn() throws Exception {
        String [] lines = new String[]{"[INFO]",
        "[INFO] ------------------------------------------------------------------------",
        "[INFO] Building CES Tools 7.5.1-SNAPSHOT",
        "[INFO] ------------------------------------------------------------------------",
        "[INFO] ",
        "[INFO] --- maven-dependency-plugin:2.3:list (default-cli) @ ces-tools ---",
        "[INFO] ",
        "[INFO] The following files have been resolved:",
        "[INFO]    org.objenesis:objenesis:jar:2.1:test",
        "[INFO]    org.mozilla:rhino:jar:1.7R3:compile",
        "[INFO]    org.javassist:javassist:jar:3.18.1-GA:compile",
        "[INFO]    org.apache.activemq:activemq-spring:jar:5.9.1:runtime",
        "[INFO]    net.sourceforge.schemacrawler:schemacrawler:jar:10.09.01:test",};
        MavenThings.ModuleLineProcessor test = new MavenThings.ModuleLineProcessor("core", "CES Tools", "/my/maven/repo");
        for(String line:lines) {
            test.process(line);
        }
        log.info(test.typeToPathsMap);
        //injector.getInstance(MavenThings.Factory.class).create(new String[]{}).computePaths();
    }
    public void testT() throws IOException {
        assertTrue(Pattern.matches(".*src\\/test\\/java.*Test\\.java$", "/data00/trunk/core/app/core/src/test/java/com/coverity/ces/service/GarbageCollectionServiceTest.java"));
    }

    public void testTest() throws IOException {
        String cwd = injector.getInstance(Key.get(String.class, Names.named("cwd")));
        BufferedReader br = Files.newBufferedReader(Paths.get(cwd,"javac_options"), Charset.defaultCharset());
        String l = br.readLine();
        l = " -classpath "+l.split(" ")[4] + " -d " + Paths.get(cwd, "app", "core", "target", "classes").toString()+
                " -sourcepath "+
                Paths.get(cwd, "app", "core", "src", "main", "java").toString() +
                " -s "+
                Paths.get(cwd, "app", "core", "src", "main", "java").toString()+
                " -g -nowarn -target 1.7 -source 1.7 -encoding UTF-8 ";
        Files.write(Paths.get(cwd, "javac_good_options"), l.getBytes());

    }

}
