package by.graph.parsers;

import by.graph.entity.Graph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for graph parsing functionality.
 */
class GraphParserTest {

    @Test
    @DisplayName("Should parse valid graph file correctly")
    void parseValidGraphFile(@TempDir Path tempDir) throws IOException {
        Path graphFile = tempDir.resolve("test_graph.txt");
        Files.write(graphFile, java.util.Arrays.asList(
                "A B 0.9",
                "B C 0.8",
                "A C 0.7"
        ));

        GraphParser parser = GraphParser.getParser();
        Graph graph = parser.parse(graphFile);

        assertThat(graph.vertices).hasSize(3);
        assertThat(graph.vertices).containsKeys("A", "B", "C");
        assertThat(graph.edges).hasSize(3);
    }

    @Test
    @DisplayName("Should skip malformed lines")
    void skipMalformedLines(@TempDir Path tempDir) throws IOException {
        Path graphFile = tempDir.resolve("test_graph.txt");
        Files.write(graphFile, java.util.Arrays.asList(
                "A B 0.9",
                "INVALID LINE",
                "B C",
                "C D 0.8"
        ));

        GraphParser parser = GraphParser.getParser();
        Graph graph = parser.parse(graphFile);

        // Should parse only valid lines
        assertThat(graph.vertices).hasSize(4);
        assertThat(graph.edges).hasSize(2);
    }

    @Test
    @DisplayName("Should handle empty file")
    void handleEmptyFile(@TempDir Path tempDir) throws IOException {
        Path graphFile = tempDir.resolve("empty_graph.txt");
        Files.write(graphFile, java.util.Arrays.asList());

        GraphParser parser = GraphParser.getParser();
        Graph graph = parser.parse(graphFile);

        assertThat(graph.vertices).isEmpty();
        assertThat(graph.edges).isEmpty();
    }

    @Test
    @DisplayName("Should handle file with whitespace variations")
    void handleWhitespaceVariations(@TempDir Path tempDir) throws IOException {
        Path graphFile = tempDir.resolve("test_graph.txt");
        Files.write(graphFile, java.util.Arrays.asList(
                "A  B  0.9",
                "B\tC\t0.8",
                "C    D    0.7"
        ));

        GraphParser parser = GraphParser.getParser();
        Graph graph = parser.parse(graphFile);

        assertThat(graph.vertices).hasSize(4);
        assertThat(graph.edges).hasSize(3);
    }

    @Test
    @DisplayName("Should calculate delta correctly")
    void calculateDeltaCorrectly(@TempDir Path tempDir) throws IOException {
        Path graphFile = tempDir.resolve("test_graph.txt");
        Files.write(graphFile, java.util.Arrays.asList(
                "A B 0.8",
                "B C 0.6",
                "C D 1.0"
        ));

        GraphParser parser = GraphParser.getParser();
        Graph graph = parser.parse(graphFile);

        // Delta should be average of edge strengths: (0.8 + 0.6 + 1.0) / 3 = 0.8
        assertThat(graph.delta).isCloseTo(0.8, within(0.001));
    }

    @Test
    @DisplayName("Should handle duplicate edges")
    void handleDuplicateEdges(@TempDir Path tempDir) throws IOException {
        Path graphFile = tempDir.resolve("test_graph.txt");
        Files.write(graphFile, java.util.Arrays.asList(
                "A B 0.9",
                "A B 0.9",
                "B C 0.8"
        ));

        GraphParser parser = GraphParser.getParser();
        Graph graph = parser.parse(graphFile);

        assertThat(graph.vertices).hasSize(3);
        // Duplicate edges will be added
        assertThat(graph.edges).hasSize(3);
    }
}
