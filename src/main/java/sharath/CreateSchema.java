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
                  ")\n",
          "drop table if exists module",
          "create table module (\n" +
                  "\tname text,\n" +
                  "\tdisplayName text,\n" +
                  "\tphase text,\n" +
                  "\tclasspath text,\n" +
                  "\tpath text\n" +
                  "\t)"
        };
        for (String stmt : stmts) {
            PreparedStatement ps = c.prepareStatement(stmt);
            ps.execute();
            ps.close();
        }
    }
}
