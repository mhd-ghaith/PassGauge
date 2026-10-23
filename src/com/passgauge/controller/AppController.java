package com.passgauge.controller;

import com.passgauge.crack.CrackResult;
import com.passgauge.crack.ParallelCracker;
import com.passgauge.crack.SequentialCracker;
import com.passgauge.strength.PasswordGenerator;
import com.passgauge.strength.StrengthAnalyzer;
import com.passgauge.strength.StrengthResult;
import com.passgauge.util.HashUtil;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.Line;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class AppController {

    // ---- shared ----
    @FXML private BorderPane rootPane;
    @FXML private ToggleButton themeToggleButton;

    // ---- Module 1: Strength Analyzer ----
    @FXML private PasswordField strengthPasswordField;
    @FXML private TextField strengthPasswordPlainField;
    @FXML private Button strengthEyeButton;
    @FXML private Spinner<Integer> genLengthSpinner;
    @FXML private Button generateButton;
    @FXML private Button copyButton;
    @FXML private javafx.scene.layout.HBox meterBox;
    @FXML private Label scoreLabel;
    @FXML private Label explanationLabel;
    @FXML private ListView<String> feedbackList;
    @FXML private ListView<String> suggestionsList;

    // ---- Module 2: Benchmark ----
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordPlainField;
    @FXML private Button passwordEyeButton;
    @FXML private Spinner<Integer> maxLengthSpinner;
    @FXML private ToggleGroup threadToggleGroup;
    @FXML private ToggleGroup chartMetricGroup;
    @FXML private Button runButton;
    @FXML private Button exportButton;
    @FXML private Label liveHashRateLabel;
    @FXML private Label sequentialTimeLabel;
    @FXML private Label parallelTimeLabel;
    @FXML private Label efficiencyLabel;
    @FXML private Label speedupLabel;
    @FXML private Label matchLabel;
    @FXML private Label statusLabel;
    @FXML private Label keyspaceLabel;
    @FXML private Label systemInfoLabel;
    @FXML private Label chipLower;
    @FXML private Label chipUpper;
    @FXML private Label chipDigit;
    @FXML private Label chipSymbol;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private VBox metricsBox;
    @FXML private BarChart<String, Number> benchmarkChart;
    @FXML private TableView<RunRecord> historyTable;
    @FXML private TableColumn<RunRecord, Number> colThreads;
    @FXML private TableColumn<RunRecord, Number> colLength;
    @FXML private TableColumn<RunRecord, String> colSeqTime;
    @FXML private TableColumn<RunRecord, String> colParTime;
    @FXML private TableColumn<RunRecord, String> colSpeedup;
    @FXML private TableColumn<RunRecord, String> colEfficiency;
    @FXML private TableColumn<RunRecord, String> colHashRate;

    private static final int MAX_HISTORY_ON_CHART = 6;
    private static final String[] SCORE_COLORS = {"#EF4444", "#F97316", "#F59E0B", "#84CC16", "#10B981"};

    // Simple eye glyph (outer eye shape + an even-odd pupil hole), 18x18 viewBox.
    // The same shape is reused for both states; a diagonal slash line is overlaid
    // on top of it when the password is hidden, turned off when revealed.
    private static final String EYE_PATH =
            "M1 9C1 9 4 3 9 3C14 3 17 9 17 9C17 9 14 15 9 15C4 15 1 9 1 9Z " +
            "M9 6.5C7.6 6.5 6.5 7.6 6.5 9C6.5 10.4 7.6 11.5 9 11.5C10.4 11.5 11.5 10.4 11.5 9C11.5 7.6 10.4 6.5 9 6.5Z";

    private boolean darkMode = false;
    private final StrengthAnalyzer strengthAnalyzer = new StrengthAnalyzer();
    private final PasswordGenerator passwordGenerator = new PasswordGenerator();
    private int lastRunThreads = -1;

    @FXML
    public void initialize() {
        initStrengthModule();
        initBenchmarkModule();
    }

    // =====================================================================
    // Module 1 — Strength Analyzer
    // =====================================================================

    private void initStrengthModule() {
        genLengthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 32, 16));
        restrictToDigits(genLengthSpinner, 8, 32);

        strengthPasswordField.textProperty().addListener((obs, old, val) -> updateStrengthAnalysis(val));
        generateButton.setOnAction(e -> {
            playPressAnimation(generateButton);
            strengthPasswordField.setText(passwordGenerator.generate(genLengthSpinner.getValue()));
        });
        copyButton.setOnAction(e -> {
            playPressAnimation(copyButton);
            ClipboardContent content = new ClipboardContent();
            content.putString(strengthPasswordField.getText() == null ? "" : strengthPasswordField.getText());
            Clipboard.getSystemClipboard().setContent(content);
        });

        wireRevealToggle(strengthPasswordField, strengthPasswordPlainField, strengthEyeButton);
        updateStrengthAnalysis("");
    }

    private void updateStrengthAnalysis(String password) {
        StrengthResult result = strengthAnalyzer.analyze(password);
        String color = SCORE_COLORS[result.getScore()];

        for (int i = 0; i < meterBox.getChildren().size(); i++) {
            Region segment = (Region) meterBox.getChildren().get(i);
            if (password != null && !password.isEmpty() && i <= result.getScore()) {
                segment.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4;");
            } else {
                segment.setStyle(null);
            }
        }

        if (password == null || password.isEmpty()) {
            scoreLabel.setText("Enter a password");
            scoreLabel.setStyle(null);
        } else {
            scoreLabel.setText(result.getLabel() + "  (" + (result.getScore() + 1) + "/5)");
            scoreLabel.setStyle("-fx-text-fill: " + color + ";");
        }

        explanationLabel.setText(result.getExplanation());

        ObservableList<String> feedback = FXCollections.observableArrayList(result.getFeedback());
        feedbackList.setItems(feedback);
        ObservableList<String> suggestions = FXCollections.observableArrayList(result.getSuggestions());
        suggestionsList.setItems(suggestions);
    }

    // =====================================================================
    // Module 2 — Benchmark
    // =====================================================================

    private void initBenchmarkModule() {
        maxLengthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 7, 4));
        restrictToDigits(maxLengthSpinner, 1, 7);
        maxLengthSpinner.valueProperty().addListener((obs, old, val) -> updateKeyspaceEstimate());

        threadToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                oldToggle.setSelected(true);
            }
        });
        chartMetricGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                oldToggle.setSelected(true);
            } else {
                renderChart();
            }
        });

        passwordField.textProperty().addListener((obs, old, val) -> {
            updateCharsetChips(val);
            updateKeyspaceEstimate();
        });

        colThreads.setCellValueFactory(d -> d.getValue().threadsProperty());
        colLength.setCellValueFactory(d -> d.getValue().lengthProperty());
        colSeqTime.setCellValueFactory(d -> d.getValue().seqTimeProperty());
        colParTime.setCellValueFactory(d -> d.getValue().parTimeProperty());
        colSpeedup.setCellValueFactory(d -> d.getValue().speedupProperty());
        colEfficiency.setCellValueFactory(d -> d.getValue().efficiencyProperty());
        colHashRate.setCellValueFactory(d -> d.getValue().hashRateProperty());
        historyTable.setItems(FXCollections.observableArrayList());

        runButton.setOnAction(e -> {
            playPressAnimation(runButton);
            runBenchmark();
        });
        exportButton.setOnAction(e -> {
            playPressAnimation(exportButton);
            exportCsv();
        });
        themeToggleButton.setOnAction(e -> toggleTheme());

        wireRevealToggle(passwordField, passwordPlainField, passwordEyeButton);
        updateCharsetChips("");
        updateKeyspaceEstimate();
        updateSystemInfoLabel();
    }

    /** Attaches a TextFormatter that silently rejects any non-digit keystroke, with no error shown. */
    private void restrictToDigits(Spinner<Integer> spinner, int min, int max) {
        Pattern digitsOnly = Pattern.compile("\\d{0,2}");
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            return digitsOnly.matcher(newText).matches() ? change : null;
        };
        spinner.getEditor().setTextFormatter(new TextFormatter<>(filter));

        // Spinner doesn't commit typed text on focus-loss by default — commit it here,
        // clamping to [min, max] and reverting to the last valid value if left blank/invalid.
        spinner.focusedProperty().addListener((obs, was, isNow) -> {
            if (!isNow) {
                String text = spinner.getEditor().getText();
                try {
                    int value = Integer.parseInt(text);
                    value = Math.max(min, Math.min(max, value));
                    spinner.getValueFactory().setValue(value);
                } catch (NumberFormatException ex) {
                    spinner.getEditor().setText(String.valueOf(spinner.getValue()));
                }
            }
        });
    }

    private char[] resolveCharsetFromPassword(String password) {
        boolean hasLower = false, hasUpper = false, hasDigit = false, hasSymbol = false;
        if (password != null) {
            for (char c : password.toCharArray()) {
                if (Character.isLowerCase(c)) hasLower = true;
                else if (Character.isUpperCase(c)) hasUpper = true;
                else if (Character.isDigit(c)) hasDigit = true;
                else hasSymbol = true;
            }
        }
        if (!hasLower && !hasUpper && !hasDigit && !hasSymbol) {
            hasLower = true;
        }
        StringBuilder sb = new StringBuilder();
        if (hasLower) sb.append("abcdefghijklmnopqrstuvwxyz");
        if (hasUpper) sb.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        if (hasDigit) sb.append("0123456789");
        if (hasSymbol) sb.append("!@#$%^&*()-_=+");
        return sb.toString().toCharArray();
    }

    private void updateCharsetChips(String password) {
        boolean hasLower = false, hasUpper = false, hasDigit = false, hasSymbol = false;
        if (password != null) {
            for (char c : password.toCharArray()) {
                if (Character.isLowerCase(c)) hasLower = true;
                else if (Character.isUpperCase(c)) hasUpper = true;
                else if (Character.isDigit(c)) hasDigit = true;
                else hasSymbol = true;
            }
        }
        boolean noneYet = !hasLower && !hasUpper && !hasDigit && !hasSymbol;
        setChipActive(chipLower, hasLower || noneYet);
        setChipActive(chipUpper, hasUpper);
        setChipActive(chipDigit, hasDigit);
        setChipActive(chipSymbol, hasSymbol);
    }

    private void setChipActive(Label chip, boolean active) {
        if (active) {
            if (!chip.getStyleClass().contains("charset-chip-active")) {
                chip.getStyleClass().add("charset-chip-active");
            }
        } else {
            chip.getStyleClass().remove("charset-chip-active");
        }
    }

    private void updateKeyspaceEstimate() {
        String password = passwordField.getText();
        int maxLength = maxLengthSpinner.getValue();
        char[] charset = resolveCharsetFromPassword(password == null ? "" : password);

        double total = 0;
        for (int len = 1; len <= maxLength; len++) {
            total += Math.pow(charset.length, len);
        }

        keyspaceLabel.getStyleClass().removeAll("keyspace-ok", "keyspace-warn", "keyspace-danger");
        keyspaceLabel.setText("~" + formatCount(total) + " candidates");
        if (total < 200_000) {
            keyspaceLabel.getStyleClass().add("keyspace-ok");
        } else if (total < 20_000_000) {
            keyspaceLabel.getStyleClass().add("keyspace-warn");
        } else {
            keyspaceLabel.getStyleClass().add("keyspace-danger");
        }
    }

    private String formatCount(double n) {
        if (n >= 1_000_000_000) return String.format("%.1fB", n / 1_000_000_000);
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000);
        if (n >= 1_000) return String.format("%.1fK", n / 1_000);
        return String.format("%.0f", n);
    }

    private int getSelectedThreadCount() {
        Toggle selected = threadToggleGroup.getSelectedToggle();
        if (selected instanceof ToggleButton tb) {
            return Integer.parseInt(tb.getText());
        }
        return 4;
    }

    private String getSelectedChartMetric() {
        Toggle selected = chartMetricGroup.getSelectedToggle();
        if (selected instanceof ToggleButton tb) {
            return tb.getText();
        }
        return "Time";
    }

    private void runBenchmark() {
        String password = passwordField.getText();
        if (password == null || password.isBlank()) {
            statusLabel.setText("Enter a password to test first.");
            return;
        }

        char[] charset = resolveCharsetFromPassword(password);
        int maxLength = maxLengthSpinner.getValue();
        int threads = getSelectedThreadCount();
        String targetHash = HashUtil.sha256(password);

        runButton.setDisable(true);
        progressIndicator.setVisible(true);
        statusLabel.setText("Running sequential pass...");
        matchLabel.setText("\u2014");
        matchLabel.getStyleClass().remove("match-found");

        AtomicLong hashCounter = new AtomicLong(0);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                SequentialCracker seq = new SequentialCracker();
                CrackResult seqResult = seq.crack(targetHash, charset, maxLength, 1, hashCounter);

                updateMessage("Running parallel pass...");
                hashCounter.set(0);

                ParallelCracker par = new ParallelCracker();
                CrackResult parResult = par.crack(targetHash, charset, maxLength, threads, hashCounter);

                Platform.runLater(() -> showResults(seqResult, parResult, threads, maxLength));
                return null;
            }
        };

        task.messageProperty().addListener((obs, old, msg) -> statusLabel.setText(msg));

        AnimationTimer liveCounter = new AnimationTimer() {
            @Override
            public void handle(long now) {
                liveHashRateLabel.setText(String.format("%,d hashes computed", hashCounter.get()));
            }
        };
        liveCounter.start();

        task.setOnSucceeded(e -> {
            liveCounter.stop();
            runButton.setDisable(false);
            progressIndicator.setVisible(false);
            statusLabel.setText("Done.");
        });
        task.setOnFailed(e -> {
            liveCounter.stop();
            runButton.setDisable(false);
            progressIndicator.setVisible(false);
            statusLabel.setText("Error: " + task.getException());
        });

        Thread thread = new Thread(task, "passgauge-benchmark");
        thread.setDaemon(true);
        thread.start();
    }

    private void showResults(CrackResult seqResult, CrackResult parResult, int threads, int maxLength) {
        double seqSeconds = seqResult.getElapsedSeconds();
        double parSeconds = parResult.getElapsedSeconds();
        double speedup = parSeconds > 0 ? seqSeconds / parSeconds : 0;
        double efficiency = threads > 0 ? (speedup / threads) * 100.0 : 0;

        sequentialTimeLabel.setText(String.format("%.4f s", seqSeconds));
        parallelTimeLabel.setText(String.format("%.4f s", parSeconds));
        speedupLabel.setText(String.format("%.2fx", speedup));
        efficiencyLabel.setText(String.format("%.0f%%", efficiency));

        if (parResult.isFound()) {
            matchLabel.setText("Found: " + parResult.getMatchedPassword());
            if (!matchLabel.getStyleClass().contains("match-found")) {
                matchLabel.getStyleClass().add("match-found");
            }
        } else {
            matchLabel.setText("Not found in search space");
            matchLabel.getStyleClass().remove("match-found");
        }

        RunRecord record = new RunRecord(threads, maxLength, seqSeconds, parSeconds,
                speedup, efficiency, parResult.getHashesPerSecond());
        historyTable.getItems().add(record);
        historyTable.scrollTo(historyTable.getItems().size() - 1);

        lastRunThreads = threads;
        updateSystemInfoLabel();
        renderChart();

        FadeTransition fade = new FadeTransition(Duration.millis(260), metricsBox);
        fade.setFromValue(0.35);
        fade.setToValue(1.0);
        fade.play();
    }

    /** Rebuilds the chart from history for whichever metric chip is currently selected. */
    private void renderChart() {
        String metric = getSelectedChartMetric();
        benchmarkChart.getData().clear();

        List<RunRecord> items = historyTable.getItems();
        int start = Math.max(0, items.size() - MAX_HISTORY_ON_CHART);
        List<RunRecord> shown = items.subList(start, items.size());

        NumberAxis yAxis = (NumberAxis) benchmarkChart.getYAxis();

        if (metric.equals("Time")) {
            yAxis.setLabel("Time (seconds)");
            XYChart.Series<String, Number> seqSeries = new XYChart.Series<>();
            seqSeries.setName("Sequential");
            XYChart.Series<String, Number> parSeries = new XYChart.Series<>();
            parSeries.setName("Parallel");
            for (int i = 0; i < shown.size(); i++) {
                RunRecord r = shown.get(i);
                String label = "#" + (start + i + 1) + " (" + r.getThreads() + "T)";
                seqSeries.getData().add(new XYChart.Data<>(label, r.getSeqSeconds()));
                parSeries.getData().add(new XYChart.Data<>(label, r.getParSeconds()));
            }
            benchmarkChart.getData().addAll(seqSeries, parSeries);
        } else if (metric.equals("Speedup")) {
            yAxis.setLabel("Speedup (x)");
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Speedup");
            for (int i = 0; i < shown.size(); i++) {
                RunRecord r = shown.get(i);
                String label = "#" + (start + i + 1) + " (" + r.getThreads() + "T)";
                series.getData().add(new XYChart.Data<>(label, r.getSpeedupValue()));
            }
            benchmarkChart.getData().add(series);
        } else {
            yAxis.setLabel("Efficiency (%)");
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Efficiency");
            for (int i = 0; i < shown.size(); i++) {
                RunRecord r = shown.get(i);
                String label = "#" + (start + i + 1) + " (" + r.getThreads() + "T)";
                series.getData().add(new XYChart.Data<>(label, r.getEfficiencyValue()));
            }
            benchmarkChart.getData().add(series);
        }
    }

    private void updateSystemInfoLabel() {
        int cores = Runtime.getRuntime().availableProcessors();
        String javaVersion = System.getProperty("java.version");
        StringBuilder sb = new StringBuilder();
        sb.append(cores).append(" cores available \u00b7 Java ").append(javaVersion);
        if (lastRunThreads > 0) {
            sb.append(" \u00b7 last run used ").append(lastRunThreads).append(" threads");
        }
        systemInfoLabel.setText(sb.toString());
    }

    private void exportCsv() {
        List<RunRecord> items = historyTable.getItems();
        if (items.isEmpty()) {
            statusLabel.setText("No results to export yet — run a benchmark first.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export benchmark results");
        chooser.setInitialFileName("passgauge-benchmark-results.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV file", "*.csv"));
        File file = chooser.showSaveDialog(rootPane.getScene().getWindow());
        if (file == null) {
            return;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("Threads,MaxLength,SequentialSeconds,ParallelSeconds,Speedup,EfficiencyPercent,HashesPerSecond");
            for (RunRecord r : items) {
                writer.println(r.toCsvRow());
            }
            statusLabel.setText("Exported " + items.size() + " run(s) to " + file.getName());
        } catch (IOException e) {
            statusLabel.setText("Export failed: " + e.getMessage());
        }
    }

    // =====================================================================
    // Shared UI helpers
    // =====================================================================

    /**
     * Links a masked PasswordField with a plain TextField showing the same
     * text, and wires an eye button that swaps which one is visible. The
     * icon's diagonal slash is shown while the password is hidden and
     * removed once it's revealed.
     */
    private void wireRevealToggle(PasswordField masked, TextField plain, Button eyeButton) {
        masked.textProperty().bindBidirectional(plain.textProperty());

        SVGPath eyeShape = new SVGPath();
        eyeShape.setContent(EYE_PATH);
        eyeShape.setFillRule(FillRule.EVEN_ODD);
        eyeShape.getStyleClass().add("eye-icon-shape");

        Line slash = new Line(1.5, 15.5, 15.5, 1.5);
        slash.getStyleClass().add("eye-icon-slash");
        slash.setVisible(true); // starting state is hidden -> slash shown

        StackPane icon = new StackPane(eyeShape, slash);
        icon.setPrefSize(18, 18);
        icon.setMaxSize(18, 18);
        eyeButton.setGraphic(icon);

        eyeButton.setOnAction(e -> {
            playPressAnimation(eyeButton);
            boolean willReveal = !plain.isVisible();
            masked.setVisible(!willReveal);
            masked.setManaged(!willReveal);
            plain.setVisible(willReveal);
            plain.setManaged(willReveal);
            slash.setVisible(!willReveal);
            if (willReveal) {
                plain.positionCaret(plain.getText() == null ? 0 : plain.getText().length());
                plain.requestFocus();
            } else {
                masked.positionCaret(masked.getText() == null ? 0 : masked.getText().length());
                masked.requestFocus();
            }
        });
    }

    private void playPressAnimation(Node node) {
        ScaleTransition press = new ScaleTransition(Duration.millis(90), node);
        press.setToX(0.94);
        press.setToY(0.94);
        press.setAutoReverse(true);
        press.setCycleCount(2);
        press.play();
    }

    private void toggleTheme() {
        darkMode = themeToggleButton.isSelected();
        Scene scene = rootPane.getScene();

        FadeTransition fadeOut = new FadeTransition(Duration.millis(140), rootPane);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.35);
        fadeOut.setOnFinished(e -> {
            scene.getStylesheets().clear();
            String cssPath = darkMode ? "/com/passgauge/css/dark.css" : "/com/passgauge/css/light.css";
            scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            themeToggleButton.setText(darkMode ? "\u2600  Light Mode" : "\uD83C\uDF19  Dark Mode");

            FadeTransition fadeIn = new FadeTransition(Duration.millis(220), rootPane);
            fadeIn.setFromValue(0.35);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }
}
