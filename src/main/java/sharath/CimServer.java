package sharath;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * @author sgururaj
 * This is the main CIM server, which serves CIM. It uses a jetty server to serve CIM
 */
public class CimServer {
    Utils.Config cfg;
    Server server;
    WebAppContext webAppContext;

    protected CimServer(Utils.Config cfg, Server server, WebAppContext webAppContext) {
        this.cfg = cfg;
        this.server = server;
        this.webAppContext = webAppContext;
    }

    public void startJettyServer() throws Exception {
        server.start();
        server.join();
    }


    public void restartCim() throws Exception {
        webAppContext.stop();
        webAppContext.start();
    }


}
