package by.graph;

import by.graph.entity.Graph;
import by.graph.entity.SourceTarget;
import by.graph.parsers.GraphParser;
import by.graph.parsers.SourceTargetParser;
import by.graph.writer.TargetWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * End-to-end integration tests for the entire SSSP pipeline
 */
class IntegrationTest {

    @Test
    @DisplayName("Should complete full pipeline: parse → compute → write")
    void fullPipeline(@TempDir Path tempDir) throws IOException {
        // Create test graph file
        Path graphFile = tempDir.resolve("graph.txt");
        Files.write(graphFile, List.of(
                "A B 0.9",
                "B C 0.8",
                "A C 0.7",
                "C D 0.6"
        ));

        // Create source/target file
        Path sourceTargetFile = tempDir.resolve("source_target.txt");
        Files.write(sourceTargetFile, List.of(
                "A",
                "---",
                "B",
                "C",
                "D"
        ));

        // Create output file
        Path outputFile = tempDir.resolve("output.txt");

        // Parse graph
        Graph graph = GraphParser.getParser().parse(graphFile);
        assertThat(graph.vertices).hasSize(4);

        // Parse source/target
        SourceTarget sourceTarget = SourceTargetParser.getParser().parse(sourceTargetFile);
        assertThat(sourceTarget.sourceVertexName).isEqualTo("A");
        assertThat(sourceTarget.targetVertexNames).containsExactlyInAnyOrder("B", "C", "D");

        // Run algorithm
        DeltaSteppingExecutor executor = new DeltaSteppingExecutor();
        Graph result = executor.findStrongestPaths(graph, sourceTarget.sourceVertexName);

        // Verify results
        assertThat(result.vertices.get("A").strongestPathToVertex.get()).isEqualTo(1.0);
        assertThat(result.vertices.get("B").strongestPathToVertex.get()).isEqualTo(0.9);
        assertThat(result.vertices.get("C").strongestPathToVertex.get()).isEqualTo(0.72); // 0.9 * 0.8
        assertThat(result.vertices.get("D").strongestPathToVertex.get()).isEqualTo(0.432); // 0.72 * 0.6

        // Write results
        TargetWriter.getWriter().writeResult(result, sourceTarget, outputFile);

        // Verify output file exists and has content
        assertThat(outputFile).exists();
        List<String> lines = Files.readAllLines(outputFile);
        assertThat(lines).hasSize(3);
        assertThat(lines).anyMatch(line -> line.contains("A B 0.9"));
        assertThat(lines).anyMatch(line -> line.contains("A C 0.72"));
        assertThat(lines).anyMatch(line -> line.contains("A D 0.432"));
    }

    @Test
    @DisplayName("Should handle disconnected components correctly")
    void disconnectedComponents(@TempDir Path tempDir) throws IOException {
        Path graphFile = tempDir.resolve("graph.txt");
        Files.write(graphFile, List.of(
                "A B 0.9",
                "C D 0.8"  // Separate component
        ));

        Path sourceTargetFile = tempDir.resolve("source_target.txt");
        Files.write(sourceTargetFile, List.of(
                "A",
                "---",
                "B",
                "C",
                "D"
        ));

        Path outputFile = tempDir.resolve("output.txt");

        Graph graph = GraphParser.getParser().parse(graphFile);
        SourceTarget sourceTarget = SourceTargetParser.getParser().parse(sourceTargetFile);

        DeltaSteppingExecutor executor = new DeltaSteppingExecutor();
        Graph result = executor.findStrongestPaths(graph, "A");

        // B is reachable, C and D are not
        assertThat(result.vertices.get("B").strongestPathToVertex.get()).isEqualTo(0.9);
        assertThat(result.vertices.get("C").strongestPathToVertex.get()).isEqualTo(-1.0);
        assertThat(result.vertices.get("D").strongestPathToVertex.get()).isEqualTo(-1.0);

        TargetWriter.getWriter().writeResult(result, sourceTarget, outputFile);

        List<String> lines = Files.readAllLines(outputFile);
        assertThat(lines).anyMatch(line -> line.contains("B 0.9"));
        assertThat(lines).anyMatch(line -> line.contains("C UNREACHABLE"));
        assertThat(lines).anyMatch(line -> line.contains("D UNREACHABLE"));
    }

    @Test
    @DisplayName("Should choose strongest path when multiple paths exist")
    void multiplePathSelection(@TempDir Path tempDir) throws IOException {
        Path graphFile = tempDir.resolve("graph.txt");
        Files.write(graphFile, List.of(
                "A B 0.5",
                "B C 0.9",  // Path 1: A→B→C = 0.45
                "A D 0.7",
                "D C 0.8"   // Path 2: A→D→C = 0.56 (stronger!)
        ));

        Path sourceTargetFile = tempDir.resolve("source_target.txt");
        Files.write(sourceTargetFile, List.of(
                "A",
                "---",
                "C"
        ));

        Graph graph = GraphParser.getParser().parse(graphFile);
        SourceTarget sourceTarget = SourceTargetParser.getParser().parse(sourceTargetFile);

        DeltaSteppingExecutor executor = new DeltaSteppingExecutor();
        Graph result = executor.findStrongestPaths(graph, "A");

        // Should choose path through D (stronger)
        assertThat(result.vertices.get("C").strongestPathToVertex.get()).isEqualTo(0.56);
        assertThat(result.vertices.get("C").previousVertexName).isEqualTo("D");
    }

    @Test
    @DisplayName("Should handle graph with cycles without infinite loop")
    void graphWithCycles(@TempDir Path tempDir) throws IOException {
        Path graphFile = tempDir.resolve("graph.txt");
        Files.write(graphFile, List.of(
                "A B 0.9",
                "B C 0.9",
                "C A 0.9"  // Creates cycle
        ));

        Path sourceTargetFile = tempDir.resolve("source_target.txt");
        Files.write(sourceTargetFile, List.of(
                "A",
                "---",
                "B",
                "C"
        ));

        Path outputFile = tempDir.resolve("output.txt");

        Graph graph = GraphParser.getParser().parse(graphFile);
        SourceTarget sourceTarget = SourceTargetParser.getParser().parse(sourceTargetFile);

        DeltaSteppingExecutor executor = new DeltaSteppingExecutor();
        Graph result = executor.findStrongestPaths(graph, "A");

        // Should complete without infinite loop
        assertThat(result.vertices.get("A").strongestPathToVertex.get()).isEqualTo(1.0);
        assertThat(result.vertices.get("B").strongestPathToVertex.get()).isEqualTo(0.9);
        assertThat(result.vertices.get("C").strongestPathToVertex.get()).isEqualTo(0.81);

        // Write should handle path reconstruction
        TargetWriter.getWriter().writeResult(result, sourceTarget, outputFile);
        assertThat(outputFile).exists();
    }

    @Test
    @DisplayName("Should handle empty target list")
    void emptyTargetList(@TempDir Path tempDir) throws IOException {
        Path graphFile = tempDir.resolve("graph.txt");
        Files.write(graphFile, List.of("A B 0.9"));

        Path sourceTargetFile = tempDir.resolve("source_target.txt");
        Files.write(sourceTargetFile, List.of(
                "A",
                "---"
        ));

        Path outputFile = tempDir.resolve("output.txt");

        Graph graph = GraphParser.getParser().parse(graphFile);
        SourceTarget sourceTarget = SourceTargetParser.getParser().parse(sourceTargetFile);

        assertThat(sourceTarget.targetVertexNames).isEmpty();

        DeltaSteppingExecutor executor = new DeltaSteppingExecutor();
        Graph result = executor.findStrongestPaths(graph, "A");

        TargetWriter.getWriter().writeResult(result, sourceTarget, outputFile);

        // Output should be empty or minimal
        List<String> lines = Files.readAllLines(outputFile);
        assertThat(lines).allMatch(String::isEmpty);
    }

    @Test
    @DisplayName("Should handle target vertex not in graph")
    void targetNotInGraph(@TempDir Path tempDir) throws IOException {
        Path graphFile = tempDir.resolve("graph.txt");
        Files.write(graphFile, List.of("A B 0.9"));

        Path sourceTargetFile = tempDir.resolve("source_target.txt");
        Files.write(sourceTargetFile, List.of(
                "A",
                "---",
                "B",
                "Z"  // Not in graph
        ));

        Path outputFile = tempDir.resolve("output.txt");

        Graph graph = GraphParser.getParser().parse(graphFile);
        SourceTarget sourceTarget = SourceTargetParser.getParser().parse(sourceTargetFile);

        DeltaSteppingExecutor executor = new DeltaSteppingExecutor();
        Graph result = executor.findStrongestPaths(graph, "A");

        TargetWriter.getWriter().writeResult(result, sourceTarget, outputFile);

        List<String> lines = Files.readAllLines(outputFile);
        assertThat(lines).anyMatch(line -> line.contains("B 0.9"));
        assertThat(lines).anyMatch(line -> line.contains("Z NOT_FOUND"));
    }

    @Test
    @DisplayName("Should handle very long paths")
    void veryLongPath(@TempDir Path tempDir) throws IOException {
        // Create a chain: A→B→C→D→...→Z
        StringBuilder graphContent = new StringBuilder();
        for (char c = 'A'; c < 'Z'; c++) {
            graphContent.append(c).append(" ").append((char)(c+1)).append(" 0.95\n");
        }

        Path graphFile = tempDir.resolve("graph.txt");
        Files.writeString(graphFile, graphContent.toString());

        Path sourceTargetFile = tempDir.resolve("source_target.txt");
        Files.write(sourceTargetFile, List.of(
                "A",
                "---",
                "Z"
        ));

        Graph graph = GraphParser.getParser().parse(graphFile);
        SourceTarget sourceTarget = SourceTargetParser.getParser().parse(sourceTargetFile);

        DeltaSteppingExecutor executor = new DeltaSteppingExecutor();
        Graph result = executor.findStrongestPaths(graph, "A");

        // Should find path through all vertices
        assertThat(result.vertices.get("Z").strongestPathToVertex.get()).isGreaterThan(0.0);
        assertThat(result.vertices.get("Z").previousVertexName).isNotNull();
    }

    @Test
    @DisplayName("Should handle malformed graph lines gracefully")
    void malformedGraphLines(@TempDir Path tempDir) throws IOException {
        Path graphFile = tempDir.resolve("graph.txt");
        Files.write(graphFile, List.of(
                "A B 0.9",
                "INVALID",
                "B C",  // Missing weight
                "C D 0.8",
                "D E not_a_number"
        ));

        Graph graph = GraphParser.getParser().parse(graphFile);

        // Should parse valid lines only
        assertThat(graph.vertices).containsKeys("A", "B", "C", "D");
        assertThat(graph.edges).hasSize(2); // Only A-B and C-D
    }

    @Test
    @DisplayName("Should handle source vertex not in graph")
    void sourceNotInGraph(@TempDir Path tempDir) throws IOException {
        Path graphFile = tempDir.resolve("graph.txt");
        Files.write(graphFile, List.of("A B 0.9"));

        Graph graph = GraphParser.getParser().parse(graphFile);

        DeltaSteppingExecutor executor = new DeltaSteppingExecutor();

        assertThatThrownBy(() -> executor.findStrongestPaths(graph, "Z"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source vertex not found");
    }
}
