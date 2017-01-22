package by.graph.entity;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

public class Graph
{
    public final ConcurrentHashMap<String, Vertex> vertices;
    public final PriorityBlockingQueue<Edge> edges;
    public final Vertex[] verticesIdsToNameMapping;

    public final double delta;

    public Graph(Map<String, Vertex> vertices, Queue<Edge> edges, double delta) {
        this.vertices = new ConcurrentHashMap<>(vertices);
        this.edges = new PriorityBlockingQueue<>(edges);
        this.verticesIdsToNameMapping = getVertexesToIdsMapping();
        this.delta = delta;
    }

    private Vertex[] getVertexesToIdsMapping() {
        Vertex[] vertexes = new Vertex[vertices.size()];
        vertices.values().stream().forEach(vertex -> {
            vertexes[vertex.id] = vertex;
        });
        return vertexes;
    }
}
