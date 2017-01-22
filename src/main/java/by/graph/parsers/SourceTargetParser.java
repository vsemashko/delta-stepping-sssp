package by.graph.parsers;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

public class SourceTargetParser
{
    final static Logger LOGGER = Logger.getLogger(GraphParser.class);

    private Path sourceTargetFile;

    private String sourceVertexName;
    private Set<String> targetVertexNames;

    public SourceTargetParser(Path sourceTargetFile) {
        this.sourceTargetFile = sourceTargetFile;
    }

    public String getSourceVertexName() {
        if (sourceVertexName == null) {
            parseSourceTargetFile();
        }
        return sourceVertexName;
    }

    public Set<String> getTargetVertexNames() {
        if (targetVertexNames == null) {
            parseSourceTargetFile();
        }
        return targetVertexNames;
    }

    private void parseSourceTargetFile() {
        this.targetVertexNames = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(sourceTargetFile)) {
            sourceVertexName = reader.readLine();
            reader.readLine();
            reader.lines()
                    .forEach(targetVertexNames::add);
        }
        catch (IOException e) {
            LOGGER.error("Error during source target file parsing: " + e.getMessage(), e);
        }
    }
}
