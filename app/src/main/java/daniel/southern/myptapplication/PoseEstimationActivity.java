package daniel.southern.myptapplication;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.util.Map;
import java.util.Objects;

import daniel.southern.myptapplication.posedetector.PoseDetectorProcessor;

@ExperimentalGetImage
public class PoseEstimationActivity extends AppCompatActivity implements View.OnClickListener{

    //constant to intent number of repetitions performed
    public static final String EXTRA_ITEM_REPS = "daniel.southern.myptapplication.EXTRA_ITEM_REPS";

    //constant to intent name of exercise performed
    public static final String EXTRA_ITEM_EXERCISE_TYPE = "daniel.southern.myptapplication.EXTRA_ITEM_EXERCISE_TYPE";

    // tag for logs
    public static final String TAG = "PoseEstimationActivity";
    @Nullable
    private ProcessCameraProvider cameraProvider;
    private PreviewView previewView;
    @Nullable private Preview previewUseCase;
    private ImageAnalysis analysisUseCase;
    private GraphicOverlay graphicOverlay;
    @Nullable private Camera camera;
    @Nullable private PoseDetectorProcessor poseDetectorProcessor;
    private CameraSelector cameraSelector;
    private Chronometer timerView;
    private CompoundButton startStopTimer;
    private Button endBtn;
    private TextView textViewSetCount;
    private int numSets = 0;
    private int[] reps = new int[3];


    //request permission for the camera
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.i(TAG, "Permission granted");

                } else {
                    Log.w(TAG, "Permission Denied.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pose_estimation);

        //select front facing camera to use for pose estimation (so user can see the screen)
        cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();

        previewView = findViewById(R.id.previewView);
        graphicOverlay = findViewById(R.id.graphicOverlay);

        new ViewModelProvider((ViewModelStoreOwner) this, (ViewModelProvider.Factory) ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()))
                .get(CameraXViewModel.class)
                .getProcessCameraProvider()
                .observe(
                        this,
                        provider -> {
                            cameraProvider = provider;
                            bindPreviewUseCase();
                            bindAnalysisUseCase();
                        });

        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
        } else{
            Log.w(TAG, "Permission not granted.");
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        textViewSetCount = findViewById(R.id.textView_setCount);

        //set set counter to 1 initially
        textViewSetCount.setText("1");

        timerView = findViewById(R.id.timerView);
        startStopTimer = findViewById(R.id.toggleTimerButton);
        endBtn = findViewById(R.id.endButton);
        endBtn.setOnClickListener(this);


        //set on check changed listener for toggle timer button
        startStopTimer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //button is checked therefore start timer
                if(isChecked){
                    Log.i(TAG, "Timer start.");
                    //call method to start timer
                    startTimer();
                }
                //not checked so timer should stop
                else{
                    Log.i(TAG, "Timer stop.");
                    saveRepetitionsForSet();
                    //begin a new set for the exercise
                    poseDetectorProcessor.startNewSet();
                    //call method to stop timer
                    stopTimer();
                }
            }
        });
    }

    /**
     * Creates and starts pose detector. Binds the camera to the application lifecycle
     */
    private void bindAnalysisUseCase() {
        PoseDetectorOptions.Builder builder = new PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE);
        poseDetectorProcessor = new PoseDetectorProcessor(this, builder.build());

        ImageAnalysis.Builder builder2 = new ImageAnalysis.Builder();
        analysisUseCase = builder2.build();

        analysisUseCase.setAnalyzer(ContextCompat.getMainExecutor(this),
                imageProxy -> {
                    graphicOverlay.setImageSourceInfo(imageProxy.getHeight(), imageProxy.getWidth());
                    poseDetectorProcessor.processImageProxy(imageProxy, graphicOverlay);
                });

        cameraProvider.bindToLifecycle(this, cameraSelector, analysisUseCase);
    }

    /**
     * Creates camera preview and binds to lifecycle so user can see real-time pose estimation
     */
    private void bindPreviewUseCase() {
        if (previewUseCase != null) {
            cameraProvider.unbind(previewUseCase);
        }

        Preview.Builder builder = new Preview.Builder();

        previewUseCase = builder.build();
        previewUseCase.setSurfaceProvider(previewView.getSurfaceProvider());
        camera =
                cameraProvider.bindToLifecycle(/* lifecycleOwner= */ this, cameraSelector, previewUseCase);
    }

    /**
     * Stops timer
     */
    private void stopTimer() {
        timerView.stop();
        timerView.setBase(SystemClock.elapsedRealtime());
    }

    /**
     * Starts timer
     */
    private void startTimer() {
        timerView.setBase(SystemClock.elapsedRealtime());
        timerView.start();
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.endButton){
            endRecording();
        }
    }

    /**
     * caches the number of reps performed in an array to be uploaded to the database later
     */
    private void saveRepetitionsForSet(){
        int repetitions;
        try{
            //get number of reps performed, needs to be cast to a string and then cast to an integer
            repetitions = Integer.parseInt(String.valueOf(poseDetectorProcessor.getPoseClassificationResult()
                        .get("reps")));

        }
        catch(NumberFormatException | NullPointerException e){
            //set number of reps to 0 if no data for reps is given
            repetitions = 0;
        }

        Log.i(TAG, "Saving " + repetitions + " reps for set " + numSets);
        //save the number of reps in array
        reps[numSets] = repetitions;
        //increment sets counter
        numSets++;
        //update set counter (add one as is 0-indexed)
        textViewSetCount.setText(String.valueOf(numSets+1));
        if(numSets >= 2){
            //disable the timer button as maximum number of sets has been reached
            startStopTimer.setEnabled(false);
            //set timer and timer button to invisible
            startStopTimer.setVisibility(View.INVISIBLE);
            timerView.setVisibility(View.INVISIBLE);
        }


    }

    /**
     * Formats the name of the exercise to be displayed in the UI and saved to database
     * @param exerciseType name of the exercise classification
     * @return formatted name of the exercise
     */
    private String formatExerciseType(String exerciseType) {
        String formattedExerciseType;
        //check exerciseType is not null or empty
        if(exerciseType != null && exerciseType != ""){
            //get the index of the end of the exercise's name (all exercise names end with a '_' followed by their 'up'
            // or 'down' state)
            int endOfExerciseName = exerciseType.indexOf("_");
            //remove all characters from the name after '_'
            formattedExerciseType = exerciseType.substring(0, endOfExerciseName);
            //capitalise the first letter of the exercise type
            formattedExerciseType = formattedExerciseType.substring(0,1).toUpperCase()
                    + formattedExerciseType.substring(1);
            return formattedExerciseType;
        }
        //return this if no exercise type is given
        return "No exercise identified";
    }

    /**
     * Ends the pose detector and exercise recording
     */
    private void endRecording() {
        //save reps for the last set performed
        saveRepetitionsForSet();

        Map<String, Object> poseClassificationResult = Objects.requireNonNull(poseDetectorProcessor)
                .getPoseClassificationResult();

        Intent intent = new Intent(this, EndRecordActivity.class);
        String exerciseType;

        try{
            exerciseType = formatExerciseType(poseClassificationResult.get("exerciseType")
                    .toString());
        }
        catch(NullPointerException e){
            exerciseType = "No Exercise Identified";
        }

        //intent data to EndRecordActivity to save to DB
        intent.putExtra(EXTRA_ITEM_EXERCISE_TYPE, exerciseType);
        intent.putExtra(EXTRA_ITEM_REPS, reps);
        //send user to EndRecordActivity
        startActivity(intent);
    }
}