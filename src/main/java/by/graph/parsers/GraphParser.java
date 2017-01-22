package by.graph.parsers;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import org.apache.log4j.Logger;

import by.graph.entity.Edge;
import by.graph.entity.Graph;

public class GraphParser
{
    private final static Logger LOGGER = Logger.getLogger(GraphParser.class);

    public static GraphParser getParser() {
        return new GraphParser();
    }

    public Graph parse(Path graphFile) {
        Graph graph = new Graph();
        try (BufferedReader reader = Files.newBufferedReader(graphFile)) {
            reader.lines()
                    .parallel()
                    .map(parseEdgeLine())
                    .filter(edge -> edge != null)
                    .forEach(graph::addEdge);
            graph.delta = graph.strengthSum.get() / graph.strengthCount.get();
        }
        catch (IOException e) {
            LOGGER.error("Error during edge graph parsing: " + e.getMessage(), e);
        }
        return graph;
    }

    private Function<String, Edge> parseEdgeLine() {
        return edgeLine -> {
            String[] edgeData = edgeLine.split("\\s+");
            if (edgeData.length != 3) {
                LOGGER.warn(String.format("Couldn't parse line, skipping: %s", edgeLine));
                return null;
            }
            return new Edge(edgeData[0], edgeData[1], Double.parseDouble(edgeData[2]));
        };
    }
}
