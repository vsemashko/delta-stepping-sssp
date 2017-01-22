package by.graph.writer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import by.graph.entity.Graph;
import by.graph.entity.SourceTarget;
import by.graph.entity.Vertex;

public class TargetWriter
{
    private final static Logger LOGGER = Logger.getLogger(TargetWriter.class);

    public static TargetWriter getWriter() {
        return new TargetWriter();
    }

    public void writeResult(Graph graph, SourceTarget sourceTarget, Path targetPath) {
        String sourceName = sourceTarget.sourceVertexName;

        try (BufferedWriter writer = Files.newBufferedWriter(targetPath)) {
            String resultText = getResultText(graph, sourceTarget, sourceName);
            writer.write(resultText);
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
        String expandedPath = getExpandedPathString(graph, targetVertex);
        double strongestPath = targetVertex.strongestPathToVertex.get();
        return String.format("%s %s %s: %s", sourceName, targetName, strongestPath, expandedPath);
    }

    private String getExpandedPathString(Graph graph, Vertex vertex) {
        List<Vertex> vertices = new ArrayList<>();
        while (vertex.previousVertexName != null) {
            vertices.add(vertex);
            vertex = graph.vertices.get(vertex.previousVertexName);
        }
        StringBuilder pathToVertex = new StringBuilder(vertex.name);
        for (int i = vertices.size() - 1; i >= 0; i--) {
            pathToVertex.append(String.format(" %s %s", vertices.get(i).strongestEdge, vertices.get(i).name));
        }
        return pathToVertex.toString();
    }
}
