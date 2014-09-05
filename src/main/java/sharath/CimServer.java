package sharath;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;

/**
 * @author sgururaj
 * This is the main CIM server
 * It uses a jetty server to serve CIM
 * It is running under the same classpath as CIM thanks to sharath.CimClassLoader
 */
public class CimServer implements ICimServer{

    Server server;
    private final WebAppContext context;

    public CimServer(int port, String cwd) {
        server = new Server(port);
        context = new WebAppContext();
        context.setExtraClasspath(cwd+"/app/core/target/classes");
        context.setDescriptor(cwd + "/app/web/src/main/webapp/WEB-INF/web.xml");
        context.setResourceBase(cwd + "/app/web/src/main/webapp/");
        context.setWar(cwd + "/app/web/src/main/webapp");
        context.setContextPath("/");
        context.setParentLoaderPriority(true);
        context.setConfigurationClasses(new String[]{WebInfConfiguration.class.getName(), WebXmlConfiguration.class.getName()});
        server.setHandler(context);
    }

    @Override
    public void startJettyServer() throws Exception {
        if(!server.isRunning()) {
            System.out.println("starting jetty server");
            server.start();
            System.out.println("Jetty is now running");
        }
    }


    @Override
    public void restartCim() throws Exception {
        if(!server.isRunning()) {
            System.out.println("starting jetty server");
            server.start();
            System.out.println("Jetty is now running");
            return;
        }
        System.out.println("Jetty already running, restarting CIM");
        context.stop();
        context.start();
        System.out.println("Done restarting.");

    }

}
