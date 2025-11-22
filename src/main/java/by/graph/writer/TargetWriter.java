package by.graph.writer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import by.graph.entity.Graph;
import by.graph.entity.SourceTarget;
import by.graph.entity.Vertex;

public class TargetWriter
{
    private final static Logger LOGGER = LogManager.getLogger(TargetWriter.class);

    public static TargetWriter getWriter() {
        return new TargetWriter();
    }

    public void writeResult(Graph graph, SourceTarget sourceTarget, Path targetPath) {
        String sourceName = sourceTarget.sourceVertexName;

        try (BufferedWriter writer = Files.newBufferedWriter(targetPath)) {
            String resultText = getResultText(graph, sourceTarget, sourceName);
            writer.write(resultText);
            LOGGER.debug(resultText);
        }
        catch (IOException e) {
            LOGGER.error("Error during writing result: " + e.getMessage(), e);
        }
    }

    private String getResultText(Graph graph, SourceTarget sourceTarget, String sourceName) {
        StringBuilder result = new StringBuilder();
        sourceTarget.targetVertexNames.forEach(targetName -> {
            result.append(getResultLine(graph, sourceName, targetName));
            result.append(System.lineSeparator());
        });
        return result.toString();
    }

    private String getResultLine(Graph graph, String sourceName, String targetName) {
        Vertex targetVertex = graph.vertices.get(targetName);
        if (targetVertex == null) {
            LOGGER.warn("Target vertex not found: " + targetName);
            return String.format("%s %s NOT_FOUND: No path", sourceName, targetName);
        }

        double strongestPath = targetVertex.strongestPathToVertex.get();
        if (strongestPath < 0) {
            // Vertex exists but is unreachable from source
            return String.format("%s %s UNREACHABLE: No path", sourceName, targetName);
        }

        String expandedPath = getExpandedPathString(graph, targetVertex);
        return String.format("%s %s %s: %s", sourceName, targetName, strongestPath, expandedPath);
    }

    private String getExpandedPathString(Graph graph, Vertex vertex) {
        if (vertex == null) {
            return "null";
        }

        List<Vertex> vertices = new ArrayList<>();
        Vertex current = vertex;

        // Prevent infinite loops in case of cycle in path reconstruction
        int maxSteps = graph.vertices.size();
        int steps = 0;

        while (current.previousVertexName != null && steps < maxSteps) {
            vertices.add(current);
            current = graph.vertices.get(current.previousVertexName);
            if (current == null) {
                LOGGER.error("Path reconstruction failed: vertex not found");
                break;
            }
            steps++;
        }

        if (steps >= maxSteps) {
            LOGGER.error("Possible cycle detected in path reconstruction");
            return "ERROR: Cycle detected";
        }

        StringBuilder pathToVertex = new StringBuilder(current != null ? current.name : "ERROR");
        for (int i = vertices.size() - 1; i >= 0; i--) {
            pathToVertex.append(String.format(" %s %s", vertices.get(i).strongestEdge.get(), vertices.get(i).name));
        }
        return pathToVertex.toString();
    }
}
