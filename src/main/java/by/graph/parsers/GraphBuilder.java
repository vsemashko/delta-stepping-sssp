package by.graph.parsers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import by.graph.entity.Edge;
import by.graph.entity.Graph;
import by.graph.entity.Vertex;

public class GraphBuilder
{
    private final ConcurrentHashMap<String, Vertex> vertices = new ConcurrentHashMap<>();
    private final PriorityBlockingQueue<Edge> edges = new PriorityBlockingQueue<>();

    private AtomicReference<Double> strengthSum = new AtomicReference<>(0d);
    private AtomicInteger strengthCount = new AtomicInteger();

    public GraphBuilder addEdge(Edge edge) {
        this.edges.add(edge);
        addToVertex(edge.from, edge);
        addToVertex(edge.to, edge);
        measureGraphStrength(edge);
        return this;
    }

    public Graph build() {
        double delta = strengthSum.get() / strengthCount.get();
        return new Graph(vertices, edges, delta);
    }

    private void measureGraphStrength(Edge edge) {
        strengthCount.incrementAndGet();
        strengthSum.accumulateAndGet(edge.strength, (sum, newValue) -> sum + newValue);
    }

    private void addToVertex(String vertexName, Edge edge) {
        if (!this.vertices.containsKey(vertexName)) {
            synchronized (this) {
                if (!this.vertices.containsKey(vertexName)) {
                    this.vertices.put(vertexName, new Vertex(vertexName));
                }
            }
        }
        this.vertices.get(vertexName).edges.add(edge);
    }
}
