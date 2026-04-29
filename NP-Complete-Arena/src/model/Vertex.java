package model;

public class Vertex {

    private int id;
    public double x;   // position for visualization
    public double y;

    public Vertex(int id) {
        this.id = id;
        this.x = 0;
        this.y = 0;
    }

    public int getId() {
        return id;
    }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    @Override
    public String toString() {
        return "Vertex(" + id + ")";
    }
}
