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
import java.util.regex.Pattern;

/**
 * @author sgururaj
 */
public class Builder extends AbstractHandler {
    private Graph graph;
    private String cwd;
    private List<String> javacSrcOptions;
    private List<String> javacTestOptions;
    private External ext;
    private final JavaCompiler jc;
    private final StandardJavaFileManager fm;
    private DependencyVisitor visitor;
    private Utils utils;
    private static final Logger log = Logger.getLogger(Builder.class);

    @Inject
    Builder(Graph graph, @Named("cwd") String cwd,
            @Named("javacSrcOptions") List<String> javacSrcOptions, @Named("javacTestOptions")List<String> javacTestOptions,
            External ext, JavaCompiler jc, StandardJavaFileManager fm, DependencyVisitor visitor, Utils utils) {

        this.graph = graph;
        this.cwd = cwd;
        this.javacSrcOptions = javacSrcOptions;
        this.javacTestOptions = javacTestOptions;
        this.ext = ext;
        this.jc = jc;
        this.fm = fm;
        this.visitor = visitor;
        this.utils = utils;
    }

    public void build(boolean compileTests) throws IOException {
        final Path src = Paths.get(cwd, "app", "core", "src");
        final Path target = Paths.get(cwd, "app", "core", "target");

        final HashMap<String, FileTime> modifiedTimes = new HashMap<>(3000);
        // first compute modifiedTime for all java files
        ext.walkFileTree(ImmutableList.of(target.resolve("classes"), target.resolve("test-classes")),
            new SimpleFileVisitor<Path>() {
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.class");
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!matcher.matches(file)) return FileVisitResult.CONTINUE;
                    modifiedTimes.put(file.toString(), attrs.lastModifiedTime());

                    if(file.toString().indexOf('$')>-1) return FileVisitResult.CONTINUE;
                    if(!graph.nodes.containsKey(file) || graph.nodes.get(file).classModTime.compareTo(attrs.lastModifiedTime())<0) {
                        log.info("updating stale dependency: "+file.toString());
                        String ll = "class mod time:"+attrs.lastModifiedTime()+" before upd graph time:"+graph.nodes.get(file).classModTime;
                        graph.update(file, visitor.getDependencies(Files.newInputStream(file)), FileTime.fromMillis(System.currentTimeMillis()));
                        ll+=" AFter upd graph time: "+graph.nodes.get(file).classModTime;
                        log.info(ll);

                    }
                    return FileVisitResult.CONTINUE;
                }
            });

        //now compute the list of dirtyJavaFiles by comparing modified times for class files.
        final Set<File> dirtyJavaFiles = new HashSet<>();
        final Set<File> dependentFiles = new HashSet<>();
        ext.walkFileTree(ImmutableList.of(src.resolve("main/java"), src.resolve("test/java")), new SimpleFileVisitor<Path>() {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.java");
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (matcher.matches(file)) {
                    String classpath = utils.toClass(file);
                    FileTime classTime = modifiedTimes.get(classpath);
                    if (classTime == null||attrs.lastModifiedTime().compareTo(classTime) > 0) {
                        dirtyJavaFiles.add(new File(file.toString()));
                        if(graph.nodes.get(Paths.get(classpath)).in.size()>0) {
                            for(Node node : graph.nodes.get(Paths.get(classpath)).in) {
                                String javaStr = utils.toJava(node.path.toString());
                                if(!Files.isRegularFile(Paths.get(javaStr))) continue;
                                dependentFiles.add(new File(javaStr));
                            }
                        }

                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });


        log.info("Compiling the following files:");
        HashSet<File> allSrcFilesToCompile = new HashSet<File>();
        HashSet<File> allTestFilesToCompile = new HashSet<File>();
        HashSet<File> allFiles = new HashSet<File>(dirtyJavaFiles);
        allFiles.addAll(dependentFiles);
        for(File file: allFiles) {
            if(Pattern.matches(".*src\\/test\\/java.*Test\\.java$", file.getAbsolutePath())) {
                allTestFilesToCompile.add(file);
                if(compileTests) log.info(file.getAbsolutePath());
            } else {
                log.info(file.getAbsolutePath());
                allSrcFilesToCompile.add(file);
            }
        }



        //now start compiling
        Iterable<? extends JavaFileObject> sources = fm.getJavaFileObjectsFromFiles(allSrcFilesToCompile);
        log.info(javacSrcOptions);
        Boolean success = true;
        if(allSrcFilesToCompile.size()>0) {
            jc.getTask(null, fm, null, javacSrcOptions, null, sources).call();
        }
        if(compileTests && allTestFilesToCompile.size()>0) {
            Iterable<? extends JavaFileObject> tests = fm.getJavaFileObjectsFromFiles(allTestFilesToCompile);
            log.info(javacSrcOptions);
            success &= jc.getTask(null, fm, null, javacTestOptions, null, tests).call();

        }
        if(success) {
            log.info("Successfully compiled.");
        } else {
            log.error("Compilation failed!");
        }


        HashSet<File> allCompiled = new HashSet<>(allSrcFilesToCompile);
        if(compileTests) allCompiled.addAll(allTestFilesToCompile);
        for (File file : allCompiled) {
            String javapath = file.getAbsolutePath();
            String classpath = utils.toClass(Paths.get(javapath));
            Set<Path> deps = visitor.getDependencies(Files.newInputStream(Paths.get(classpath)));
            graph.update(Paths.get(classpath), deps, FileTime.fromMillis(System.currentTimeMillis()));
        }
        log.info("updated dependencies!");
    }

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        log.info("Request from client: "+s);
        if(s.equals("/compile")) {
            build("tests".equals(request.getParameter("include")));
            return;
        }
        log.error("Unknown request from client: "+s);

    }
}
