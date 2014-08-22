package sharath;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;

/**
 * @author sgururaj
 */
public class Builder extends AbstractHandler {
    private Graph graph;
    private String cwd;
    private List<String> javacOptions;
    private External ext;
    private final JavaCompiler jc;
    private final StandardJavaFileManager fm;
    private DependencyVisitor visitor;
    private static final Logger log = Logger.getLogger(Builder.class);

    @Inject
    Builder(Graph graph, @Named("cwd") String cwd,
            @Named("javacOptions") List<String> javacOptions,
            External ext, JavaCompiler jc, StandardJavaFileManager fm, DependencyVisitor visitor) {

        this.graph = graph;
        this.cwd = cwd;
        this.javacOptions = javacOptions;
        this.ext = ext;
        this.jc = jc;
        this.fm = fm;
        this.visitor = visitor;
    }

    public void build() throws IOException {
        final Path src = Paths.get(cwd, "app", "core", "src");
        final Path target = Paths.get(cwd, "app", "core", "target");

        final HashMap<String, FileTime> modifiedTimes = new HashMap<>(3000);
        // first compute modifiedTime for all java files
        ext.walkFileTree(ImmutableList.of(target.resolve("classes"), target.resolve("test-classes")),
            new SimpleFileVisitor<Path>() {
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.class");
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (matcher.matches(file)) {
                        String filename = file.subpath(target.getNameCount() + 1, file.getNameCount()).toString();
                        String canonical = filename.substring(0, filename.length() - 6);
                        modifiedTimes.put(canonical, attrs.lastModifiedTime());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

        //now compute the list of dirtyJavaFiles by comparing modified times for class files.
        final ArrayList<File> dirtyJavaFiles = new ArrayList<>();
        ext.walkFileTree(ImmutableList.of(src.resolve("main/java"), src.resolve("test/java")), new SimpleFileVisitor<Path>() {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.java");
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (matcher.matches(file)) {
                    String filename = file.subpath(target.getNameCount() + 2, file.getNameCount()).toString();
                    String canonical = filename.substring(0, filename.length() - 5);
                    //log.error(canonical);
                    FileTime classTime = modifiedTimes.get(canonical);
                    if (classTime == null||attrs.lastModifiedTime().compareTo(classTime) > 0) {
                        dirtyJavaFiles.add(new File(file.toString()));
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        log.info("Compiling the following files");
        for (File file : dirtyJavaFiles) {
            log.info(file.getAbsolutePath());
        }

        //now start compiling all the dirty java files.
        Iterable<? extends JavaFileObject> sources = fm.getJavaFileObjectsFromFiles(dirtyJavaFiles);
        Boolean success = jc.getTask(null, fm, null, javacOptions, null, sources).call();
        if(success) {
            log.info("Successfully compiled.");
        } else {
            log.error("Compilation failed!");
        }

        for (File file : dirtyJavaFiles) {
            String javapath = file.getAbsolutePath();
            String classpath = javapath.substring(0, javapath.length()-4)+"class";
            Set<Path> deps = visitor.getDependencies(Files.newInputStream(Paths.get(classpath)));
            graph.update(Paths.get(classpath), deps, FileTime.fromMillis(System.currentTimeMillis()));
        }
        log.info("updated dependencies!");
    }

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        log.info("Request from client: "+s);
        if(s.equals("/compile")) {
            build();
            return;
        }
        log.error("Unknown request from client: "+s);

    }
}
