package by.graph.parsers;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import by.graph.entity.SourceTarget;

public class SourceTargetParser
{
    final static Logger LOGGER = LogManager.getLogger(SourceTargetParser.class);

    public static SourceTargetParser getParser() {
        return new SourceTargetParser();
    }

    public SourceTarget parse(Path sourceTargetFile) {
        Set<String> targetVertexNames = new HashSet<>();
        String sourceVertexName = null;
        try (BufferedReader reader = Files.newBufferedReader(sourceTargetFile)) {
            sourceVertexName = reader.readLine();
            if (sourceVertexName == null || sourceVertexName.trim().isEmpty()) {
                throw new IllegalArgumentException("Source vertex name is missing or empty");
            }
            sourceVertexName = sourceVertexName.trim();

            reader.readLine(); // Skip delimiter line
            reader.lines()
                    .filter(line -> line != null && !line.trim().isEmpty())
                    .map(String::trim)
                    .forEach(targetVertexNames::add);
        }
        catch (IOException e) {
            LOGGER.error("Error during source target file parsing: " + e.getMessage(), e);
            throw new RuntimeException("Failed to parse source target file", e);
        }

        if (targetVertexNames.isEmpty()) {
            LOGGER.warn("No target vertices found in file");
        }

        return new SourceTarget(sourceVertexName, targetVertexNames);
    }
}
