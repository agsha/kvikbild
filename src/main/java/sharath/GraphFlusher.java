package sharath;

import com.google.inject.Inject;
import org.apache.log4j.Logger;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

/**
 * Created by sgururaj on 8/21/14.
 */
public class GraphFlusher extends Thread {
    private final Connection c;
    private final Graph graph;
    private CreateSchema cs;
    private static final Logger log = Logger.getLogger(GraphFlusher.class);

    @Inject
    public GraphFlusher(Connection c, Graph graph, CreateSchema cs) {

        this.c = c;
        this.graph = graph;
        this.cs = cs;
    }

    @Override
    public void run() {
        while (true) {
            try {
                //log.info("graph flusher woke up.");
                sleep(2000);
                flushGraph();

            } catch (InterruptedException e) {
                log.error("sleep interrupted", e);
            } catch (SQLException e) {
                log.error("sql error", e);
            }
        }
    }

    public void flushGraph() throws SQLException {
        synchronized (graph) {
            if (!graph.isDirty()) return;
            long time = System.currentTimeMillis();
            cs.create(c);
            c.setAutoCommit(false);
            String sql = "insert into dependency(classpath, deps, class_mod_time) values(?, ?, ?)";
            PreparedStatement ps = c.prepareStatement(sql);
            int count = 0;
            for (Map.Entry<Path, Node> entry : graph.nodes.entrySet()) {
                Node value = entry.getValue();
                StringBuffer sb = new StringBuffer();
                if (value.out.size() == 0) continue;
                for (Node node : value.out) {
                    sb.append(",");
                    sb.append(node.path.toString());
                }
                long classtime = value.classModTime != null ? value.classModTime.toMillis() : 0;
                ps.setString(1, value.path.toString());
                ps.setString(2, sb.substring(1));
                ps.setLong(3, classtime);
                ps.addBatch();
            }
            ps.executeBatch();
            c.commit();
            graph.setDirty(false);
            log.info("flushed graph to disk in time(ms):" + (System.currentTimeMillis() - time));
        }

    }
}
