public class Edge {
    public final String from;
    public String to;
    public double strength;

    public Edge(String from, String to, double strength) {
        this.from = from;
        this.to = to;
        this.strength = strength;
    }
}