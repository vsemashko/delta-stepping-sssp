package by.graph;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import by.graph.iterator.BucketIterator;
import by.graph.entity.Edge;
import by.graph.entity.Graph;
import by.graph.entity.Vertex;

public class DeltaSteppingExecutor
{
    public Graph graph;
    public String sourceVertexName;

    AtomicIntegerArray vertexBuckets;
    Vertex[] vertexes;

    private final static int NOT_IN_BUCKET_ID = -1;

    DeltaSteppingExecutor(Graph graph, String sourceVertexName) {
        this.graph = graph;
        this.sourceVertexName = sourceVertexName;
    }

    public Graph findStrongestPaths() {
        vertexBuckets = new AtomicIntegerArray(graph.vertices.size());

        vertexes = new Vertex[graph.vertices.size()];
        graph.vertices.values().stream().forEach(vertex -> {
            vertexes[vertex.id] = vertex;
        });

        getVertexIdStream(vertexes.length).forEach(i -> vertexBuckets.set(i, NOT_IN_BUCKET_ID));
        relax(graph.vertices.get(sourceVertexName), null, 1d);

        getBucketStream()
                .filter((List<Vertex> bucket) -> bucket.size() > 0)
                .forEach((List<Vertex> bucket) -> {
                    bucket.forEach(vertex -> {
                        vertex.edges.forEach(edge -> {
                            Vertex neighbourVertex = graph.vertices.get(edge.getNeighbourName(vertex.name));
                            relax(neighbourVertex, vertex, vertex.strongestPathToVertex.get() * edge.strength, edge);
                        });
                        vertexBuckets.set(vertex.id, NOT_IN_BUCKET_ID);
                    });
                });
        return graph;
    }

    public void relax(Vertex vertex, Vertex prevVertex, double newStrength, Edge edge) {
        if (vertex.strongestPathToVertex.get() < newStrength) {
            int index = this.getBucketIndex(newStrength);
            vertexBuckets.set(vertex.id, index);
            vertex.previousVertexName = prevVertex == null ? null : prevVertex.name;
            vertex.strongestPathToVertex.set(newStrength);
            if (edge != null) {
                vertex.strongestEdge.set(edge.strength);
            }
        }
    }

    public void relax(Vertex vertex, Vertex prevVertex, double newStrength) {
        relax(vertex, prevVertex, newStrength, null);
    }

    private Stream<List<Vertex>> getBucketStream() {
        Iterator<List<Vertex>> bucketIterator = new BucketIterator(vertexes, vertexBuckets);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(bucketIterator, Spliterator
                .DISTINCT), false);
    }

    private Stream<Integer> getVertexIdStream(int verticesSize) {
        return Stream.iterate(0, n -> n + 1)
                .limit(verticesSize);
    }

    private int getBucketIndex(double strength) {
        return (int) Math.round(1 - strength) * 10;
    }
}
