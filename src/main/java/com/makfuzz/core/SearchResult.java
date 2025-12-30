package com.makfuzz.core;

import java.util.List;

import lombok.Data;

@Data
public class SearchResult {
	private List<LineSimResult> results;
	private double maxUnderThreshold = 0.0;
	private double minAboveThreshold = 0.0;
	private double maxAboveThreshold = 0.0;
	private SimResult maxUnderCandidate = null;
	private SimResult minAboveCandidate = null;
	private SimResult maxAboveCandidate = null;
	private int totalFound;

	public SearchResult(List<LineSimResult> results, double maxUnder, double minAbove, double maxAbove,
			SimResult maxUnderCand, SimResult minAboveCand, SimResult maxAboveCand, int totalFound) {
		this.results = results;
		this.maxUnderThreshold = maxUnder;
		this.minAboveThreshold = minAbove;
		this.maxAboveThreshold = maxAbove;
		this.maxUnderCandidate = maxUnderCand;
		this.minAboveCandidate = minAboveCand;
		this.maxAboveCandidate = maxAboveCand;
		this.totalFound = totalFound;
	}

	public SearchResult(List<LineSimResult> results) {
		super();
		this.results = results;
	}
}
