package by.graph.iterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.Stream;

import by.graph.entity.Vertex;

public class BucketIterator implements Iterator<List<Vertex>>
{
    AtomicIntegerArray vertexBuckets;
    Vertex[] vertexes;

    public BucketIterator(Vertex[] vertexes, AtomicIntegerArray vertexBuckets) {
        this.vertexes = vertexes;
        this.vertexBuckets = vertexBuckets;
    }

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

    private Stream<Integer> getVertexIdStream(int verticesSize) {
        return Stream.iterate(0, n -> n + 1)
                .limit(verticesSize);
    }
}
