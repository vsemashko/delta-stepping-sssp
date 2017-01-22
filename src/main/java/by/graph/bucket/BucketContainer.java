package by.graph.bucket;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import by.graph.entity.Vertex;

public class BucketContainer
{
    private final Vertex[] vertices;
    private AtomicIntegerArray vertexBuckets;

    private final static int NOT_IN_BUCKET_ID = -1;

    public BucketContainer(Vertex[] vertices) {
        this.vertices = vertices;
        this.vertexBuckets = this.initVertexBuckets(vertices.length);
    }

    public Stream<List<Vertex>> getBucketStream() {
        Iterator<List<Vertex>> bucketIterator = new BucketIterator(vertices, vertexBuckets);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(bucketIterator, Spliterator
                .DISTINCT), false);
    }

    public void moveToAppropriateBucket(Vertex vertex, double strength) {
        vertexBuckets.set(vertex.id, this.getBucketIndex(strength));
    }

    public void removeVertex(Vertex vertex) {
        vertexBuckets.set(vertex.id, NOT_IN_BUCKET_ID);
    }

    private int getBucketIndex(double strength) {
        return (int) Math.round(1 - strength) * 10;
    }

    private AtomicIntegerArray initVertexBuckets(int size) {
        AtomicIntegerArray vertexBuckets = new AtomicIntegerArray(size);
        Stream.iterate(0, n -> n + 1)
                .limit(size)
                .forEach(i -> vertexBuckets.set(i, NOT_IN_BUCKET_ID));
        return vertexBuckets;
    }
}
