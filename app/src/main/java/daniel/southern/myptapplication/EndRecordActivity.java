package daniel.southern.myptapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ExperimentalGetImage;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EndRecordActivity extends AppCompatActivity implements View.OnClickListener{
    public static final String TAG = "EndRecordActivity";
    private TextView currentDate;
    private EditText weightInput;
    private EditText notesInput;
    private TextView exerciseTypeDisplay;
    private Button saveBtn;
    private Button discardBtn;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore database = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference storageRef;
    private int[] reps = new int[3];
    private String exerciseType;

    @ExperimentalGetImage
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end_record);

        //initialise Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        //retrieve current user to check if they're already logged in
        currentUser = mAuth.getCurrentUser();
        //check user is logged in before proceeding
        updateUI(currentUser);

        //get intent that started activity
        Intent intent = getIntent();
        //store data for repetitions of exercise
        reps = intent.getIntArrayExtra(PoseEstimationActivity.EXTRA_ITEM_REPS);
        //store exercise type detected in previous activity
        exerciseType = intent.getStringExtra(PoseEstimationActivity.EXTRA_ITEM_EXERCISE_TYPE);


        currentDate = findViewById(R.id.textView_currentDate);
        weightInput = findViewById(R.id.editText_weight);
        notesInput = findViewById(R.id.editText_notes);
        exerciseTypeDisplay = findViewById(R.id.textView_exerciseType);

        //set text view to show the exercise type detected in previous activity
        exerciseTypeDisplay.setText(exerciseType);

        //set textview to show current date by calling method
        currentDate.setText(getTodayDate());

        saveBtn = findViewById(R.id.button_save);
        saveBtn.setOnClickListener(this);
        discardBtn = findViewById(R.id.button_discard);
        discardBtn.setOnClickListener(this);
    }

    private void updateUI(FirebaseUser currentUser) {
        //send user to login page if not already logged in
        if(currentUser == null){
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
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
        //AlertDialog to request confirmation from user before discarding exercise
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Discard Exercise")
                .setMessage("Are you sure you wish to discard this exercise? If you confirm all data for this exercise" +
                        " will be lost.")
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i(TAG, "User has confirmed to discard exercise.");
                        //return user to main activity
                        Intent intent = new Intent(EndRecordActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i(TAG, "Discard exercise request cancelled.");
                        //do nothing as user wants to continue on this activity
                    }
                });

        AlertDialog ad = builder.create();
        ad.show();

    }

    private void saveExercise() {
        Log.i(TAG, "Save Exercise Clicked");
        int weight;
        //get current date
        Date date = Calendar.getInstance().getTime();
        try{
        //store user input for weight
        weight = Integer.parseInt(weightInput.getText().toString());
        }catch(NumberFormatException e){
            //no weight was given therefore string is empty, set weight to 0
            weight = 0;
        }

        //store user input for notes
        String notes = notesInput.getText().toString();

        //create Hashmap for upload to Document
        Map<String, Object> exerciseLog = new HashMap<>();
        exerciseLog.put("exerciseType", exerciseType);
        exerciseLog.put("date", date);
        exerciseLog.put("set1", reps[0]);
        exerciseLog.put("set2", reps[1]);
        exerciseLog.put("set3", reps[2]);
        exerciseLog.put("weight", weight);
        exerciseLog.put("notes", notes);
        //TODO: potentially need to change this when allowing other types of accounts
        exerciseLog.put("user", currentUser.getEmail());

        //add new document with generated ID
        database.collection("exerciseLogs")
                .add(exerciseLog)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        //successfully added new exercise log
                        Log.i(TAG, "onSuccess: exerciseLog uploaded to database.");
                        //provide user feedback that exercise has been uploaded
                        Toast.makeText(EndRecordActivity.this, "New Exercise Saved!", Toast.LENGTH_SHORT).show();
                        //return back to main activity to view all Items
                        Intent intent = new Intent(EndRecordActivity.this, MainActivity.class);
                        startActivity(intent);

                    }

                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                        //user feedback to advise was unable to save new item
                        Toast.makeText(EndRecordActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
                    }
                });
    }
}