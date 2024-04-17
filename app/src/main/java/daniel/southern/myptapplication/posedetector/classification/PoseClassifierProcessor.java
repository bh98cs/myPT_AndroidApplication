package daniel.southern.myptapplication.posedetector.classification;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.WorkerThread;

import com.google.android.gms.common.internal.Preconditions;
import com.google.mlkit.vision.pose.Pose;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for pose classification and repetition counting
 */
public class PoseClassifierProcessor {

        private static final String TAG = "PoseClassifierProcessor";
        //dataset for recognised exercises
        private static final String POSE_SAMPLES_FILE = "pose/fitness_pose_samples.csv";

        // declare classes of exercise
        private static final String PUSHUPS_CLASS = "pushups_down";
        private static final String SQUATS_CLASS = "squats_down";
        private static final String[] POSE_CLASSES = {
                PUSHUPS_CLASS, SQUATS_CLASS
        };
        private EMASmoothing emaSmoothing;
        private List<RepetitionCounter> repCounters;
        private PoseClassifier poseClassifier;
        //string to hold number of reps performed in previous set. String variable used as
        //the hashmap requires a string and cannot take an integer
        private String lastRepResult;

    /**
     * {@link PoseClassifierProcessor} class constructor
     * @param context application environment
     */
    @WorkerThread
        public PoseClassifierProcessor(Context context) {
            Preconditions.checkState(Looper.myLooper() != Looper.getMainLooper());
            emaSmoothing = new EMASmoothing();
            repCounters = new ArrayList<>();
            //last rep result set to blank
            lastRepResult = "";

            loadPoseSamples(context);
        }

    /**
     * Reads a CSV file containing pose coordinates to create a list of recognised poses
     * @param context application environment
     */
    private void loadPoseSamples(Context context) {
            List<PoseSample> poseSamples = new ArrayList<>();
            //read data file
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(context.getAssets().open(POSE_SAMPLES_FILE)));
                String csvLine = reader.readLine();
                while (csvLine != null) {
                    // If line is not a valid, we'll get null and skip adding to the list.
                    PoseSample poseSample = PoseSample.getPoseSample(csvLine, ",");
                    if (poseSample != null) {
                        poseSamples.add(poseSample);
                    }
                    csvLine = reader.readLine();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error when loading pose samples.\n" + e);
            }
            poseClassifier = new PoseClassifier(poseSamples);
            for (String className : POSE_CLASSES) {
                //create repetition counter for exercises
                repCounters.add(new RepetitionCounter(className));
            }

        }

    /**
     * Method to start a new set. Repetition counter is reset to 0.
     */
    public void startNewSet(){
            for(RepetitionCounter repetitionCounter : repCounters){
                    repetitionCounter.resetNumRepeats();
            }
        }


    /**
     * Retrieves the result of the pose estimation and classification
     * @param pose the {@link Pose} to be classified
     * @return list of pose classification results
     */
    @WorkerThread
        public Map<String, Object> getPoseResult(Pose pose) {

            Preconditions.checkState(Looper.myLooper() != Looper.getMainLooper());
            Map<String, Object> result = new HashMap<>();
            ClassificationResult classification = poseClassifier.classify(pose);

            // Feed pose to smoothing even if no pose found.
            classification = emaSmoothing.getSmoothedResult(classification);

            // Return without updating repCounter if no pose identified
            if (pose.getAllPoseLandmarks().isEmpty()) {
                result.put("reps", lastRepResult);
                return result;
            }
            //counting repetitions
            for (RepetitionCounter repCounter : repCounters) {
                //number of reps before this pose classification
                int repsBefore = repCounter.getNumRepeats();
                //number of reps after pose classification
                int repsAfter = repCounter.addClassificationResult(classification);
                //if the number of repetitions has increased, the user has successfully performed a repetition
                if (repsAfter > repsBefore) {
                    // Play a beep when rep counter updates to signal to the user
                    ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP);
                    //retrieve number of repetitions performed
                    lastRepResult = String.valueOf(repsAfter);
                    break;
                }
            }
            //add number of reps performed to the result
            result.put("reps", lastRepResult);


            // Add maxConfidence class of current frame to result if pose is found.
            if (!pose.getAllPoseLandmarks().isEmpty()) {
                String maxConfidenceClass = classification.getMaxConfidenceClass();
                result.put("exerciseType", maxConfidenceClass);
            }

            return result;
        }


}
