package sharath;

import com.google.inject.Provider;

/**
 * @author sgururaj
 */
public class ICimServerProvider implements Provider<ICimServer> {
    @Override
    public ICimServer get() {
        ClassLoader loader = new ClassLoader() {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                return super.loadClass(name, resolve);    // NOCOMMIT
            }
        };
        return null;
    }
}
