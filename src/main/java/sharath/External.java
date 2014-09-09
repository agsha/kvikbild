package sharath;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author sgururaj
 */
public class External {
    private static final Logger log = Logger.getLogger(External.class.getName());

    public void mkdir(Path path) throws IOException {
        try {
            Files.createDirectories(path);
        } catch(FileAlreadyExistsException e) {
            if(!Files.isDirectory(path)) {
                throw e;
            }
            log.warn("directory already exists");
        }
    }

    public void walkFileTree(List<? extends Path> starts,  FileVisitor<? super Path> visitor) throws IOException {
        for (Path start : starts) {
            Files.walkFileTree(start, visitor);
        }
    }

    public Path walkFileTree(Path start,  FileVisitor<? super Path> visitor) throws IOException {
        return Files.walkFileTree(start, visitor);
    }

    static class ProcessHelper {
        private String dir;
        public ProcessHelper(String dir) {
            this.dir = dir;
        }

        public void execute(String... command) throws IOException, InterruptedException, ProcessError {
            int ret = new ProcessBuilder(command).directory(new File(dir)).inheritIO().start().waitFor();
            if(ret!=0) throw new ProcessError("terminated with non zero status: "+ret);
        }

        public void execute(List<? extends LineProcessor> processors, String... command) throws IOException {
            processors = new ArrayList<>(processors);
            Process p = new ProcessBuilder(command).directory(new File(dir)).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ( (line = br.readLine()) != null) {
                //System.out.println(line);
                for (Iterator<? extends LineProcessor> it = processors.iterator(); it.hasNext();) {
                    LineProcessor next = it.next();
                    if (next.process(line) == LineProcessor.Action.STOP) {
                        it.remove();
                    }
                }
                if (processors.isEmpty()) {
                    p.destroy();
                    br.close();
                    return;
                }
            }

        }
        public void execute(LineProcessor processor, String... command) throws IOException {
            execute(ImmutableList.of(processor), command);
        }

        static class Factory {
            private Utils.Config cfg;
            @Inject
            public Factory(Utils.Config cfg) {
                this.cfg = cfg;
            }
            public ProcessHelper forDir(String dir) {
                return new ProcessHelper(dir);
            }
            public ProcessHelper forCwd() {
                return new ProcessHelper(cfg.cwd);
            }
        }
        static interface LineProcessor {
            enum Action {
                STOP, PROCEED;
            }
            public Action process(String line);
        }

        static class ProcessError extends Exception {
            public ProcessError(String s) {
                super(s);
            }
        }

    }

}
