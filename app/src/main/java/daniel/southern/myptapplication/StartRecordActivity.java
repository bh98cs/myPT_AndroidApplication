package daniel.southern.myptapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextClock;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class StartRecordActivity extends AppCompatActivity implements View.OnClickListener{

    private TextClock clock;
    private TextView currentDate;
    private Button cancelBtn;
    private Button startRecBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_record);
        clock = findViewById(R.id.textClock);
        //set clock to 24 hour format
        clock.setFormat24Hour("HH:mm:ss");
        currentDate = findViewById(R.id.textView_currentDate);

        //set textview to show current date by calling method
        currentDate.setText(getTodayDate());

        cancelBtn = findViewById(R.id.button_cancel);
        cancelBtn.setOnClickListener(this);
        startRecBtn = findViewById(R.id.button_start);
        startRecBtn.setOnClickListener(this);
    }

    //method to retrieve current date
    private String getTodayDate() {
        //get current date
        Date today = Calendar.getInstance().getTime();
        //create simple format for the date to be displayed in
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
        //return current date in the specified format
        return dateFormat.format(today);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.button_cancel){
            cancelButtonClicked();
        } else if (v.getId() == R.id.button_start) {
            startRecButtonClicked();
        }
    }

    private void startRecButtonClicked() {
        Intent intent = new Intent(this, PoseEstimationActivity.class);
        startActivity(intent);
    }

    private void cancelButtonClicked() {
        //TODO: Add Alert dialogue to confirm cancel
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}