package by.graph.entity;

public class Edge implements Comparable
{
    public final String from;
    public final String to;
    public final double strength;

    public Edge(String from, String to, double strength) {
        this.from = from;
        this.to = to;
        this.strength = strength;
    }

    public String getNeighbourName(String vertexName) {
        return from.equals(vertexName) ? to : from;
    }

    @Override
    public int compareTo(Object o) {
        return Double.compare(this.strength, ((Edge) o).strength);
    }
}
