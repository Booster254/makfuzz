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
    public double weight;
    public double minSpellingScore;
    public double minPhoneticScore;
    public MatchingType matchingType;
    public Pattern pattern;

    public Criteria(String value, double weight, double minSpellingScore, double minPhoneticScore, MatchingType matchingType) {
        super();
        this.value = StringUtils.isBlank(value) ? null : value.trim().toUpperCase();
        this.weight = weight;
        this.minSpellingScore = minSpellingScore;
        this.minPhoneticScore = minPhoneticScore;
        this.matchingType = matchingType;
        
        // Compile regex pattern if matchingType is REGEX
        if (matchingType == MatchingType.REGEX && this.value != null) {
            try {
                this.pattern = Pattern.compile(this.value);
            } catch (Exception e) {
                // If pattern compilation fails, set to null
                this.pattern = null;
            }
        }
    }

    public static Criteria similarity(String value, double weight, double minSpelling, double minPhonetic) {
        return new Criteria(value, weight, minSpelling, minPhonetic, MatchingType.SIMILARITY);
    }

    public static Criteria exact(String value, double weight) {
        return new Criteria(value, weight, -1, -1, MatchingType.EXACT);
    }
    
    public static Criteria regex(String value, double weight) {
        return new Criteria(value, weight, -1, -1, MatchingType.REGEX);
    }
    
    public boolean isBlank() {
        return value == null;
    }
}
