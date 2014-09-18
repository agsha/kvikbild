package sharath;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author sgururaj
 */
public class MavenThings {

    private String[] args;
    private ModuleLineProcessor.Factory.Factory2 factory2;
    private Connection conn;
    private static final Logger log = LogManager.getLogger(MavenThings.class);
    private Map<String, String> moduleToPath;
    private External.ProcessHelper ph;

    private MavenThings(String[] args, ModuleLineProcessor.Factory.Factory2 factory2, Connection conn, Map<String, String> moduleToPath, External.ProcessHelper ph) {
        this.args = args;
        this.factory2 = factory2;
        this.conn = conn;
        this.moduleToPath = moduleToPath;
        this.ph = ph;
    }
    public static void main(String[] args) throws Exception {
        Injector injector = Guice.createInjector(new MyModule());
        MavenThings mavenThings = injector.getInstance(Factory.class).create(args);
        mavenThings.computePaths();
    }

    void computePaths() throws IOException, External.ProcessHelper.ProcessError, InterruptedException, SQLException {
        //get the maven home
        String mavenRepo = getMavenRepo();
        ModuleLineProcessor.Factory factory = factory2.create(mavenRepo);
        ph.execute("mvn", "dependency:go-offline");
        List<ModuleLineProcessor> moduleProcessorList = ImmutableList.of(
                factory.create("ces", "Coverity Integrity Server"),
                factory.create("base", "Base shared code"),
                factory.create("license", "License consumer"),
                factory.create("cimlistener", "cimlistener"),
                factory.create("structext", "Structured Text Library"),
                factory.create("app", "CIM Application Parent"),
                factory.create("core", "CIM Application Core"),
                factory.create("ws", "CIM Web Services"),
                factory.create("web", "CIM Web Application"),
                factory.create("findbugs-checkers", "FindBugs Checker Information"),
                factory.create("ces-tools", "CES Tools"),
                factory.create("tomcat", "Tomcat Packaging")
                );
        ph.execute(moduleProcessorList, "mvn", "dependency:list");
        conn.prepareStatement("delete from module").execute();
        String sql = "insert into module \n" +
                "(name, displayName, phase, classpath, path) \n" +
                "values (?, ?, ?, ?, ?)";
        try {
            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement(sql);

            for (ModuleLineProcessor processor : moduleProcessorList) {
                for (Map.Entry<String, ArrayList<String>> entry : processor.phaseToPaths.entrySet()) {
                    ps.setString(1, processor.name);
                    ps.setString(2, processor.displayName);

                    ps.setString(3, entry.getKey());
                    ps.setString(4, Joiner.on(":").join(entry.getValue()));
                    ps.setString(5, moduleToPath.get(processor.name));
                    ps.addBatch();
                }
            }
            ps.executeBatch();
            conn.commit();
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private String getMavenRepo() throws IOException {
        final Pattern pattern = Pattern.compile("<localRepository.*>(.*)<\\/localRepository>");
        final String[]ret = new String[1];
        External.ProcessHelper.LineProcessor processor = new External.ProcessHelper.LineProcessor() {
            @Override
            public Action process(String line) {
                Matcher matcher = pattern.matcher(line);
                if(!matcher.find()) return Action.PROCEED;
                ret[0] = matcher.group(1);
                return Action.STOP;
            }
        };
        ph.execute(processor, "mvn", "help:effective-settings");
        return ret[0];
    }


    static class Factory {
        private ModuleLineProcessor.Factory.Factory2 factory2;
        private Connection conn;
        private Utils.Config cfg;
        private External.ProcessHelper.Factory phFactory;

        @Inject
        public Factory(ModuleLineProcessor.Factory.Factory2 factory2, Connection conn, Utils.Config cfg, External.ProcessHelper.Factory phFactory) {
            this.factory2 = factory2;
            this.conn = conn;
            this.cfg = cfg;
            this.phFactory = phFactory;
        }
        public MavenThings create(String[]args) {
            return new MavenThings(args, factory2, conn, cfg.moduleToPath, phFactory.forCwd());
        }
    }

    static class ModuleLineProcessor implements External.ProcessHelper.LineProcessor {
        String name;
        String displayName;
        private String mavenRepo;
        private String cwd;
        private Map<String, String> moduleToPath;
        int state = 0;
        Pattern start;
        HashMap<String, ArrayList<String>> phaseToPaths = new HashMap<>();
        ModuleLineProcessor(String name, String displayName, String mavenRepo, String cwd, Map<String, String> moduleToPath) {
            this.name = name;
            this.displayName = displayName;
            this.mavenRepo = mavenRepo;
            this.cwd = cwd;
            this.moduleToPath = moduleToPath;
            this.start = Pattern.compile("--- maven-dependency-plugin:.*:list \\(default-cli\\) @ "+name+" ---");
            //log.info(start.toString());
        }

        @Override
        public Action process(String line) {
            //log.info(line+" "+state);
            if(state==0&&start.matcher(line).find()) {
                state=1;
            } else if(state>=1&&state<3) {
                state++;
            } else if(state==3 && (line.contains("none")||line.split("\\s+").length<2)) {
                state = 4;
                return Action.STOP;
            } else if(state==3) {
                //log.info(line);
                line = line.split("\\s+")[1];

                String[] split = line.split(":");
                String phase = split[split.length-1];
                String version = split[split.length-2];
                String classifier = split.length==6?"-"+split[split.length-3]:"";
                String ext = split[2];
                String filename = split[1];
                String[] paths = split[0].split("\\.");
                if(!phaseToPaths.containsKey(phase)) {
                    phaseToPaths.put(phase, new ArrayList<String>());
                }
                log.info(name+", "+line+", "+phase+", "+version+", "+ext+", "+filename+", "+ Arrays.toString(paths));

                for (String module : moduleToPath.keySet()) {
                    String value = moduleToPath.get(module);
                    String prefix = value.contains("/app/")?"com.coverity.cim:"+module:"com.coverity:"+module;

                    if(line.startsWith(prefix)) {
                        String classpath = moduleToPath.get(module);
                        if(!Files.exists(Paths.get(classpath))) {
                            log.info("Oh no! this path doesnt exist: "+classpath);
                        }

                        phaseToPaths.get(phase).add(Paths.get(classpath, "target", "classes").toString());
                        return Action.PROCEED;
                    }
                }

                Path p = Paths.get(mavenRepo, paths).resolve(filename).resolve(version).resolve(filename+"-"+version+classifier+"."+ext);
                if(!Files.exists(p)) {
                    log.info("Oh no! this path doesnt exist: "+p.toString());
                }
                phaseToPaths.get(phase).add(p.toAbsolutePath().toString());
            }
            return Action.PROCEED;
        }
        static class Factory {
            private Utils.Config cfg;
            private String mavenRepo;
            private Map<String, String> moduleToPath;

            public Factory(Utils.Config cfg, String mavenRepo, Map<String, String> moduleToPath) {
                this.cfg = cfg;
                this.mavenRepo = mavenRepo;
                this.moduleToPath = moduleToPath;
            }


            public ModuleLineProcessor create(String name, String displayName) {
                return new ModuleLineProcessor(name, displayName, mavenRepo, cfg.cwd, moduleToPath);
            }
            static class Factory2 {
                private Utils.Config cfg;

                @Inject
                Factory2(Utils.Config cfg) {
                    this.cfg = cfg;
                }

                public Factory create(String mavenRepo) {
                    return new Factory(cfg, mavenRepo, cfg.moduleToPath);
                }
            }
        }
    }
}
