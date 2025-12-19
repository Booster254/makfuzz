package j25;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;

import com.fasterxml.jackson.databind.ObjectMapper;

public class BestMatchV1 {

//	public static List<SimpleEntry<String, Double>> bestMatch(
//	        Collection<String> candidates, String target) {
//
//	    LevenshteinDistance ld = LevenshteinDistance.getDefaultInstance();
//
//	    return candidates.stream()
//	            .distinct()
//	            .map(c -> {
//	                int dist = ld.apply(c, target);
//	                double sim = 1.0 - (dist / (double) Math.max(c.length(), target.length()));
//	                return new AbstractMap.SimpleEntry<>(c, sim);
//	            })
//	            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
//	            .toList();
//	}

	public static List<PersonCompResult> bestMatch(Collection<String[]> candidates, String[] target, int topN) {

		LevenshteinDistance ld = LevenshteinDistance.getDefaultInstance();

		return candidates.stream().filter(t -> t.length >= 2).map(t -> {
			int dist1 = ld.apply(t[0], target[0]);
			int dist2 = ld.apply(t[1], target[1]);

			double sim1 = 1.0 - (dist1 / (double) Math.max(t[0].length(), target[0].length()));
			double sim2 = 1.0 - (dist2 / (double) Math.max(t[1].length(), target[1].length()));

			double score = sim1 * 0.3 + sim2 * 0.7;

			return new PersonCompResult(t[0], t[1], score, sim1, sim2);
		}).distinct().sorted((a, b) -> Double.compare(b.score, a.score)).limit(topN).collect(Collectors.toList());
	}

	public static void main(String[] args) throws IOException {
		List<String> firstNames = FileUtils.readLines(new File("./names.csv"), StandardCharsets.UTF_8);
		List<String[]> all = firstNames.stream().map(s -> s.split(",")).toList();

		String[] fn = { "jalal", "mansour" };

		List<PersonCompResult> bfn = bestMatch(all, fn, 100);

		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);

		System.out.println(mapper.writeValueAsString(bfn));
	}

	private static class PersonCompResult implements Comparator<PersonCompResult> {

		String fn;

		String ln;

		double score;
		double scoreFn;
		double scoreLn;

		@Override
		public int compare(PersonCompResult o1, PersonCompResult o2) {
			return ((int) (o1.score - o2.score)) * 1000;
		}

		public PersonCompResult(String fn, String ln, double score, double scoreFn, double scoreLn) {
			super();
			this.fn = fn;
			this.ln = ln;
			this.score = score;
			this.scoreFn = scoreFn;
			this.scoreLn = scoreLn;
		}

		@Override
		public int hashCode() {
			return Objects.hash(fn, ln);
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
			PersonCompResult other = (PersonCompResult) obj;

			return Objects.equals(fn, other.fn) && Objects.equals(ln, other.ln);
		}

		public String getFn() {
			return fn;
		}

		public void setFn(String fn) {
			this.fn = fn;
		}

		public String getLn() {
			return ln;
		}

		public void setLn(String ln) {
			this.ln = ln;
		}

		public double getScore() {
			return score;
		}

		public void setScore(double score) {
			this.score = score;
		}

		public double getScoreFn() {
			return scoreFn;
		}

		public void setScoreFn(double scoreFn) {
			this.scoreFn = scoreFn;
		}

		public double getScoreLn() {
			return scoreLn;
		}

		public void setScoreLn(double scoreLn) {
			this.scoreLn = scoreLn;
		}

	}
}
