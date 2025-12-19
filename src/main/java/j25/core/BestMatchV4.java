package j25.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.vickumar1981.stringdistance.util.StringDistance;

public class BestMatchV4 {

	static final String SEP = "[,;]";

	public static void main(String[] args) throws IOException {

		List<String> firstNames = FileUtils.readLines(new File("./names.csv"), StandardCharsets.UTF_8);

		List<String[]> db = firstNames.stream().map(s -> s.toUpperCase().split(SEP)).toList();

		List<Criteria> criterias = new ArrayList<>();
//		criterias.add(Criteria.similarity("abdelah", 2, 0.8));
//		criterias.add(Criteria.regex(".+h.*m.*d", 5));
		criterias.add(Criteria.similarity("ahmed", 1, 0.5d));
//		criterias.add(Criteria.similarity("said", 0, 10));

		List<SimResult> bfn = bestMatch(db, criterias, 0.0, 100);
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
		System.out.println(mapper.writeValueAsString(bfn));
	}
	
	public static List<SimResult> bestMatch(Collection<String[]> candidates, List<Criteria> criterias, double threshold,
			int topN) {

		if (criterias == null || criterias.isEmpty()) {
			return java.util.Collections.emptyList();
		}

		int count = criterias.size();
		List<Integer> activeIndexes = new java.util.ArrayList<>();
		double totalWeight = 0.0;

		for (int i = 0; i < count; i++) {
			Criteria cI = criterias.get(i);
			if (cI != null && !cI.isBlank()) {
				activeIndexes.add(i);
				totalWeight += cI.weight;
			}
		}

		if (activeIndexes.isEmpty() || totalWeight <= 0) {
			return java.util.Collections.emptyList();
		}

		final double finalTotalWeight = totalWeight;

		return candidates.stream()
				.filter(t -> t.length > activeIndexes.get(activeIndexes.size() - 1))
				.map(t -> {
					double[] scoreDetails = new double[count];
					double weightedScoreSum = 0d;

					for (int i : activeIndexes) {
						Criteria cI = criterias.get(i);
						double simScore = 0.0;

						if (cI.matchingType == Criteria.MatchingType.EXACT) {
							if (t[i] != null && t[i].equalsIgnoreCase(cI.value)) {
								simScore = 1.0;
							} else {
								// For EXACT, if it doesn't match, we can disqualify early
								return null;
							}
						} else if (cI.matchingType == Criteria.MatchingType.REGEX) {
							if (cI.pattern != null && t[i] != null && cI.pattern.matcher(t[i]).matches()) {
								simScore = 1.0;
							} else {
								// Similarly for REGEX, if we specify a regex, we typically want it to match
								return null;
							}
						} else {
							// SIMILARITY
							simScore = (t[i] == null) ? 0.0 : StringDistance.jaro(t[i], cI.value);
							
							if (simScore < cI.minScoreIfSimilarity) {
								// If it fails to meet the minimum similarity for this field, disqualify
								return null;
							}
						}

						double weightedPart = simScore * cI.weight;
						scoreDetails[i] = weightedPart;
						weightedScoreSum += weightedPart;
					}

					// Normalize by total weight to get a score between 0 and 1
					double finalScore = weightedScoreSum / finalTotalWeight;

					return new SimResult(t, finalScore, scoreDetails);
				})
				.filter(p -> p != null && p.getScore() >= threshold)
				.distinct()
				.sorted() // Uses the corrected Comparable implementation
				.limit(topN)
				.collect(Collectors.toList());
	}
}