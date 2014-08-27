package sharath;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author sgururaj
 */
public class Builder extends AbstractHandler {
    private static final Logger log = Logger.getLogger(Builder.class);
    private CoreCompileTask coreCompileTask;

    @Inject
    Builder(CoreCompileTask coreCompileTask) {

        this.coreCompileTask = coreCompileTask;
    }

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        log.info("Request from client: "+s);
        if(s.equals("/compile")) {
            coreCompileTask.doCompile(true);
        } else if(s.equals("/runTest")) {
            coreCompileTask.doCompile(true);
            try {
                coreCompileTask.runTest(request.getParameter("class"));
            } catch (InterruptedException e) {
                log.error("error while running test", e);
            }

        } else {
            log.error("Unknown request from client: "+s);
        }

    }
}
