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
import java.nio.file.Path;
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
        install(new FactoryModuleBuilder().build(App.AppFactory.class));
        String cwd = "/data00/trunk/cim";
        bindConstant().annotatedWith(Names.named("cwd")).to(cwd);
        bind(JavaCompiler.class).toInstance(ToolProvider.getSystemJavaCompiler());
        bindConstant().annotatedWith(Names.named("port")).to(Integer.valueOf(8000));

        //core bindings
        bind(Path.class).annotatedWith(Names.named("coreSrc")).toInstance(Paths.get(cwd, "app", "core", "src", "main", "java"));
        bind(Path.class).annotatedWith(Names.named("coreDest")).toInstance(Paths.get(cwd, "app", "core", "target", "classes"));
        bind(Path.class).annotatedWith(Names.named("coreSrcTest")).toInstance(Paths.get(cwd, "app", "core", "src", "test", "java"));
        bind(Path.class).annotatedWith(Names.named("coreDestTest")).toInstance(Paths.get(cwd, "app", "core", "target", "test-classes"));
        bind(Graph.class).annotatedWith(Names.named("core")).toInstance(new Graph());
        bind(new TypeLiteral<List<String>>() {})
                .annotatedWith(Names.named("coreJavacSrcOptions"))
                .toInstance(getOptions("javac_core_src_options.ubuntu"));
        bind(new TypeLiteral<List<String>>() {})
                .annotatedWith(Names.named("coreJavacTestOptions"))
                .toInstance(getOptions("javac_core_test_options.ubuntu"));
        bind(StandardJavaFileManager.class)
                .annotatedWith(Names.named("core"))
                .toInstance(ToolProvider
                        .getSystemJavaCompiler()
                        .getStandardFileManager(null, null, null));


        bindConstant().annotatedWith(Names.named("port")).to(Integer.valueOf(8000));

    }

    @Provides
    @Singleton
    @Named("core")
    public DependencyVisitor getCoreDependencyVisitor(@Named("coreDest") Path coreDest, @Named("coreDestTest")Path coreDestTest, DependencyVisitor.DependencyVisitorFactory factory) {
        return factory.create(coreDest, coreDestTest);
    }

    @Provides
    @Singleton
    @Named("core")
    public Utils getUtils(@Named("coreSrc") Path coreSrc, @Named("coreSrcTest")Path coreTest, @Named("coreDest") Path coreDest, @Named("coreDestTest")Path coreDestTest) {
        return new Utils(coreSrc.toString(),
                coreTest.toString(), coreDest.toString(), coreDestTest.toString());
    }


    @Provides
    @Singleton
    public Server getJettyServer(@Named("port") int port, Builder builder) {
        Server server = new Server(port);
        server.setHandler(builder);
        return server;
    }

    private List<String> getOptions(String optionsFile) {
        try {
            //log.info(IOUtils.readLines(ClassLoader.getSystemResourceAsStream("javac_core_src_options.mac")).get(1));
            return Arrays.asList(IOUtils.readLines(ClassLoader.getSystemResourceAsStream(optionsFile)).get(0).split("\\s+"));
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
