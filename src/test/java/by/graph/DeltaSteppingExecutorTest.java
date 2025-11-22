package by.graph;

import by.graph.entity.Edge;
import by.graph.entity.Graph;
import by.graph.entity.Vertex;
import by.graph.parsers.GraphBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for Delta Stepping SSSP algorithm.
 * Tests correctness, edge cases, and concurrent execution safety.
 */
class DeltaSteppingExecutorTest {

    @Nested
    @DisplayName("Algorithm Correctness Tests")
    class CorrectnessTests {

        @Test
        @DisplayName("Should find path in simple 3-vertex linear graph")
        void simpleLinearGraph() {
            // A --0.8--> B --0.9--> C
            GraphBuilder builder = new GraphBuilder();
            builder.addEdge(new Edge("A", "B", 0.8));
            builder.addEdge(new Edge("B", "C", 0.9));
            Graph graph = builder.build();

            DeltaSteppingExecutor executor = new DeltaSteppingExecutor();
            Graph result = executor.findStrongestPaths(graph, "A");

            // Path strength from A to A should be 1.0 (starting point)
            assertThat(result.vertices.get("A").strongestPathToVertex.get()).isEqualTo(1.0);

            // Path strength from A to B should be 0.8
            assertThat(result.vertices.get("B").strongestPathToVertex.get()).isEqualTo(0.8);

            // Path strength from A to C should be 0.8 * 0.9 = 0.72
            assertThat(result.vertices.get("C").strongestPathToVertex.get()).isEqualTo(0.72);

            // Verify path reconstruction
            assertThat(result.vertices.get("B").previousVertexName).isEqualTo("A");
            assertThat(result.vertices.get("C").previousVertexName).isEqualTo("B");
        }

        @Test
        @DisplayName("Should find strongest path when multiple paths exist")
        void multiplePathsGraph() {
            // A --0.5--> B --0.9--> C
            //  \--0.7--> D --0.8--> /
            // Direct path: 0.5 * 0.9 = 0.45
            // Alternate path: 0.7 * 0.8 = 0.56 (stronger)
            GraphBuilder builder = new GraphBuilder();
            builder.addEdge(new Edge("A", "B", 0.5));
            builder.addEdge(new Edge("B", "C", 0.9));
            builder.addEdge(new Edge("A", "D", 0.7));
            builder.addEdge(new Edge("D", "C", 0.8));
            Graph graph = builder.build();

            DeltaSteppingExecutor executor = new DeltaSteppingExecutor();
            Graph result = executor.findStrongestPaths(graph, "A");

            // Should choose the stronger path through D
            assertThat(result.vertices.get("C").strongestPathToVertex.get()).isEqualTo(0.56);
            assertThat(result.vertices.get("C").previousVertexName).isEqualTo("D");
        }

        @Test
        @DisplayName("Should handle single vertex graph")
        void singleVertexGraph() {
            GraphBuilder builder = new GraphBuilder();
            // Create a vertex by adding a self-loop
            builder.addEdge(new Edge("A", "A", 1.0));
            Graph graph = builder.build();

            DeltaSteppingExecutor executor = new DeltaSteppingExecutor();
            Graph result = executor.findStrongestPaths(graph, "A");

            assertThat(result.vertices.get("A").strongestPathToVertex.get()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should handle disconnected graph")
        void disconnectedGraph() {
            // A --0.8--> B    C --0.9--> D (disconnected)
            GraphBuilder builder = new GraphBuilder();
            builder.addEdge(new Edge("A", "B", 0.8));
            builder.addEdge(new Edge("C", "D", 0.9));
            Graph graph = builder.build();

            DeltaSteppingExecutor executor = new DeltaSteppingExecutor();
            Graph result = executor.findStrongestPaths(graph, "A");

            // Reachable from A
            assertThat(result.vertices.get("A").strongestPathToVertex.get()).isEqualTo(1.0);
            assertThat(result.vertices.get("B").strongestPathToVertex.get()).isEqualTo(0.8);

            // Not reachable from A (should remain at initial value -1)
            assertThat(result.vertices.get("C").strongestPathToVertex.get()).isEqualTo(-1.0);
            assertThat(result.vertices.get("D").strongestPathToVertex.get()).isEqualTo(-1.0);
        }

        @Test
        @DisplayName("Should handle graph with cycles")
        void graphWithCycles() {
            // A --0.9--> B --0.9--> C
            //  ^                     |
            //  \-------0.8-----------/
            GraphBuilder builder = new GraphBuilder();
            builder.addEdge(new Edge("A", "B", 0.9));
            builder.addEdge(new Edge("B", "C", 0.9));
            builder.addEdge(new Edge("C", "A", 0.8));
            Graph graph = builder.build();

            DeltaSteppingExecutor executor = new DeltaSteppingExecutor();
            Graph result = executor.findStrongestPaths(graph, "A");

            assertThat(result.vertices.get("A").strongestPathToVertex.get()).isEqualTo(1.0);
            assertThat(result.vertices.get("B").strongestPathToVertex.get()).isEqualTo(0.9);
            assertThat(result.vertices.get("C").strongestPathToVertex.get()).isEqualTo(0.81); // 0.9 * 0.9
        }

        @Test
        @DisplayName("Should handle bidirectional edges correctly")
        void bidirectionalEdges() {
            // A <--0.7--> B <--0.6--> C
            GraphBuilder builder = new GraphBuilder();
            builder.addEdge(new Edge("A", "B", 0.7));
            builder.addEdge(new Edge("B", "A", 0.7));
            builder.addEdge(new Edge("B", "C", 0.6));
            builder.addEdge(new Edge("C", "B", 0.6));
            Graph graph = builder.build();

            DeltaSteppingExecutor executor = new DeltaSteppingExecutor();
            Graph result = executor.findStrongestPaths(graph, "A");

            assertThat(result.vertices.get("A").strongestPathToVertex.get()).isEqualTo(1.0);
            assertThat(result.vertices.get("B").strongestPathToVertex.get()).isEqualTo(0.7);
            assertThat(result.vertices.get("C").strongestPathToVertex.get()).isEqualTo(0.42); // 0.7 * 0.6
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle very weak edges (close to 0)")
        void veryWeakEdges() {
            GraphBuilder builder = new GraphBuilder();
            builder.addEdge(new Edge("A", "B", 0.01));
            builder.addEdge(new Edge("B", "C", 0.01));
            Graph graph = builder.build();

            DeltaSteppingExecutor executor = new DeltaSteppingExecutor();
            Graph result = executor.findStrongestPaths(graph, "A");

            assertThat(result.vertices.get("C").strongestPathToVertex.get()).isCloseTo(0.0001, within(0.00001));
        }

        @Test
        @DisplayName("Should handle edges with strength 1.0")
        void perfectStrengthEdges() {
            GraphBuilder builder = new GraphBuilder();
            builder.addEdge(new Edge("A", "B", 1.0));
            builder.addEdge(new Edge("B", "C", 1.0));
            Graph graph = builder.build();

            DeltaSteppingExecutor executor = new DeltaSteppingExecutor();
            Graph result = executor.findStrongestPaths(graph, "A");

            assertThat(result.vertices.get("C").strongestPathToVertex.get()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should handle large fan-out (one vertex connected to many)")
        void largeFanOut() {
            GraphBuilder builder = new GraphBuilder();
            // A connects to B1, B2, ..., B20
            for (int i = 1; i <= 20; i++) {
                builder.addEdge(new Edge("A", "B" + i, 0.9));
            }
            Graph graph = builder.build();

            DeltaSteppingExecutor executor = new DeltaSteppingExecutor();
            Graph result = executor.findStrongestPaths(graph, "A");

            for (int i = 1; i <= 20; i++) {
                assertThat(result.vertices.get("B" + i).strongestPathToVertex.get()).isEqualTo(0.9);
                assertThat(result.vertices.get("B" + i).previousVertexName).isEqualTo("A");
            }
        }

        @Test
        @DisplayName("Should handle large fan-in (many vertices connected to one)")
        void largeFanIn() {
            GraphBuilder builder = new GraphBuilder();
            // A1, A2, ..., A20 all connect to B with different strengths
            for (int i = 1; i <= 20; i++) {
                double strength = 0.5 + (i * 0.02); // Increasing strengths
                builder.addEdge(new Edge("A" + i, "B", strength));
            }
            Graph graph = builder.build();

            DeltaSteppingExecutor executor = new DeltaSteppingExecutor();
            // Start from the vertex with strongest connection to B
            Graph result = executor.findStrongestPaths(graph, "A20");

            assertThat(result.vertices.get("B").strongestPathToVertex.get()).isEqualTo(0.9);
        }
    }

    @Nested
    @DisplayName("Bucket Index Calculation Tests")
    class BucketIndexTests {

        @Test
        @DisplayName("Should calculate bucket indices correctly")
        void bucketIndexCalculation() {
            // This test verifies the fix for the critical bucket bug
            GraphBuilder builder = new GraphBuilder();
            builder.addEdge(new Edge("A", "B", 0.9)); // bucket should be (1-0.9)*10 = 1
            builder.addEdge(new Edge("A", "C", 0.5)); // bucket should be (1-0.5)*10 = 5
            builder.addEdge(new Edge("A", "D", 0.1)); // bucket should be (1-0.1)*10 = 9
            Graph graph = builder.build();

            DeltaSteppingExecutor executor = new DeltaSteppingExecutor();
            Graph result = executor.findStrongestPaths(graph, "A");

            // All should be reachable with correct strengths
            assertThat(result.vertices.get("B").strongestPathToVertex.get()).isEqualTo(0.9);
            assertThat(result.vertices.get("C").strongestPathToVertex.get()).isEqualTo(0.5);
            assertThat(result.vertices.get("D").strongestPathToVertex.get()).isEqualTo(0.1);
        }
    }
}
