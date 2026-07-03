package com.codesec.codex.benchmark;

import java.util.ArrayList;
import java.util.List;

public class BenchmarkResult {
    private String name;
    private double precision;
    private double recall;
    private double f1;
    private int[][] confusionMatrix;
    private int sampleCount;
    private long durationMs;

    public BenchmarkResult() {}

    public BenchmarkResult(String name, double precision, double recall, double f1,
                           int[][] confusionMatrix, int sampleCount, long durationMs) {
        this.name = name;
        this.precision = precision;
        this.recall = recall;
        this.f1 = f1;
        this.confusionMatrix = confusionMatrix;
        this.sampleCount = sampleCount;
        this.durationMs = durationMs;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPrecision() { return precision; }
    public void setPrecision(double precision) { this.precision = precision; }
    public double getRecall() { return recall; }
    public void setRecall(double recall) { this.recall = recall; }
    public double getF1() { return f1; }
    public void setF1(double f1) { this.f1 = f1; }
    public int[][] getConfusionMatrix() { return confusionMatrix; }
    public void setConfusionMatrix(int[][] confusionMatrix) { this.confusionMatrix = confusionMatrix; }
    public int getSampleCount() { return sampleCount; }
    public void setSampleCount(int sampleCount) { this.sampleCount = sampleCount; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public static BenchmarkResult aggregate(List<BenchmarkResult> results) {
        double avgP = results.stream().mapToDouble(BenchmarkResult::getPrecision).average().orElse(0);
        double avgR = results.stream().mapToDouble(BenchmarkResult::getRecall).average().orElse(0);
        double avgF1 = results.stream().mapToDouble(BenchmarkResult::getF1).average().orElse(0);
        int totalSamples = results.stream().mapToInt(BenchmarkResult::getSampleCount).sum();
        long totalMs = results.stream().mapToLong(BenchmarkResult::getDurationMs).sum();
        int size = results.isEmpty() ? 0 : results.get(0).getConfusionMatrix().length;
        int[][] aggMatrix = new int[size][size];
        for (BenchmarkResult r : results) {
            for (int i = 0; i < size && i < r.getConfusionMatrix().length; i++) {
                for (int j = 0; j < size && j < r.getConfusionMatrix()[i].length; j++) {
                    aggMatrix[i][j] += r.getConfusionMatrix()[i][j];
                }
            }
        }
        BenchmarkResult agg = new BenchmarkResult("Overall", avgP, avgR, avgF1, aggMatrix, totalSamples, totalMs);
        return agg;
    }
}
