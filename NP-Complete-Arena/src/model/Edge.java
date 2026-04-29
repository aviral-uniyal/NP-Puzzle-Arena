package model;

public class Edge {

    private int u;
    private int v;

    public Edge(int u, int v) {
        this.u = u;
        this.v = v;
    }

    public int getU() {
        return u;
    }

    public int getV() {
        return v;
    }

    @Override
    public String toString() {
        return "Edge(" + u + "," + v + ")";
    }
}
