package sharath;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

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
    private ICimServer cimServer;

    private App(String[] args, Graph coreGraph, GraphFlusher flusher, Server server, ICimServer cimServer) {

        this.args = args;
        this.coreGraph = coreGraph;
        this.flusher = flusher;
        this.server = server;
        this.cimServer = cimServer;
    }

    public static void main( String[] args ) throws Exception {
        System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StrErrLog");
        System.setProperty("org.eclipse.jetty.LEVEL", "ALL");
        System.setProperty("org.eclipse.jetty.websocket.LEVEL", "ALL");
        System.setProperty("org.jboss.logging.provider", "slf4j");


        Injector injector = Guice.createInjector(new MyModule());
        App app = injector.getInstance(Factory.class).create(args);
        app.start();
    }


    public void start() throws Exception {
        flusher.addGraph(coreGraph, "core_graph", "core");
        flusher.loadAll();

        flusher.start();
        server.start();
        //server.join();
        cimServer.restartCim();
    }

    static class Factory {
        private final Graph.Factory graphFactory;
        private final GraphFlusher flusher;
        private final Server server;
        private ICimServer cimServer;

        @Inject
        public Factory(Graph.Factory graphFactory, GraphFlusher flusher, Server server, ICimServer cimServer) {

            this.graphFactory = graphFactory;
            this.flusher = flusher;
            this.server = server;
            this.cimServer = cimServer;
        }

        public App create(String[]args) {
            return new App(args, graphFactory.getForName("core"), flusher, server, cimServer);
        }
    }
}
