package com.makfuzz.core;

import lombok.Data;

@Data
public class SimResult implements Comparable<SimResult> {

	private Criteria criteria;

	private double score;

	private double spellingScore;

	private double phoneticScore;

	private int columnIndex;

	@Override
	public int compareTo(SimResult o) {
		return Double.compare(this.score, o.score);
	}

}
