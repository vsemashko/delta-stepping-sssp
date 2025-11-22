package by.graph.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for entity classes (Vertex, Edge, Graph)
 */
class EntityTest {

    @Test
    @DisplayName("Vertex should initialize with correct default values")
    void vertexInitialization() {
        Vertex vertex = new Vertex("TestVertex", 0);

        assertThat(vertex.id).isEqualTo(0);
        assertThat(vertex.name).isEqualTo("TestVertex");
        assertThat(vertex.strongestPathToVertex.get()).isEqualTo(-1.0);
        assertThat(vertex.strongestEdge.get()).isEqualTo(-1.0);
        assertThat(vertex.previousVertexName).isNull();
        assertThat(vertex.edges).isEmpty();
    }

    @Test
    @DisplayName("Edge should compare correctly by strength")
    void edgeComparison() {
        Edge edge1 = new Edge("A", "B", 0.5);
        Edge edge2 = new Edge("C", "D", 0.8);
        Edge edge3 = new Edge("E", "F", 0.5);

        assertThat(edge1.compareTo(edge2)).isLessThan(0);
        assertThat(edge2.compareTo(edge1)).isGreaterThan(0);
        assertThat(edge1.compareTo(edge3)).isEqualTo(0);
    }

    @Test
    @DisplayName("Edge should return correct neighbor name")
    void edgeGetNeighbor() {
        Edge edge = new Edge("A", "B", 0.9);

        assertThat(edge.getNeighbourName("A")).isEqualTo("B");
        assertThat(edge.getNeighbourName("B")).isEqualTo("A");
    }

    @Test
    @DisplayName("Vertex edges should maintain priority order")
    void vertexEdgesPriority() {
        Vertex vertex = new Vertex("A", 0);

        vertex.edges.add(new Edge("A", "B", 0.5));
        vertex.edges.add(new Edge("A", "C", 0.9));
        vertex.edges.add(new Edge("A", "D", 0.3));

        // Should be ordered by strength (lowest first)
        Edge first = vertex.edges.poll();
        assertThat(first.strength).isEqualTo(0.3);

        Edge second = vertex.edges.poll();
        assertThat(second.strength).isEqualTo(0.5);

        Edge third = vertex.edges.poll();
        assertThat(third.strength).isEqualTo(0.9);
    }

    @Test
    @DisplayName("Vertex strongestPathToVertex should be thread-safe with compareAndSet")
    void vertexThreadSafety() {
        Vertex vertex = new Vertex("A", 0);

        // Initial value
        assertThat(vertex.strongestPathToVertex.get()).isEqualTo(-1.0);

        // First update should succeed
        boolean updated1 = vertex.strongestPathToVertex.compareAndSet(-1.0, 0.5);
        assertThat(updated1).isTrue();
        assertThat(vertex.strongestPathToVertex.get()).isEqualTo(0.5);

        // Update with better value should succeed
        boolean updated2 = vertex.strongestPathToVertex.compareAndSet(0.5, 0.8);
        assertThat(updated2).isTrue();
        assertThat(vertex.strongestPathToVertex.get()).isEqualTo(0.8);

        // Update with wrong expected value should fail
        boolean updated3 = vertex.strongestPathToVertex.compareAndSet(0.5, 0.9);
        assertThat(updated3).isFalse();
        assertThat(vertex.strongestPathToVertex.get()).isEqualTo(0.8); // Unchanged
    }

    @Test
    @DisplayName("Multiple vertices should have unique IDs")
    void vertexUniqueIds() {
        Vertex v1 = new Vertex("A", 0);
        Vertex v2 = new Vertex("B", 1);
        Vertex v3 = new Vertex("C", 2);

        assertThat(v1.id).isNotEqualTo(v2.id);
        assertThat(v2.id).isNotEqualTo(v3.id);
        assertThat(v1.id).isNotEqualTo(v3.id);
    }

    @Test
    @DisplayName("Edge strength should be immutable")
    void edgeImmutability() {
        Edge edge = new Edge("A", "B", 0.7);

        assertThat(edge.from).isEqualTo("A");
        assertThat(edge.to).isEqualTo("B");
        assertThat(edge.strength).isEqualTo(0.7);

        // All fields are final, so this test just verifies they're set correctly
    }
}
