package by.graph.entity;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Graph
{
    public final ConcurrentHashMap<String, Vertex> vertices = new ConcurrentHashMap<>();
    public final PriorityBlockingQueue<Edge> edges = new PriorityBlockingQueue<>();

    public AtomicReference<Double> strengthSum = new AtomicReference<>(0d);
    public AtomicInteger strengthCount = new AtomicInteger();

    public double delta = 0d;


    public void addEdge(Edge edge) {
        this.edges.add(edge);
        addToVertex(edge.from, edge);
        addToVertex(edge.to, edge);
        measureGraphStrength(edge);
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
