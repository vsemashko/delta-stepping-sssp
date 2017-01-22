package by.graph.parsers;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import by.graph.entity.SourceTarget;

public class SourceTargetParser
{
    final static Logger LOGGER = Logger.getLogger(GraphParser.class);

    public static SourceTargetParser getParser() {
        return new SourceTargetParser();
    }

    public SourceTarget parse(Path sourceTargetFile) {
        Set<String> targetVertexNames = new HashSet<>();
        String sourceVertexName = null;
        try (BufferedReader reader = Files.newBufferedReader(sourceTargetFile)) {
            sourceVertexName = reader.readLine();
            reader.readLine();
            reader.lines()
                    .forEach(targetVertexNames::add);
        }
        catch (IOException e) {
            LOGGER.error("Error during source target file parsing: " + e.getMessage(), e);
        }
        return new SourceTarget(sourceVertexName, targetVertexNames);
    }
}
