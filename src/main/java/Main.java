import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        if (args.length != 3) {
            throw new RuntimeException("Incorrect input. Example params input: path_to_graph_file path_to_source_target_file target_path");
        }

        Path graphFile = Paths.get(args[0]);
        Path sourceFile = Paths.get(args[1]);
        Path targetFile = Paths.get(args[2]);

        validateFileExistence(graphFile, "graph");
        validateFileExistence(sourceFile, "source_target");

        List<Edge> edges = parseEdges(graphFile);

        System.out.println("QWERTY " + edges.size());
    }

    private static List<Edge> parseEdges(Path graphFile) {
        List<Edge> edges = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(graphFile)) {
            edges = reader.lines()
                    .map(parseEdgeLine())
                    .filter(edge -> edge != null)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return edges;
    }

    private static Function<String, Edge> parseEdgeLine() {
        return edgeLine -> {
            String[] edgeData = edgeLine.split("\\s+");
            if (edgeData.length != 3) {
                System.out.println(edgeLine);
                return null;
            }
            return new Edge(edgeData[0], edgeData[1], Double.parseDouble(edgeData[2]));
        };
    }

    private static void validateFileExistence(Path file, String fileType) {
        if (Files.notExists(file)) {
            throw new RuntimeException(String.format("Couldn't find %s file by specified path: %s", fileType, file));
        }
    }
}
