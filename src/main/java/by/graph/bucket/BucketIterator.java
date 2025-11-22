package by.graph.bucket;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.IntStream;

import by.graph.entity.Vertex;

/**
 * Iterator for efficiently traversing buckets in order.
 * Computes buckets once during construction and iterates through them.
 */
public class BucketIterator implements Iterator<List<Vertex>>
{
    private final Iterator<List<Vertex>> bucketListIterator;

    public BucketIterator(Vertex[] vertices, AtomicIntegerArray vertexBuckets) {
        // Build all buckets once during construction
        Map<Integer, List<Vertex>> buckets = buildBuckets(vertices, vertexBuckets);
        this.bucketListIterator = buckets.values().iterator();
    }

    @Override
    public boolean hasNext() {
        return bucketListIterator.hasNext();
    }

    @Override
    public List<Vertex> next() {
        return bucketListIterator.next();
    }

    /**
     * Builds a map of bucket index to list of vertices in that bucket.
     * Uses TreeMap to maintain bucket order (lowest index first).
     */
    private Map<Integer, List<Vertex>> buildBuckets(Vertex[] vertices, AtomicIntegerArray vertexBuckets) {
        Map<Integer, List<Vertex>> buckets = new TreeMap<>();

        IntStream.range(0, vertexBuckets.length())
                .forEach(i -> {
                    int bucketIndex = vertexBuckets.get(i);
                    if (bucketIndex >= 0) {
                        buckets.computeIfAbsent(bucketIndex, k -> new ArrayList<>())
                               .add(vertices[i]);
                    }
                });

        return buckets;
    }
}
