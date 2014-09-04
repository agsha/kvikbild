package sharath;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Server;

import java.lang.reflect.Method;

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

    private App(String[] args, Graph coreGraph, GraphFlusher flusher, Server server) {

        this.args = args;
        this.coreGraph = coreGraph;
        this.flusher = flusher;
        this.server = server;
    }

    public static void main( String[] args ) throws Exception {
        Injector injector = Guice.createInjector(new MyModule());
        App app = injector.getInstance(Factory.class).create(args);
        app.start();
    }


    public void start() throws Exception {
        flusher.addGraph(coreGraph, "core_graph", "core");
        flusher.loadAll();

        flusher.start();
        server.start();
        server.join();
    }

    static class Factory {
        private final Graph.Factory graphFactory;
        private final GraphFlusher flusher;
        private final Server server;

        @Inject
        public Factory(Graph.Factory graphFactory, GraphFlusher flusher, Server server) {

            this.graphFactory = graphFactory;
            this.flusher = flusher;
            this.server = server;
        }

        public App create(String[]args) {
            return new App(args, graphFactory.getForName("core"), flusher, server);
        }
    }
}
