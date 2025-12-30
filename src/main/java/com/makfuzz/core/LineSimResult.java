package com.makfuzz.core;

import java.util.List;

import lombok.Data;

@Data
public class LineSimResult implements Comparable<LineSimResult> {

	private String[] candidate;

	private List<SimResult> simResults;

	private double score;

	public void initSimResults(List<Criteria> critierias) {
		simResults = critierias.stream().map(c -> {
			SimResult sr = new SimResult();
			sr.setCriteria(c);
			return sr;
		}).toList();
	}

	@Override
	public int compareTo(LineSimResult o) {
		// Descending: highest score first
		return Double.compare(o.score, this.score);
	}

}
