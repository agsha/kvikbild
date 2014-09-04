package sharath;

import com.google.inject.*;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Arrays;
import java.util.List;

/**
 * @author sgururaj
 */
public class MyModule extends AbstractModule {
    private static final Logger log = Logger.getLogger(MyModule.class);

    @Override
    protected void configure() {
        bind(JavaCompiler.class).toInstance(ToolProvider.getSystemJavaCompiler());
        bindConstant().annotatedWith(Names.named("javaagentJrebel")).to("-javaagent:/Users/sgururaj/Library/Application Support/IntelliJIdea13/jr-ide-idea/lib/jrebel/jrebel.jar");
        bindConstant().annotatedWith(Names.named("javaagentJmockit")).to("-javaagent:/Users/sgururaj/.m2/repository/org/jmockit/jmockit/1.8/jmockit-1.8.jar");

        bind(Builder.class).toProvider(Builder.BuilderProvider.class);
    }
    @Provides
    @Singleton
    public Utils.Config getConfig() {
        return new Utils.Config("/data00/trunk/cim", 8000, 8001);
    }

    @Provides
    @Singleton
    @Named("core")
    public  Utils.CimModule getCoreCimModule(Utils.Config cfg) {
        String cwd = cfg.cwd;
        return new Utils.CimModule(
                Paths.get(cwd, "app", "core", "src", "main", "java"),
                Paths.get(cwd, "app", "core", "target", "classes"),
                Paths.get(cwd, "app", "core", "src", "test", "java"),
                Paths.get(cwd, "app", "core", "target", "test-classes"),
                Paths.get(cwd, "app", "core", "src", "main", "resources"),
                Paths.get(cwd, "app", "core", "target", "classes"),
                Paths.get(cwd, "app", "core", "src", "test", "resources"),
                Paths.get(cwd, "app", "core", "target", "test-classes"),
                getOptions("javac_core_src_options.ubuntu"),
                getOptions("javac_core_test_options.ubuntu"));
    }

    @Provides
    @Singleton
    @Named("core")
    public StandardJavaFileManager getCoreStandardJavaFileManager() {
        return ToolProvider
                .getSystemJavaCompiler()
                .getStandardFileManager(null, null, null);
    }

    @Provides
    @Singleton
    public Server getJettyServer(Utils.Config cfg, Builder builder) {
        Server server = new Server(cfg.port);
        server.setHandler(builder);
        return server;
    }

    private List<String> getOptions(String optionsFile) {
        try {
            //log.info(IOUtils.readLines(ClassLoader.getSystemResourceAsStream("javac_core_src_options.mac")).get(1));
            return Arrays.asList(IOUtils.readLines(getClass().getClassLoader().getResourceAsStream(optionsFile)).get(0).split("\\s+"));
        } catch (IOException e) {
            log.error("error occured", e);
            throw new RuntimeException(e);
        }
    }

    @Provides
    @Singleton
    Connection getConnection() {
        Connection c ;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:database.db");
            log.info("Opened database successfully");
            ps = c.prepareStatement("SELECT name FROM sqlite_master WHERE type='table';");
            rs = ps.executeQuery();
            if(rs.next()) {
                if(rs.getString(1).equals("core_graph"))
                    return c;
            }
            new CreateSchema().create(c);
            return c;
        } catch ( Exception e ) {
            log.error( "unable to create jdbc connection", e );
            throw new RuntimeException(e);
        } finally {
            try {
                ps.close();
                rs.close();
            } catch (SQLException e) {
                log.error("error while closing resultset and preparedstatement", e);
                throw new RuntimeException(e);
            }

        }
    }
}
