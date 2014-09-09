package sharath;

import org.apache.jasper.servlet.JspServlet;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.eclipse.jetty.annotations.ServletContainerInitializersStarter;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author sgururaj
 *         This is the main CIM server
 *         It uses a jetty server to serve CIM
 *         It is running under the same classpath as CIM thanks to sharath.CimClassLoader
 */
public class CimServer implements ICimServer {

    private final WebAppContext context;
    Server server;

    public CimServer(int port, String cwd) throws IOException {
        Thread.currentThread().setContextClassLoader(CimServer.class.getClassLoader());
        server = new Server(port);
        context = new WebAppContext();
        context.setExtraClasspath(cwd + "/app/core/target/classes/");
        context.setDescriptor(cwd + "/app/web/src/main/webapp/WEB-INF/web.xml");
        context.setResourceBase(cwd + "/app/web/src/main/webapp/");
        context.setWar(cwd + "/app/web/src/main/webapp");
        context.setContextPath("/");
        context.setParentLoaderPriority(true);
        context.setConfigurationClasses(new String[]{WebInfConfiguration.class.getName(), WebXmlConfiguration.class.getName()});
        server.setHandler(context);

        //jsp settings
        // stolen from https://github.com/jetty-project/embedded-jetty-jsp/blob/master/src/main/java/org/eclipse/jetty/demo/Main.java
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File scratchDir = new File(tempDir.toString(),"embedded-jetty-jsp");

        if (!scratchDir.exists())
        {
            if (!scratchDir.mkdirs())
            {
                throw new IOException("Unable to create scratch directory: " + scratchDir);
            }
        }
        // Set JSP to use Standard JavaC always
        System.out.println("hi");
        System.setProperty("org.apache.jasper.compiler.disablejsr199","false");
        context.setAttribute("javax.servlet.context.tempdir",scratchDir);
        context.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());
        JettyJasperInitializer sci = new JettyJasperInitializer();
        ServletContainerInitializersStarter sciStarter = new ServletContainerInitializersStarter(context);
        ContainerInitializer initializer = new ContainerInitializer(sci, null);
        List<ContainerInitializer> initializers = new ArrayList<ContainerInitializer>();
        initializers.add(initializer);

        context.setAttribute("org.eclipse.jetty.containerInitializers", initializers);
        context.addBean(sciStarter, true);
        // Set Classloader of Context to be sane (needed for JSTL)
        // JSP requires a non-System classloader, this simply wraps the
        // embedded System classloader in a way that makes it suitable
        // for JSP to use
        ClassLoader jspClassLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader());
        context.setClassLoader(jspClassLoader);

        // Add JSP Servlet (must be named "jsp")
        ServletHolder holderJsp = new ServletHolder("jsp",JspServlet.class);
        holderJsp.setInitOrder(0);
        holderJsp.setInitParameter("logVerbosityLevel","VERBOSE");
        holderJsp.setInitParameter("fork","false");
        holderJsp.setInitParameter("xpoweredBy","false");
        holderJsp.setInitParameter("compilerTargetVM","1.7");
        holderJsp.setInitParameter("compilerSourceVM","1.7");
        holderJsp.setInitParameter("keepgenerated","true");
        context.addServlet(holderJsp,"*.jsp");
    }

    @Override
    public void startJettyServer() throws Exception {
        if (!server.isRunning()) {
            System.out.println("starting jetty server");
            ClassLoader oldCls = Thread.currentThread().getContextClassLoader();
            try {
                server.start();
            } finally {
                Thread.currentThread().setContextClassLoader(oldCls);
            }
            System.out.println("Jetty is now running");
        }
    }


    @Override
    public void restartCim() throws Exception {
        ClassLoader oldCls = Thread.currentThread().getContextClassLoader();

        if (!server.isRunning()) {
            System.out.println("starting jetty server");

            try {
                Thread.currentThread().setContextClassLoader(CimServer.class.getClassLoader());

                server.start();
            } finally {
                Thread.currentThread().setContextClassLoader(oldCls);

            }
            System.out.println("Jetty is now running");
            return;
        }
        System.out.println("Jetty already running, restarting CIM");

        try {
            Thread.currentThread().setContextClassLoader(CimServer.class.getClassLoader());

            context.stop();
            context.start();
        } finally {
            Thread.currentThread().setContextClassLoader(oldCls);

        }
        System.out.println("Done restarting.");

    }

}
