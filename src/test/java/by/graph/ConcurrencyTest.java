package by.graph;

import by.graph.entity.Edge;
import by.graph.entity.Graph;
import by.graph.parsers.GraphBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Concurrency tests to detect deadlocks, livelocks, and race conditions.
 */
class ConcurrencyTest {

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Should not deadlock when processing graph with many edges")
    void noDeadlockOnComplexGraph() {
        GraphBuilder builder = new GraphBuilder();

        // Create a complex graph with many interconnections
        for (int i = 0; i < 50; i++) {
            for (int j = i + 1; j < 50; j++) {
                double strength = 0.5 + (Math.random() * 0.5);
                builder.addEdge(new Edge("V" + i, "V" + j, strength));
            }
        }

        Graph graph = builder.build();
        DeltaSteppingExecutor executor = new DeltaSteppingExecutor();

        // Should complete without deadlock
        Graph result = executor.findStrongestPaths(graph, "V0");

        assertThat(result).isNotNull();
        assertThat(result.vertices.get("V0").strongestPathToVertex.get()).isEqualTo(1.0);
    }

    @RepeatedTest(5)
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Should produce consistent results across multiple executions")
    void consistentResultsAcrossExecutions() {
        GraphBuilder builder = new GraphBuilder();

        // Build a deterministic graph
        builder.addEdge(new Edge("A", "B", 0.9));
        builder.addEdge(new Edge("A", "C", 0.8));
        builder.addEdge(new Edge("B", "D", 0.7));
        builder.addEdge(new Edge("C", "D", 0.6));
        builder.addEdge(new Edge("C", "E", 0.5));
        builder.addEdge(new Edge("D", "E", 0.4));

        Graph graph = builder.build();
        DeltaSteppingExecutor executor = new DeltaSteppingExecutor();

        // Run the algorithm
        Graph result = executor.findStrongestPaths(graph, "A");

        // Results should be consistent
        assertThat(result.vertices.get("D").strongestPathToVertex.get()).isEqualTo(0.63); // 0.9 * 0.7
        assertThat(result.vertices.get("E").strongestPathToVertex.get()).isEqualTo(0.4); // 0.5 * 0.8
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    @DisplayName("Should handle concurrent graph parsing safely")
    void concurrentGraphParsing() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Future<Graph>> futures = new ArrayList<>();

        // Parse multiple graphs concurrently
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            Future<Graph> future = executorService.submit(() -> {
                try {
                    GraphBuilder builder = new GraphBuilder();
                    for (int i = 0; i < 20; i++) {
                        builder.addEdge(new Edge("T" + threadId + "V" + i, "T" + threadId + "V" + (i + 1), 0.9));
                    }
                    return builder.build();
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        // Wait for all to complete
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // Verify all graphs were built successfully
        for (Future<Graph> future : futures) {
            assertThat(future.get()).isNotNull();
        }

        executorService.shutdown();
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    @DisplayName("Should not have race conditions when building graph with parallel edges")
    void noRaceConditionsInGraphBuilding() {
        GraphBuilder builder = new GraphBuilder();

        // Add many edges in parallel (GraphParser uses parallel streams)
        IntStream.range(0, 100).parallel().forEach(i -> {
            builder.addEdge(new Edge("A" + (i / 10), "B" + (i % 10), 0.5 + (i % 50) / 100.0));
        });

        Graph graph = builder.build();

        // Verify graph integrity
        assertThat(graph.vertices).isNotEmpty();
        assertThat(graph.edges).isNotEmpty();

        // Each vertex should have a unique ID
        long uniqueIds = graph.vertices.values().stream()
                .map(v -> v.id)
                .distinct()
                .count();
        assertThat(uniqueIds).isEqualTo(graph.vertices.size());
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Should handle graphs with many vertices efficiently")
    void largeGraphPerformance() {
        GraphBuilder builder = new GraphBuilder();

        // Create a graph with 1000 vertices in a chain
        for (int i = 0; i < 999; i++) {
            builder.addEdge(new Edge("V" + i, "V" + (i + 1), 0.999));
        }

        Graph graph = builder.build();
        DeltaSteppingExecutor executor = new DeltaSteppingExecutor();

        long startTime = System.currentTimeMillis();
        Graph result = executor.findStrongestPaths(graph, "V0");
        long endTime = System.currentTimeMillis();

        // Should complete reasonably quickly (within timeout)
        assertThat(endTime - startTime).isLessThan(10000);

        // Verify correctness
        assertThat(result.vertices.get("V0").strongestPathToVertex.get()).isEqualTo(1.0);
        assertThat(result.vertices.get("V999").strongestPathToVertex.get()).isGreaterThan(0.0);
    }

    @RepeatedTest(10)
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Should not have livelocks when processing cyclic graphs")
    void noLivelocksOnCyclicGraphs() {
        GraphBuilder builder = new GraphBuilder();

        // Create a graph with multiple cycles
        builder.addEdge(new Edge("A", "B", 0.9));
        builder.addEdge(new Edge("B", "C", 0.9));
        builder.addEdge(new Edge("C", "A", 0.9)); // Cycle 1

        builder.addEdge(new Edge("C", "D", 0.8));
        builder.addEdge(new Edge("D", "E", 0.8));
        builder.addEdge(new Edge("E", "C", 0.8)); // Cycle 2

        Graph graph = builder.build();
        DeltaSteppingExecutor executor = new DeltaSteppingExecutor();

        // Should terminate without livelock
        Graph result = executor.findStrongestPaths(graph, "A");

        assertThat(result).isNotNull();
        assertThat(result.vertices.get("A").strongestPathToVertex.get()).isEqualTo(1.0);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Should handle vertex relaxation without race conditions")
    void vertexRelaxationThreadSafety() throws InterruptedException {
        // Create a simple graph
        GraphBuilder builder = new GraphBuilder();
        builder.addEdge(new Edge("A", "B", 0.9));
        builder.addEdge(new Edge("A", "C", 0.8));
        Graph graph = builder.build();

        // Run the algorithm multiple times concurrently
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Future<Double>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            Future<Double> future = executorService.submit(() -> {
                try {
                    DeltaSteppingExecutor executor = new DeltaSteppingExecutor();
                    Graph result = executor.findStrongestPaths(graph, "A");
                    return result.vertices.get("B").strongestPathToVertex.get();
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        boolean completed = latch.await(8, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        // All executions should produce the same result
        for (Future<Double> future : futures) {
            assertThat(future.get()).isEqualTo(0.9);
        }

        executorService.shutdown();
    }
}
