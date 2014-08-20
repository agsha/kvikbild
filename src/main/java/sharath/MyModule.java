package sharath;

import com.google.common.io.Files;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.apache.log4j.Logger;
import sharath.External;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;

/**
 * @author sgururaj
 */
public class MyModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new FactoryModuleBuilder().build(AppFactory.class));
        bindConstant().annotatedWith(Names.named("cwd")).to("/data00/trunk/cim");
    }
}

class JavacOptionsProvider implements Provider<String> {

    private String cwd;
    private static final Logger log = Logger.getLogger(JavacOptionsProvider.class);


    JavacOptionsProvider(@Named("cwd")  String cwd) {
        this.cwd = cwd;
    }
    public String get() {
        try {
            return Files.readLines(new File(cwd, "javac_options"), Charset.defaultCharset()).get(0);
        } catch (IOException e) {
            log.error("error occured", e);
            throw new RuntimeException(e);
        }
    }
}
interface AppFactory {
    public App create(String[] args);
}
