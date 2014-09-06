package sharath;

import com.google.common.base.Joiner;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author sgururaj
 */
public class MavenThings {

    private String[] args;
    private static final Logger log = Logger.getLogger(MavenThings.class);

    private MavenThings(String[] args) {
        this.args = args;
    }
    public static void main(String[] args) throws Exception {
        Injector injector = Guice.createInjector(new MyModule());
        MavenThings mavenThings = injector.getInstance(Factory.class).create(args);
        mavenThings.computePaths();
    }

    void computePaths() throws Exception {
        //get the maven home
        String mavenRepo = getMavenRepo();
        int ret = new ProcessBuilder("mvn", "dependency:go-offline").inheritIO().start().waitFor();
        if(ret!=0) throw new Exception("non-normal return value");




    }

    private String getMavenRepo() throws IOException {
        final Pattern pattern = Pattern.compile("<localRepository.*>(.*)<\\/localRepository>");
        final String[]ret = new String[1];
        LineProcessor processor = new LineProcessor() {
            @Override
            public Action process(String line) {
                Matcher matcher = pattern.matcher(line);
                if(!matcher.find()) return Action.PROCEED;
                ret[0] = matcher.group(1);
                return Action.STOP;
            }
        };
        execute(processor, "mvn", "help:effective-settings");
        return ret[0];
    }

    private void execute(LineProcessor processor, String... command ) throws IOException {
        Process p = new ProcessBuilder(command).start();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ( (line = br.readLine()) != null) {
            System.out.println(line);
            LineProcessor.Action action = processor.process(line);
            if(action == LineProcessor.Action.STOP) {
                p.destroy();
                br.close();
                return;
            }
        }
    }


    static class Factory {
        public MavenThings create(String[]args) {
            return new MavenThings(args);
        }
    }

    static interface LineProcessor {
        enum Action {
            STOP, PROCEED;
        }
        public Action process(String line);
    }

    static class ModuleLineProcessor implements LineProcessor {
        String name;
        String displayName;
        private String cwd;
        int state = 0;
        Pattern boundary = Pattern.compile("----------------------------------------------");
        Pattern endOfPath = Pattern.compile("[INFO]\\s+$");
        HashMap<String, ArrayList<String>> typeToPathsMap = new HashMap<>();

        Pattern displayPattern;
        ModuleLineProcessor(String name, String displayName, String cwd) {
            this.name = name;
            this.displayName = displayName;
            this.cwd = cwd;
            this.displayPattern = Pattern.compile("Building "+displayName);
        }

        @Override
        public Action process(String line) {
            if(state==0&&boundary.matcher(line).find()) {
                state=1;
            } else if(state==1&&displayPattern.matcher(line).find()) {
                state=2;
            } else if(state==2&&boundary.matcher(line).find()) {
                state=3;
            } else if(state>=3&&state<7) {
                state++;
            } else if(state==7 && !endOfPath.matcher(line).find()) {
                line = line.split("\\s+")[1];
                String[] split = line.split(":");
                int count = split.length-1;
                String phase = split[count--];
                String version = split[count--];
                String ext = split[count--];
                String fileMainName = split[count--];
                Path p = Paths.get(fileMainName);
                while(count>=0) {
                    p = Paths.get(split[count], p.toString());
                    count--;
                }
                Path path = Paths.get(Paths.get(cwd).toAbsolutePath().toString(), p.toString(), fileMainName+"-"+version+"."+ext);
                if(!Files.exists(path)) {
                    log.info("Oh no! this path doesnt exist: "+path.toString());
                }
                if(!typeToPathsMap.containsKey(phase)) {
                    typeToPathsMap.put(phase, new ArrayList<String>());
                }
                typeToPathsMap.get(phase).add(path.toString());
            } else if(state==7) {
                state = 8;
            }
            return Action.PROCEED;
        }
    }
}
