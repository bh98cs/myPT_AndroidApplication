package daniel.southern.myptapplication.posedetector;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.android.odml.image.MediaMlImageBuilder;
import com.google.android.odml.image.MlImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import daniel.southern.myptapplication.GraphicOverlay;
import daniel.southern.myptapplication.ScopedExecutor;
import daniel.southern.myptapplication.posedetector.classification.PoseClassifierProcessor;

/**
 * Class to run the pose detector
 */
public class PoseDetectorProcessor {

    private static final String TAG = "PoseDetectorProcessor";
    private final PoseDetector detector;
    private final Context context;
    private final Executor classificationExecutor;
    private final ScopedExecutor executor;
    private PoseWithClassification poseWithClassification;
    private PoseClassifierProcessor poseClassifierProcessor;
    private Boolean shutDown = false;

    /**
     * subclass to run pose detection with pose classification
     */
    protected static class PoseWithClassification {

        private final Pose pose;
        private final Map<String, Object> classificationResult;

        public PoseWithClassification(Pose pose, Map<String, Object> classificationResult) {
            this.pose = pose;
            this.classificationResult = classificationResult;
        }
        public Pose getPose(){
            return pose;
        }
        public Map<String, Object> getClassificationResult(){return classificationResult;}

    }

    public PoseDetectorProcessor(
            Context context,
            PoseDetectorOptionsBase options) {
        detector = PoseDetection.getClient(options);
        this.context = context;
        classificationExecutor = Executors.newSingleThreadExecutor();
        executor = new ScopedExecutor(TaskExecutors.MAIN_THREAD);

    }

    public void stop() {
        detector.close();
    }

    public Map<String, Object> getPoseClassificationResult(){
        try{
            return poseWithClassification.getClassificationResult();
        }
        catch (NullPointerException e){
            return null;
        }
    }

    public void startNewSet(){
        //check pose classifier has been instantiated
        if(poseClassifierProcessor != null){
            //start new set
            poseClassifierProcessor.startNewSet();
        }
    }

    /**
     * Method to check whether user has entered the pose to signal the end of the pose detection.
     * End of pose position is entered by holding both hands above head.
     */
    private void isEndOfExercise(){
        Pose pose = poseWithClassification.getPose();
        if(pose != null){
            float rightWristY = 0;
            float leftWristY = 0;
            float head = 0;
            try{
                rightWristY = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST).getPosition().y;
                leftWristY = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST).getPosition().y;
                //can use any of the landmarks on the face to get a coordinate of the head
                head = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_INNER).getPosition().y;
            }
            catch(NullPointerException e){
            }

            float calcRightHand = rightWristY - head;
            float calcLeftHand = leftWristY - head;
            if(calcLeftHand < 0 && calcRightHand < 0){
                Log.i(TAG, "END OF EXERCISE");
                shutDown = true;
            }
        }
    }

    /**
     * Detect the user's pose within an image
     * @param image image containing user pose
     * @return result of {@link PoseWithClassification}
     */
    protected Task<PoseWithClassification> detectInImage(MlImage image) {
        return detector
                .process(image)
                .continueWith(
                        classificationExecutor,
                        task -> {
                            Pose pose = task.getResult();
                            Map<String, Object> classificationResult = new HashMap<>();

                            if (poseClassifierProcessor == null) {
                                poseClassifierProcessor = new PoseClassifierProcessor(context);
                            }
                            classificationResult = poseClassifierProcessor.getPoseResult(pose);

                            poseWithClassification = new PoseWithClassification(pose, classificationResult);
                            return poseWithClassification;
                        });
    }

    protected void onSuccess(
            @NonNull PoseWithClassification poseWithClassification,
            @NonNull GraphicOverlay graphicOverlay) {
        List<String> classificationResultList = new ArrayList<>();
        try{
            String reps = poseWithClassification.classificationResult.get("reps").toString();
            String exerciseType = poseWithClassification.classificationResult.get("exerciseType").toString();
            //add number of repetitions performed and the name of exercise to the results list
            classificationResultList.add("Reps: " + reps);
            classificationResultList.add(formatExerciseType(exerciseType));
        }
        catch (NullPointerException ignored){
        }

        //check if user is signalling to end exercise
        isEndOfExercise();

        graphicOverlay.add(
                new PoseGraphic(
                        graphicOverlay,
                        poseWithClassification.pose,
                        classificationResultList));
        if(shutDown){
            stop();
            graphicOverlay.clear();
            Toast.makeText(graphicOverlay.getContext(), "Exercise Ended", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Formats the name of the exercise
     * @param exerciseType name of the detected exercise
     * @return name of the exercise formatted for use in the UI
     */
    private String formatExerciseType(String exerciseType) {
        //check exerciseType is not null or empty
        if(exerciseType != null && exerciseType != ""){
            //capitalise the first letter of the exercise type
            return exerciseType.substring(0,1).toUpperCase()
                    + exerciseType.substring(1);
        }
        //return this if no exercise type is given
        return "No exercise identified";
    }

    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Pose detection failed!", e);
    }

    @ExperimentalGetImage
    public void processImageProxy(ImageProxy image, GraphicOverlay graphicOverlay) {

        MlImage mlImage =
                new MediaMlImageBuilder(image.getImage())
                        .setRotation(image.getImageInfo().getRotationDegrees())
                        .build();

        requestDetectInImage(
                mlImage,
                graphicOverlay)
                .addOnCompleteListener(results -> image.close());
    }

    private Task<PoseWithClassification> requestDetectInImage(
            final MlImage image,
            final GraphicOverlay graphicOverlay) {
        return setUpListener(
                detectInImage(image), graphicOverlay);
    }

    private Task<PoseWithClassification> setUpListener(
            Task<PoseWithClassification> task,
            final GraphicOverlay graphicOverlay) {

        return task.addOnSuccessListener(executor, results -> {
                    graphicOverlay.clear();
                    this.onSuccess(results, graphicOverlay);
                    graphicOverlay.postInvalidate();
                }
        ).addOnFailureListener(executor,
                e -> {
            //check user has not opted to end exercise
            if(!shutDown){
                graphicOverlay.clear();
                graphicOverlay.postInvalidate();
                String error = "Failed to process. Error: " + e.getLocalizedMessage();
                Toast.makeText(
                                graphicOverlay.getContext(),
                                error + "\nCause: " + e.getCause(),
                                Toast.LENGTH_SHORT)
                        .show();
                Log.d(TAG, error);
                e.printStackTrace();
                this.onFailure(e);
            }
            });

    }


}
