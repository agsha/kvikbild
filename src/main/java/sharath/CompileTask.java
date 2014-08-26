package sharath;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import java.nio.file.Path;
import java.nio.file.PathMatcher;

/**
 * Created by sgururaj on 8/25/14.
 */
public class CompileTask {
    private final Path srcTest;
    private final Path destTest;
    private final Graph graph;
    private final Convertor converter;
    private External ext;
    private final Path src;
    private final Path dest;

    private CompileTask(Path src, Path dest, Path srcTest, Path destTest, Graph graph, Convertor converter, External ext) {
        this.src = src;
        this.dest = dest;
        this.srcTest = srcTest;
        this.destTest = destTest;
        this.graph = graph;
        this.converter = converter;
        this.ext = ext;
    }


    public boolean doCompile() {
        return true;
    }


    static interface Convertor {
        String toClass(String src);

        String toJava(String claz);
    }

    static class CompileTaskFactory {
        private External ext;

        @Inject
        CompileTaskFactory(External ext) {
            this.ext = ext;
        }

        public CompileTask create(Path src, Path dest, Path srcTest, Path destTest, Graph graph, Convertor converter) {
            return new CompileTask(src, dest, srcTest, destTest, graph, converter, ext);
        }

    }

}

