package by.graph.entity;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Vertex
{
    public final int id;
    public final String name;
    public final PriorityBlockingQueue<Edge> edges = new PriorityBlockingQueue<>();

    public String previousVertexName;
    public final AtomicReference<Double> strongestPathToVertex = new AtomicReference<>(NO_STRENGTH);
    public final AtomicReference<Double> strongestEdge = new AtomicReference<>(NO_STRENGTH);

    private static final Double NO_STRENGTH = -1d;
    private static AtomicInteger ID_SEQUENCE_GENERATOR = new AtomicInteger(0);

    public Vertex(String name) {
        this.id = ID_SEQUENCE_GENERATOR.getAndIncrement();
        this.name = name;
    }
}
