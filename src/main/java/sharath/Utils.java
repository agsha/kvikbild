package sharath;

import com.google.inject.Singleton;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by sgururaj on 8/22/14.
 */
@Singleton
public class Utils {
    private final String src;
    private final String srcTest;
    private final String dest;
    private final String destTest;

    //cwd/app/core/target/classes(test-classes)
    //cwd/app/core/src/main(test)/java

    public Utils(String src, String srcTest, String dest, String destTest) {

        this.src = src;
        this.srcTest = srcTest;
        this.dest = dest;
        this.destTest = destTest;
    }

    public String toClass(Path file) {
        return file.toString()
                .replaceAll(src, dest)
                .replaceAll(srcTest, destTest)
                .replaceAll("java$", "class");
    }

    public String toJava(Path file) {
        return file.toString()
                .replaceAll(dest, src)
                .replaceAll(destTest, srcTest)
                .replaceAll("class$", "java");

    }
}
