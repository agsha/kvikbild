package sharath;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.sql.*;

/**
 * @author sgururaj
 */
public class MyModule extends AbstractModule {
    private static final Logger log = LogManager.getLogger(MyModule.class);

    @Override
    protected void configure() {
        bind(JavaCompiler.class).toInstance(ToolProvider.getSystemJavaCompiler());
        bindConstant().annotatedWith(Names.named("javaagentJrebel")).to("-javaagent:/Users/sgururaj/Library/Application Support/IntelliJIdea13/jr-ide-idea/lib/jrebel/jrebel.jar");
        bindConstant().annotatedWith(Names.named("javaagentJmockit")).to("-javaagent:/Users/sgururaj/.m2/repository/org/jmockit/jmockit/1.8/jmockit-1.8.jar");

        bind(ICimServer.class).toProvider(ICimServerProvider.class).in(Singleton.class);
        bind(CimClassLoader.class).toProvider(CimClassLoader.Provider.class).in(Singleton.class);
        bind(Builder.class).toProvider(Builder.BuilderProvider.class);
    }
    @Provides
    @Singleton
    public Utils.Config getConfig(Utils.Config.Factory factory) throws IOException {
        String jettyClassPath = IOUtils.readLines(getClass().getClassLoader().getResourceAsStream("jetty_classpath.mac")).get(0);
        return factory.create("/Users/sgururaj/projects/cim", 8000, 8001, jettyClassPath);
    }


    @Provides
    @Singleton
    public Server getJettyServer(Utils.Config cfg, Builder builder) {

        Server server = new Server(cfg.port);
        server.setHandler(builder);
        return server;
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
