package sharath;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.apache.log4j.Logger;

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
 * Created by sgururaj on 8/25/14.
 */
public class CompileTask {
    private final Path srcTest;
    private final Path destTest;
    private final Graph graph;
    private External ext;
    private DependencyVisitor visitor;
    private final JavaCompiler jc;
    private final StandardJavaFileManager fm;
    private Utils utils;
    private final List<String> javacSrcOptions;
    private final List<String> javacTestOptions;
    private final Path src;
    private final Path dest;
    private static final Logger log = Logger.getLogger(CompileTask.class);

    protected CompileTask(Path src, Path dest, Path srcTest, Path destTest, Graph graph, External ext, DependencyVisitor visitor, JavaCompiler jc, StandardJavaFileManager fm, Utils utils, List<String> javacSrcOptions, List<String> javacTestOptions) {
        this.src = src;
        this.dest = dest;
        this.srcTest = srcTest;
        this.destTest = destTest;
        this.graph = graph;
        this.ext = ext;
        this.visitor = visitor;
        this.jc = jc;
        this.fm = fm;
        this.utils = utils;
        this.javacSrcOptions = javacSrcOptions;
        this.javacTestOptions = javacTestOptions;
    }


    public boolean doCompile(boolean compileTests) throws IOException {
        ext.mkdir(dest);
        ext.mkdir(destTest);
        ext.mkdir(dest.getParent().resolve(Paths.get("generated-sources", "annotations")));
        ext.mkdir(dest.getParent().resolve(Paths.get("generated-test-sources", "test-annotations")));

        final HashMap<String, FileTime> modifiedTimes = new HashMap<>(3000);

        ext.walkFileTree(ImmutableList.of(dest, destTest),
            new SimpleFileVisitor<Path>() {
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.class");
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!matcher.matches(file)) return FileVisitResult.CONTINUE;
                    modifiedTimes.put(file.toString(), attrs.lastModifiedTime());

                    if(file.toString().indexOf('$')>-1) return FileVisitResult.CONTINUE;
                    if(graph.nodes.containsKey(file)
                            && graph.nodes.get(file).classModTime.compareTo(attrs.lastModifiedTime())>0)
                        return FileVisitResult.CONTINUE;
                    log.info("updating stale dependency: " + file.toString());
                    graph.update(file,
                            visitor.getDependencies(Files.newInputStream(file)),
                            FileTime.fromMillis(System.currentTimeMillis()));
                    return FileVisitResult.CONTINUE;
                }
            });

        HashSet<Path> toBeDeleted = new HashSet<>();
        for (Map.Entry<Path, Node> entry : graph.nodes.entrySet()) {
            if(!Files.isRegularFile(entry.getKey())) {
                toBeDeleted.add(entry.getKey());
            }
        }
        graph.delete(toBeDeleted);
        //now compute the list of dirtyJavaFiles by comparing modified times for class files.
        final Set<File> dirtyJavaFiles = new HashSet<>();
        final Set<File> dependentFiles = new HashSet<>();
        ext.walkFileTree(ImmutableList.of(src, srcTest), new SimpleFileVisitor<Path>() {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.java");
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (matcher.matches(file)) {
                    String classpath = utils.toClass(file);
                    FileTime classTime = modifiedTimes.get(classpath);
                    if (classTime == null||attrs.lastModifiedTime().compareTo(classTime) > 0) {
                        dirtyJavaFiles.add(new File(file.toString()));
                        if(graph.nodes.get(Paths.get(classpath))==null) return FileVisitResult.CONTINUE;
                        if(graph.nodes.get(Paths.get(classpath)).in.size()>0) {
                            for(Node node : graph.nodes.get(Paths.get(classpath)).in) {
                                String javaStr = utils.toJava(node.path);
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
        HashSet<File> allSrcFilesToCompile = new HashSet<>();
        HashSet<File> allTestFilesToCompile = new HashSet<>();
        HashSet<File> allFiles = new HashSet<>(dirtyJavaFiles);
        allFiles.addAll(dependentFiles);
        for(File file: allFiles) {
            if(Pattern.matches(".*src\\/test\\/java.*\\.java$", file.getAbsolutePath())) {
                allTestFilesToCompile.add(file);
                if(compileTests) log.info(file.getAbsolutePath());
            } else {
                log.info(file.getAbsolutePath());
                allSrcFilesToCompile.add(file);
            }
        }

        //log.info(javacSrcOptions);

        //now start compiling
        Iterable<? extends JavaFileObject> sources = fm.getJavaFileObjectsFromFiles(allSrcFilesToCompile);
        Boolean success = true;
        if(allSrcFilesToCompile.size()>0) {
            jc.getTask(null, fm, null, javacSrcOptions, null, sources).call();
        }
        if(compileTests && allTestFilesToCompile.size()>0) {
            log.info("compiling test files");
            log.info(javacTestOptions);
            Iterable<? extends JavaFileObject> tests = fm.getJavaFileObjectsFromFiles(allTestFilesToCompile);
            success &= jc.getTask(null, fm, null, javacTestOptions, null, tests).call();

        }

        return success;
    }
    public void runTest(String claz) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("java","-Xdebug", "-Xrunjdwp:server=y,transport=dt_socket,address=4005,suspend=n", "-classpath", javacTestOptions.get(3), "com.coverity.TestRunner", claz);
        log.info(Joiner.on(" ").join(pb.command()));
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        Process process = pb.start();
        process.waitFor();
        log.info("finished running the test");
    }


}

