package sharath;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.regex.Pattern;


/**
 * Unit test for simple App.
 */
public class AppTest
    extends TestCase
{
    private static final Logger log = Logger.getLogger(AppTest.class.getName());
    private static final FileSystem fs = FileSystems.getDefault();
    private final String cwd;
    private final Utils utils;
    Injector injector = Guice.createInjector(new MyModule());
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
        cwd = injector.getInstance(Key.get(String.class, Names.named("cwd")));
        utils = injector.getInstance(Key.get(Utils.class, Names.named("core")));
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
    public void testApp() throws Exception {
        App app = injector.getInstance(App.AppFactory.class).create(new String[0]);
        app.start();
        Builder builder = injector.getInstance(Builder.class);
    }

    public void testPb() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("echo", "yoyo");
        log.info(pb.command());
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        Process process = pb.start();
        process.waitFor();


    }

    public void testtt() {
        log.error(
        utils.toClass(Paths.get("/Users/sgururaj/projects/cim/app/core/src/main/java/com/coverity/ces/util/ConversionUtils.java")));
        log.error(utils.toJava(Paths.get("/Users/sgururaj/projects/cim/app/core/target/classes/com/coverity/ces/util/ConversionUtils.class")));
    }

    public void testTTTT() {
        log.error("foofoofoofoof");
    }


    public void testExtractLine() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(Paths.get(cwd, "build.log").toString()));
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

    public void testT() throws IOException {
        assertTrue(Pattern.matches(".*src\\/test\\/java.*Test\\.java$", "/data00/trunk/cim/app/core/src/test/java/com/coverity/ces/service/GarbageCollectionServiceTest.java"));
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
