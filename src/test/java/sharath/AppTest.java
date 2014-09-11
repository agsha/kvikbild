package sharath;

import com.google.inject.Guice;
import com.google.inject.Injector;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;
import org.junit.Ignore;

import java.io.IOException;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * Unit test for simple App.
 */
public class AppTest
    extends TestCase
{
    private static final Logger log = Logger.getLogger(AppTest.class.getName());
    private final Utils.Config cfg;
    Injector injector = Guice.createInjector(new MyModule());
    /**
     * Create the test case
     *
     */
    public AppTest(  ) throws SQLException {
        cfg = injector.getInstance(Utils.Config.class);
    }


    @org.junit.Test
    @Ignore
    public void testCreateSchema() throws InterruptedException, SQLException, External.ProcessHelper.ProcessError, IOException {
        new CreateSchema().create(injector.getInstance(Connection.class));

    }

    @org.junit.Test
    @Ignore
    public void testNow() throws InterruptedException, SQLException, External.ProcessHelper.ProcessError, IOException {
        injector.getInstance(MavenThings.Factory.class).create(new String[]{}).computePaths();
    }

    public void testT() throws SQLException {
        Connection conn = injector.getInstance(Connection.class);
        ResultSet rs = conn.prepareStatement("select classpath from module where name='web' and (phase='runtime' or phase='compile')").executeQuery();
        Set<String> coreCompile = new HashSet<>();
        while(rs.next()) {
            coreCompile.addAll(Arrays.asList(rs.getString(1).split(":")));
        }
        log.info(coreCompile);

        rs = conn.prepareStatement("select classpath from module where name='core' and phase='test'").executeQuery();
        Set<String> webCompile = new HashSet<>();
        while(rs.next()) {
            webCompile.addAll(Arrays.asList(rs.getString(1).split(":")));
        }
        Set<String> ss = new HashSet<>(coreCompile);
        ss.removeAll(webCompile);
        //log.info("only in core: "+ss);

        ss = new HashSet<>(webCompile);
        ss.removeAll(coreCompile);
        //log.info("only in web: "+ss);

        ss = new HashSet<>(webCompile);
        ss.retainAll(coreCompile);
        log.info("common to both: "+ss);
    }


    public void testList() throws SQLException {
        CimModule.AllModules all = injector.getInstance(CimModule.AllModules.class);
        for (String path : all.forName("web").srcRuntimeOptions.split(":")) {
            Path p = Paths.get(path);
            log.info(p.getFileName().toString());
        }
        for(String path:cfg.jettyClasspath.split(":")) {
            Path p = Paths.get(path);
            log.info(p.getFileName().toString());

        }
    }

    @org.junit.Test
    public void testCim() throws SQLException {
        CimModule.Factory fact = injector.getInstance(CimModule.Factory.class);
        CimModule web = fact.create("web");
        int count = 0;
        for(String str : web.javacTestOptions.get(3).split(":")) {
            count++;
            if(!Files.exists(Paths.get(str))) {
                log.info("oh noooooooooooo"+str);
            }
        }
        log.info(count);
    }

    public void testrt() {
        log.info(System.getProperty("user.name"));
    }

}
