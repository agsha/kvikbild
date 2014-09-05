package sharath;

/**
 * @author sgururaj
 */
public interface ICimServer {

    void startJettyServer() throws Exception;

    void restartCim() throws Exception;
}
