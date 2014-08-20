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
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.AbstractSet;
import java.util.Iterator;


/**
 * Unit test for simple App.
 */
public class AppTest
    extends TestCase
{
    private static final Logger log = Logger.getLogger(AppTest.class.getName());
    private static final FileSystem fs = FileSystems.getDefault();
    Injector injector = Guice.createInjector(new MyModule());
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
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
    public void testApp() throws IOException {
        App app = injector.getInstance(AppFactory.class).create(new String[0]);
        app.start();
        Builder builder = injector.getInstance(Builder.class);
        builder.build();
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
