package by.graph.entity;

import java.util.Collections;
import java.util.Set;

public class SourceTarget
{
    public final String sourceVertexName;
    public final Set<String> targetVertexNames;

    public SourceTarget(String sourceVertexName, Set<String> targetVertexNames) {
        this.targetVertexNames = Collections.unmodifiableSet(targetVertexNames);
        this.sourceVertexName = sourceVertexName;
    }
}
