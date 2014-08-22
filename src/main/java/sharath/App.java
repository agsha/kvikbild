package sharath;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Hello world!
 *
 */
public class App
{
    private final String[] args;
    private final External ext;
    private static final Logger log = Logger.getLogger(App.class);
    private final String cwd;
    private Graph graph;
    private Connection c;
    private DependencyVisitor visitor;
    private GraphFlusher flusher;
    private Server server;
    private Utils utils;

    @Inject
    public App(@Assisted String[] args,  External ext, @Named("cwd") String cwd, Graph graph, Connection c, DependencyVisitor visitor, GraphFlusher flusher, Server server, Utils utils) {

        this.args = args;
        this.ext = ext;
        this.cwd = cwd;
        this.graph = graph;
        this.c = c;
        this.visitor = visitor;
        this.flusher = flusher;
        this.server = server;
        this.utils = utils;
    }

    public static void main( String[] args ) throws Exception {
        Injector injector = Guice.createInjector(new MyModule());
        App app = injector.getInstance(AppFactory.class).create(args);
        app.start();
    }

    public void start() throws Exception {
        PreparedStatement ps = c.prepareStatement("select * from dependency");
        ResultSet rs = ps.executeQuery();
        while(rs.next()) {
            Path classpath = Paths.get(rs.getString("classpath"));
            String[] depsArray = rs.getString("deps").split(",");
            List<Path> deps = new ArrayList<>(depsArray.length);
            for (String dep : depsArray) {
                deps.add(Paths.get(dep));
            }
            graph.add(classpath, deps, FileTime.fromMillis(rs.getLong("class_mod_time")));
        }
        rs.close();
        ps.close();

        Path classesDir = Paths.get(cwd, "app", "core", "target", "classes");
        Path testClasses = Paths.get(cwd, "app", "core", "target", "test-classes");

        ext.walkFileTree(ImmutableList.of(classesDir, testClasses), new SimpleFileVisitor<Path>() {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.class");

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!matcher.matches(file)) return FileVisitResult.CONTINUE;
                String classpath = file.toString();
                // when should the graph be updated
                // when the info in the graph does not have the latest class information.
                // or if this classfile has no corresponding java file
                if (graph.nodes.containsKey(file) && graph.nodes.get(file).classModTime.compareTo(attrs.lastModifiedTime()) > 0)
                    return FileVisitResult.CONTINUE;



                Path javaFile = utils.toJavap(file);
                log.info(javaFile);
                if (!Files.isRegularFile(javaFile)) return FileVisitResult.CONTINUE;

                Set<Path> deps = visitor.getDependencies(Files.newInputStream(file));
                log.info(file);
                log.info("--------------------");
                log.info(deps);
                graph.update(file, deps, FileTime.fromMillis(System.currentTimeMillis()));
                return FileVisitResult.CONTINUE;

            }
        });
        flusher.start();
        server.start();
        server.join();
    }

    static interface AppFactory {
        public App create(String[] args);
    }
}
