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

public class PoseClassifierProcessor {

        private static final String TAG = "PoseClassifierProcessor";
        //TODO: Create your own pose samples file to include deadlifts
        private static final String POSE_SAMPLES_FILE = "pose/fitness_pose_samples.csv";

        // Specify classes for which we want rep counting.
        // These are the labels in the given {@code POSE_SAMPLES_FILE}. You can set your own class labels
        // for your pose samples.
        private static final String PUSHUPS_CLASS = "pushups_down";
        private static final String SQUATS_CLASS = "squats_down";
        private static final String[] POSE_CLASSES = {
                PUSHUPS_CLASS, SQUATS_CLASS
        };

        //private final boolean isStreamMode;

        private EMASmoothing emaSmoothing;
        private List<RepetitionCounter> repCounters;
        private PoseClassifier poseClassifier;
        private String lastRepResult;

        @WorkerThread
        public PoseClassifierProcessor(Context context) {
            Preconditions.checkState(Looper.myLooper() != Looper.getMainLooper());
            emaSmoothing = new EMASmoothing();
            repCounters = new ArrayList<>();
            lastRepResult = "";

            loadPoseSamples(context);
        }

        private void loadPoseSamples(Context context) {
            List<PoseSample> poseSamples = new ArrayList<>();
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
                repCounters.add(new RepetitionCounter(className));
            }

        }

    public void startNewSet(){
            for(RepetitionCounter repetitionCounter : repCounters){
                repetitionCounter.resetNumRepeats();
            }
        }

        /**
         * Given a new {@link Pose} input, returns a list of formatted {@link String}s with Pose
         * classification results.
         *
         * <p>Currently it returns up to 2 strings as following:
         * 0: PoseClass : X reps
         * 1: PoseClass : [0.0-1.0] confidence
         */
        @WorkerThread
        public Map<String, Object> getPoseResult(Pose pose) {

            Preconditions.checkState(Looper.myLooper() != Looper.getMainLooper());
            Map<String, Object> result = new HashMap<>();
            ClassificationResult classification = poseClassifier.classify(pose);

            // Feed pose to smoothing even if no pose found.
            classification = emaSmoothing.getSmoothedResult(classification);

            // Return early without updating repCounter if no pose found.
            if (pose.getAllPoseLandmarks().isEmpty()) {
                result.put("reps", lastRepResult);
                return result;
            }
            for (RepetitionCounter repCounter : repCounters) {
                int repsBefore = repCounter.getNumRepeats();
                int repsAfter = repCounter.addClassificationResult(classification);
                if (repsAfter > repsBefore) {
                    // Play a fun beep when rep counter updates.
                    ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP);
                    lastRepResult = String.valueOf(repsAfter);
                    break;
                }
            }
            result.put("reps", lastRepResult);


            // Add maxConfidence class of current frame to result if pose is found.
            if (!pose.getAllPoseLandmarks().isEmpty()) {
                String maxConfidenceClass = classification.getMaxConfidenceClass();
                result.put("exerciseType", maxConfidenceClass);
            }

            return result;
        }


}
