package sharath;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;

import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Hello world!
 *
 */
public class App
{
    private final String[] args;
    private final External ext;
    private static final Logger log = Logger.getLogger(App.class);
    private final String cwd;
    private Graph graph;
    private Connection c;
    private GraphFlusher flusher;
    private Server server;

    @Inject
    public App(@Assisted String[] args, External ext, @Named("cwd") String cwd, Graph graph, Connection c, GraphFlusher flusher, Server server) {

        this.args = args;
        this.ext = ext;
        this.cwd = cwd;
        this.graph = graph;
        this.c = c;
        this.flusher = flusher;
        this.server = server;
    }

    public static void main( String[] args ) throws Exception {
        Injector injector = Guice.createInjector(new MyModule());
        App app = injector.getInstance(AppFactory.class).create(args);
        app.start();
    }

    public void start() throws Exception {
        PreparedStatement ps = c.prepareStatement("select * from dependency");
        ResultSet rs = ps.executeQuery();
        while(rs.next()) {
            Path classpath = Paths.get(rs.getString("classpath"));
            String[] depsArray = rs.getString("deps").split(",");
            List<Path> deps = new ArrayList<>(depsArray.length);
            for (String dep : depsArray) {
                deps.add(Paths.get(dep));
            }
            graph.update(classpath, deps, FileTime.fromMillis(rs.getLong("class_mod_time")));
        }
        graph.setDirty(false);
        rs.close();
        ps.close();

        flusher.start();
        server.start();
        server.join();
    }

    static interface AppFactory {
        public App create(String[] args);
    }
}
