package com.makfuzz.core;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import lombok.Data;

@Data
public class Criteria {

	public enum MatchingType {
		REGEX, SIMILARITY, EXACT
	}

	public String value;
	public double minSpellingScore;
	public double minPhoneticScore;
	public MatchingType matchingType;
	public Pattern pattern;

	private double spellingWeight;
	private double phoneticWeight;

	public Criteria(String value, double spellingWeight, double phoneticWeight, double minSpellingScore,
			double minPhoneticScore, MatchingType matchingType) {
		super();
		this.value = StringUtils.isBlank(value) ? null : value.trim().toUpperCase();
		this.spellingWeight = spellingWeight;
		this.phoneticWeight = phoneticWeight;
		this.minSpellingScore = minSpellingScore;
		this.minPhoneticScore = minPhoneticScore;
		this.matchingType = matchingType;

		// Compile regex pattern if matchingType is REGEX
		if (matchingType == MatchingType.REGEX && this.value != null) {
			try {
				this.pattern = Pattern.compile(this.value);
			} catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

	public static Criteria similarity(String value, double spellingWeight, double phoneticWeight, double minSpelling,
			double minPhonetic) {
		return new Criteria(value, spellingWeight, phoneticWeight, minSpelling, minPhonetic, MatchingType.SIMILARITY);
	}

	public static Criteria exact(String value, double spellingWeight, double phoneticWeight) {
		return new Criteria(value, spellingWeight, phoneticWeight, -1, -1, MatchingType.EXACT);
	}

	public static Criteria regex(String value, double spellingWeight, double phoneticWeight) {
		return new Criteria(value, spellingWeight, phoneticWeight, -1, -1, MatchingType.REGEX);
	}

	public boolean isBlank() {
		return StringUtils.isBlank(value);
	}
}
