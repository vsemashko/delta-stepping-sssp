package by.graph;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import by.graph.entity.Graph;
import by.graph.entity.SourceTarget;
import by.graph.parsers.GraphParser;
import by.graph.parsers.SourceTargetParser;
import by.graph.writer.TargetWriter;

public class Main
{
    public static void main(String[] args) {
        if (args.length != 3) {
            throw new RuntimeException("Incorrect input. Example params input: path_to_graph_file path_to_source_target_file target_path");
        }

        Path graphFile = Paths.get(args[0]);
        Path sourceTargetFile = Paths.get(args[1]);
        Path targetFile = Paths.get(args[2]);

        validateFileExistence(graphFile);
        validateFileExistence(sourceTargetFile);

        Graph graph = GraphParser.getParser().parse(graphFile);
        SourceTarget sourceTarget = SourceTargetParser.getParser().parse(sourceTargetFile);

        DeltaSteppingExecutor executor = new DeltaSteppingExecutor(graph, sourceTarget.sourceVertexName);
        Graph resultGraph = executor.findStrongestPaths();

        TargetWriter.getWriter().writeResult(resultGraph, sourceTarget, targetFile);
    }

    private static void validateFileExistence(Path file) {
        if (Files.notExists(file)) {
            throw new RuntimeException(String.format("Couldn't find file by specified path: %s", file));
        }
    }
}
