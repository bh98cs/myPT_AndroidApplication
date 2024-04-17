package daniel.southern.myptapplication.posedetector.classification;

import static java.util.Collections.max;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ClassificationResult {

    //hash map to store confidence scores for exercises
    private final Map<String, Float> classConfidences;

    /**
     * ClassificationResult class constructor.
     * Creates new hashmap to store classes and confidence scores
     */
    public ClassificationResult() {
        classConfidences = new HashMap<>();
    }

    /**
     * Retrieves all classes.
     * @return set view of exercise classes
     */
    public Set<String> getAllClasses() {
        return classConfidences.keySet();
    }

    /**
     * Returns confidence score for the selected exercise class
     * @param className name of the exercise class
     * @return confidence score for the exercise class passed as a parameter
     */
    public float getClassConfidence(String className) {
        return classConfidences.containsKey(className) ? classConfidences.get(className) : 0;
    }

    /**
     * Retrieves name of the exercise with the highest confidence score
     * @return name of the exercise with the highest confidence score
     */
    public String getMaxConfidenceClass() {
        return max(
                classConfidences.entrySet(),
                (entry1, entry2) -> (int) (entry1.getValue() - entry2.getValue()))
                .getKey();
    }

    /**
     * Increments the confidence score for the exercise class passed as a parameter
     * @param className name of exercise class to increment confidence score for
     */
    public void incrementClassConfidence(String className) {
        classConfidences.put(className,
                classConfidences.containsKey(className) ? classConfidences.get(className) + 1 : 1);
    }

    /**
     * Adds exercise name and its confidence score to the classConfidences hash map
     * @param className name of the exercise class
     * @param confidence confidence score for the exercise
     */
    public void putClassConfidence(String className, float confidence) {
        classConfidences.put(className, confidence);
    }
}
