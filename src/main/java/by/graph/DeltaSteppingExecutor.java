package by.graph;

import java.util.ArrayList;
import java.util.List;

import by.graph.bucket.BucketContainer;
import by.graph.entity.Edge;
import by.graph.entity.Graph;
import by.graph.entity.Vertex;

/**
 * Executor for the Delta Stepping algorithm for Single Source Shortest Path.
 * This implementation finds the strongest (maximum product) path from source to all vertices.
 *
 * Thread-safe implementation using Compare-And-Swap (CAS) operations to prevent race conditions.
 */
public class DeltaSteppingExecutor
{
    public Graph findStrongestPaths(Graph graph, String sourceVertexName) {
        Vertex sourceVertex = graph.vertices.get(sourceVertexName);
        if (sourceVertex == null) {
            throw new IllegalArgumentException("Source vertex not found: " + sourceVertexName);
        }

        BucketContainer bucketContainer = new BucketContainer(graph.verticesIdsToNameMapping);
        relax(sourceVertex, null, 1.0, bucketContainer, null);

        // Process buckets in order
        bucketContainer.getBucketStream()
                .filter(bucket -> !bucket.isEmpty())
                .forEach(bucket -> processBucket(bucket, graph, bucketContainer));

        return graph;
    }

    /**
     * Process all vertices in a bucket.
     * Creates a snapshot of the bucket to avoid concurrent modification.
     */
    private void processBucket(List<Vertex> bucket, Graph graph, BucketContainer bucketContainer) {
        // Create a copy to avoid concurrent modification
        List<Vertex> bucketCopy = new ArrayList<>(bucket);

        bucketCopy.forEach(vertex -> {
            // Process all edges from this vertex
            vertex.edges.forEach(edge -> {
                String neighbourName = edge.getNeighbourName(vertex.name);
                Vertex neighbourVertex = graph.vertices.get(neighbourName);

                if (neighbourVertex != null) {
                    double currentStrength = vertex.strongestPathToVertex.get();
                    double newStrength = currentStrength * edge.strength;
                    relax(neighbourVertex, vertex, newStrength, bucketContainer, edge);
                }
            });

            // Remove vertex from bucket after processing
            bucketContainer.removeVertex(vertex);
        });
    }

    /**
     * Relax operation with thread-safe Compare-And-Swap (CAS) pattern.
     * Only updates if the new strength is better than the current strength.
     */
    private void relax(Vertex vertex, Vertex prevVertex, double newStrength, BucketContainer bucketContainer, Edge edge) {
        if (vertex == null) {
            return;
        }

        // Use CAS loop to handle race conditions
        double currentStrength;
        do {
            currentStrength = vertex.strongestPathToVertex.get();

            // If current strength is already better, no need to update
            if (currentStrength >= newStrength) {
                return;
            }

            // Try to update atomically
        } while (!vertex.strongestPathToVertex.compareAndSet(currentStrength, newStrength));

        // After successful CAS, update other fields
        // Note: These updates are not atomic with the strength update,
        // but that's acceptable as they're informational
        synchronized (vertex) {
            // Double-check the strength is still what we set it to
            if (vertex.strongestPathToVertex.get() == newStrength) {
                vertex.previousVertexName = prevVertex == null ? null : prevVertex.name;
                if (edge != null) {
                    vertex.strongestEdge.set(edge.strength);
                }
                bucketContainer.moveToAppropriateBucket(vertex, newStrength);
            }
        }
    }
}
