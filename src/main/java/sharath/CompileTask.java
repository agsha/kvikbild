package sharath;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.log4j.Logger;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
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
    private static final Logger log = Logger.getLogger(CompileTask.class);
    private final Utils.CimModule cimModule;
    private final Graph graph;
    private final External ext;
    private final DependencyVisitor visitor;
    private final JavaCompiler jc;
    private final Utils.StandardJavaFileManagerFactory fmFactory;
    private final Utils utils;
    private final String javaAgentJrebel;
    private final String javaagentJmockit;

    protected CompileTask(Utils.CimModule cimModule, Graph graph, External ext, DependencyVisitor visitor, JavaCompiler jc, Utils.StandardJavaFileManagerFactory fmFactory, Utils utils, String javaAgentJrebel, String javaagentJmockit) {
        this.cimModule = cimModule;
        this.graph = graph;
        this.ext = ext;
        this.visitor = visitor;
        this.jc = jc;
        this.fmFactory = fmFactory;
        this.utils = utils;
        this.javaAgentJrebel = javaAgentJrebel;
        this.javaagentJmockit = javaagentJmockit;
    }


    public boolean doCompile(boolean compileTests) throws IOException {
        ext.mkdir(cimModule.dest);
        ext.mkdir(cimModule.destTest);
        ext.mkdir(cimModule.dest.getParent().resolve(Paths.get("generated-sources", "annotations")));
        ext.mkdir(cimModule.dest.getParent().resolve(Paths.get("generated-test-sources", "test-annotations")));

        final HashMap<String, FileTime> modifiedTimes = new HashMap<>(3000);

        ext.walkFileTree(ImmutableList.of(cimModule.dest, cimModule.destTest),
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
        ext.walkFileTree(ImmutableList.of(cimModule.src, cimModule.srcTest), new SimpleFileVisitor<Path>() {
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
        Iterable<? extends JavaFileObject> sources = fmFactory.forCoreSrc().getJavaFileObjectsFromFiles(allSrcFilesToCompile);
        Boolean success = true;
        if(allSrcFilesToCompile.size()>0) {
            jc.getTask(null, fmFactory.forCoreSrc(), null, cimModule.javacSrcOptions, null, sources).call();
        }
        if(compileTests && allTestFilesToCompile.size()>0) {
            log.info("compiling test files");
            Joiner sp = Joiner.on(" ");
            Iterable<? extends JavaFileObject> tests = fmFactory.forCoreTest().getJavaFileObjectsFromFiles(allTestFilesToCompile);
            log.info(sp.join(new String[]{"javac", sp.join(cimModule.javacTestOptions), sp.join(allTestFilesToCompile)}));
            success &= jc.getTask(null, fmFactory.forCoreTest(), null,cimModule.javacTestOptions, null, tests).call();

        }

        return success;
    }
    public void runTest(String claz) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("java","-Xdebug", "-Xrunjdwp:server=y,transport=dt_socket,address=4005,suspend=n", "-classpath", cimModule.javacTestOptions.get(3), "com.coverity.TestRunner", claz);
        log.info(Joiner.on(" ").join(pb.command()));
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        Process process = pb.start();
        process.waitFor();
        log.info("finished running the test");
    }

    public void runNailgun() {
        Runnable nailgunRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessBuilder pb = new ProcessBuilder(
                            "java",
                            "-Xdebug",
                            "-Xrunjdwp:server=y,transport=dt_socket,address=4005,suspend=n",
                            "-Xmx1g",
                            "-XX:MaxPermSize=512M",
                            javaAgentJrebel,
                            javaagentJmockit,
                            "-classpath",
                            cimModule.javacTestOptions.get(3),
                            "org.junit.runner.JUnitCore",
                            "com.coverity.ces.test.CoreTestNailGunServer");

                    //ProcessBuilder pb = new ProcessBuilder("ls", "-la", "/Users/sgururaj/Library/Application Support/IntelliJIdea13/jr-ide-idea/lib/jrebel/jrebel.jar");
                    pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                    Process process = null;
                    try {
                        process = pb.start();
                    } catch (IOException e) {
                        log.error("io exception", e);
                        return;
                    }
                    try {
                        process.waitFor();

                    } catch (InterruptedException e) {
                        log.error("Interrupted exception", e);
                    }
                    log.info("finished running the test");
                } catch (RuntimeException e) {
                    log.error("runtime exception", e);
                }
            }
        };
        Thread t = new Thread(nailgunRunnable);
        t.start();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                log.info("hiiiiiiiiiiiiiiiiiii");
//                System.out.println("hiiiiiiiiiiiiiiiiiiiii");
//            }
//        }).start();
    }

    static class Factory {
        private final Utils.CimModule coreModule;
        private final Graph.Factory graphFactory;
        private final External ext;
        private final DependencyVisitor.Factory visitorFactory;
        private final JavaCompiler jc;
        private final Utils.StandardJavaFileManagerFactory fmFactory;
        private final Utils.Factory utilsFactory;
        private final String javaAgentJrebel;
        private final String javaagentJmockit;

        @Inject
        public Factory(@Named("core")Utils.CimModule coreModule, Graph.Factory graphFactory, External ext, DependencyVisitor.Factory visitorFactory, JavaCompiler jc, Utils.StandardJavaFileManagerFactory fmFactory, Utils.Factory utilsFactory, @Named("javaagentJrebel")String javaAgentJrebel, @Named("javaagentJmockit")String javaagentJmockit) {

            this.coreModule = coreModule;
            this.graphFactory = graphFactory;
            this.ext = ext;
            this.visitorFactory = visitorFactory;
            this.jc = jc;
            this.fmFactory = fmFactory;
            this.utilsFactory = utilsFactory;
            this.javaAgentJrebel = javaAgentJrebel;
            this.javaagentJmockit = javaagentJmockit;
        }
        public CompileTask createCoreCompileTask() {
            return new CompileTask(coreModule,
                    graphFactory.getForName("core"),
                    ext,
                    visitorFactory.create(coreModule.dest, coreModule.destTest),
                    jc,
                    fmFactory,
                    utilsFactory.createCoreUtils(),
                    javaAgentJrebel,
                    javaagentJmockit);
        }
    }
}

