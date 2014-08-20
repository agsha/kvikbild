package sharath;

import com.google.common.base.Strings;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

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

    @Inject
    public App(@Assisted String[] args,  External ext, @Named("cwd") String cwd, Graph graph) {

        this.args = args;
        this.ext = ext;
        this.cwd = cwd;
        this.graph = graph;
    }

    public static void main( String[] args )
    {
        Injector injector = Guice.createInjector(new MyModule());
        App app = injector.getInstance(AppFactory.class).create(args);
        app.start();
    }

    public void start() {
        try {
            ext.mkdir( Paths.get(cwd, ".cimdeps"));
            Path depFile = Paths.get(cwd, ".cimdeps", "deps");
            if(!Files.exists(depFile)) {
                Files.createFile(depFile);
            }
            BufferedReader br = Files.newBufferedReader(depFile, Charset.defaultCharset());
            while(true) {
                String line = br.readLine();
                if(line==null) break;
                Path from = Paths.get(line);
                ArrayList<Path> list = new ArrayList<>();
                while(true) {
                    String child = br.readLine();
                    if(Strings.isNullOrEmpty(child)) break;
                    list.add(Paths.get(child));
                }
                graph.add(from, list);
            }
        } catch (IOException e) {
            log.error("error", e);
        }
    }

    static interface AppFactory {
        public App create(String[] args);
    }
}
