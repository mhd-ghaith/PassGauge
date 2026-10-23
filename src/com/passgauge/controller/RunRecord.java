package com.passgauge.controller;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * One row of benchmark history shown in the results table, the CSV export,
 * and (via its raw numeric fields) the dynamic chart.
 */
public class RunRecord {
    private final IntegerProperty threads;
    private final IntegerProperty length;
    private final StringProperty seqTime;
    private final StringProperty parTime;
    private final StringProperty speedup;
    private final StringProperty efficiency;
    private final StringProperty hashRate;

    // Raw values, kept alongside the formatted strings above so the chart
    // and CSV export can use exact numbers instead of re-parsing text.
    private final double seqSeconds;
    private final double parSeconds;
    private final double speedupValue;
    private final double efficiencyValue;
    private final double hashesPerSecond;

    public RunRecord(int threads, int length, double seqSeconds, double parSeconds,
                      double speedupValue, double efficiencyValue, double hashesPerSecond) {
        this.threads = new SimpleIntegerProperty(threads);
        this.length = new SimpleIntegerProperty(length);
        this.seqSeconds = seqSeconds;
        this.parSeconds = parSeconds;
        this.speedupValue = speedupValue;
        this.efficiencyValue = efficiencyValue;
        this.hashesPerSecond = hashesPerSecond;

        this.seqTime = new SimpleStringProperty(String.format("%.4f s", seqSeconds));
        this.parTime = new SimpleStringProperty(String.format("%.4f s", parSeconds));
        this.speedup = new SimpleStringProperty(String.format("%.2fx", speedupValue));
        this.efficiency = new SimpleStringProperty(String.format("%.0f%%", efficiencyValue));
        this.hashRate = new SimpleStringProperty(String.format("%,.0f h/s", hashesPerSecond));
    }

    public IntegerProperty threadsProperty() { return threads; }
    public IntegerProperty lengthProperty() { return length; }
    public StringProperty seqTimeProperty() { return seqTime; }
    public StringProperty parTimeProperty() { return parTime; }
    public StringProperty speedupProperty() { return speedup; }
    public StringProperty efficiencyProperty() { return efficiency; }
    public StringProperty hashRateProperty() { return hashRate; }

    public int getThreads() { return threads.get(); }
    public int getLength() { return length.get(); }
    public double getSeqSeconds() { return seqSeconds; }
    public double getParSeconds() { return parSeconds; }
    public double getSpeedupValue() { return speedupValue; }
    public double getEfficiencyValue() { return efficiencyValue; }
    public double getHashesPerSecond() { return hashesPerSecond; }

    /** One CSV row (no header), matching the columns the table shows. */
    public String toCsvRow() {
        return threads.get() + "," + length.get() + "," +
                String.format("%.6f", seqSeconds) + "," +
                String.format("%.6f", parSeconds) + "," +
                String.format("%.4f", speedupValue) + "," +
                String.format("%.2f", efficiencyValue) + "," +
                String.format("%.0f", hashesPerSecond);
    }
}
