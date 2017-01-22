package by.graph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DeltaSteppingExecutor
{
    public Graph graph;
    public final ConcurrentHashMap<String, PriorityBlockingQueue<Edge>> strongEdges = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, PriorityBlockingQueue<Edge>> weakEdges = new ConcurrentHashMap<>();
    double delta;

    DeltaSteppingExecutor(Graph graph) {
        this.graph = graph;
    }

    AtomicIntegerArray vertexBuckets;
    Vertex[] vertexes;

    private final static int NOT_IN_BUCKET_ID = -1;

    public Graph findStrongestPaths() {
        delta = graph.delta;
        vertexBuckets = new AtomicIntegerArray(graph.vertices.size());

        vertexes = new Vertex[graph.vertices.size()];
        graph.vertices.values().stream().forEach(vertex -> {
            vertexes[vertex.id] = vertex;
        });

        getVertexIdStream(vertexes.length).forEach(i -> vertexBuckets.set(i, NOT_IN_BUCKET_ID));

        graph.edges.parallelStream().forEach(splitByDelta(graph.delta));
        relax(graph.vertices.get(graph.sourceVertexName), null, 1d);

        getBucketStream(graph)
                .filter((List<Vertex> bucket) -> bucket.size() > 0)
                .forEach((List<Vertex> bucket) -> {
                    List<Vertex> s;
                    bucket.forEach(vertex -> {
                        if (vertexBuckets.get(vertex.id) < 0) {
                            return;
                        }
                        //vertexBuckets.set(vertex.id, IN_PROCESSING_ID);
                        vertex.edges.forEach(edge -> {
                            Vertex neighbourVertex = graph.vertices.get(edge.getNeighbourName(vertex.name));
                            relax(neighbourVertex, vertex, vertex.strongestPathToVertex.get() * edge.strength, edge);
                        });
                /*strongEdges.get(vertex.name).forEach(edge -> {
                    Vertex neighbourVertex = graph.vertices.get(edge.getNeighbourName(vertex.name));
                    relax(neighbourVertex, vertex, vertex.strongestPathToVertex.get() * edge.strength);
                });
                weakEdges.get(vertex.name).forEach(edge -> {
                    Vertex neighbourVertex = graph.vertices.get(edge.getNeighbourName(vertex.name));
                    relax(neighbourVertex, vertex, vertex.strongestPathToVertex.get() * edge.strength);
                });*/
                        vertexBuckets.set(vertex.id, NOT_IN_BUCKET_ID);
                    });
                });

        System.out.println("finish!");
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

    private Stream<List<Vertex>> getBucketStream(Graph graph) {
        Iterator<List<Vertex>> bucketIterator = getBucketIterator(graph);
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

    private Consumer<Edge> splitByDelta(double delta) {
        return edge -> {
            if (edge.strength >= delta) {
                addToVertexEdges(strongEdges, edge.from, edge);
                addToVertexEdges(strongEdges, edge.to, edge);
            } else {
                addToVertexEdges(weakEdges, edge.from, edge);
                addToVertexEdges(weakEdges, edge.to, edge);
            }
        };
    }

    private void addToVertexEdges(ConcurrentHashMap<String, PriorityBlockingQueue<Edge>> vertexEdgesMap, String vertexName, Edge edge) {
        vertexEdgesMap.putIfAbsent(vertexName, new PriorityBlockingQueue<>());
        vertexEdgesMap.get(vertexName).add(edge);
    }

    private Iterator<List<Vertex>> getBucketIterator(Graph graph) {
        return new Iterator<List<Vertex>>()
        {
            Map<Integer, List<Vertex>> buckets = null;

            @Override
            public boolean hasNext() {
                if (buckets == null) {
                    buckets = getBuckets();
                }
                return buckets.size() > 0;
            }

            @Override
            public List<Vertex> next() {
                if (buckets == null) {
                    buckets = getBuckets();
                }
                Map<Integer, List<Vertex>> result = buckets;
                buckets = null;
                return result.size() > 0 ? result.values().iterator().next() : new ArrayList<>();
            }

            private Map<Integer, List<Vertex>> getBuckets() {
                Map<Integer, List<Vertex>> buckets = new TreeMap<>();
                getVertexIdStream(vertexes.length).forEach(i -> {
                    int bucketIndex = vertexBuckets.get(i);
                    if (bucketIndex >= 0) {
                        if (buckets.get(bucketIndex) == null) {
                            buckets.put(bucketIndex, new ArrayList<>());
                        }
                        buckets.get(bucketIndex).add(vertexes[i]);
                    }
                });
                return buckets;
            }
        };
    }
}
