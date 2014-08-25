package sharath;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.*;

/**
 * @author sgururaj
 */
@Singleton
public class Graph {
    HashMap<Path, Node> nodes = new HashMap<Path, Node>();
    private boolean dirty;


    private Node getOrCreateNode(Path path) {
        Node node = nodes.get(path);
        if (node == null) {
            node = new Node(path);
            nodes.put(path, node);
        }
        return node;
    }

    public Node update(Path file, Iterable<Path> deps, FileTime fileTime) {
        synchronized (this) {
            dirty = true;
            Node node = getOrCreateNode(file);
            node.classModTime = fileTime;
            ArrayList<Node> oldOuts = node.out;
            node.out = new ArrayList<>();
            for (Path p : deps) {
                Node to = getOrCreateNode(p);
                node.out.add(to);
                to.in.add(node);
            }
            for (Node out : oldOuts) {
                out.in.remove(node);

            }
            return node;
        }
    }

    public Node delete(Path file) {
        return delete(ImmutableSet.of(file));
    }

    public void setDirty(boolean dirty) {
        synchronized (this){
            this.dirty = dirty;
        }
    }

    @Override
    public String toString() {
        return nodes.toString();
    }

    public boolean isDirty() {
        return dirty;
    }

    public Node delete(Iterable<Path> toBeDeleted) {
        synchronized (this) {
            for (Path file : toBeDeleted) {
                if (!nodes.containsKey(file)) return null;
                Node node = nodes.remove(file);
                for (Node n : node.out) {
                    n.in.remove(node);
                }
                return node;

            }
            return null;

        }

    }
}

class Node {
    Path path;
    FileTime classModTime = FileTime.fromMillis(0);
    ArrayList<Node> out = new ArrayList<>();
    ArrayList<Node> in = new ArrayList<>();

    Node(Path path) {

        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Node node = (Node) o;

        if (path != null ? !path.equals(node.path) : node.path != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return path != null ? path.hashCode() : 0;
    }
}