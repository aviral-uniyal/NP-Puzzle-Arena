package algorithms;

import java.util.ArrayList;
import java.util.List;

public class HamiltonianSolver {

    public int statesExplored = 0;
    public int backtracks = 0;


    public List<Integer> solveHamiltonianBacktracking(int n, List<List<Integer>> graph) {
        statesExplored = 0;
        backtracks = 0;
        boolean[] visited = new boolean[n];
        List<Integer> path = new ArrayList<>();

        visited[0] = true;
        path.add(0);

        if (backtrack(n, graph, visited, path)) {
            path.add(0); 
            return path;
        }
        return new ArrayList<>();
    }

    private boolean backtrack(int n, List<List<Integer>> graph, boolean[] visited, List<Integer> path) {
        if (path.size() == n) {
            statesExplored++;
            int last = path.get(path.size() - 1);
            return graph.get(last).contains(0);
        }

        int current = path.get(path.size() - 1);

        for (int neighbor : graph.get(current)) {
            statesExplored++;
            if (!visited[neighbor]) {
                visited[neighbor] = true;
                path.add(neighbor);

                if (backtrack(n, graph, visited, path)) {
                    return true;
                }

                path.remove(path.size() - 1);
                visited[neighbor] = false;
                backtracks++;
            }
        }
        return false;
    }


    public List<Integer> solveHamiltonianDP(int n, List<List<Integer>> graph) {
        statesExplored = 0;
        backtracks = 0;
        int fullMask = (1 << n) - 1;
        Boolean[][] dp = new Boolean[1 << n][n];
        int[][] parent = new int[1 << n][n];

        for (int[] row : parent) {
            java.util.Arrays.fill(row, -1);
        }

        boolean found = dpSolve(0, 1, n, graph, dp, parent, fullMask);

        if (!found) {
            return new ArrayList<>();
        }

        return reconstructPath(n, parent, fullMask);
    }

    private boolean dpSolve(int current, int mask, int n, List<List<Integer>> graph,
                            Boolean[][] dp, int[][] parent, int fullMask) {
        if (mask == fullMask) {
            statesExplored++;
            return graph.get(current).contains(0);
        }

        if (dp[mask][current] != null) {
            return dp[mask][current];
        }

        for (int neighbor : graph.get(current)) {
            statesExplored++;
            if ((mask & (1 << neighbor)) != 0) continue;

            int newMask = mask | (1 << neighbor);
            if (dpSolve(neighbor, newMask, n, graph, dp, parent, fullMask)) {
                dp[mask][current] = true;
                parent[mask][current] = neighbor;
                return true;
            }
        }

        dp[mask][current] = false;
        return false;
    }


    public List<Integer> solveHamiltonianBacktrackingVisual(int n, List<List<Integer>> graph, VisualizationCallback cb) {
        statesExplored = 0;
        backtracks = 0;

        boolean[] visited = new boolean[n];
        List<Integer> path = new ArrayList<>();
        visited[0] = true;
        path.add(0);

        cb.onMaskUpdate("N/A");
        cb.onPathUpdate(new ArrayList<>(path));
        cb.onHighlightNode(-1);
        cb.onAction("Starting from node 0", 0, -1, statesExplored, backtracks);
        cb.onAlert(null, null);
        sleep(800, cb);

        if (backtrackVisual(n, graph, visited, path, -1, cb)) {
            List<Integer> cycle = new ArrayList<>(path);
            cycle.add(0);
            cb.onPathUpdate(new ArrayList<>(cycle));
            cb.onHighlightNode(-1);
            cb.onAlert("COMPLETED", "green");
            cb.onAction("Hamiltonian Cycle Found!", path.get(path.size() - 1), 0, statesExplored, backtracks);
            sleep(1100, cb);
            return cycle;
        }

        cb.onAlert(null, null);
        cb.onAction("No Hamiltonian Cycle Found", -1, -1, statesExplored, backtracks);
        cb.onHighlightNode(-1);
        return new ArrayList<>();
    }

    private boolean backtrackVisual(int n, List<List<Integer>> graph, boolean[] visited,
                                    List<Integer> path, int parent,
                                    VisualizationCallback cb) {
        cb.checkPauseOrStop();

        int current = path.get(path.size() - 1);
        cb.onHighlightNode(-1);
        cb.onAlert(null, null);
        sleep(800, cb);

        if (path.size() == n) {
            statesExplored++;
            cb.onHighlightNode(0);
            cb.onEdgeHighlight(current, 0, "ORANGE");
            cb.onAction("Checking return to node 0", current, 0, statesExplored, backtracks);
            sleep(800, cb);

            if (graph.get(current).contains(0)) {
                cb.onHighlightNode(-1);
                cb.onEdgeHighlight(current, 0, "GREEN");
                cb.onAction("Valid \u2014 cycle closes!", current, 0, statesExplored, backtracks);
                sleep(800, cb);
                return true;
            }

            cb.onHighlightNode(-1);
            cb.onEdgeHighlight(current, 0, "RED");
            cb.onAlert("NO CYCLE", "red");
            cb.onAction("No edge back to start node", current, 0, statesExplored, backtracks);
            sleep(1100, cb);
            cb.onEdgeHighlight(current, 0, "DEFAULT");
            cb.onAlert(null, null);
            return false;
        }

        List<Integer> candidates = new ArrayList<>();
        List<Integer> visitedCandidates = new ArrayList<>();

        for (int candidate : graph.get(current)) {
            if (candidate == parent) continue; 
            if (visited[candidate]) {
                visitedCandidates.add(candidate);
            } else {
                candidates.add(candidate);
            }
        }
        
        candidates.addAll(visitedCandidates);

        for (int candidate : candidates) {
            cb.checkPauseOrStop();

            statesExplored++;

            cb.onHighlightNode(candidate);
            cb.onEdgeHighlight(current, candidate, "ORANGE");
            cb.onAction("Trying node " + candidate, current, candidate, statesExplored, backtracks);
            sleep(800, cb);

            if (visited[candidate]) {
                cb.onEdgeHighlight(current, candidate, "RED");
                cb.onAction("Already visited node " + candidate, current, candidate, statesExplored, backtracks);
                sleep(1100, cb);
                cb.onEdgeHighlight(current, candidate, "DEFAULT");
                cb.onHighlightNode(-1);
                continue;
            }

            visited[candidate] = true;
            path.add(candidate);
            cb.onHighlightNode(-1);
            cb.onEdgeHighlight(current, candidate, "GREEN");
            cb.onPathUpdate(new ArrayList<>(path));
            cb.onAction("Added node " + candidate + " to path", candidate, -1, statesExplored, backtracks);
            sleep(800, cb);

            if (backtrackVisual(n, graph, visited, path, current, cb)) {
                return true;
            }

            backtracks++;
            cb.onAlert("BACKTRACKING", "blue");
            cb.onAction("Backtracking to node " + current, current, candidate, statesExplored, backtracks);
            sleep(800, cb);

            path.remove(path.size() - 1);
            visited[candidate] = false;
            cb.onPathUpdate(new ArrayList<>(path));
            cb.onEdgeHighlight(current, candidate, "DEFAULT");
            cb.onHighlightNode(-1);
            sleep(700, cb);
            cb.onAlert(null, null);
        }

        cb.onAlert("NO NEIGHBORS", "red");
        cb.onAction("No neighbors left for node " + current, current, -1, statesExplored, backtracks);
        sleep(800, cb);
        cb.onAlert(null, null);

        return false;
    }


    public List<Integer> solveHamiltonianDPVisual(int n, List<List<Integer>> graph, VisualizationCallback cb) {
        statesExplored = 0;
        backtracks = 0;

        int fullMask = (1 << n) - 1;
        Boolean[][] dp = new Boolean[1 << n][n];
        int[][] par = new int[1 << n][n];
        for (int[] row : par) java.util.Arrays.fill(row, -1);

        List<Integer> maskOrder = new ArrayList<>();
        maskOrder.add(0);

        cb.onHighlightNode(0);
        cb.onPathUpdate(maskToNodeList(1, n));
        cb.onMaskUpdate(orderedMaskString(maskOrder));
        cb.onAction("Initial state: node 0", 0, -1, statesExplored, 0);
        cb.onAlert(null, null);
        sleep(800, cb);

        boolean found = dpSolveVisual(0, 1, n, graph, dp, par, fullMask, maskOrder, cb);

        if (!found) {
            cb.onHighlightNode(-1);
            cb.onPathUpdate(new ArrayList<>());
            cb.onNodeTempColor(-1, "CLEAR_ALL");
            cb.onAlert("NO CYCLE POSSIBLE", "red");
            cb.onAction("No Hamiltonian Cycle", -1, -1, statesExplored, 0);
            sleep(1100, cb);
            return new ArrayList<>();
        }

        List<Integer> cycle = reconstructPath(n, par, fullMask);

        cb.onHighlightNode(-1);
        cb.onNodeTempColor(-1, "CLEAR_ALL");
        cb.onEdgeHighlight(-1, -1, "CLEAR_ALL");
        cb.onCycleFound(cycle);
        cb.onPathUpdate(cycle);

        StringBuilder cycleStr = new StringBuilder();
        for (int i = 0; i < cycle.size(); i++) {
            if (i > 0) cycleStr.append(" \u2192 ");
            cycleStr.append(cycle.get(i));
        }

        cb.onAlert("COMPLETED", "green");
        cb.onAction("Hamiltonian Cycle Found!\nCycle: " + cycleStr.toString(),
                     cycle.get(cycle.size() - 2), 0, statesExplored, 0);
        sleep(1100, cb);
        return cycle;
    }

    private boolean dpSolveVisual(int current, int mask, int n, List<List<Integer>> graph,
                                  Boolean[][] dp, int[][] par, int fullMask,
                                  List<Integer> maskOrder, VisualizationCallback cb) {
        cb.checkPauseOrStop();

        // Show current state
        cb.onHighlightNode(current);
        cb.onPathUpdate(maskToNodeList(mask, n));
        cb.onMaskUpdate(orderedMaskString(maskOrder));
        cb.onAlert(null, null);
        sleep(700, cb);

        if (dp[mask][current] != null) {
            statesExplored++;
            String result = dp[mask][current] ? "Memoized: \u2713" : "Memoized: \u2717";
            cb.onAlert("MEMOIZED", "blue");
            cb.onAction(result, current, -1, statesExplored, 0);
            sleep(500, cb);
            cb.onAlert(null, null);
            return dp[mask][current];
        }

        if (mask == fullMask) {
            statesExplored++;

            cb.onHighlightNode(0);
            cb.onEdgeHighlight(current, 0, "ORANGE");
            cb.onAction("Trying node 0", current, 0, statesExplored, 0);
            sleep(900, cb);

            if (graph.get(current).contains(0)) {
                cb.onEdgeHighlight(current, 0, "GREEN");
                cb.onNodeTempColor(0, "GREEN");
                cb.onAlert("STATE ACCEPTED", "green");
                cb.onAction("STATE ACCEPTED", current, 0, statesExplored, 0);
                sleep(1000, cb);
                cb.onEdgeHighlight(current, 0, "DEFAULT");
                cb.onNodeTempColor(0, null);
                cb.onAlert(null, null);
                cb.onHighlightNode(current);
                return true;
            }

            cb.onEdgeHighlight(current, 0, "RED");
            cb.onNodeTempColor(0, "RED");
            cb.onAlert("NO CYCLE POSSIBLE", "red");
            cb.onAction("NO CYCLE POSSIBLE", current, 0, statesExplored, 0);
            sleep(1100, cb);
            cb.onEdgeHighlight(current, 0, "DEFAULT");
            cb.onNodeTempColor(0, null);
            cb.onAlert(null, null);
            cb.onHighlightNode(current);
            return false;
        }

        for (int v = 0; v < n; v++) {
            if (v == current) continue;

            cb.checkPauseOrStop();
            statesExplored++;

            cb.onHighlightNode(v);
            cb.onEdgeHighlight(current, v, "ORANGE");
            cb.onAction("Trying node " + v, current, v, statesExplored, 0);
            sleep(900, cb);

            if ((mask & (1 << v)) != 0) {
                cb.onEdgeHighlight(current, v, "RED");
                cb.onNodeTempColor(v, "RED");
                cb.onAlert("ALREADY IN SET", "red");
                cb.onAction("ALREADY IN SET", current, v, statesExplored, 0);
                sleep(1000, cb);
                cb.onEdgeHighlight(current, v, "DEFAULT");
                cb.onNodeTempColor(v, null);
                cb.onAlert(null, null);
                cb.onHighlightNode(current);
                continue;
            }

            if (!graph.get(current).contains(v)) {
                cb.onEdgeHighlight(current, v, "RED");
                cb.onNodeTempColor(v, "RED");
                cb.onAlert("NO EDGE", "red");
                cb.onAction("NO EDGE", current, v, statesExplored, 0);
                sleep(1000, cb);
                cb.onEdgeHighlight(current, v, "DEFAULT");
                cb.onNodeTempColor(v, null);
                cb.onAlert(null, null);
                cb.onHighlightNode(current);
                continue;
            }

            cb.onEdgeHighlight(current, v, "GREEN");
            cb.onNodeTempColor(v, "GREEN");
            cb.onAlert("STATE ACCEPTED", "green");
            cb.onAction("STATE ACCEPTED", current, v, statesExplored, 0);
            sleep(1000, cb);
            cb.onEdgeHighlight(current, v, "DEFAULT");
            cb.onNodeTempColor(v, null);
            cb.onAlert(null, null);

            int newMask = mask | (1 << v);
            maskOrder.add(v);

            if (dpSolveVisual(v, newMask, n, graph, dp, par, fullMask, maskOrder, cb)) {
                dp[mask][current] = true;
                par[mask][current] = v;
                return true;
            }

            maskOrder.remove(maskOrder.size() - 1);
            cb.onHighlightNode(current);
            cb.onPathUpdate(maskToNodeList(mask, n));
            cb.onMaskUpdate(orderedMaskString(maskOrder));
            sleep(500, cb);
        }

        cb.onAlert("NO TRANSITIONS POSSIBLE", "orange");
        cb.onAction("NO TRANSITIONS POSSIBLE", current, -1, statesExplored, 0);
        sleep(1000, cb);
        cb.onAlert(null, null);

        dp[mask][current] = false;
        return false;
    }


    private String orderedMaskString(List<Integer> maskOrder) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < maskOrder.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(maskOrder.get(i));
        }
        sb.append("}");
        return sb.toString();
    }

    private String maskToNodeSet(int mask, int n) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (int i = 0; i < n; i++) {
            if ((mask & (1 << i)) != 0) {
                if (!first) sb.append(", ");
                sb.append(i);
                first = false;
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private List<Integer> maskToNodeList(int mask, int n) {
        List<Integer> nodes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if ((mask & (1 << i)) != 0) nodes.add(i);
        }
        return nodes;
    }

    private String maskToBinary(int mask, int n) {
        String bits = Integer.toBinaryString(mask);
        if (bits.length() >= n) return bits;
        StringBuilder sb = new StringBuilder();
        for (int i = bits.length(); i < n; i++) sb.append('0');
        sb.append(bits);
        return sb.toString();
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

    public interface VisualizationCallback {
        void onHighlightNode(int nodeId);
        void onEdgeHighlight(int u, int v, String type);
        void onAction(String action, int currentNode, int tryingNode, int states, int backtracks);
        void onPathUpdate(List<Integer> path);
        void onMaskUpdate(String mask);
        void onCycleFound(List<Integer> cycle);
        void onAlert(String message, String color);
        void onNodeTempColor(int nodeId, String color);
        void checkPauseOrStop();
    }

    private List<Integer> reconstructPath(int n, int[][] par, int fullMask) {
        List<Integer> path = new ArrayList<>();
        int current = 0;
        int mask = 1; 

        path.add(0);

        while (mask != fullMask) {
            int next = par[mask][current];
            path.add(next);
            mask = mask | (1 << next);
            current = next;
        }

        path.add(0); 
        return path;
    }
}
