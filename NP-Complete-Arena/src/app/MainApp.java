package app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import algorithms.GraphColoring;
import algorithms.HamiltonianSolver;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.ListCell;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import model.Graph;
import visualization.GraphView;

public class MainApp extends Application {

    private Label lblAlgorithm, lblCurrentNode, lblAction, lblComparingWith;
    private Label lblCurrentPath, lblMask, lblStates, lblConflicts, lblStatus, lblTimeTaken, lblTimeComplexity;
    private Label rowComparingWithName, rowConflictsName;
    private Label lblGraphFeedback;

    private volatile boolean running = false;
    private volatile boolean isPaused = false;
    private volatile boolean isStopped = false;

    // thrown from checkPauseOrStop() to unwind the algorithm thread
    private static class StopVisualization extends RuntimeException {
    }

    @Override
    public void start(Stage stage) {
        Graph g = new Graph();
        for (int i = 0; i < 8; i++)
            g.addVertex(i);

        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(2, 3);
        g.addEdge(3, 0);
        g.addEdge(1, 4);
        g.addEdge(4, 5);
        g.addEdge(5, 2);
        g.addEdge(3, 6);
        g.addEdge(6, 7);
        g.addEdge(7, 0);
        g.addEdge(5, 7);
        g.addEdge(4, 6);

        GraphColoring gc = new GraphColoring(g);
        HamiltonianSolver hs = new HamiltonianSolver();
        int[] colors = gc.getColors();
        GraphView view = new GraphView(g, colors);
        view.setPrefSize(620, 460);
        view.setStyle("-fx-background-color: #2a2a2a;");

        lblGraphFeedback = new Label("");
        lblGraphFeedback.setFont(Font.font("System", FontWeight.BOLD, 22));
        lblGraphFeedback.setAlignment(Pos.CENTER);
        lblGraphFeedback.setMaxWidth(Double.MAX_VALUE);
        lblGraphFeedback.setPadding(new Insets(6, 8, 6, 8));
        lblGraphFeedback.setMinHeight(42);
        lblGraphFeedback.setPrefHeight(42);
        lblGraphFeedback.setMaxHeight(42);
        lblGraphFeedback.setText(" ");

        VBox leftArea = new VBox(6, lblGraphFeedback, view);
        leftArea.setPadding(new Insets(12, 12, 12, 12));
        leftArea.setStyle("-fx-background-color: #1e1e1e;");
        VBox.setVgrow(view, Priority.ALWAYS);

        VBox statusPanel = buildStatusPanel();
        statusPanel.setMinWidth(340);
        statusPanel.setPrefWidth(340);
        statusPanel.setMaxWidth(340);

        HBox centerArea = new HBox(0, leftArea, statusPanel);
        centerArea.setStyle("-fx-background-color: #1e1e1e;");
        HBox.setHgrow(leftArea, Priority.ALWAYS);

        Label statusLabel = new Label("Select a problem and run an algorithm");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-padding: 5px; -fx-text-fill: #aaaaaa;");

        ComboBox<String> problemBox = new ComboBox<>(
                FXCollections.observableArrayList("Graph Coloring", "Hamiltonian Cycle"));
        problemBox.setValue("Graph Coloring");

        // Style the dropdown list cells and the selected-value cell so text is visible
        problemBox.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                    setTextFill(Color.web("#ffffff"));
                    setStyle("-fx-background-color: #3a3a3a;");
                }
            };
            cell.setOnMouseEntered(ev -> cell.setStyle("-fx-background-color: #505050;"));
            cell.setOnMouseExited(ev -> cell.setStyle("-fx-background-color: #3a3a3a;"));
            return cell;
        });
        problemBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setTextFill(Color.web("#ffffff"));
            }
        });

        Button greedyBtn = new Button("▶ Run Greedy");
        Button backtrackBtn = new Button("▶ Run Backtracking");
        Button checkBtn = new Button("Check Solution");
        Button hamiltonianDfsBtn = new Button("▶ Run Backtracking");
        Button hamiltonianDpBtn = new Button("▶ Run Bitmask DP");
        Button validateBtn = new Button("Validate");
        Button resetBtn = new Button("Reset");
        Button undoBtn = new Button("Undo");

        Button pauseBtn = new Button("⏸  Pause");
        Button stopBtn = new Button("■  Stop");

        applyButtonStyle(
                greedyBtn, backtrackBtn, checkBtn,
                hamiltonianDfsBtn, hamiltonianDpBtn, validateBtn,
                resetBtn, undoBtn, pauseBtn, stopBtn);

        HBox buttonBar = new HBox(10);
        buttonBar.setPadding(new Insets(10, 12, 5, 12));

        HBox runBar = new HBox(10, pauseBtn, stopBtn);
        runBar.setPadding(new Insets(0, 12, 8, 12));
        runBar.setVisible(false);
        runBar.setManaged(false);

        HBox manualBar = new HBox(10, undoBtn);
        manualBar.setPadding(new Insets(0, 12, 10, 12));
        manualBar.setStyle("-fx-background-color: #1e1e1e;");
        manualBar.setMinHeight(42);
        manualBar.setPrefHeight(42);

        List<Integer> manualPath = new ArrayList<>();
        final boolean[] manualClosed = { false };
        final boolean[] manualValidated = { false };
        final boolean[] algorithmResultShown = { false };

        Runnable refreshManualView = () -> {
            view.showManualHamiltonianPath(manualPath, manualClosed[0]);
            boolean canUndo = "Hamiltonian Cycle".equals(problemBox.getValue())
                    && !running
                    && !algorithmResultShown[0]
                    && !manualValidated[0]
                    && (!manualPath.isEmpty() || manualClosed[0]);
            undoBtn.setDisable(!canUndo);
        };

        Runnable clearHamiltonianState = () -> {
            manualPath.clear();
            manualClosed[0] = false;
            manualValidated[0] = false;
            algorithmResultShown[0] = false;
            view.clearManualHamiltonianPath();
            view.clearHamiltonianHighlight();
            refreshManualView.run();
        };

        Runnable enableAllButtons = () -> {
            greedyBtn.setDisable(false);
            backtrackBtn.setDisable(false);
            checkBtn.setDisable(false);
            hamiltonianDfsBtn.setDisable(false);
            hamiltonianDpBtn.setDisable(false);
            validateBtn.setDisable(false);
            resetBtn.setDisable(false);
            // Re-enable manual node coloring when algorithm finishes
            if ("Graph Coloring".equals(problemBox.getValue())) {
                view.setColoringInteractionEnabled(true);
            }
            refreshManualView.run();
        };

        Runnable doStop = () -> {
            clearHamiltonianState.run();
            view.clearAlert();
            view.resetVisualizationHighlights();
            clearGraphFeedback();
            gc.resetColors();
            view.setColors(gc.getColors());
            resetStatusPanel("");
            setStatus("Stopped");
            setProblemHint(statusLabel, problemBox.getValue());
            runBar.setVisible(false);
            runBar.setManaged(false);
            pauseBtn.setText("⏸  Pause");
            isPaused = false;
            isStopped = false;
            running = false;
            // Re-enable manual node coloring on stop
            if ("Graph Coloring".equals(problemBox.getValue())) {
                view.setColoringInteractionEnabled(true);
            }
            enableAllButtons.run();
        };

        pauseBtn.setOnAction(e -> {
            if (!running)
                return;
            isPaused = !isPaused;
            pauseBtn.setText(isPaused ? "▶  Play" : "⏸  Pause");
            setStatus(isPaused ? "Paused" : "Running");
        });

        stopBtn.setOnAction(e -> {
            if (!running)
                return;
            isStopped = true;
            isPaused = false;
            pauseBtn.setText("⏸  Pause");
            setStatus("Stopped");
        });

        greedyBtn.setOnAction(e -> {
            if (running)
                return;
            running = true;
            isPaused = false;
            isStopped = false;

            greedyBtn.setDisable(true);
            backtrackBtn.setDisable(true);
            checkBtn.setDisable(true);
            hamiltonianDfsBtn.setDisable(true);
            hamiltonianDpBtn.setDisable(true);
            validateBtn.setDisable(true);
            resetBtn.setDisable(true);

            runBar.setVisible(true);
            runBar.setManaged(true);
            pauseBtn.setText("⏸  Pause");

            configureStatusPanelForGraphColoring();
            resetStatusPanel("Greedy");
            setStatus("Running");
            lblCurrentPath.setText("N/A");
            lblMask.setText("N/A");

            clearHamiltonianState.run();
            gc.resetColors();
            view.setColors(gc.getColors());
            view.clearAlert();
            view.resetVisualizationHighlights();
            clearGraphFeedback();
            // Disable manual node coloring while greedy runs
            view.setColoringInteractionEnabled(false);

            long startTime = System.nanoTime();
            setTimeTaken("calculating...");
            setTimeComplexity("calculating...");

            new Thread(() -> {
                try {
                    gc.greedyColoringVisual(makeColoringCallback(view));
                    long ms = (System.nanoTime() - startTime) / 1_000_000;
                    Platform.runLater(() -> {
                        view.resetVisualizationHighlights();
                        view.setColors(gc.getColors());

                        int used = maxColor(gc.getColors());
                        setAction("Completed, colored using " + used + " colors", -1, gc.statesExplored, gc.conflicts);
                        setTimeTaken(ms + " ms");
                        setTimeComplexity("O(V + E)");
                        setStatus("Completed");
                        setProblemHint(statusLabel, problemBox.getValue());
                        showGraphFeedback("COMPLETED", "COMPLETED");

                        runBar.setVisible(false);
                        runBar.setManaged(false);
                        pauseBtn.setText("⏸  Pause");
                        running = false;
                        isPaused = false;
                        isStopped = false;
                        enableAllButtons.run();
                    });
                } catch (StopVisualization ex) {
                    Platform.runLater(doStop);
                }
            }).start();
        });

        backtrackBtn.setOnAction(e -> {
            if (running)
                return;
            running = true;
            isPaused = false;
            isStopped = false;

            greedyBtn.setDisable(true);
            backtrackBtn.setDisable(true);
            checkBtn.setDisable(true);
            hamiltonianDfsBtn.setDisable(true);
            hamiltonianDpBtn.setDisable(true);
            validateBtn.setDisable(true);
            resetBtn.setDisable(true);

            runBar.setVisible(true);
            runBar.setManaged(true);
            pauseBtn.setText("⏸  Pause");

            configureStatusPanelForGraphColoring();
            resetStatusPanel("Backtracking");
            setStatus("Running");
            lblMask.setText("N/A");

            clearHamiltonianState.run();
            gc.resetColors();
            view.setColors(gc.getColors());
            view.clearAlert();
            view.resetVisualizationHighlights();
            clearGraphFeedback();
            // Disable manual node coloring while backtracking runs
            view.setColoringInteractionEnabled(false);

            int m = 3;
            int n = g.getVertices().size();
            long startTime = System.nanoTime();
            setTimeTaken("calculating...");
            setTimeComplexity("calculating...");

            new Thread(() -> {
                try {
                    boolean solved = gc.solveColoringVisual(m, makeColoringCallback(view));
                    long ms = (System.nanoTime() - startTime) / 1_000_000;
                    Platform.runLater(() -> {
                        view.resetVisualizationHighlights();
                        view.clearPathNodes();
                        view.setColors(gc.getColors());
                        String result = solved
                                ? "Completed, coloring with m=" + m + " colors"
                                : "No solution with m=" + m;
                        setAction(result, -1, gc.statesExplored, gc.conflicts);
                        setTimeTaken(ms + " ms");
                        setTimeComplexity("O(m^n)  [m=" + m + ", n=" + n + "]");
                        setStatus("Completed");

                        if (solved) {
                            setProblemHint(statusLabel, problemBox.getValue());
                            showGraphFeedback("COMPLETED", "COMPLETED");
                        } else {
                            statusLabel.setText("No solution (m=" + m + ")");
                            clearGraphFeedback();
                        }

                        runBar.setVisible(false);
                        runBar.setManaged(false);
                        pauseBtn.setText("⏸  Pause");
                        running = false;
                        isPaused = false;
                        isStopped = false;
                        enableAllButtons.run();
                    });
                } catch (StopVisualization ex) {
                    Platform.runLater(doStop);
                }
            }).start();
        });

        checkBtn.setOnAction(e -> statusLabel.setText(validateColoring(g, gc.getColors())));

        hamiltonianDfsBtn.setOnAction(e -> {
            if (running)
                return;
            running = true;
            isPaused = false;
            isStopped = false;

            greedyBtn.setDisable(true);
            backtrackBtn.setDisable(true);
            checkBtn.setDisable(true);
            hamiltonianDfsBtn.setDisable(true);
            hamiltonianDpBtn.setDisable(true);
            validateBtn.setDisable(true);
            resetBtn.setDisable(true);
            undoBtn.setDisable(true);

            runBar.setVisible(true);
            runBar.setManaged(true);
            pauseBtn.setText("⏸  Pause");

            configureStatusPanelForHamiltonianBacktracking();
            resetStatusPanel("DFS + Backtracking");
            setStatus("Running");
            setTimeTaken("calculating...");
            setTimeComplexity("calculating...");
            statusLabel.setText("Running DFS + Backtracking...");

            clearHamiltonianState.run();
            gc.resetColors();
            view.setColors(gc.getColors());
            view.clearAlert();
            view.resetVisualizationHighlights();
            clearGraphFeedback();

            List<Integer> vertices = g.getVertices();
            HamiltonianSolver.VisualizationCallback callback = makeHamiltonianCallback(view, vertices, true);

            long startTime = System.nanoTime();
            new Thread(() -> {
                try {
                    List<Integer> cycle = runHamiltonianSolverVisual(g, hs, true, callback);
                    long ms = (System.nanoTime() - startTime) / 1_000_000;
                    Platform.runLater(() -> {
                        if (cycle.isEmpty()) {
                            // No solution — clear everything
                            view.clearPathNodes();
                            view.resetVisualizationHighlights();
                            setHamiltonianAction("No Hamiltonian Cycle Found", -1, -1, hs.statesExplored,
                                    hs.backtracks);
                            statusLabel.setText("No Hamiltonian Cycle Found");
                            clearGraphFeedback();
                        } else {
                            // Cycle found — keep step-by-step visualization as final state
                            algorithmResultShown[0] = true;
                            lblCurrentPath.setText(cycle.toString());
                            int current = cycle.size() > 1 ? cycle.get(cycle.size() - 2) : cycle.get(0);
                            setHamiltonianAction("Hamiltonian Cycle Found!", current, cycle.get(0), hs.statesExplored,
                                    hs.backtracks);
                            statusLabel.setText("Hamiltonian Cycle: " + cycle);
                            showGraphFeedback("COMPLETED", "COMPLETED");
                        }

                        setTimeTaken(ms + " ms");
                        setTimeComplexity("O(n!)");
                        setStatus("Completed");

                        runBar.setVisible(false);
                        runBar.setManaged(false);
                        pauseBtn.setText("⏸  Pause");
                        running = false;
                        isPaused = false;
                        isStopped = false;
                        enableAllButtons.run();
                    });
                } catch (StopVisualization ex) {
                    Platform.runLater(doStop);
                }
            }).start();
        });

        hamiltonianDpBtn.setOnAction(e -> {
            if (running)
                return;
            running = true;
            isPaused = false;
            isStopped = false;

            greedyBtn.setDisable(true);
            backtrackBtn.setDisable(true);
            checkBtn.setDisable(true);
            hamiltonianDfsBtn.setDisable(true);
            hamiltonianDpBtn.setDisable(true);
            validateBtn.setDisable(true);
            resetBtn.setDisable(true);
            undoBtn.setDisable(true);

            runBar.setVisible(true);
            runBar.setManaged(true);
            pauseBtn.setText("⏸  Pause");

            configureStatusPanelForHamiltonianDp();
            resetStatusPanel("Hamiltonian (Bitmask DP)");
            setStatus("Running");
            lblCurrentPath.setText("N/A");
            setTimeTaken("calculating...");
            setTimeComplexity("calculating...");
            statusLabel.setText("Running Hamiltonian (Bitmask DP)...");

            clearHamiltonianState.run();
            gc.resetColors();
            view.setColors(gc.getColors());
            view.clearAlert();
            view.resetVisualizationHighlights();
            clearGraphFeedback();

            List<Integer> vertices = g.getVertices();
            HamiltonianSolver.VisualizationCallback callback = makeHamiltonianCallback(view, vertices, false);

            long startTime = System.nanoTime();
            new Thread(() -> {
                try {
                    List<Integer> cycle = runHamiltonianSolverVisual(g, hs, false, callback);
                    long ms = (System.nanoTime() - startTime) / 1_000_000;
                    Platform.runLater(() -> {
                        if (cycle.isEmpty()) {
                            view.clearPathNodes();
                            view.resetVisualizationHighlights();
                            setHamiltonianAction("No Hamiltonian Cycle Found", -1, -1, hs.statesExplored,
                                    hs.backtracks);
                            statusLabel.setText("No Hamiltonian Cycle Found");
                            clearGraphFeedback();
                        } else {
                            // Don't reset — the solver already set up permanent cycle display
                            algorithmResultShown[0] = true;
                            // Build cycle string: "0 → 1 → 2 → ... → 0"
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < cycle.size(); i++) {
                                if (i > 0) sb.append(" \u2192 ");
                                sb.append(cycle.get(i));
                            }
                            int current = cycle.size() > 1 ? cycle.get(cycle.size() - 2) : cycle.get(0);
                            setHamiltonianAction("Hamiltonian Cycle Found!\nCycle: " + sb.toString(),
                                    current, cycle.get(0), hs.statesExplored, hs.backtracks);
                            statusLabel.setText("Hamiltonian Cycle: " + sb.toString());
                            showGraphFeedback("COMPLETED", "COMPLETED");
                        }

                        setTimeTaken(ms + " ms");
                        setTimeComplexity("O(n^2 * 2^n)");
                        setStatus("Completed");

                        runBar.setVisible(false);
                        runBar.setManaged(false);
                        pauseBtn.setText("⏸  Pause");
                        running = false;
                        isPaused = false;
                        isStopped = false;
                        enableAllButtons.run();
                    });
                } catch (StopVisualization ex) {
                    Platform.runLater(doStop);
                }
            }).start();
        });

        validateBtn.setOnAction(e -> {
            if (!"Hamiltonian Cycle".equals(problemBox.getValue()) || algorithmResultShown[0]) {
                statusLabel.setText("Invalid or Incomplete Hamiltonian Cycle");
                return;
            }
            if (!isValidManualHamiltonianCycle(g, manualPath, manualClosed[0])) {
                statusLabel.setText("Invalid or Incomplete Hamiltonian Cycle");
                return;
            }
            List<Integer> cycle = new ArrayList<>(manualPath);
            cycle.add(manualPath.get(0));
            manualValidated[0] = true;
            view.clearManualHamiltonianPath();
            view.highlightHamiltonianCycle(cycle);
            statusLabel.setText("Valid Hamiltonian Cycle: " + cycle);
            showGraphFeedback("COMPLETED", "COMPLETED");
            refreshManualView.run();
        });

        undoBtn.setOnAction(e -> {
            if (!"Hamiltonian Cycle".equals(problemBox.getValue()))
                return;
            if (running || algorithmResultShown[0] || manualValidated[0])
                return;
            if (manualClosed[0]) {
                manualClosed[0] = false;
                view.showManualHamiltonianPath(manualPath, false);
                if (!manualPath.isEmpty())
                    statusLabel.setText("Start: " + manualPath.get(0));
                refreshManualView.run();
                return;
            }
            if (manualPath.isEmpty())
                return;
            manualPath.remove(manualPath.size() - 1);
            view.showManualHamiltonianPath(manualPath, false);
            statusLabel.setText(manualPath.isEmpty() ? "Click nodes to build path" : "Start: " + manualPath.get(0));
            refreshManualView.run();
        });

        resetBtn.setOnAction(e -> {
            if (running)
                return;
            clearHamiltonianState.run();
            gc.resetColors();
            view.setColors(gc.getColors());
            view.resetVisualizationHighlights();
            view.clearAlert();
            clearGraphFeedback();
            resetStatusPanel("");
            setProblemHint(statusLabel, problemBox.getValue());
            isPaused = false;
            isStopped = false;
            pauseBtn.setText("⏸  Pause");
            // Re-enable manual node coloring on reset
            if ("Graph Coloring".equals(problemBox.getValue())) {
                view.setColoringInteractionEnabled(true);
            }
        });

        Runnable refreshControls = () -> {
            String mode = problemBox.getValue();
            buttonBar.getChildren().clear();
            if ("Graph Coloring".equals(mode)) {
                configureStatusPanelForGraphColoring();
                view.setColoringInteractionEnabled(true);
                view.setNodeClickHandler(null);
                buttonBar.getChildren().addAll(greedyBtn, backtrackBtn, checkBtn, resetBtn);
                manualBar.setVisible(false);
                manualBar.setManaged(true);
                statusLabel.setText("Click nodes to color, or run an algorithm");
            } else {
                configureStatusPanelForHamiltonianBacktracking();
                view.setColoringInteractionEnabled(false);
                view.setNodeClickHandler(nodeId -> {
                    if (running || algorithmResultShown[0] || manualValidated[0])
                        return;
                    if (manualPath.isEmpty()) {
                        manualPath.add(nodeId);
                        view.showManualHamiltonianPath(manualPath, false);
                        statusLabel.setText("Start: " + nodeId);
                        refreshManualView.run();
                        return;
                    }
                    int first = manualPath.get(0);
                    int last = manualPath.get(manualPath.size() - 1);
                    if (nodeId == first) {
                        if (manualPath.size() == g.getVertices().size() && isConnected(g, last, first)) {
                            manualClosed[0] = true;
                            view.showManualHamiltonianPath(manualPath, true);
                            statusLabel.setText("Cycle closed. Click Validate");
                        } else
                            statusLabel.setText("Invalid move");
                        refreshManualView.run();
                        return;
                    }
                    if (manualPath.contains(nodeId) || !isConnected(g, last, nodeId)) {
                        statusLabel.setText("Invalid move");
                        refreshManualView.run();
                        return;
                    }
                    manualPath.add(nodeId);
                    view.showManualHamiltonianPath(manualPath, false);
                    statusLabel.setText("Start: " + first);
                    refreshManualView.run();
                });
                buttonBar.getChildren().addAll(hamiltonianDfsBtn, hamiltonianDpBtn, validateBtn, resetBtn);
                manualBar.setVisible(true);
                manualBar.setManaged(true);
                refreshManualView.run();
                statusLabel.setText("Click nodes to build a path, or run an algorithm");
            }
        };

        problemBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (running)
                return;
            clearHamiltonianState.run();
            gc.resetColors();
            view.setColors(gc.getColors());
            view.resetVisualizationHighlights();
            view.clearAlert();
            clearGraphFeedback();
            resetStatusPanel("");
            setProblemHint(statusLabel, newValue);
            refreshControls.run();
        });

        refreshControls.run();

        Label problemLabel = new Label("Problem:");
        problemLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 13px;");
        problemBox.setStyle(
                "-fx-background-color: #3a3a3a; -fx-text-fill: #ffffff; -fx-border-color: #555555; -fx-border-radius: 3; -fx-background-radius: 3;");
        HBox problemBar = new HBox(10, problemLabel, problemBox, statusLabel);
        problemBar.setPadding(new Insets(12, 12, 0, 12));
        problemBar.setAlignment(Pos.CENTER_LEFT);
        problemBar.setStyle("-fx-background-color: #1e1e1e;");

        buttonBar.setStyle("-fx-background-color: #1e1e1e;");
        runBar.setStyle("-fx-background-color: #1e1e1e;");
        VBox topBar = new VBox(problemBar, buttonBar, runBar);
        topBar.setStyle("-fx-background-color: #1e1e1e;");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1e1e1e;");
        root.setTop(topBar);
        root.setCenter(centerArea);
        root.setBottom(manualBar);

        Scene scene = new Scene(root, 960, 700);
        stage.setTitle("NP Puzzle Arena");
        stage.setScene(scene);
        stage.show();
    }

    private VBox buildStatusPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(14, 14, 14, 14));
        panel.setStyle(
                "-fx-background-color: #252525; -fx-border-color: #3a3a3a; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label title = new Label("Algorithm Status");
        title.setFont(Font.font("System", FontWeight.BOLD, 15));
        title.setTextFill(Color.web("#ffffff"));
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);

        lblAlgorithm = makeStatLabel("—");
        lblCurrentNode = makeStatLabel("—");
        lblAction = makeStatLabel("—");
        lblComparingWith = makeStatLabel("—");
        lblCurrentPath = makeStatLabel("—");
        lblMask = makeStatLabel("N/A");
        lblStates = makeStatLabel("0");
        lblConflicts = makeStatLabel("0");
        lblStatus = makeStatLabel("Idle");
        lblTimeTaken = makeStatLabel("—");
        lblTimeComplexity = makeStatLabel("—");

        rowComparingWithName = makeRowNameLabel("Comparing With");
        rowConflictsName = makeRowNameLabel("Conflicts");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #3a3a3a;");
        sep.setPadding(new Insets(2, 0, 2, 0));

        panel.getChildren().addAll(
                title,
                sep,
                makeRow(makeRowNameLabel("Algorithm"), lblAlgorithm),
                makeRow(makeRowNameLabel("Current Node"), lblCurrentNode),
                makeRow(makeRowNameLabel("Action"), lblAction),
                makeRow(rowComparingWithName, lblComparingWith),
                makeRow(makeRowNameLabel("Current Path"), lblCurrentPath),
                makeRow(makeRowNameLabel("Mask"), lblMask),
                makeRow(makeRowNameLabel("States Explored"), lblStates),
                makeRow(rowConflictsName, lblConflicts),
                makeRow(makeRowNameLabel("Status"), lblStatus),
                makeRow(makeRowNameLabel("Time Taken"), lblTimeTaken),
                makeRow(makeRowNameLabel("Time Complexity"), lblTimeComplexity));
        return panel;
    }

    private VBox makeRow(Label nameLabel, Label valueLabel) {
        nameLabel.setText(nameLabel.getText() + ":");
        nameLabel.setTextFill(Color.web("#aaaaaa"));
        nameLabel.setFont(Font.font("System", 11));
        VBox row = new VBox(2, nameLabel, valueLabel);
        return row;
    }

    private Label makeRowNameLabel(String name) {
        Label nameLabel = new Label(name);
        nameLabel.setTextFill(Color.web("#aaaaaa"));
        nameLabel.setFont(Font.font("System", 11));
        return nameLabel;
    }

    private Label makeStatLabel(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.web("#e0e0e0"));
        l.setFont(Font.font("Monospaced", 12));
        l.setWrapText(true);
        l.setMaxWidth(290);
        l.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        return l;
    }

    private void resetStatusPanel(String algorithm) {
        lblAlgorithm.setText(algorithm.isEmpty() ? "—" : algorithm);
        lblCurrentNode.setText("—");
        lblAction.setText("—");
        lblComparingWith.setText("—");
        lblCurrentPath.setText("—");
        lblMask.setText("N/A");
        lblStates.setText("0");
        lblConflicts.setText("0");
        lblStatus.setText("Idle");
        lblTimeTaken.setText("—");
        lblTimeComplexity.setText("—");
    }

    private void configureStatusPanelForGraphColoring() {
        rowComparingWithName.setText("Comparing With:");
        rowConflictsName.setText("Conflicts:");
    }

    private void configureStatusPanelForHamiltonianBacktracking() {
        rowComparingWithName.setText("Trying Node:");
        rowConflictsName.setText("Backtracks:");
    }

    private void configureStatusPanelForHamiltonianDp() {
        rowComparingWithName.setText("Trying Node:");
        rowConflictsName.setText("Transitions:");
    }

    private void setAction(String action, int node, int states, int conflicts) {
        lblAction.setText(action);
        updateActionFeedbackStyle(action);
        if (node >= 0)
            lblCurrentNode.setText(String.valueOf(node));
        else
            lblCurrentNode.setText("—");
        lblStates.setText(String.valueOf(states));
        lblConflicts.setText(String.valueOf(conflicts));
    }

    private void setHamiltonianAction(String action, int currentNode, int tryingNode, int states, int backtracks) {
        lblAction.setText(action);
        updateActionFeedbackStyle(action);
        lblCurrentNode.setText(currentNode >= 0 ? String.valueOf(currentNode) : "—");
        lblComparingWith.setText(tryingNode >= 0 ? String.valueOf(tryingNode) : "—");
        lblStates.setText(String.valueOf(states));
        lblConflicts.setText(String.valueOf(backtracks));
    }

    private void setTimeTaken(String v) {
        lblTimeTaken.setText(v);
    }

    private void setTimeComplexity(String v) {
        lblTimeComplexity.setText(v);
    }

    private void setStatus(String status) {
        lblStatus.setText(status);
    }

    private void setProblemHint(Label statusLabel, String mode) {
        if ("Hamiltonian Cycle".equals(mode)) {
            statusLabel.setText("Click nodes to build a path, or run an algorithm");
        } else {
            statusLabel.setText("Click nodes to color, or run an algorithm");
        }
    }

    private void applyButtonStyle(Button... buttons) {
        String style = "-fx-background-color: #3a3a3a;"
                + "-fx-text-fill: #ffffff;"
                + "-fx-border-color: #555555;"
                + "-fx-border-width: 1;"
                + "-fx-background-radius: 4;"
                + "-fx-border-radius: 4;"
                + "-fx-cursor: hand;"
                + "-fx-padding: 5 12 5 12;";
        String hoverStyle = "-fx-background-color: #505050;"
                + "-fx-text-fill: #ffffff;"
                + "-fx-border-color: #555555;"
                + "-fx-border-width: 1;"
                + "-fx-background-radius: 4;"
                + "-fx-border-radius: 4;"
                + "-fx-cursor: hand;"
                + "-fx-padding: 5 12 5 12;";
        String disabledStyle = "-fx-background-color: #2a2a2a;"
                + "-fx-text-fill: #777777;"
                + "-fx-border-color: #3a3a3a;"
                + "-fx-border-width: 1;"
                + "-fx-background-radius: 4;"
                + "-fx-border-radius: 4;"
                + "-fx-padding: 5 12 5 12;";
        for (Button button : buttons) {
            button.setStyle(style);
            button.setOnMouseEntered(e -> {
                if (!button.isDisabled())
                    button.setStyle(hoverStyle);
            });
            button.setOnMouseExited(e -> {
                if (!button.isDisabled())
                    button.setStyle(style);
            });
            button.disabledProperty().addListener(
                    (obs, wasDisabled, isNowDisabled) -> button.setStyle(isNowDisabled ? disabledStyle : style));
        }
    }

    private void updateActionFeedbackStyle(String action) {
        String upper = action == null ? "" : action.toUpperCase();
        // Red: conflicts, invalid states, failures
        if (upper.contains("CONFLICT") || upper.contains("ALREADY VISITED") || upper.contains("NO EDGE")
                || upper.contains("INVALID") || upper.contains("ALREADY IN SET")
                || upper.contains("DEAD STATE") || upper.contains("NO CYCLE") || upper.contains("NO NEIGHBORS")) {
            lblAction.setTextFill(Color.web("#ff4d4d"));
            lblAction.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
            return;
        }
        // Orange: no transitions possible
        if (upper.contains("NO TRANSITIONS")) {
            lblAction.setTextFill(Color.web("#ff8c00"));
            lblAction.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
            return;
        }
        // Green: success states
        if (upper.contains("COMPLETED") || upper.contains("HAMILTONIAN CYCLE FOUND")
                || upper.contains("STATE ACCEPTED") || upper.contains("CYCLE CLOSES")) {
            lblAction.setTextFill(Color.web("#4dff88"));
            lblAction.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
            return;
        }
        // Blue: informational
        if (upper.contains("BACKTRACKING") || upper.contains("BACKTRACK")
                || upper.contains("MEMOIZED") || upper.contains("TRYING NODE")) {
            lblAction.setTextFill(Color.web("#4da6ff"));
            lblAction.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
            return;
        }
        lblAction.setTextFill(Color.web("#e0e0e0"));
        lblAction.setFont(Font.font("Monospaced", FontWeight.NORMAL, 12));
    }

    private void showGraphFeedback(String message, String type) {
        if (lblGraphFeedback == null)
            return;
        String upper = type == null ? "" : type.toUpperCase();
        String color = "#e0e0e0";
        // Green
        if ("COMPLETED".equals(upper) || "STATE_ACCEPTED".equals(upper))
            color = "#00ff99";
        // Red
        else if ("CONFLICT".equals(upper) || "NO_CYCLE".equals(upper) || "NO_NEIGHBORS".equals(upper)
                || "ALREADY_IN_SET".equals(upper) || "NO_EDGE".equals(upper))
            color = "#ff4d4d";
        // Blue
        else if ("BACKTRACKING".equals(upper) || "MEMOIZED".equals(upper))
            color = "#4da6ff";
        // Orange
        else if ("NO_TRANSITIONS".equals(upper))
            color = "#ff8c00";

        lblGraphFeedback.setText(message);
        lblGraphFeedback.setTextFill(Color.web(color));
        lblGraphFeedback.setFont(Font.font("System", FontWeight.BOLD, 22));
        lblGraphFeedback.setVisible(true);
        lblGraphFeedback.setManaged(true);
    }

    private void clearGraphFeedback() {
        if (lblGraphFeedback == null)
            return;
        lblGraphFeedback.setText(" ");
        lblGraphFeedback.setTextFill(Color.TRANSPARENT);
    }

    private GraphColoring.VisualizationCallback makeColoringCallback(GraphView view) {
        return new GraphColoring.VisualizationCallback() {
            public void onHighlightNode(int nodeId) {
                Platform.runLater(() -> {
                    view.setHighlightedNode(nodeId);
                    lblCurrentNode.setText(nodeId >= 0 ? String.valueOf(nodeId) : "—");
                });
            }

            public void onEdgeHighlight(int u, int v, String type) {
                Platform.runLater(() -> view.setEdgeHighlight(u, v, type));
            }

            public void onColorUpdate(int[] c) {
                Platform.runLater(() -> view.setColors(c));
            }

            public void onAction(String action, int currentNode, int states, int conflicts) {
                Platform.runLater(() -> setAction(action, currentNode, states, conflicts));
            }

            public void onComparingWith(int nodeId) {
                Platform.runLater(() -> lblComparingWith.setText(nodeId >= 0 ? String.valueOf(nodeId) : "—"));
            }

            public void onPathUpdate(List<Integer> path) {
                Platform.runLater(() -> {
                    lblCurrentPath.setText(path.isEmpty() ? "—" : path.toString());
                    view.setPathNodes(path);
                });
            }

            public void onAlert(String message, String color) {
                Platform.runLater(() -> {
                    if (message == null) {
                        clearGraphFeedback();
                    } else {
                        String upper = message.toUpperCase();
                        if (upper.contains("NO NEIGHBORS"))
                            showGraphFeedback("NO NEIGHBORS LEFT", "NO_NEIGHBORS");
                        else if (upper.contains("NO CYCLE"))
                            showGraphFeedback("NO CYCLE POSSIBLE", "NO_CYCLE");
                        else if (upper.contains("CONFLICT"))
                            showGraphFeedback("CONFLICT", "CONFLICT");
                        else if (upper.contains("COMPLETED"))
                            showGraphFeedback("COMPLETED", "COMPLETED");
                        else if (upper.contains("BACKTRACKING"))
                            showGraphFeedback("BACKTRACKING", "BACKTRACKING");
                    }
                });
            }

            public void checkPauseOrStop() {
                // block while paused; throw to stop
                while (isPaused && !isStopped) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                if (isStopped)
                    throw new StopVisualization();
            }
        };
    }

    private int toVertexId(List<Integer> vertices, int index) {
        if (index < 0 || index >= vertices.size())
            return -1;
        return vertices.get(index);
    }

    private List<Integer> toVertexPath(List<Integer> vertices, List<Integer> indexPath) {
        List<Integer> mapped = new ArrayList<>();
        for (int index : indexPath) {
            int id = toVertexId(vertices, index);
            if (id >= 0)
                mapped.add(id);
        }
        return mapped;
    }

    private HamiltonianSolver.VisualizationCallback makeHamiltonianCallback(
            GraphView view,
            List<Integer> vertices,
            boolean backtrackingMode) {
        return new HamiltonianSolver.VisualizationCallback() {
            public void onHighlightNode(int nodeId) {
                int mapped = toVertexId(vertices, nodeId);
                Platform.runLater(() -> {
                    view.setHighlightedNode(mapped);
                    lblCurrentNode.setText(mapped >= 0 ? String.valueOf(mapped) : "—");
                });
            }

            public void onEdgeHighlight(int u, int v, String type) {
                // Handle CLEAR_ALL to reset all edges (used before final cycle display)
                if ("CLEAR_ALL".equals(type)) {
                    Platform.runLater(() -> view.resetVisualizationHighlights());
                    return;
                }
                int mu = toVertexId(vertices, u);
                int mv = toVertexId(vertices, v);
                if (mu < 0 || mv < 0)
                    return;
                if (backtrackingMode) {
                    Platform.runLater(() -> view.setEdgeHighlightDirected(mu, mv, type));
                } else {
                    Platform.runLater(() -> view.setEdgeHighlight(mu, mv, type));
                }
            }

            public void onAction(String action, int currentNode, int tryingNode, int states, int backtracks) {
                int current = toVertexId(vertices, currentNode);
                int trying = toVertexId(vertices, tryingNode);
                Platform.runLater(() -> setHamiltonianAction(action, current, trying, states, backtracks));
            }

            public void onPathUpdate(List<Integer> path) {
                List<Integer> mapped = toVertexPath(vertices, path);
                Platform.runLater(() -> {
                    lblCurrentPath.setText(mapped.isEmpty() ? "—" : mapped.toString());
                    view.setPathNodes(mapped);
                });
            }

            public void onMaskUpdate(String mask) {
                Platform.runLater(() -> lblMask.setText(mask));
            }

            public void onCycleFound(List<Integer> cycle) {
                if (!backtrackingMode) {
                    // DP mode: show final cycle with permanent BLUE nodes + GREEN arrows
                    List<Integer> mapped = toVertexPath(vertices, cycle);
                    Platform.runLater(() -> {
                        view.clearPathNodes();
                        view.highlightHamiltonianCycle(mapped);
                    });
                }
            }

            public void onAlert(String message, String color) {
                Platform.runLater(() -> {
                    if (message == null) {
                        clearGraphFeedback();
                    } else {
                        String upper = message.toUpperCase();
                        // DP-specific messages (above graph only shows results, NOT "TRYING")
                        if (upper.contains("STATE ACCEPTED"))
                            showGraphFeedback("STATE ACCEPTED", "STATE_ACCEPTED");
                        else if (upper.contains("ALREADY IN SET"))
                            showGraphFeedback("ALREADY IN SET", "ALREADY_IN_SET");
                        else if (upper.contains("NO EDGE"))
                            showGraphFeedback("NO EDGE", "NO_EDGE");
                        else if (upper.contains("MEMOIZED"))
                            showGraphFeedback("MEMOIZED", "MEMOIZED");
                        else if (upper.contains("NO TRANSITIONS"))
                            showGraphFeedback("NO TRANSITIONS POSSIBLE", "NO_TRANSITIONS");
                        // Shared messages
                        else if (upper.contains("NO NEIGHBORS"))
                            showGraphFeedback("NO NEIGHBORS LEFT", "NO_NEIGHBORS");
                        else if (upper.contains("NO CYCLE"))
                            showGraphFeedback("NO CYCLE POSSIBLE", "NO_CYCLE");
                        else if (upper.contains("CONFLICT"))
                            showGraphFeedback("CONFLICT", "CONFLICT");
                        else if (upper.contains("COMPLETED"))
                            showGraphFeedback("COMPLETED", "COMPLETED");
                        else if (upper.contains("BACKTRACKING"))
                            showGraphFeedback("BACKTRACKING", "BACKTRACKING");
                    }
                });
            }

            public void onNodeTempColor(int nodeId, String color) {
                Platform.runLater(() -> {
                    if ("CLEAR_ALL".equals(color)) {
                        view.clearAllNodeTempColors();
                    } else if (color == null) {
                        view.clearNodeTempColor(toVertexId(vertices, nodeId));
                    } else {
                        int mapped = toVertexId(vertices, nodeId);
                        if (mapped >= 0) view.setNodeTempColor(mapped, color);
                    }
                });
            }

            public void checkPauseOrStop() {
                while (isPaused && !isStopped) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                if (isStopped)
                    throw new StopVisualization();
            }
        };
    }

    private String validateColoring(Graph g, int[] colors) {
        List<Integer> verts = g.getVertices();
        for (int i = 0; i < verts.size(); i++)
            if (colors[i] == 0)
                return "Incomplete Coloring";
        for (int i = 0; i < verts.size(); i++) {
            int u = verts.get(i);
            for (int v : g.getNeighbors(u)) {
                int j = verts.indexOf(v);
                if (colors[i] == colors[j])
                    return "Invalid Coloring (conflict)";
            }
        }
        return "Valid Coloring";
    }

    private int maxColor(int[] colors) {
        int max = 0;
        for (int c : colors)
            if (c > max)
                max = c;
        return max;
    }

    private boolean isConnected(Graph g, int u, int v) {
        return g.getNeighbors(u).contains(v);
    }

    private boolean isValidManualHamiltonianCycle(Graph g, List<Integer> path, boolean closed) {
        int n = g.getVertices().size();
        if (!closed || path.size() != n)
            return false;
        Set<Integer> unique = new HashSet<>(path);
        if (unique.size() != n)
            return false;
        for (int i = 0; i < path.size() - 1; i++)
            if (!isConnected(g, path.get(i), path.get(i + 1)))
                return false;
        return isConnected(g, path.get(path.size() - 1), path.get(0));
    }

    private List<Integer> runHamiltonianSolverVisual(
            Graph g,
            HamiltonianSolver hs,
            boolean useBacktracking,
            HamiltonianSolver.VisualizationCallback callback) {
        List<Integer> vertices = g.getVertices();
        int n = vertices.size();
        Map<Integer, Integer> toIndex = new HashMap<>();
        for (int i = 0; i < n; i++)
            toIndex.put(vertices.get(i), i);
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++)
            adj.add(new ArrayList<>());
        for (int i = 0; i < n; i++) {
            int id = vertices.get(i);
            for (int nid : g.getNeighbors(id)) {
                Integer ni = toIndex.get(nid);
                if (ni != null)
                    adj.get(i).add(ni);
            }
        }
        List<Integer> indexCycle = useBacktracking
                ? hs.solveHamiltonianBacktrackingVisual(n, adj, callback)
                : hs.solveHamiltonianDPVisual(n, adj, callback);
        if (indexCycle.isEmpty())
            return indexCycle;
        List<Integer> result = new ArrayList<>();
        for (int idx : indexCycle)
            result.add(vertices.get(idx));
        return result;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
