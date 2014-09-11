package sharath;

import com.google.inject.Guice;
import com.google.inject.Injector;
import junit.framework.TestCase;
import org.apache.log4j.Logger;

public class MavenThingsTest extends TestCase {
    Injector injector = Guice.createInjector(new MyModule());
    private static final Logger log = Logger.getLogger(MavenThingsTest.class.getName());
    private final Utils.Config cfg;

    public MavenThingsTest() {
        cfg = injector.getInstance(Utils.Config.class);
    }

    public void testMaven() {
        String log = "[INFO] \n" +
                "[INFO]                                                                         \n" +
                "[INFO] ------------------------------------------------------------------------\n" +
                "[INFO] Building Base shared code harmony-SNAPSHOT\n" +
                "[INFO] ------------------------------------------------------------------------\n" +
                "[INFO] \n" +
                "[INFO] --- maven-dependency-plugin:2.8:list (default-cli) @ base ---\n" +
                "[INFO] \n" +
                "[INFO] The following files have been resolved:\n" +
                "[INFO]    none\n" +
                "[INFO] \n" +
                "[INFO]                                                                         \n" +
                "[INFO] ------------------------------------------------------------------------\n" +
                "[INFO] Building License consumer harmony-SNAPSHOT\n" +
                "[INFO] ------------------------------------------------------------------------\n" +
                "[INFO] \n" +
                "[INFO] --- maven-dependency-plugin:2.8:list (default-cli) @ license ---\n" +
                "[INFO] \n" +
                "[INFO] The following files have been resolved:\n" +
                "[INFO]    flexlm:flexlm:jar:11.5:compile\n" +
                "[INFO]    cov-foundation:cov-foundation:jar:5.5:compile\n" +
                "[INFO]    org.bouncycastle:bcprov-jdk15on:jar:1.48:compile\n" +
                "[INFO] \n" +
                "[INFO]                                                                         \n" +
                "[INFO] ------------------------------------------------------------------------\n" +
                "[INFO] Building cimlistener harmony-SNAPSHOT\n" +
                "[INFO] ------------------------------------------------------------------------\n" +
                "[INFO] \n" +
                "[INFO] --- maven-dependency-plugin:2.8:list (default-cli) @ cimlistener ---\n" +
                "[INFO] \n" +
                "[INFO] The following files have been resolved:\n" +
                "[INFO]    org.apache.tomcat:tomcat-util:jar:7.0.55:compile\n" +
                "[INFO]    org.apache.tomcat:tomcat-annotations-api:jar:7.0.55:compile\n" +
                "[INFO]    org.apache.tomcat:tomcat-catalina:jar:7.0.55:compile\n" +
                "[INFO]    org.apache.tomcat:tomcat-juli:jar:7.0.55:compile\n";
    }
}