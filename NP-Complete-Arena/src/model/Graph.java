package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Graph {

    private Map<Integer, List<Integer>> adj;
    private List<Integer> vertices;

    public Graph() {
        adj = new HashMap<>();
        vertices = new ArrayList<>();
    }

    public void addVertex(int id) {
        if (vertices.contains(id)) {
            return;
        }
        vertices.add(id);
        adj.put(id, new ArrayList<>());
    }

    public void addEdge(int u, int v) {
        if (u == v) {
            return;
        }
        addVertex(u);
        addVertex(v);
        List<Integer> nu = adj.get(u);
        List<Integer> nv = adj.get(v);
        if (nu.contains(v)) {
            return;
        }
        nu.add(v);
        nv.add(u);
    }

    public List<Integer> getNeighbors(int id) {
        List<Integer> n = adj.get(id);
        if (n == null) {
            return new ArrayList<>();
        }
        return n;
    }

    public List<Integer> getVertices() {
        return vertices;
    }

    public static Graph randomGraph(int n, int e) {
        Graph g = new Graph();
        Random r = new Random();
        for (int i = 0; i < n; i++) {
            g.addVertex(i);
        }
        int maxE = n * (n - 1) / 2;
        if (e > maxE) {
            e = maxE;
        }
        int added = 0;
        while (added < e) {
            int u = r.nextInt(n);
            int v = r.nextInt(n);
            if (u == v) {
                continue;
            }
            if (g.adj.get(u).contains(v)) {
                continue;
            }
            g.addEdge(u, v);
            added++;
        }
        return g;
    }
}
