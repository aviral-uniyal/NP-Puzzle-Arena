package algorithms;

import model.Graph;
import java.util.ArrayList;
import java.util.List;

public class GraphColoring {

    private Graph graph;
    private int[] colors;
    private int numVertices;

    public int statesExplored = 0;
    public int conflicts = 0;

    public GraphColoring(Graph graph) {
        this.graph = graph;
        this.numVertices = graph.getVertices().size();
        this.colors = new int[numVertices];
    }


    public boolean solveColoring(int m) {
        for (int i = 0; i < numVertices; i++) colors[i] = 0;
        return solve(0, m);
    }

    private boolean solve(int vertex, int m) {
        if (vertex == numVertices) return true;
        int vertexId = graph.getVertices().get(vertex);
        for (int c = 1; c <= m; c++) {
            if (isSafe(vertexId, vertex, c)) {
                colors[vertex] = c;
                if (solve(vertex + 1, m)) return true;
                colors[vertex] = 0;
            }
        }
        return false;
    }

    public int greedyColoring() {
        for (int i = 0; i < numVertices; i++) colors[i] = 0;
        List<Integer> allVertices = graph.getVertices();
        int maxColor = 0;
        for (int i = 0; i < numVertices; i++) {
            int vertexId = allVertices.get(i);
            boolean[] used = new boolean[numVertices + 1];
            for (int neighbor : graph.getNeighbors(vertexId)) {
                int ni = allVertices.indexOf(neighbor);
                if (colors[ni] != 0) used[colors[ni]] = true;
            }
            for (int c = 1; c <= numVertices; c++) {
                if (!used[c]) { colors[i] = c; if (c > maxColor) maxColor = c; break; }
            }
        }
        return maxColor;
    }

    private boolean isSafe(int vertexId, int vertexIndex, int c) {
        List<Integer> neighbors = graph.getNeighbors(vertexId);
        List<Integer> allVertices = graph.getVertices();
        for (int neighbor : neighbors) {
            int ni = allVertices.indexOf(neighbor);
            if (colors[ni] == c) return false;
        }
        return true;
    }


    public boolean solveColoringVisual(int m, VisualizationCallback cb) {
        for (int i = 0; i < numVertices; i++) colors[i] = 0;
        statesExplored = 0;
        conflicts = 0;
        List<Integer> currentPath = new ArrayList<>();
        return solveVisual(0, m, currentPath, cb);
    }

    private boolean solveVisual(int vertex, int m, List<Integer> path, VisualizationCallback cb) {
        if (vertex == numVertices) return true;

        int vertexId = graph.getVertices().get(vertex);

        cb.onHighlightNode(vertexId);
        cb.onAction("Processing node " + vertexId, vertexId, statesExplored, conflicts);
        sleep(800, cb);

        for (int c = 1; c <= m; c++) {
            statesExplored++;
            cb.onAction("Trying Color " + c, vertexId, statesExplored, conflicts);

            colors[vertex] = c;
            cb.onColorUpdate(colors.clone());
            sleep(700, cb);

            boolean conflict = false;
            for (int neighbor : graph.getNeighbors(vertexId)) {
                int ni = graph.getVertices().indexOf(neighbor);

                cb.onEdgeHighlight(vertexId, neighbor, "ORANGE");
                cb.onComparingWith(neighbor);
                sleep(700, cb);

                if (colors[ni] == c) {
                    conflicts++;
                    cb.onEdgeHighlight(vertexId, neighbor, "RED");
                    cb.onAction("Conflict with node " + neighbor, vertexId, statesExplored, conflicts);
                    cb.onAlert("⚠  CONFLICT", "red");
                    sleep(1100, cb);
                    cb.onAlert(null, null); // clear alert
                    cb.onEdgeHighlight(vertexId, neighbor, "DEFAULT");
                    conflict = true;
                    break;
                } else {
                    cb.onEdgeHighlight(vertexId, neighbor, "DEFAULT");
                }
            }

            if (!conflict) {
                cb.onHighlightNode(-1);
                path.add(vertexId);
                cb.onPathUpdate(new ArrayList<>(path));
                sleep(400, cb);

                if (solveVisual(vertex + 1, m, path, cb)) {
                    return true;
                }

                path.remove(path.size() - 1);
                cb.onPathUpdate(new ArrayList<>(path));

                cb.onHighlightNode(vertexId);
            }
        }

        cb.onAlert("⚠  CONFLICT", "red");
        cb.onAction("All colors failed for node " + vertexId, vertexId, statesExplored, conflicts);
        sleep(900, cb);

        cb.onAlert("BACKTRACKING", "blue");
        cb.onAction("Backtracking from node " + vertexId, vertexId, statesExplored, conflicts);
        sleep(1000, cb);
        cb.onAlert(null, null);

        colors[vertex] = 0;
        cb.onColorUpdate(colors.clone());
        cb.onHighlightNode(-1);
        return false;
    }


    public int greedyColoringVisual(VisualizationCallback cb) {
        for (int i = 0; i < numVertices; i++) colors[i] = 0;
        statesExplored = 0;
        conflicts = 0;
        List<Integer> allVertices = graph.getVertices();
        int maxColor = 0;

        for (int i = 0; i < numVertices; i++) {
            int vertexId = allVertices.get(i);
            cb.onHighlightNode(vertexId);
            sleep(700, cb);

            int chosenColor = 0;
            for (int c = 1; c <= numVertices; c++) {
                statesExplored++;

                colors[i] = c;
                cb.onColorUpdate(colors.clone());
                cb.onAction("Trying Color " + c, vertexId, statesExplored, conflicts);
                sleep(500, cb);

                boolean hasConflict = false;
                for (int neighbor : graph.getNeighbors(vertexId)) {
                    int ni = allVertices.indexOf(neighbor);
                    cb.onEdgeHighlight(vertexId, neighbor, "ORANGE");
                    cb.onComparingWith(neighbor);
                    sleep(550, cb);

                    if (colors[ni] == c) {
                        conflicts++;
                        hasConflict = true;
                        cb.onEdgeHighlight(vertexId, neighbor, "RED");
                        cb.onAction("Conflict! Color " + c + " used by node " + neighbor, vertexId, statesExplored, conflicts);
                        cb.onAlert("CONFLICT", "red");
                        sleep(900, cb);
                        cb.onAlert(null, null);
                        cb.onEdgeHighlight(vertexId, neighbor, "DEFAULT");
                        break;
                    }

                    cb.onEdgeHighlight(vertexId, neighbor, "DEFAULT");
                }

                cb.onComparingWith(-1);
                if (!hasConflict) {
                    chosenColor = c;
                    break;
                }
            }

            if (chosenColor != 0) {
                if (chosenColor > maxColor) maxColor = chosenColor;
                cb.onAction("Assigning smallest valid color: " + chosenColor, vertexId, statesExplored, conflicts);
                cb.onColorUpdate(colors.clone());
                sleep(700, cb);
            }

            cb.onHighlightNode(-1);
        }
        return maxColor;
    }

    private void sleep(long ms, VisualizationCallback cb) {
        long deadline = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < deadline) {
            long remaining = deadline - System.currentTimeMillis();
            try { Thread.sleep(Math.min(50, Math.max(1, remaining))); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            cb.checkPauseOrStop();
        }
    }

    public int[] getColors() { return colors; }

    public void resetColors() {
        for (int i = 0; i < numVertices; i++) colors[i] = 0;
    }

    public interface VisualizationCallback {
        void onHighlightNode(int nodeId);
        void onEdgeHighlight(int u, int v, String type);
        void onColorUpdate(int[] colors);
        void onAction(String action, int currentNode, int states, int conflicts);
        void onComparingWith(int nodeId);
        void onPathUpdate(List<Integer> path);
        void onAlert(String message, String color); 
        void checkPauseOrStop();                    
    }
}
