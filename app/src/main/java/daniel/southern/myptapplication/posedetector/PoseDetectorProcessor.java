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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import daniel.southern.myptapplication.GraphicOverlay;
import daniel.southern.myptapplication.ScopedExecutor;
import daniel.southern.myptapplication.posedetector.classification.PoseClassifierProcessor;

public class PoseDetectorProcessor {

    private static final String TAG = "PoseDetectorProcessor";
    private final PoseDetector detector;
    private final Context context;
    private final Executor classificationExecutor;
    private final ScopedExecutor executor;
    private boolean isShutdown;
    private PoseWithClassification poseWithClassification;

    private PoseClassifierProcessor poseClassifierProcessor;
    protected static class PoseWithClassification {

        private final Pose pose;
        private final Map<String, Object> classificationResult;

        public PoseWithClassification(Pose pose, Map<String, Object> classificationResult) {
            this.pose = pose;
            this.classificationResult = classificationResult;
        }

        public Pose getPose() {
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
        return poseWithClassification.getClassificationResult();
    }


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
            classificationResultList.add(reps + " reps");
            classificationResultList.add(exerciseType);
        }
        catch (NullPointerException e){
        }

        graphicOverlay.add(
                new PoseGraphic(
                        graphicOverlay,
                        poseWithClassification.pose,
                        classificationResultList));
    }

    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Pose detection failed!", e);
    }

    @ExperimentalGetImage
    public void processImageProxy(ImageProxy image, GraphicOverlay graphicOverlay) {
        if (isShutdown) {
            image.close();
            return;
        }

        MlImage mlImage =
                new MediaMlImageBuilder(image.getImage())
                        .setRotation(image.getImageInfo().getRotationDegrees())
                        .build();

        requestDetectInImage(
                mlImage,
                graphicOverlay)
                // When the image is from CameraX analysis use case, must call image.close() on received
                // images when finished using them. Otherwise, new images may not be received or the
                // camera may stall.
                // Currently MlImage doesn't support ImageProxy directly, so we still need to call
                // ImageProxy.close() here.
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
                });
    }


}
