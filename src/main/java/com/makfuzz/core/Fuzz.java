package com.makfuzz.core;
 
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.codec.language.bm.NameType;
import org.apache.commons.codec.language.bm.PhoneticEngine;
import org.apache.commons.codec.language.bm.RuleType;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

public class Fuzz {

	static final String SEP = "[,;]";
	private static final JaroWinklerSimilarity SPELLING_STRATEGY = new JaroWinklerSimilarity();
	
	// Beider-Morse is optimized for Surnames and Generic Names (French, Arabic, etc.)
	private static final PhoneticEngine PHONETIC_ENGINE = new PhoneticEngine(NameType.GENERIC, RuleType.APPROX, true);

	// Cache to avoid recalculating expensive phonetic codes for repeating names
	// This makes a HUGE difference in performance on large datasets
	private static final java.util.Map<String, String> PHONETIC_CACHE = new java.util.concurrent.ConcurrentHashMap<>(2000);

	public static List<SimResult> bestMatch(Collection<String[]> candidates, List<Criteria> criterias, double threshold,
			int topN) {

		if (criterias == null || criterias.isEmpty()) {
			return java.util.Collections.emptyList();
		}

		int count = criterias.size();
		List<Integer> activeIndexes = new java.util.ArrayList<>();
		
		// Pre-optimized criteria data
		String[] criteriaValues = new String[count];
		String[] criteriaPhoneticCodes = new String[count];
		double totalWeight = 0.0;

		for (int i = 0; i < count; i++) {
			Criteria cI = criterias.get(i);
			if (cI != null && !cI.isBlank()) {
				activeIndexes.add(i);
				totalWeight += cI.weight;
				criteriaValues[i] = cI.value;
				// PRE-OPTIMIZATION: Calculate search criteria phonetic code ONCE
				criteriaPhoneticCodes[i] = PHONETIC_ENGINE.encode(cI.value);
			}
		}

		if (activeIndexes.isEmpty()) {
			return candidates.stream()
					.limit(topN)
					.map(t -> new SimResult(t, 1.0))
					.collect(Collectors.toList());
		}

		if (totalWeight <= 0) {
			return java.util.Collections.emptyList();
		}

		final double finalTotalWeight = totalWeight;

		// Use parallelStream for faster processing on large datasets
		return candidates.parallelStream()
				.map(t -> {
					double[] spellingDetails = new double[count];
					double[] phoneticDetails = new double[count];
					double weightedChoiceSum = 0d;
					double weightedSpellingSum = 0d;
					double weightedPhoneticSum = 0d;

					for (int i : activeIndexes) {
						Criteria cI = criterias.get(i);
						double spellingScore = 0.0;
						double phoneticScore = 0.0;
						double choiceScore = 0.0;
						
						String cellValue = (t != null && i < t.length) ? t[i].trim().toUpperCase() : null;

						if (cellValue == null || cellValue.isEmpty()) {
							// Skip or treat as zero match
						} else if (cI.matchingType == Criteria.MatchingType.EXACT) {
							if (cellValue.equalsIgnoreCase(criteriaValues[i])) {
								spellingScore = phoneticScore = choiceScore = 1.0;
							} else {
								return null;
							}
						} else if (cI.matchingType == Criteria.MatchingType.REGEX) {
							if (cI.pattern != null && cI.pattern.matcher(cellValue).matches()) {
								spellingScore = phoneticScore = choiceScore = 1.0;
							} else {
								return null;
							}
						} else {
							// 1. Calculate Spelling (Jaro-Winkler)
							spellingScore = SPELLING_STRATEGY.apply(cellValue, criteriaValues[i]);
							
							// 2. Calculate Fuzzy Phonetic Score (Beider-Morse)
							// CACHING: Use the cache for cells, but pre-calculated codes for criteria
							String code1 = PHONETIC_CACHE.computeIfAbsent(cellValue, PHONETIC_ENGINE::encode);
							String code2 = criteriaPhoneticCodes[i];
							
							if (code1.equals(code2)) {
								phoneticScore = 1.0;
							} else {
								// Clean up Beider-Morse noise "(|)" for better fuzzy comparison
								String clean1 = code1.replaceAll("[()|]", "");
								String clean2 = code2.replaceAll("[()|]", "");
								phoneticScore = SPELLING_STRATEGY.apply(clean1, clean2);
							}

							// Calculation based on user formula: average of spelling and phonetic
							choiceScore = (spellingScore + phoneticScore) / 2.0;

							// STRICT FILTERING: Must pass BOTH thresholds independently
							boolean spellingPasses = spellingScore >= cI.minSpellingScore;
							boolean phoneticPasses = phoneticScore >= cI.minPhoneticScore;
							
							if (!spellingPasses || !phoneticPasses) {
								return null;
							}
						}

						spellingDetails[i] = spellingScore;
						phoneticDetails[i] = phoneticScore;
						
						weightedChoiceSum += choiceScore * cI.weight;
						weightedSpellingSum += spellingScore * cI.weight;
						weightedPhoneticSum += phoneticScore * cI.weight;
					}

					double finalScore = weightedChoiceSum / finalTotalWeight;
					SimResult result = new SimResult(t, finalScore);
					result.setSpellingScore(weightedSpellingSum / finalTotalWeight);
					result.setSpellingScoreDetails(spellingDetails);
					result.setPhoneticScore(weightedPhoneticSum / finalTotalWeight);
					result.setPhoneticScoreDetails(phoneticDetails);
					
					return result;
				})
				.filter(p -> p != null && p.getScore() >= threshold)
				.distinct()
				.sorted(java.util.Comparator.comparingDouble(SimResult::getScore).reversed())
				.limit(topN)
				.collect(Collectors.toList());
	}
}