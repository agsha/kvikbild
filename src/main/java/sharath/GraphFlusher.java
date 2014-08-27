package sharath;

import com.google.inject.Inject;
import org.apache.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sgururaj on 8/21/14.
 */
public class GraphFlusher extends Thread {
    private final Connection c;
    private CreateSchema cs;
    private HashMap<String, GraphTable> graphTable = new HashMap<>();
    private static final Logger log = Logger.getLogger(GraphFlusher.class);

    @Inject
    public GraphFlusher(Connection c, CreateSchema cs) {

        this.c = c;
        this.cs = cs;
    }

    @Override
    public void run() {
        while (true) {
            try {
                //log.info("graph flusher woke up.");
                sleep(2000);
                for (Map.Entry<String, GraphTable> entry : graphTable.entrySet()) {
                    flushGraph(entry.getValue().graph, entry.getValue().tableName);
                }
            } catch (InterruptedException e) {
                log.error("sleep interrupted", e);
            } catch (SQLException e) {
                log.error("sql error", e);
            }
        }
    }

    public void flushGraph(Graph graph, String tableName) throws SQLException {
        synchronized (graph) {
            if (!graph.isDirty()) return;
            long time = System.currentTimeMillis();
            cs.create(c);
            c.setAutoCommit(false);
            String sql = "insert into "+tableName+" (classpath, deps, class_mod_time) values(?, ?, ?)";
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

    public void addGraph(Graph graph, String tableName, String name) {
        graphTable.put(name, new GraphTable(name, graph, tableName));
    }

    public void loadAll() throws SQLException {
        for (Map.Entry<String, GraphTable> entry : graphTable.entrySet()) {
            load(entry.getValue().graph, entry.getValue().tableName);
        }
    }
    public void load(Graph graph, String tableName) throws SQLException {
        PreparedStatement ps = c.prepareStatement("select * from "+tableName);
        ResultSet rs = ps.executeQuery();
        while(rs.next()) {
            Path classpath = Paths.get(rs.getString("classpath"));
            String[] depsArray = rs.getString("deps").split(",");
            List<Path> deps = new ArrayList<>(depsArray.length);
            for (String dep : depsArray) {
                deps.add(Paths.get(dep));
            }
            graph.update(classpath, deps, FileTime.fromMillis(rs.getLong("class_mod_time")));
        }
        graph.setDirty(false);
        rs.close();
        ps.close();

    }

    static class GraphTable {
        String name;
        Graph graph;
        String tableName;

        GraphTable(String name, Graph graph, String tableName) {
            this.name = name;
            this.graph = graph;
            this.tableName = tableName;
        }
    }
}
