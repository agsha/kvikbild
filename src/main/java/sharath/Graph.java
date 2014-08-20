package sharath;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author sgururaj
 */
public class Graph {
    HashMap<Path, Node> nodes = new HashMap<>();
    public Node add(Path path, ArrayList<Path> children) {
        Node node = getOrCreateNode(path);
        for(Path p: children) {
            Node to = getOrCreateNode(p);
            node.out.add(to);
            to.in.add(node);
        }
        return node;
    }

    private Node getOrCreateNode(Path path) {
        Node node = nodes.get(path);
        if(node == null) {
            node = new Node(path);
            nodes.put(path, node);
        }
        return node;
    }
}

class Node {
    Path path;
    ArrayList<Node> out = new ArrayList<>();
    ArrayList<Node> in = new ArrayList<>();
    Node(Path path) {

        this.path = path;
    }
}