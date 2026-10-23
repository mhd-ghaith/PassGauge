package com.passgauge.strength;

import java.util.List;

/**
 * Outcome of analyzing one password. Holds no reference to the password
 * itself, by design \u2014 only the derived assessment.
 */
public final class StrengthResult {

    public static final String[] LABELS = {"Very Weak", "Weak", "Fair", "Good", "Strong"};

    private final int score;              // 0-4, zxcvbn-style band
    private final double estimatedBits;   // rough entropy estimate after penalties
    private final List<String> feedback;  // what's wrong
    private final List<String> suggestions; // how to improve
    private final String explanation;     // one-paragraph plain-English summary

    public StrengthResult(int score, double estimatedBits, List<String> feedback,
                           List<String> suggestions, String explanation) {
        this.score = Math.max(0, Math.min(4, score));
        this.estimatedBits = estimatedBits;
        this.feedback = feedback;
        this.suggestions = suggestions;
        this.explanation = explanation;
    }

    public int getScore() { return score; }
    public String getLabel() { return LABELS[score]; }
    public double getEstimatedBits() { return estimatedBits; }
    public List<String> getFeedback() { return feedback; }
    public List<String> getSuggestions() { return suggestions; }
    public String getExplanation() { return explanation; }
}
