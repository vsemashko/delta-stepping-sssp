package by.graph;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import by.graph.parsers.GraphParser;
import by.graph.parsers.SourceTargetParser;

public class Main
{
    final static Logger LOGGER = Logger.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length != 3) {
            throw new RuntimeException("Incorrect input. Example params input: path_to_graph_file path_to_source_target_file target_path");
        }

        long start = System.currentTimeMillis();

        Path graphFile = Paths.get(args[0]);
        Path sourceFile = Paths.get(args[1]);
        Path targetFile = Paths.get(args[2]);

        validateFileExistence(graphFile, "graph");
        validateFileExistence(sourceFile, "source_target");

        Graph graph = GraphParser.getParser().parseGraph(graphFile);
        SourceTargetParser sourceTargetParser = new SourceTargetParser(sourceFile);
        graph.sourceVertexName = sourceTargetParser.getSourceVertexName();

        DeltaSteppingExecutor executor = new DeltaSteppingExecutor(graph);
        Graph resultGraph = executor.findStrongestPaths();

        sourceTargetParser.getTargetVertexNames().forEach(targetName -> {
            Vertex vertex = resultGraph.vertices.get(targetName);
            List<Vertex> vertices = new ArrayList<>();

            while (vertex.previousVertexName != null) {
                vertices.add(vertex);
                vertex = resultGraph.vertices.get(vertex.previousVertexName);
            }
            String pathToVertex = vertex.name;
            for (int i = vertices.size() - 1; i >= 0; i--) {
                pathToVertex += String.format(" %s %s", vertices.get(i).strongestEdge, vertices.get(i).name);
            }
            double strongestPath = resultGraph.vertices.get(targetName).strongestPathToVertex.get();
            System.out.println(String.format("%s %s %s: %s", graph.sourceVertexName, targetName, strongestPath, pathToVertex));
        });

        System.out.println("Result time = " + (System.currentTimeMillis() - start));
    }

    private static void validateFileExistence(Path file, String fileType) {
        if (Files.notExists(file)) {
            throw new RuntimeException(String.format("Couldn't find %s file by specified path: %s", fileType, file));
        }
    }
}
