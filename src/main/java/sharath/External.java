package sharath;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author sgururaj
 */
public class External {
    private static final Logger log = Logger.getLogger(External.class.getName());

    public void mkdir(Path path) throws IOException {
        try {
            Files.createDirectory(path);
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

}
