package j25;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vickumar1981.stringdistance.util.StringDistance;

public class BestMatchV3 {

	static final String SEP = ",";

	public static List<SimResult> bestMatch(Collection<String[]> candidates, Criteria[] criteria, double threshold,
			int topN) {

		List<Integer> nonEmptyIndexes = new java.util.ArrayList<>();
		double totalWeight = 0.0;
		
		for (int i = 0; i < criteria.length; i++) {
			if (criteria[i] != null && !criteria[i].isBlank()) {
				nonEmptyIndexes.add(i);
				totalWeight += criteria[i].weight;
			}
		}

		if (nonEmptyIndexes.isEmpty()) {
			return java.util.Collections.emptyList();
		}

		final double finalTotalWeight = totalWeight;

		return candidates.stream()
				.filter(t -> t.length > nonEmptyIndexes.get(nonEmptyIndexes.size() - 1))
				.map(t -> {
					double[] scoreDetails = new double[criteria.length];
					double score = 0d;
					boolean allExactMatch = true;

					for (int i : nonEmptyIndexes) {
						Criteria ci = criteria[i];
						double sim;

						if (ci.exact) {
							sim = ci.value.equalsIgnoreCase(t[i]) ? 1.0 : 0.0;
							// If exact match is required and it doesn't match, disqualify
							if (sim == 0.0) {
								allExactMatch = false;
							}
						} else {
							sim = StringDistance.jaro(t[i], ci.value);
						}

						scoreDetails[i] = sim;
						score += ci.weight * sim;
					}

					// If any exact-match criterion failed, return null to filter out
					if (!allExactMatch) {
						return null;
					}

					// Normalize by total weight to get a score between 0 and 1
					score = score / finalTotalWeight;

					return new SimResult(t, score, scoreDetails);
				})
				.filter(p -> p != null && p.score >= threshold)
				.distinct()
				.sorted((a, b) -> Double.compare(b.score, a.score))
				.limit(topN)
				.collect(Collectors.toList());
	}

	public static void main(String[] args) throws IOException {

		List<String> firstNames = FileUtils.readLines(new File("./names.csv"), StandardCharsets.UTF_8);

		List<String[]> db = firstNames.stream().map(s -> s.toUpperCase().split(SEP)).toList();

		Criteria[] criterias = { new Criteria("adil", true, 0.3), new Criteria("AZMI", true, 0.7) };

		List<SimResult> bfn = bestMatch(db, criterias, 0.7, 100);
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
		System.out.println(mapper.writeValueAsString(bfn));
	}

	private static class Criteria {

		String value;
		boolean exact;
		double weight;

		public Criteria(String value, boolean exact, double weight) {
			super();
			this.value = StringUtils.isBlank(value) ? null : value.trim().toUpperCase();
			this.exact = exact;
			this.weight = weight;
		}

		public boolean isBlank() {
			return value == null;
		}
	}

	private static class SimResult implements Comparator<SimResult> {
		String[] candidate;
		double score;
		double[] scoreDetails;

		@Override
		public int compare(SimResult o1, SimResult o2) {
			return ((int) (o1.score - o2.score)) * 1000;
		}

		public SimResult(String[] candidate, double score, double[] scoreDetails) {
			super();
			this.candidate = candidate;
			this.scoreDetails = scoreDetails;
			this.score = score;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(candidate);
			return result;
		}

		public String[] getCandidate() {
			return candidate;
		}

		public void setCandidate(String[] candidate) {
			this.candidate = candidate;
		}

		public double getScore() {
			return score;
		}

		public void setScore(double score) {
			this.score = score;
		}

		public double[] getScoreDetails() {
			return scoreDetails;
		}

		public void setScoreDetails(double[] scoreDetails) {
			this.scoreDetails = scoreDetails;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			SimResult other = (SimResult) obj;
			return Arrays.equals(candidate, other.candidate);
		}
	}
}