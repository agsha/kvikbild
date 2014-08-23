package sharath;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.google.inject.*;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
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
        bindConstant().annotatedWith(Names.named("cwd")).to("/data00/trunk/cim");
        bind(new TypeLiteral<List<String>>(){}).annotatedWith(Names.named("javacSrcOptions")).toProvider(JavacSrcOptionsProvider.class).in(Singleton.class);
        bind(new TypeLiteral<List<String>>(){}).annotatedWith(Names.named("javacTestOptions")).toProvider(JavacTestOptionsProvider.class).in(Singleton.class);
        bindConstant().annotatedWith(Names.named("port")).to(Integer.valueOf(8000));

    }

    @Provides
    @Singleton
    JavaCompiler provideJavaCompiler() {
        return ToolProvider.getSystemJavaCompiler();
    }

    @Provides
    @Singleton
    StandardJavaFileManager provideStandardJavaFileManager(JavaCompiler jc) {
        return jc.getStandardFileManager(null, null, null);
    }

    @Provides
    @Singleton
    public Server getJettyServer(@Named("port") int port, Builder builder) {
        Server server = new Server(port);
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
                if(rs.getString(1).equals("dependency"))
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

class JavacSrcOptionsProvider implements Provider<List<String>> {

    private String cwd;
    private static final Logger log = Logger.getLogger(JavacSrcOptionsProvider.class);


    @Inject
    JavacSrcOptionsProvider(@Named("cwd") String cwd) {
        this.cwd = cwd;
    }
    public List<String> get() {
        try {
            return Arrays.asList(Files.readLines(new File(cwd, "javac_options"), Charset.defaultCharset()).get(0).split(" "));
        } catch (IOException e) {
            log.error("error occured", e);
            throw new RuntimeException(e);
        }
    }
}

class JavacTestOptionsProvider implements Provider<List<String>> {

    private String cwd;
    private static final Logger log = Logger.getLogger(JavacTestOptionsProvider.class);


    @Inject
    JavacTestOptionsProvider(@Named("cwd") String cwd) {
        this.cwd = cwd;
    }
    public List<String> get() {
        return ImmutableList.of("Coming soon!");
    }
}


