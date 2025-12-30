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

	// Engines
	private static final FrenchSoundex FRENCH_ENGINE = new FrenchSoundex();
	private static final PhoneticEngine DEFAULT_ENGINE = new PhoneticEngine(NameType.GENERIC, RuleType.APPROX, true);

	// Cache to avoid recalculating expensive phonetic codes for repeating names
	// This makes a HUGE difference in performance on large datasets
	private static final java.util.Map<String, String> PHONETIC_CACHE = new java.util.concurrent.ConcurrentHashMap<>(
			2000);
	private static String currentCacheLang = "";

	public static SearchResult bestMatch(Collection<String[]> candidates, List<Criteria> criterias,
			List<Integer> searchColumnIndexes, double threshold, int topN, String lang) {

		if (criterias == null || criterias.isEmpty()) {
			return new SearchResult(java.util.Collections.emptyList(), 0, 0, 0, null, null, null, 0);
		}

		// Clear cache if language changed
		if (!currentCacheLang.equalsIgnoreCase(lang)) {
			PHONETIC_CACHE.clear();
			currentCacheLang = lang;
		}

		boolean isFrench = "fr".equalsIgnoreCase(lang);

		int count = criterias.size();

		// Pre-optimized criteria data
		String[] criteriaValues = new String[count];
		String[] criteriaPhoneticCodes = new String[count];

		for (int i = 0; i < count; i++) {
			Criteria cI = criterias.get(i);
			if (cI != null && !cI.isBlank()) {
				criteriaValues[i] = cI.value;
				// PRE-OPTIMIZATION: Calculate search criteria phonetic code ONCE
				criteriaPhoneticCodes[i] = isFrench ? FRENCH_ENGINE.encode(cI.value) : DEFAULT_ENGINE.encode(cI.value);
			}
		}

		// We need to collect all that pass criteria to calculate stats accurately
		List<LineSimResult> allPotential = candidates.parallelStream().map(t -> {

			LineSimResult lsr = new LineSimResult();
			lsr.setCandidate(t);

			lsr.initSimResults(criterias);

			List<SimResult> simResults = lsr.getSimResults();

			for (int i = 0; i < count; i++) {
				SimResult sr = simResults.get(i);
				Criteria c = sr.getCriteria();
				if (c == null || c.isBlank()) continue;

				String critValue = c.getValue();
				String critPhonetic = criteriaPhoneticCodes[i];

				for (int idx : searchColumnIndexes) {
					if (idx < 0 || idx >= t.length) continue;
					String cellValue = (t[idx] != null) ? t[idx].trim().toUpperCase() : "";
					if (cellValue.isEmpty()) continue;

					double spellingScore = 0.0;
					double phoneticScore = 0.0;

					if (c.getMatchingType() == Criteria.MatchingType.EXACT) {
						spellingScore = cellValue.equals(critValue) ? 1.0 : 0.0;
						phoneticScore = 1.0; // Phonetic irrelevant for EXACT
					} else if (c.getMatchingType() == Criteria.MatchingType.REGEX && c.getPattern() != null) {
						spellingScore = c.getPattern().matcher(cellValue).find() ? 1.0 : 0.0;
						phoneticScore = 1.0; // Phonetic irrelevant for REGEX
					} else {
						// SIMILARITY
						spellingScore = SPELLING_STRATEGY.apply(cellValue, critValue);

						// Phonetic Score
						String cellPhonetic = PHONETIC_CACHE.computeIfAbsent(cellValue, k -> isFrench ? FRENCH_ENGINE.encode(k) : DEFAULT_ENGINE.encode(k));

						if (cellPhonetic.equals(critPhonetic)) {
							phoneticScore = 1.0;
						} else {
							phoneticScore = SPELLING_STRATEGY.apply(cellPhonetic, critPhonetic);
						}
					}

					double score = calculateScore(c, spellingScore, phoneticScore);

					if (Double.compare(score, sr.getScore()) > 0 && spellingScore >= c.getMinSpellingScore() && phoneticScore >= c.getMinPhoneticScore()) {
						sr.setPhoneticScore(phoneticScore);
						sr.setSpellingScore(spellingScore);
						sr.setScore(score);
						sr.setColumnIndex(idx);
					}
				}
			}

			lsr.setScore(calculateScore(lsr));

			return lsr;

		}).filter(p -> p != null && p.getScore() > 0 && p.getScore() >= threshold).sorted().limit(topN).collect(Collectors.toList());

		return new SearchResult(allPotential);
	}

	private static double calculateScore(Criteria cr, double spellingScore, double phoneticScore) {
		double totalWeight = cr.getSpellingWeight() + cr.getPhoneticWeight();
		if (totalWeight == 0) return 0.0;
		return (spellingScore * cr.getSpellingWeight() + phoneticScore * cr.getPhoneticWeight()) / totalWeight;
	}

	private static double calculateScore(LineSimResult lsr) {
		if (lsr.getSimResults() == null || lsr.getSimResults().isEmpty()) {
			return 0.0;
		}

		double totalScore = 1.0;
		boolean hasActiveCriteria = false;

		for (SimResult sr : lsr.getSimResults()) {
			Criteria c = sr.getCriteria();
			if (c == null || c.isBlank()) continue;

			hasActiveCriteria = true;
			totalScore *= sr.getScore();
		}

		return hasActiveCriteria ? totalScore : 0.0;
	}
}