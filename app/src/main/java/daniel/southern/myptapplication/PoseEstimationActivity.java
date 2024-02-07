package daniel.southern.myptapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Chronometer;
import android.widget.CompoundButton;

public class PoseEstimationActivity extends AppCompatActivity {

    public static final String TAG = "PoseEstimationActivity";
    Chronometer timerView;
    CompoundButton startStopTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pose_estimation);

        timerView = findViewById(R.id.timerView);
        startStopTimer = findViewById(R.id.toggleTimerButton);


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
                    //call method to stop timer
                    stopTimer();
                }
            }
        });
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
}