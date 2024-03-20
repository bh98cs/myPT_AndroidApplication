package daniel.southern.myptapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditExerciseLogActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "EditExerciseLogActivity";
    private FirebaseAuth mAuth;
    //declare instance to store current user details
    private FirebaseUser currentUser;
    //declare instances of FireBase database references for uploading data to
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private final FirebaseFirestore database = FirebaseFirestore.getInstance();

    //reference for the item selected for editing
    private DocumentReference editExerciseLogRef;
    private TextView textView_date;
    private TextView textView_exerciseType;
    private EditText editText_set1;
    private EditText editText_set2;
    private EditText editText_set3;
    private EditText editText_weight;
    private EditText editText_notes;
    private String editNotes;
    private Button button_save;
    private Button button_discard;
    private ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_exercise_log);
        //initialise progress bar
        progressBar = findViewById(R.id.progressBar);
        //set progress bar to visible whilst loading data
        progressBar.setVisibility(View.VISIBLE);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        //retrieve current user to check if they're already logged in
        currentUser = mAuth.getCurrentUser();
        //check user is logged in before proceeding
        checkLoggedIn(currentUser);
        //get intent that started activity
        Intent intent = getIntent();
        //store document reference for item being edited
        String firebaseDocId = intent.getStringExtra(MainActivity.EXTRA_ITEM_FIREBASE_ID);
        textView_date = findViewById(R.id.textView_date);
        textView_exerciseType = findViewById(R.id.textView_exerciseType);
        editText_set1 = findViewById(R.id.editText_set1);
        editText_set2 = findViewById(R.id.editText_set2);
        editText_set3 = findViewById(R.id.editText_set3);
        editText_weight = findViewById(R.id.editText_weight);
        editText_notes = findViewById(R.id.editText_notes);

        button_discard = findViewById(R.id.button_discard);
        button_discard.setOnClickListener(this);
        button_save = findViewById(R.id.button_save);
        button_save.setOnClickListener(this);

        //check if intent to this activity passed a document ID
        if(firebaseDocId != null){
            Log.d(TAG, "onCreate: Retrieving data for " + firebaseDocId);
            //retrieve item from firebase using ID intented from MainActivity
            editExerciseLogRef = database.collection("exerciseLogs").document(firebaseDocId);
            //position is not default value therefore a position of item to edit has been given
            loadExerciseLogDetails();
        }
        else{
            //TODO: Alert dialogue to say no exercise data found
        }
    }

    private void checkLoggedIn(FirebaseUser currentUser) {
        //send user to login page if not already logged in
        if(currentUser == null){
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
    }
    private void loadExerciseLogDetails() {
        editExerciseLogRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                //set progress bar to invisible as task has completed
                progressBar.setVisibility(View.INVISIBLE);
                //check if item has been successfully retrieved
                if(task.isSuccessful()){
                    //get result of task
                    DocumentSnapshot document = task.getResult();
                    //check if the item being edited exists within the database
                    if(document.exists()){
                        //document retrieved successfully
                        Log.i(TAG, "Firebase Document retrieved!");
                        //load data into local ExerciseLog object
                        ExerciseLog exerciseLog = document.toObject(ExerciseLog.class);
                        //set format for date
                        DateFormat df = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
                        //convert date to a string
                        String date = df.format(exerciseLog.getDate());

                        //display data on activity
                        textView_exerciseType.setText(exerciseLog.getExerciseType());
                        textView_date.setText(date);
                        editText_set1.setText(String.valueOf(exerciseLog.getSet1()));
                        editText_set2.setText(String.valueOf(exerciseLog.getSet2()));
                        editText_set3.setText(String.valueOf(exerciseLog.getSet3()));
                        editText_weight.setText(String.valueOf(exerciseLog.getWeight()));
                        editText_notes.setText(exerciseLog.getNotes());
                    }
                    else{
                        //document is not in FireStore database (this should not occur)
                        Log.w(TAG, "Document does not exist.");
                    }
                }else {
                    //unable to retrieve data from Firebase Database
                    Log.e(TAG, "Failed to retrieve FireBase Document.", task.getException());
                    Toast.makeText(EditExerciseLogActivity.this, "Unable to retrieve data.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void updateExerciseLog() {
        //display progress bar whilst saving data
        progressBar.setVisibility(View.VISIBLE);
        //disable save button for error prevention
        button_save.setClickable(false);
        //create new hashmap
        Map<String, Object> editedExerciseLog = new HashMap<>();

        //retrieve user input from views
        int set1 = Integer.parseInt(editText_set1.getText().toString());
        int set2 = Integer.parseInt(editText_set2.getText().toString());
        int set3 = Integer.parseInt(editText_set3.getText().toString());
        int weight = Integer.parseInt(editText_weight.getText().toString());
        String notes = editText_notes.getText().toString();

        //add user input to hash map
        editedExerciseLog.put("set1", set1);
        editedExerciseLog.put("set2", set2);
        editedExerciseLog.put("set3", set3);
        editedExerciseLog.put("weight", weight);
        editedExerciseLog.put("notes", notes);

        editExerciseLogRef.update(editedExerciseLog)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //user feedback to confirm item successfully updated
                        Toast.makeText(EditExerciseLogActivity.this, "Updated Successfully", Toast.LENGTH_SHORT).show();
                        //return user to main screen
                        Intent intent = new Intent(EditExerciseLogActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //user feedback to advise update was unsuccessful
                        Toast.makeText(EditExerciseLogActivity.this, "Unable to Update details", Toast.LENGTH_SHORT).show();
                        //log the error causing update to fail
                        Log.e(TAG, "onFailure: Unable to update Firebase Document", e);
                        //re-enable save button so user can try again
                        button_save.setClickable(true);
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        //set progress bar to invisible when complete
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                });


    }


    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.button_discard){
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        } else if (v.getId() == R.id.button_save) {
            updateExerciseLog();
        }
    }
}