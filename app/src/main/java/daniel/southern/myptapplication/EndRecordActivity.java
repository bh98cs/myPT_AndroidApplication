package daniel.southern.myptapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EndRecordActivity extends AppCompatActivity implements View.OnClickListener{
    //TODO: Continue to implement code for this activity
    public static final String TAG = "EndRecordActivity";
    private TextView currentDate;
    private EditText weight;
    private EditText notes;
    private TextView exerciseType;
    private Button saveBtn;
    private Button discardBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end_record);

        currentDate = findViewById(R.id.textView_currentDate);
        weight = findViewById(R.id.editText_weight);
        notes = findViewById(R.id.editText_notes);

        //set textview to show current date by calling method
        currentDate.setText(getTodayDate());

        saveBtn = findViewById(R.id.button_save);
        saveBtn.setOnClickListener(this);
        discardBtn = findViewById(R.id.button_discard);
        discardBtn.setOnClickListener(this);
    }

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
        if(v.getId() == R.id.button_save){
            saveExercise();
        } else if (v.getId() == R.id.button_discard) {
            discardExercise();
        }
    }

    private void discardExercise() {
        //TODO: add code to confirm discard exercise
        Log.i(TAG, "Discard Exercise clicked.");
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void saveExercise() {
        //TODO: add code to save exercise
        Log.i(TAG, "Save Exercise Clicked");
    }
}