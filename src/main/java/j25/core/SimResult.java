package j25.core;

import java.util.Arrays;

import lombok.Data;

@Data
public class SimResult implements Comparable<SimResult> {
	
    private String[] candidate; 
    private double score;
    private double[] scoreDetails;

    public SimResult(String[] candidate, double score, double[] scoreDetails) {
        this.candidate = candidate;
        this.scoreDetails = scoreDetails;
        this.score = score;
    }

    @Override
    public int compareTo(SimResult other) {
        // Sort by score DESCENDING
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
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SimResult other = (SimResult) obj;
        return Arrays.equals(candidate, other.candidate);
    }

}
