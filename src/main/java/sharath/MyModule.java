package sharath;

import com.google.common.io.Files;
import com.google.inject.*;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.apache.log4j.Logger;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * @author sgururaj
 */
public class MyModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new FactoryModuleBuilder().build(App.AppFactory.class));
        bindConstant().annotatedWith(Names.named("cwd")).to("/Users/sgururaj/projects/cim");
        bind(new TypeLiteral<List<String>>(){}).annotatedWith(Names.named("javacOptions")).toProvider(JavacOptionsProvider.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    JavaCompiler provideJavaCompiler() {
        return ToolProvider.getSystemJavaCompiler();
    }

    @Provides
    @Singleton
    StandardJavaFileManager provideStandardJavaFileManager(JavaCompiler jc) {
        return jc.getStandardFileManager(null, null, null);
    }
}

class JavacOptionsProvider implements Provider<List<String>> {

    private String cwd;
    private static final Logger log = Logger.getLogger(JavacOptionsProvider.class);


    JavacOptionsProvider(@Named("cwd")  String cwd) {
        this.cwd = cwd;
    }
    public List<String> get() {
        try {
            return Arrays.asList(Files.readLines(new File(cwd, "javac_options"), Charset.defaultCharset()).get(0).split(" "));
        } catch (IOException e) {
            log.error("error occured", e);
            throw new RuntimeException(e);
        }
    }
}
