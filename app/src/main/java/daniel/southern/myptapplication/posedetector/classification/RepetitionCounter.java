package daniel.southern.myptapplication.posedetector.classification;

import android.util.Log;

/**
 * Class to count the number of repetitions performed for an exercise
 */
public class RepetitionCounter {
    public static final String TAG = "RepetitionCounter";
    //top K thresholds for entering and exiting a pose
    private static final float DEFAULT_ENTER_THRESHOLD = 6f;
    private static final float DEFAULT_EXIT_THRESHOLD = 4f;

    private final String className;
    private final float enterThreshold;
    private final float exitThreshold;

    private int numRepeats;
    private boolean poseEntered;

    /**
     * {@link RepetitionCounter} class constructor
     * @param className name of the exercise class
     */
    public RepetitionCounter(String className) {
        this(className, DEFAULT_ENTER_THRESHOLD, DEFAULT_EXIT_THRESHOLD);
    }

    public RepetitionCounter(String className, float enterThreshold, float exitThreshold) {
        this.className = className;
        this.enterThreshold = enterThreshold;
        this.exitThreshold = exitThreshold;
        numRepeats = 0;
        poseEntered = false;
    }

    /**
     * Method to add a new {@link ClassificationResult} and update the reps
     *
     * @param classificationResult {@link ClassificationResult}
     * @return number of repetitions performed
     */
    public int addClassificationResult(ClassificationResult classificationResult) {
        float poseConfidence = classificationResult.getClassConfidence(className);

        if (!poseEntered) {
            poseEntered = poseConfidence > enterThreshold;
            return numRepeats;
        }

        if (poseConfidence < exitThreshold) {
            numRepeats++;
            poseEntered = false;
        }

        return numRepeats;
    }

    /**
     * Reset the number of repetitions performed for exercise class
     */
    public void resetNumRepeats(){
        Log.i(TAG, "Resetting numRepeats from " + numRepeats + " to 0.");
        numRepeats = 0;
    }

    public String getClassName() {
        return className;
    }

    public int getNumRepeats() {
        return numRepeats;
    }

}
