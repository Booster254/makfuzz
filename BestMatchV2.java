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

public class BestMatchV2 {

	static final String SEP = ",";
    
    public static List<SimResult> bestMatch(Collection<String[]> candidates, String[] searched, double[] weights, double threshold, int topN) {
    	
		String[] searched2 = StringUtils.join(searched, SEP).toUpperCase().split(SEP);
    	
    	int c = searched2.length;
    	
        return candidates.stream()
                .filter(t -> t.length >= 2)
                .map(t -> {
                	double[] scoreDetails = new double[c];
                    
                    double score = 0d;
                    
                    for (int i = 0 ; i< c ; i++) {
                	   double sim = StringDistance.jaro(t[i], searched2[i]);
                	   score+= weights[i] * sim;
                    }
                    
                    return new SimResult(t, score, scoreDetails);
                })
                .filter(p -> p.score >= threshold)
                .distinct()
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(topN)
                .collect(Collectors.toList());
    }
    
    public static void main(String[] args) throws IOException {
    	
        List<String> firstNames = FileUtils.readLines(new File("./names.csv"), StandardCharsets.UTF_8);
        
        List<String[]> db = firstNames.stream().map(s -> s.toUpperCase().split(SEP)).toList();
        
        String[] searched = { "mohammed ali", "said" };
        
        double[] weights = {0.4d,0.6d};
        
        List<SimResult> bfn = bestMatch(db, searched,weights,0.7, 100);
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
        System.out.println(mapper.writeValueAsString(bfn));
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