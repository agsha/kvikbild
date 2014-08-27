package sharath;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import org.eclipse.jetty.server.Server;

/**
 * Hello world!
 *
 */
public class App
{
    private final String[] args;
    private Graph coreGraph;
    private GraphFlusher flusher;
    private Server server;

    @Inject
    public App(@Assisted String[] args, @Named("core")Graph coreGraph , GraphFlusher flusher, Server server) {

        this.args = args;
        this.coreGraph = coreGraph;
        this.flusher = flusher;
        this.server = server;
    }

    public static void main( String[] args ) throws Exception {
        Injector injector = Guice.createInjector(new MyModule());
        App app = injector.getInstance(AppFactory.class).create(args);
        app.start();
    }

    public void start() throws Exception {
        flusher.addGraph(coreGraph, "core_graph", "core");
        flusher.loadAll();

        flusher.start();
        server.start();
        server.join();
    }

    static interface AppFactory {
        public App create(String[] args);
    }
}
