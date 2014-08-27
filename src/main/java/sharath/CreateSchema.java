package sharath;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by sgururaj on 8/21/14.
 */
public class CreateSchema {
    public void create(Connection c) throws SQLException {
        String stmts[] = new String[] {
          "drop table if exists core_graph",
          "CREATE TABLE core_graph (\n" +
                  "classpath text PRIMARY KEY NOT NULL,\n" +
                  "deps text not null,\n" +
                  "class_mod_time BIGINT not null\n" +
                  ")\n"
        };
        for (String stmt : stmts) {
            PreparedStatement ps = c.prepareStatement(stmt);
            ps.execute();
            ps.close();
        }
    }
}
