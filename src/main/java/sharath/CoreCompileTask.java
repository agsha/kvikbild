package sharath;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import java.nio.file.Path;
import java.util.List;

/**
 * @author sgururaj
 */
public class CoreCompileTask extends CompileTask {
    @Inject
    public CoreCompileTask(@Named("coreSrc") Path src, @Named("coreDest") Path dest, @Named("coreSrcTest") Path srcTest, @Named("coreDestTest")Path destTest, @Named("core") Graph graph, External ext, @Named("core") DependencyVisitor visitor, JavaCompiler jc, @Named("core") StandardJavaFileManager fm, @Named("core")Utils utils, @Named("coreJavacSrcOptions") List<String> javacSrcOptions, @Named("coreJavacTestOptions") List<String> javacTestOptions) {
        super(src, dest, srcTest, destTest, graph, ext, visitor, jc, fm, utils, javacSrcOptions, javacTestOptions);
    }
}
