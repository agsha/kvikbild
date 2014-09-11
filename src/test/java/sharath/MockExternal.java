package sharath;

import java.io.IOException;
import java.util.List;

/**
 * Created by sgururaj on 9/6/14.
 */
public class MockExternal extends External.ProcessHelper{

    public MockExternal(String dir) {
        super(dir);
    }

    @Override
    public void execute(List<? extends External.ProcessHelper.LineProcessor> processors, String... command) throws IOException {

    }
}
