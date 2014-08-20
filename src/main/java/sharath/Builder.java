package sharath;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author sgururaj
 */
public class Builder {
    private Graph graph;
    private String cwd;
    private External ext;
    private static final Logger log = Logger.getLogger(Builder.class);

    @Inject
    Builder(Graph graph,  @Named("cwd") String cwd, External ext) {

        this.graph = graph;
        this.cwd = cwd;
        this.ext = ext;
    }

    public void build() throws IOException {
        final Path src = Paths.get(cwd, "app", "core", "src");
        final HashMap<String, FileTime> modifiedTimes = new HashMap<>(3000);
        // first compute modifiedTime for all java files
        ext.walkFileTree(ImmutableList.of(src.resolve("main/java"), src.resolve("test/java")),
                new SimpleFileVisitor<Path>() {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.java");


            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (matcher.matches(file)) {
                    String filename = file.subpath(src.getNameCount() + 2, file.getNameCount()).toString();
                    modifiedTimes.put(filename.substring(0, filename.length() - 5), attrs.lastModifiedTime());
                }
                return FileVisitResult.CONTINUE;
            }
        });

        //now compute the list of dirtyJavaFiles by comparing modified times for class files.
        final ArrayList<String> dirtyJavaFiles = new ArrayList<>();
        final Path target = Paths.get(cwd, "app", "core", "target");
        ext.walkFileTree(ImmutableList.of(target.resolve("classes"), target.resolve("test-classes")),
                new SimpleFileVisitor<Path>() {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.class");


            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (matcher.matches(file)) {
                    String filename = file.subpath(target.getNameCount()+1, file.getNameCount()).toString();
                    String canonical = filename.substring(0, filename.length()-6);
                    //log.error(canonical);
                    FileTime javaTime = modifiedTimes.get(canonical);
                    if(javaTime==null) return FileVisitResult.CONTINUE;
                    if(attrs.lastModifiedTime().compareTo(javaTime)<0) {
                        dirtyJavaFiles.add(canonical);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        //now start compiling all the dirty java files.

    }
}
