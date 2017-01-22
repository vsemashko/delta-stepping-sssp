package by.graph;

import java.util.List;

import by.graph.bucket.BucketContainer;
import by.graph.entity.Edge;
import by.graph.entity.Graph;
import by.graph.entity.Vertex;

public class DeltaSteppingExecutor
{
    public Graph findStrongestPaths(Graph graph, String sourceVertexName) {
        BucketContainer bucketContainer = new BucketContainer(graph.verticesIdsToNameMapping);
        relax(graph.vertices.get(sourceVertexName), null, 1d, bucketContainer);

        bucketContainer.getBucketStream()
                .filter((List<Vertex> bucket) -> bucket.size() > 0)
                .forEach((List<Vertex> bucket) -> {
                    bucket.forEach(vertex -> {
                        vertex.edges.forEach(edge -> {
                            Vertex neighbourVertex = graph.vertices.get(edge.getNeighbourName(vertex.name));
                            relax(neighbourVertex, vertex, vertex.strongestPathToVertex.get() * edge.strength, bucketContainer, edge);
                        });
                        bucketContainer.removeVertex(vertex);
                    });
                });
        return graph;
    }

    private void relax(Vertex vertex, Vertex prevVertex, double newStrength, BucketContainer bucketContainer, Edge edge) {
        if (vertex.strongestPathToVertex.get() < newStrength) {
            bucketContainer.moveToAppropriateBucket(vertex, newStrength);
            vertex.previousVertexName = prevVertex == null ? null : prevVertex.name;
            vertex.strongestPathToVertex.set(newStrength);
            if (edge != null) {
                vertex.strongestEdge.set(edge.strength);
            }
        }
    }

    private void relax(Vertex vertex, Vertex prevVertex, double newStrength, BucketContainer bucketContainer) {
        relax(vertex, prevVertex, newStrength, bucketContainer, null);
    }
}
