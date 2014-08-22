package sharath;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * @author sgururaj
 */
@Singleton
public class Graph {
    HashMap<Path, Node> nodes = new HashMap<>();
    private boolean dirty;

    public synchronized Node  add(Path path, Iterable<Path> children, FileTime classModTime) {
        dirty = true;
        Node node = getOrCreateNode(path, classModTime);
        for(Path p: children) {
            Node to = getOrCreateNode(p, classModTime);
            node.out.add(to);
            to.in.add(node);
        }
        return node;
    }

    private Node getOrCreateNode(Path path, FileTime classModTime) {
        Node node = nodes.get(path);
        if(node == null) {
            node = new Node(path, classModTime);
            nodes.put(path, node);
        }
        node.classModTime = classModTime;
        return node;
    }

    public synchronized Node update(Path file, Iterable<Path> deps, FileTime fileTime) {
        dirty = true;
        if(!nodes.containsKey(file)) return add(file, deps, fileTime);
        Node node = nodes.get(file);
        ArrayList<Node> oldOuts = node.out;
        node.out = new ArrayList<>();
        for(Path p: deps) {
            Node to = getOrCreateNode(p, fileTime);
            node.out.add(to);
            to.in.add(node);
        }
        for (Node out : oldOuts) {
            out.in.remove(node);

        }
        return node;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public String toString() {
        return nodes.toString();
    }

    public boolean isDirty() {
        return dirty;
    }
}

class Node {
    Path path;
    FileTime classModTime;
    FileTime javaModTime;
    ArrayList<Node> out = new ArrayList<>();
    ArrayList<Node> in = new ArrayList<>();
    Node(Path path, FileTime classModTime) {

        this.path = path;
        this.classModTime = classModTime;
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