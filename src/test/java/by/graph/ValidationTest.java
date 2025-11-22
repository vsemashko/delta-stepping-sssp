package by.graph;

import by.graph.entity.SourceTarget;
import by.graph.parsers.GraphParser;
import by.graph.parsers.SourceTargetParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for input validation and error handling
 */
class ValidationTest {

    @Test
    @DisplayName("Should reject empty source vertex name")
    void emptySourceVertex(@TempDir Path tempDir) throws IOException {
        Path sourceTargetFile = tempDir.resolve("source_target.txt");
        Files.write(sourceTargetFile, List.of(
                "",  // Empty source
                "---",
                "B"
        ));

        SourceTargetParser parser = SourceTargetParser.getParser();

        assertThatThrownBy(() -> parser.parse(sourceTargetFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source vertex name is missing or empty");
    }

    @Test
    @DisplayName("Should reject whitespace-only source vertex name")
    void whitespaceOnlySourceVertex(@TempDir Path tempDir) throws IOException {
        Path sourceTargetFile = tempDir.resolve("source_target.txt");
        Files.write(sourceTargetFile, List.of(
                "   ",  // Whitespace only
                "---",
                "B"
        ));

        SourceTargetParser parser = SourceTargetParser.getParser();

        assertThatThrownBy(() -> parser.parse(sourceTargetFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source vertex name is missing or empty");
    }

    @Test
    @DisplayName("Should trim whitespace from vertex names")
    void trimVertexNames(@TempDir Path tempDir) throws IOException {
        Path sourceTargetFile = tempDir.resolve("source_target.txt");
        Files.write(sourceTargetFile, List.of(
                "  A  ",  // Spaces around name
                "---",
                "  B  ",
                "  C  "
        ));

        SourceTargetParser parser = SourceTargetParser.getParser();
        SourceTarget result = parser.parse(sourceTargetFile);

        assertThat(result.sourceVertexName).isEqualTo("A");
        assertThat(result.targetVertexNames).containsExactlyInAnyOrder("B", "C");
    }

    @Test
    @DisplayName("Should filter out empty target lines")
    void filterEmptyTargetLines(@TempDir Path tempDir) throws IOException {
        Path sourceTargetFile = tempDir.resolve("source_target.txt");
        Files.write(sourceTargetFile, List.of(
                "A",
                "---",
                "B",
                "",  // Empty line
                "   ",  // Whitespace only
                "C"
        ));

        SourceTargetParser parser = SourceTargetParser.getParser();
        SourceTarget result = parser.parse(sourceTargetFile);

        assertThat(result.targetVertexNames).containsExactlyInAnyOrder("B", "C");
    }

    @Test
    @DisplayName("Should handle file with only source vertex")
    void onlySourceVertex(@TempDir Path tempDir) throws IOException {
        Path sourceTargetFile = tempDir.resolve("source_target.txt");
        Files.write(sourceTargetFile, List.of("A"));

        SourceTargetParser parser = SourceTargetParser.getParser();
        SourceTarget result = parser.parse(sourceTargetFile);

        assertThat(result.sourceVertexName).isEqualTo("A");
        assertThat(result.targetVertexNames).isEmpty();
    }

    @Test
    @DisplayName("Should handle negative edge weights")
    void negativeEdgeWeights(@TempDir Path tempDir) throws IOException {
        Path graphFile = tempDir.resolve("graph.txt");
        Files.write(graphFile, List.of(
                "A B 0.9",
                "B C -0.5",  // Negative weight
                "C D 0.8"
        ));

        GraphParser parser = GraphParser.getParser();
        var graph = parser.parse(graphFile);

        // Parser doesn't validate weights, but algorithm should handle it
        assertThat(graph.edges).hasSize(3);
    }

    @Test
    @DisplayName("Should handle zero edge weights")
    void zeroEdgeWeights(@TempDir Path tempDir) throws IOException {
        Path graphFile = tempDir.resolve("graph.txt");
        Files.write(graphFile, List.of(
                "A B 0.9",
                "B C 0.0",  // Zero weight
                "C D 0.8"
        ));

        GraphParser parser = GraphParser.getParser();
        var graph = parser.parse(graphFile);

        assertThat(graph.edges).hasSize(3);
    }

    @Test
    @DisplayName("Should handle weights greater than 1.0")
    void largeEdgeWeights(@TempDir Path tempDir) throws IOException {
        Path graphFile = tempDir.resolve("graph.txt");
        Files.write(graphFile, List.of(
                "A B 1.5",  // > 1.0
                "B C 2.0"
        ));

        GraphParser parser = GraphParser.getParser();
        var graph = parser.parse(graphFile);

        assertThat(graph.edges).hasSize(2);
    }

    @Test
    @DisplayName("Should handle very small edge weights")
    void verySmallEdgeWeights(@TempDir Path tempDir) throws IOException {
        Path graphFile = tempDir.resolve("graph.txt");
        Files.write(graphFile, List.of(
                "A B 0.000001",
                "B C 0.0001"
        ));

        GraphParser parser = GraphParser.getParser();
        var graph = parser.parse(graphFile);

        DeltaSteppingExecutor executor = new DeltaSteppingExecutor();
        var result = executor.findStrongestPaths(graph, "A");

        // Should handle small values without underflow
        assertThat(result.vertices.get("C").strongestPathToVertex.get()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Should handle duplicate edges between same vertices")
    void duplicateEdges(@TempDir Path tempDir) throws IOException {
        Path graphFile = tempDir.resolve("graph.txt");
        Files.write(graphFile, List.of(
                "A B 0.5",
                "A B 0.9",  // Duplicate with different weight
                "A B 0.7"   // Another duplicate
        ));

        GraphParser parser = GraphParser.getParser();
        var graph = parser.parse(graphFile);

        // All edges are added
        assertThat(graph.edges).hasSize(3);

        // Vertex should have all edges
        assertThat(graph.vertices.get("A").edges).hasSize(3);
    }

    @Test
    @DisplayName("Should handle self-loops")
    void selfLoops(@TempDir Path tempDir) throws IOException {
        Path graphFile = tempDir.resolve("graph.txt");
        Files.write(graphFile, List.of(
                "A A 0.9",  // Self-loop
                "A B 0.8"
        ));

        GraphParser parser = GraphParser.getParser();
        var graph = parser.parse(graphFile);

        assertThat(graph.edges).hasSize(2);
        assertThat(graph.vertices).hasSize(2);
    }

    @Test
    @DisplayName("Should handle graph with single vertex")
    void singleVertexGraph(@TempDir Path tempDir) throws IOException {
        Path graphFile = tempDir.resolve("graph.txt");
        Files.write(graphFile, List.of("A A 1.0"));  // Self-loop

        Path sourceTargetFile = tempDir.resolve("source_target.txt");
        Files.write(sourceTargetFile, List.of(
                "A",
                "---",
                "A"
        ));

        GraphParser graphParser = GraphParser.getParser();
        var graph = graphParser.parse(graphFile);

        SourceTargetParser stParser = SourceTargetParser.getParser();
        var sourceTarget = stParser.parse(sourceTargetFile);

        DeltaSteppingExecutor executor = new DeltaSteppingExecutor();
        var result = executor.findStrongestPaths(graph, "A");

        assertThat(result.vertices.get("A").strongestPathToVertex.get()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should handle unicode vertex names")
    void unicodeVertexNames(@TempDir Path tempDir) throws IOException {
        Path graphFile = tempDir.resolve("graph.txt");
        Files.write(graphFile, List.of(
                "北京 上海 0.9",
                "上海 深圳 0.8"
        ));

        GraphParser parser = GraphParser.getParser();
        var graph = parser.parse(graphFile);

        assertThat(graph.vertices).containsKeys("北京", "上海", "深圳");
    }

    @Test
    @DisplayName("Should handle very long vertex names")
    void longVertexNames(@TempDir Path tempDir) throws IOException {
        String longName = "A".repeat(1000);
        Path graphFile = tempDir.resolve("graph.txt");
        Files.write(graphFile, List.of(
                longName + " B 0.9"
        ));

        GraphParser parser = GraphParser.getParser();
        var graph = parser.parse(graphFile);

        assertThat(graph.vertices).containsKey(longName);
    }

    @Test
    @DisplayName("Should handle special characters in vertex names")
    void specialCharactersInNames(@TempDir Path tempDir) throws IOException {
        Path graphFile = tempDir.resolve("graph.txt");
        Files.write(graphFile, List.of(
                "A-1 B_2 0.9",
                "B_2 C.3 0.8"
        ));

        GraphParser parser = GraphParser.getParser();
        var graph = parser.parse(graphFile);

        assertThat(graph.vertices).containsKeys("A-1", "B_2", "C.3");
    }

    @Test
    @DisplayName("Should handle missing source target file gracefully")
    void missingFile(@TempDir Path tempDir) {
        Path nonExistent = tempDir.resolve("does_not_exist.txt");

        SourceTargetParser parser = SourceTargetParser.getParser();

        assertThatThrownBy(() -> parser.parse(nonExistent))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to parse source target file");
    }
}
