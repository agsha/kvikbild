package sharath;

import com.google.inject.Inject;
import com.google.inject.Provider;
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
    private CompileTask compileTask;
    private ResourceTask resourceTask;

    Builder(CompileTask compileTask, ResourceTask resourceTask) {

        this.compileTask = compileTask;
        this.resourceTask = resourceTask;
    }

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        log.info("Request from client: "+s);
        if(s.equals("/compile")) {
            compileTask.doCompile(true);
            resourceTask.updateResources();
        } else if(s.equals("/runTest")) {
            compileTask.doCompile(true);
            resourceTask.updateResources();

            try {
                compileTask.runTest(request.getParameter("class"));

            } catch (InterruptedException e) {
                log.error("error while running test", e);
            }

        } else if(s.equals("/nailgun")) {
            compileTask.doCompile(true);
            resourceTask.updateResources();
            compileTask.runNailgun();
        } else {
            log.error("Unknown request from client: "+s);
        }

    }

    static class BuilderProvider implements Provider<Builder> {

        CompileTask.Factory compileTaskFactory;
        private ResourceTask.Factory resourceFactory;

        @Inject
        BuilderProvider(CompileTask.Factory compileTaskFactory, ResourceTask.Factory resourceFactory) {
            this.compileTaskFactory = compileTaskFactory;
            this.resourceFactory = resourceFactory;
        }

        @Override
        public Builder get() {
            return new Builder(compileTaskFactory.createCoreCompileTask(), resourceFactory.createCoreResourceTask());
        }
    }
}
