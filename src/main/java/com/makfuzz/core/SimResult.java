package com.makfuzz.core;

import java.util.Arrays;
import lombok.Data;

@Data
public class SimResult implements Comparable<SimResult> {
    
    private String[] candidate; 
    private double score; // Total combined weighted score used for sorting

    // Spelling Similarity Details (Apache Commons Text)
    private double spellingScore;
    private double[] spellingScoreDetails;

    // Phonetic Similarity Details (Double Metaphone)
    private double phoneticScore;
    private double[] phoneticScoreDetails;

    public SimResult(String[] candidate, double score) {
        this.candidate = candidate;
        this.score = score;
    }

    @Override
    public int compareTo(SimResult other) {
        // Sort by total score DESCENDING
        return Double.compare(other.score, this.score);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(candidate);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SimResult other = (SimResult) obj;
        return Arrays.equals(candidate, other.candidate);
    }
}
