package visualization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import model.Graph;

public class GraphView extends Pane {

    private Graph graph;
    private int[] colors;
    private Map<Integer, double[]> positions;
    private int totalColors = 4;
    private boolean coloringInteractionEnabled = true;

    // hamiltonian highlights
    private Set<Integer> highlightedVertices = new HashSet<>();
    private Set<String> highlightedEdges = new HashSet<>();
    private List<int[]> highlightedDirections = new ArrayList<>();
    private Set<Integer> manualVertices = new HashSet<>();
    private Set<String> manualEdges = new HashSet<>();
    private Consumer<Integer> nodeClickHandler;

    // step-by-step coloring visualization state
    private int highlightedNode = -1;
    private Map<String, String> edgeColors = new HashMap<>();
    // step-by-step direction arrows (for Hamiltonian DFS path edges)
    private List<int[]> stepDirections = new ArrayList<>();

    // backtracking path visualization state
    private Set<Integer> pathNodes = new HashSet<>();
    // temporary node color overrides (for DP GREEN/RED flashes)
    private Map<Integer, String> tempNodeColors = new HashMap<>();
    private static final double PATH_RADIUS_BOOST = 3.0;

    // alert overlay (conflict / completed)
    private String alertText = null;
    private String alertColor = "red"; // "red" or "green"

    private static final Color[] COLOR_MAP = {
        Color.web("#bdbdbd"),  // 0 = uncolored (light grey)
        Color.web("#ff4d4d"),  // 1 = red
        Color.web("#4dff88"),  // 2 = green
        Color.web("#4da6ff"),  // 3 = blue
        Color.web("#f1c40f"),  // 4 = yellow
        Color.web("#9b59b6"),  // 5 = purple
        Color.web("#e67e22")   // 6 = orange
    };

    public GraphView(Graph graph, int[] colors) {
        this.graph = graph;
        this.colors = colors;
        this.positions = new HashMap<>();
        assignPositions();
        draw();
    }

    private void assignPositions() {
        double[][] fixed = {
            { 220, 130 }, // vertex 0
            { 370, 130 }, // vertex 1
            { 370, 280 }, // vertex 2
            { 220, 280 }, // vertex 3
            { 500, 180 }, // vertex 4
            { 500, 330 }, // vertex 5
            { 110, 180 }, // vertex 6
            { 110, 330 }  // vertex 7
        };
        List<Integer> verts = graph.getVertices();
        for (int i = 0; i < verts.size(); i++) {
            if (i < fixed.length) positions.put(verts.get(i), fixed[i]);
            else positions.put(verts.get(i), new double[]{ 100 + i * 80, 250 });
        }
    }

    /** Compute the effective radius for a given vertex ID. */
    private double effectiveRadius(int vertexId) {
        double base = 22;
        // Current node: NO size change. Only assigned/path nodes grow.
        if (pathNodes.contains(vertexId)) base += PATH_RADIUS_BOOST;
        return base;
    }

    private void draw() {
        // draw edges first — endpoints adjusted to node boundaries
        for (int u : graph.getVertices()) {
            for (int v : graph.getNeighbors(u)) {
                if (u < v) {
                    double[] pu = positions.get(u);
                    double[] pv = positions.get(v);

                    // Shorten line so it stops at node boundary
                    double dx = pv[0] - pu[0];
                    double dy = pv[1] - pu[1];
                    double len = Math.sqrt(dx * dx + dy * dy);
                    double ux_ = (len == 0) ? 0 : dx / len;
                    double uy_ = (len == 0) ? 0 : dy / len;
                    double rU = effectiveRadius(u);
                    double rV = effectiveRadius(v);
                    double x1 = pu[0] + ux_ * rU;
                    double y1 = pu[1] + uy_ * rU;
                    double x2 = pv[0] - ux_ * rV;
                    double y2 = pv[1] - uy_ * rV;

                    Line line = new Line(x1, y1, x2, y2);

                    String ekey = edgeKey(u, v);
                    String ecol = edgeColors.getOrDefault(ekey, "DEFAULT");

                    if (highlightedEdges.contains(ekey)) {
                        line.setStroke(Color.web("#00ff99"));
                        line.setStrokeWidth(4.5);
                    } else if (manualEdges.contains(ekey)) {
                        line.setStroke(Color.web("#ff8c00"));
                        line.setStrokeWidth(4.5);
                    } else if ("GREEN".equals(ecol)) {
                        line.setStroke(Color.web("#00ff99"));
                        line.setStrokeWidth(4);
                    } else if ("ORANGE".equals(ecol)) {
                        line.setStroke(Color.web("#ffa500"));
                        line.setStrokeWidth(4);
                    } else if ("RED".equals(ecol)) {
                        line.setStroke(Color.web("#ff3b3b"));
                        line.setStrokeWidth(4);
                    } else {
                        line.setStroke(Color.web("#888888"));
                        line.setStrokeWidth(2.5);
                    }
                    getChildren().add(line);
                }
            }
        }

        // draw direction arrows for Hamiltonian solution
        for (int[] dir : highlightedDirections) drawArrow(dir[0], dir[1]);
        // draw step-by-step DFS path arrows
        for (int[] dir : stepDirections) drawArrow(dir[0], dir[1]);

        // alert overlay (conflict / completed)
        if (alertText != null) {
            javafx.scene.control.Label alert = new javafx.scene.control.Label(alertText);
            alert.setFont(Font.font(null, FontWeight.BOLD, 14));
            if ("green".equals(alertColor)) {
                alert.setTextFill(Color.web("#00ff99"));
                alert.setStyle("-fx-background-color: rgba(0,60,30,0.82); -fx-background-radius: 6; -fx-border-color: #00ff99; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 5 14 5 14;");
            } else if ("blue".equals(alertColor)) {
                alert.setTextFill(Color.web("#4da6ff"));
                alert.setStyle("-fx-background-color: rgba(0,30,60,0.82); -fx-background-radius: 6; -fx-border-color: #4da6ff; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 5 14 5 14;");
            } else {
                alert.setTextFill(Color.web("#ff4d4d"));
                alert.setStyle("-fx-background-color: rgba(60,0,0,0.82); -fx-background-radius: 6; -fx-border-color: #ff4d4d; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 5 14 5 14;");
            }
            alert.setLayoutX(18);
            alert.setLayoutY(12);
            getChildren().add(alert);
        }

        // draw vertices
        List<Integer> verts = graph.getVertices();
        for (int i = 0; i < verts.size(); i++) {
            int id = verts.get(i);
            double[] pos = positions.get(id);

            Color fill = COLOR_MAP[0];
            if (colors != null && i < colors.length && colors[i] < COLOR_MAP.length)
                fill = COLOR_MAP[colors[i]];
            if (manualVertices.contains(id)) fill = Color.web("#f1c40f");
            if (highlightedVertices.contains(id)) fill = Color.web("#4da6ff");

            boolean isCurrentNode = (id == highlightedNode);
            boolean isPathNode    = pathNodes.contains(id);
            double radius = effectiveRadius(id);

            // If node is in path (DFS recursion stack / DP mask), override fill to blue
            if (isPathNode) {
                fill = Color.web("#4da6ff");
            }

            // Temporary node color override: border only (fill stays unchanged)
            String tempCol = tempNodeColors.get(id);

            Circle circle = new Circle(pos[0], pos[1], radius, fill);

            // Border priority: temp GREEN/RED border > orange (current) > default
            if ("GREEN".equals(tempCol)) {
                circle.setStroke(Color.web("#00ff99"));
                circle.setStrokeWidth(4.0);
                circle.setEffect(new javafx.scene.effect.DropShadow(10, Color.web("#00ff9988")));
            } else if ("RED".equals(tempCol)) {
                circle.setStroke(Color.web("#ff3b3b"));
                circle.setStrokeWidth(4.0);
                circle.setEffect(new javafx.scene.effect.DropShadow(10, Color.web("#ff3b3b88")));
            } else if (isCurrentNode) {
                circle.setStroke(Color.web("#ff8c00"));
                circle.setStrokeWidth(4.0);
                circle.setEffect(new javafx.scene.effect.DropShadow(10, Color.web("#ff8c0088")));
            } else {
                circle.setStroke(Color.web("#444444"));
                circle.setStrokeWidth(2);
                circle.setEffect(new javafx.scene.effect.DropShadow(6, Color.web("#00000088")));
            }

            final int index = i;
            circle.setOnMouseClicked(e -> {
                if (coloringInteractionEnabled) {
                    if (colors[index] == 0) colors[index] = 1;
                    else colors[index] = (colors[index] % totalColors) + 1;
                    redraw();
                    return;
                }
                if (nodeClickHandler != null) nodeClickHandler.accept(id);
            });

            Text label = new Text(pos[0] - 5, pos[1] + 5, String.valueOf(id));
            label.setFill(Color.web("#1a1a1a"));
            label.setFont(Font.font(null, FontWeight.BOLD, 14));
            label.setMouseTransparent(true);

            getChildren().addAll(circle, label);
        }
    }

    // --- alert overlay API ---

    public void showAlert(String text, String color) {
        this.alertText = text;
        this.alertColor = color == null ? "red" : color;
        redraw();
    }

    public void clearAlert() {
        this.alertText = null;
        redraw();
    }

    // --- visualization highlight API ---

    public void setHighlightedNode(int nodeId) {
        this.highlightedNode = nodeId;
        redraw();
    }

    public void setEdgeHighlight(int u, int v, String type) {
        edgeColors.put(edgeKey(u, v), type);
        redraw();
    }

    /** Set edge color AND track direction arrow for GREEN path edges. */
    public void setEdgeHighlightDirected(int from, int to, String type) {
        String key = edgeKey(from, to);
        String current = edgeColors.getOrDefault(key, "DEFAULT");

        // Safety: never overwrite a GREEN path edge with ORANGE or RED
        if ("GREEN".equals(current) && ("ORANGE".equals(type) || "RED".equals(type))) {
            return;
        }

        edgeColors.put(key, type);
        if ("GREEN".equals(type)) {
            stepDirections.add(new int[]{ from, to });
        } else {
            // Remove arrow when edge is reset (backtracking only)
            stepDirections.removeIf(d -> edgeKey(d[0], d[1]).equals(key));
        }
        redraw();
    }

    /** Set the current recursion path nodes (blue border + bigger size). */
    public void setPathNodes(List<Integer> nodeIds) {
        pathNodes.clear();
        if (nodeIds != null) pathNodes.addAll(nodeIds);
        redraw();
    }

    /** Clear all path node highlights. */
    public void clearPathNodes() {
        pathNodes.clear();
        redraw();
    }

    public void resetVisualizationHighlights() {
        highlightedNode = -1;
        edgeColors.clear();
        pathNodes.clear();
        stepDirections.clear();
        tempNodeColors.clear();
        redraw();
    }

    /** Set a temporary fill color override on a node (GREEN or RED). */
    public void setNodeTempColor(int nodeId, String color) {
        tempNodeColors.put(nodeId, color);
        redraw();
    }

    /** Clear temporary fill color override on a node. */
    public void clearNodeTempColor(int nodeId) {
        tempNodeColors.remove(nodeId);
        redraw();
    }

    /** Clear all temporary node color overrides. */
    public void clearAllNodeTempColors() {
        tempNodeColors.clear();
        redraw();
    }

    // --- standard API ---

    public void setColors(int[] newColors) {
        this.colors = newColors;
        redraw();
    }

    public void setTotalColors(int totalColors) { this.totalColors = totalColors; }

    public void setColoringInteractionEnabled(boolean enabled) {
        this.coloringInteractionEnabled = enabled;
        redraw();
    }

    public void setNodeClickHandler(Consumer<Integer> nodeClickHandler) {
        this.nodeClickHandler = nodeClickHandler;
    }

    public void showManualHamiltonianPath(List<Integer> selectedNodes, boolean closed) {
        manualVertices.clear();
        manualEdges.clear();
        if (selectedNodes == null || selectedNodes.isEmpty()) { redraw(); return; }
        manualVertices.addAll(selectedNodes);
        for (int i = 0; i < selectedNodes.size() - 1; i++)
            manualEdges.add(edgeKey(selectedNodes.get(i), selectedNodes.get(i + 1)));
        if (closed && selectedNodes.size() > 1)
            manualEdges.add(edgeKey(selectedNodes.get(selectedNodes.size() - 1), selectedNodes.get(0)));
        redraw();
    }

    public void clearManualHamiltonianPath() {
        manualVertices.clear(); manualEdges.clear(); redraw();
    }

    public void highlightHamiltonianCycle(List<Integer> cycle) {
        clearHamiltonianHighlight();
        if (cycle == null || cycle.size() < 2) { redraw(); return; }
        for (int n : cycle) highlightedVertices.add(n);
        for (int i = 0; i < cycle.size() - 1; i++) {
            int u = cycle.get(i), v = cycle.get(i + 1);
            highlightedEdges.add(edgeKey(u, v));
            highlightedDirections.add(new int[]{ u, v });
        }
        redraw();
    }

    public void clearHamiltonianHighlight() {
        highlightedVertices.clear();
        highlightedEdges.clear();
        highlightedDirections.clear();
        redraw();
    }

    private String edgeKey(int u, int v) {
        int a = Math.min(u, v), b = Math.max(u, v);
        return a + "-" + b;
    }

    private void drawArrow(int from, int to) {
        double[] p1 = positions.get(from), p2 = positions.get(to);
        if (p1 == null || p2 == null) return;
        double dx = p2[0] - p1[0], dy = p2[1] - p1[1];
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len == 0) return;
        double ux = dx / len, uy = dy / len;
        double tipX = p2[0] - ux * 24, tipY = p2[1] - uy * 24;
        double headLength = 12, headWidth = 7;
        double baseX = tipX - ux * headLength, baseY = tipY - uy * headLength;
        double px = -uy, py = ux;
        Line s1 = new Line(tipX, tipY, baseX + px * headWidth, baseY + py * headWidth);
        Line s2 = new Line(tipX, tipY, baseX - px * headWidth, baseY - py * headWidth);
        s1.setStroke(Color.web("#00ff99")); s1.setStrokeWidth(3);
        s2.setStroke(Color.web("#00ff99")); s2.setStrokeWidth(3);
        getChildren().addAll(s1, s2);
    }

    public void redraw() {
        getChildren().clear();
        draw();
    }
}
