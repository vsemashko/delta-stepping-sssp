package by.graph.bucket;

import by.graph.entity.Vertex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for BucketContainer functionality
 */
class BucketContainerTest {

    @Test
    @DisplayName("Should initialize all vertices as not in bucket")
    void initialization() {
        Vertex[] vertices = createVertices(5);
        BucketContainer container = new BucketContainer(vertices);

        List<List<Vertex>> buckets = container.getBucketStream().collect(Collectors.toList());
        assertThat(buckets).isEmpty(); // No vertices in any bucket initially
    }

    @Test
    @DisplayName("Should place vertices in correct buckets based on strength")
    void bucketPlacement() {
        Vertex[] vertices = createVertices(3);
        BucketContainer container = new BucketContainer(vertices);

        // Add vertices to different buckets
        container.moveToAppropriateBucket(vertices[0], 0.9);  // bucket 1: (1-0.9)*10 = 1
        container.moveToAppropriateBucket(vertices[1], 0.5);  // bucket 5: (1-0.5)*10 = 5
        container.moveToAppropriateBucket(vertices[2], 0.1);  // bucket 9: (1-0.1)*10 = 9

        List<List<Vertex>> buckets = container.getBucketStream().collect(Collectors.toList());

        assertThat(buckets).hasSize(3);
        assertThat(buckets.get(0)).containsExactly(vertices[0]); // Bucket 1 (strongest)
        assertThat(buckets.get(1)).containsExactly(vertices[1]); // Bucket 5
        assertThat(buckets.get(2)).containsExactly(vertices[2]); // Bucket 9 (weakest)
    }

    @Test
    @DisplayName("Should return buckets in order from strongest to weakest")
    void bucketOrdering() {
        Vertex[] vertices = createVertices(5);
        BucketContainer container = new BucketContainer(vertices);

        // Add in random order
        container.moveToAppropriateBucket(vertices[2], 0.3);  // bucket 7
        container.moveToAppropriateBucket(vertices[0], 0.95); // bucket 1
        container.moveToAppropriateBucket(vertices[4], 0.1);  // bucket 9
        container.moveToAppropriateBucket(vertices[1], 0.7);  // bucket 3
        container.moveToAppropriateBucket(vertices[3], 0.5);  // bucket 5

        List<List<Vertex>> buckets = container.getBucketStream().collect(Collectors.toList());

        // Should be ordered: bucket 1, 3, 5, 7, 9
        assertThat(buckets).hasSize(5);
        assertThat(buckets.get(0).get(0).name).isEqualTo("V0"); // 0.95 strength
        assertThat(buckets.get(1).get(0).name).isEqualTo("V1"); // 0.7 strength
        assertThat(buckets.get(2).get(0).name).isEqualTo("V3"); // 0.5 strength
        assertThat(buckets.get(3).get(0).name).isEqualTo("V2"); // 0.3 strength
        assertThat(buckets.get(4).get(0).name).isEqualTo("V4"); // 0.1 strength
    }

    @Test
    @DisplayName("Should handle multiple vertices in same bucket")
    void multipleSameBucket() {
        Vertex[] vertices = createVertices(3);
        BucketContainer container = new BucketContainer(vertices);

        // Put all in same bucket (strength 0.5 → bucket 5)
        container.moveToAppropriateBucket(vertices[0], 0.5);
        container.moveToAppropriateBucket(vertices[1], 0.52); // rounds to bucket 5
        container.moveToAppropriateBucket(vertices[2], 0.48); // rounds to bucket 5

        List<List<Vertex>> buckets = container.getBucketStream().collect(Collectors.toList());

        assertThat(buckets).hasSize(1);
        assertThat(buckets.get(0)).hasSize(3);
        assertThat(buckets.get(0)).containsExactlyInAnyOrder(vertices[0], vertices[1], vertices[2]);
    }

    @Test
    @DisplayName("Should remove vertices from buckets")
    void vertexRemoval() {
        Vertex[] vertices = createVertices(3);
        BucketContainer container = new BucketContainer(vertices);

        container.moveToAppropriateBucket(vertices[0], 0.9);
        container.moveToAppropriateBucket(vertices[1], 0.5);
        container.moveToAppropriateBucket(vertices[2], 0.1);

        // Remove middle vertex
        container.removeVertex(vertices[1]);

        List<List<Vertex>> buckets = container.getBucketStream().collect(Collectors.toList());

        assertThat(buckets).hasSize(2);
        assertThat(buckets.get(0)).containsExactly(vertices[0]);
        assertThat(buckets.get(1)).containsExactly(vertices[2]);
    }

    @Test
    @DisplayName("Should handle moving vertex to different bucket")
    void vertexMovement() {
        Vertex[] vertices = createVertices(1);
        BucketContainer container = new BucketContainer(vertices);

        // Start in bucket 9
        container.moveToAppropriateBucket(vertices[0], 0.1);
        List<List<Vertex>> buckets1 = container.getBucketStream().collect(Collectors.toList());
        assertThat(buckets1).hasSize(1);

        // Move to bucket 1
        container.moveToAppropriateBucket(vertices[0], 0.9);
        List<List<Vertex>> buckets2 = container.getBucketStream().collect(Collectors.toList());
        assertThat(buckets2).hasSize(1);
        assertThat(buckets2.get(0)).containsExactly(vertices[0]);
    }

    @Test
    @DisplayName("Should handle extreme strength values")
    void extremeValues() {
        Vertex[] vertices = createVertices(3);
        BucketContainer container = new BucketContainer(vertices);

        container.moveToAppropriateBucket(vertices[0], 1.0);  // Perfect strength → bucket 0
        container.moveToAppropriateBucket(vertices[1], 0.0);  // Zero strength → bucket 10
        container.moveToAppropriateBucket(vertices[2], 0.5);  // Middle → bucket 5

        List<List<Vertex>> buckets = container.getBucketStream().collect(Collectors.toList());

        assertThat(buckets).hasSize(3);
        // Buckets should be in order: 0, 5, 10
    }

    @Test
    @DisplayName("Should handle empty buckets correctly")
    void emptyBuckets() {
        Vertex[] vertices = createVertices(2);
        BucketContainer container = new BucketContainer(vertices);

        container.moveToAppropriateBucket(vertices[0], 1.0);  // bucket 0
        container.moveToAppropriateBucket(vertices[1], 0.0);  // bucket 10
        // Buckets 1-9 are empty

        List<List<Vertex>> buckets = container.getBucketStream().collect(Collectors.toList());

        // Should only return non-empty buckets
        assertThat(buckets).hasSize(2);
    }

    @Test
    @DisplayName("Should handle large number of vertices")
    void largeNumberOfVertices() {
        Vertex[] vertices = createVertices(1000);
        BucketContainer container = new BucketContainer(vertices);

        // Place vertices with different strengths
        for (int i = 0; i < 1000; i++) {
            double strength = i / 1000.0; // 0.0 to 0.999
            container.moveToAppropriateBucket(vertices[i], strength);
        }

        List<List<Vertex>> buckets = container.getBucketStream().collect(Collectors.toList());

        // Should have multiple buckets
        assertThat(buckets).isNotEmpty();

        // Total vertices should be 1000
        int totalVertices = buckets.stream().mapToInt(List::size).sum();
        assertThat(totalVertices).isEqualTo(1000);
    }

    private Vertex[] createVertices(int count) {
        Vertex[] vertices = new Vertex[count];
        for (int i = 0; i < count; i++) {
            vertices[i] = new Vertex("V" + i, i);
        }
        return vertices;
    }
}
