import java.util.HashSet;
import java.util.Set;

public class Vertex {
    public final String name;
    public final Set<Edge> edges = new HashSet<Edge>();

    public Vertex(String name) {
        this.name = name;
    }

    public void addEdge(Edge edge) {
        this.edges.add(edge);
    }
}