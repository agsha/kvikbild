package sharath;

import com.google.inject.Singleton;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by sgururaj on 8/22/14.
 */
@Singleton
public class Utils {

    //cwd/app/core/target/classes(test-classes)
    //cwd/app/core/src/main(test)/java

    public String toJava(String str) {
        if(str.endsWith(".java")) return str;
        return str
                .replaceAll("target/classes", "src/main/java")
                .replaceAll("target/test-classes", "src/test/java")
                .replaceAll("class$", "java");
    }
    public String toJava(Path path) {
        return toJava(path.toString());
    }
    public Path toJavap(Path path) {
        return Paths.get(toJava(path.toString()));
    }

    public String toClass(String str) {
        if(str.endsWith(".class")) return str;
        return str
                .replaceAll("src/main/java", "target/classes")
                .replaceAll("src/test/java", "target/test-classes")
                .replaceAll("java$", "class");
    }

    public String toClass(Path path) {
        return toClass(path.toString());
    }
    public Path toClassp(Path path) {
        return Paths.get(toClass(path.toString()));
    }

}
