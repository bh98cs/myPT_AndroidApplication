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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;
import com.google.protobuf.StringValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import daniel.southern.myptapplication.posedetector.PoseDetectorProcessor;
import daniel.southern.myptapplication.posedetector.classification.PoseClassifier;

@ExperimentalGetImage
public class PoseEstimationActivity extends AppCompatActivity implements View.OnClickListener{

    public static final String EXTRA_ITEM_SET1 = "daniel.southern.myptapplication.EXTRA_ITEM_SET1";
    public static final String EXTRA_ITEM_SET2 = "daniel.southern.myptapplication.EXTRA_ITEM_SET2";
    public static final String EXTRA_ITEM_SET3 = "daniel.southern.myptapplication.EXTRA_ITEM_SET3";
    public static final String EXTRA_ITEM_EXERCISE_TYPE = "daniel.southern.myptapplication.EXTRA_ITEM_EXERCISE_TYPE";

    public static final String TAG = "PoseEstimationActivity";
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
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
    private int numSets = 0;
    private int[] reps = new int[3];


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
                    calculateRepsForSet();
                    //call method to stop timer
                    stopTimer();
                }
            }
        });
    }

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

    //method to stop timer
    private void stopTimer() {
        timerView.stop();
        timerView.setBase(SystemClock.elapsedRealtime());
    }

    //method to start timer
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

    private void calculateRepsForSet(){
        int totalReps;
        try{
        //get number of reps performed, needs to be cast to a string and then cast to an integer
         totalReps = Integer.parseInt(String.valueOf(poseDetectorProcessor.getPoseClassificationResult()
                .get("reps")));
        }
        catch(NumberFormatException e){
            //set
            totalReps = 0;
        }

        if(numSets > 0){
            //for loop to go through all entries in reps array
            for(int i = 0; i < numSets; i++){
                //subtract previously saved reps from save reps variable
                //the saveReps variable is the TOTAL reps performed, this is how we
                // calculate the number of reps performed in this set
                totalReps -= reps[i];
            }
        }
        Log.i(TAG, "Saving " + totalReps + " reps for set " + numSets);
        //save the number of reps in array
        reps[numSets] = totalReps;
        //increment number of sets performed
        numSets++;
        if(numSets >= 2){
            //disable the timer button as maximum number of sets has been reached
            startStopTimer.setEnabled(false);
        }
    }

    private void endRecording() {
        //save reps for the last set performed
        calculateRepsForSet();

        Map<String, Object> poseClassificationResult = poseDetectorProcessor.getPoseClassificationResult();

        Intent intent = new Intent(this, EndRecordActivity.class);
        String exerciseType = poseClassificationResult.get("exerciseType").toString();

        //intent data to EndRecordActivity to save to DB
        intent.putExtra(EXTRA_ITEM_EXERCISE_TYPE, exerciseType);
        //TODO: intent data as an array if possible
        intent.putExtra(EXTRA_ITEM_SET1, reps[0]);
        intent.putExtra(EXTRA_ITEM_SET2, reps[1]);
        intent.putExtra(EXTRA_ITEM_SET3, reps[2]);
        //send user to EndRecordActivity
        startActivity(intent);
    }
}