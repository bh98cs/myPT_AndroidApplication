package daniel.southern.myptapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toolbar;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class ViewProgressActivity extends AppCompatActivity {

    public static final String TAG = "ViewProgressActivity";
    private LineChart lineChart;
    private Spinner spinner_selectedExercise;
    private String selectedExercise;
    private Toolbar toolbar;
    private String[] exercisesArray;
    private ArrayList<String> exercises = new ArrayList<>();

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private final FirebaseFirestore database = FirebaseFirestore.getInstance();
    private final CollectionReference exerciseLogsRef = database.collection("exerciseLogs");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_progress);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        //retrieve current user to check if they're already logged in
        currentUser = mAuth.getCurrentUser();

        lineChart = findViewById(R.id.line_chart);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(false);

        //initialise spinner
        spinner_selectedExercise = findViewById(R.id.spinner_selectExercise);
        //initialise array for spinner options
        initList();
        //update UI depending on whether user is logged in
        updateUI(currentUser);
        //create and initialise bottom navigation view
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(navListener);

    }

    private void updateUI(FirebaseUser currentUser) {
        //send user to homepage if not already logged in
        if(currentUser == null){
            //current user is null therefore they are not logged in. Send them to homepage
            Intent intent = new Intent(this, HomePageActivity.class);
            startActivity(intent);
        }
        else{
            //user is logged in - load data
            createLineChart();
        }
    }


    private void initList() {
        exerciseLogsRef.whereEqualTo("user", mAuth.getCurrentUser().getEmail())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        Log.i(TAG, "onSuccess: Retrieved user's exercises.");
                        for(QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots){
                            ExerciseLog exerciseLog = queryDocumentSnapshot.toObject(ExerciseLog.class);
                            //check if list already contains the exercise
                            if(!exercises.contains(exerciseLog.getExerciseType())){
                                //add the exercise if not already in the list
                                exercises.add(exerciseLog.getExerciseType());
                            }
                        }
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        //convert the ArrayList to an Array so that it can be used by ArrayAdapter
                        exercisesArray = new String[exercises.size()];
                        for(int i = 0; i < exercises.size(); i++){
                            exercisesArray[i] = exercises.get(i);
                            Log.i(TAG, exercises.get(i) + " added to array");
                        }
                        // setup the adapter for the spinner
                        setUpSpinner();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Unable to retrieve exercise data", e);
                    }
                });

    }

    private void setUpSpinner() {
        Log.i(TAG, "setUpSpinner: length = " + exercisesArray.length);
        if(exercisesArray.length == 0){
            spinner_selectedExercise.setVisibility(View.INVISIBLE);
            //no saved exercises therefore display user feedback
            TextView textView = findViewById(R.id.textView_previousLogs);
            textView.setText(R.string.no_exercises);
        }
        else{
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(ViewProgressActivity.this, android.R.layout.simple_spinner_item, exercisesArray);
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
            spinner_selectedExercise.setAdapter(arrayAdapter);
            spinner_selectedExercise.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG, "onItemSelected: item selected");
                    createLineChart();
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    Log.i(TAG, "Nothing Selected");
                }
            });
        }
    }

    private void createLineChart() {
        Log.d(TAG, "createLineChart: creating line chart");
        LineDataSet lineDataSet = new LineDataSet(retrieveData(), "data set");
        lineDataSet.setFillAlpha(110);
        lineDataSet.setColor(Color.BLACK);
        lineDataSet.setLineWidth(3f);
        ArrayList<ILineDataSet> iLineDataSets = new ArrayList<>();
        iLineDataSets.add(lineDataSet);
        LineData lineData = new LineData(iLineDataSets);
        lineChart.setData(lineData);

        //lineChart.invalidate();
    }

    private ArrayList<Entry> retrieveData(){
        //create lists to hold data for visualisation
        ArrayList<Entry> dataset = new ArrayList<>();

        //get exercise selected from spinner
        selectedExercise = (String)spinner_selectedExercise.getSelectedItem();
        //retrieve data for exercises of the same type as selected
        exerciseLogsRef.whereEqualTo("user", mAuth.getCurrentUser().getEmail())
                .whereEqualTo("exerciseType", selectedExercise)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            for (QueryDocumentSnapshot document : task.getResult()){
                                Log.d(TAG, "Retrieved data successfully.");
                                //add sets to an array
                                long[] sets = {(long) document.get("set1"),
                                        (long) document.get("set2"),
                                        (long) document.get("set3")};
                                //retrieve data from exercise log
                                long weight = (long) document.get("weight");
                                Date date = document.getTimestamp("date").toDate();
                                int numericDate = date.getDate();
                                //sort array into ascending order
                                Arrays.sort(sets);
                                //retrieve max reps done in this exercise
                                long maxReps = sets[sets.length-1];
                                //call method to calculate est 1 rep max
                                long estOneRM = calculateOneRM(weight, maxReps);
                                Log.d(TAG, "onComplete: adding " + numericDate + " " + estOneRM + " to line graph");
                                //TODO: PROBLEM LIES HERE! Neither the numericDate or estOneRM can be input into the Entry().
                                // I think it must be two integers passed into this constructor
                                dataset.add(new Entry(numericDate, estOneRM));

                            }
                        }else {
                            Log.d(TAG, "Unable to retrieve data", task.getException());
                        }
                    }
                });
        return dataset;
    }

    private long calculateOneRM(long weight, long maxReps) {
        //calculate estimated one rep max using Epley's equation
        return (long) ((0.033 * maxReps * weight) + weight);
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener navListener = item -> {
        //retrieve id of button clicked on nav bar
        int itemId = item.getItemId();
        //user clicked to record new exercise
        if (itemId == R.id.record) {
            Log.i(TAG, "Loading StartRecordActivity");
            Intent intent = new Intent(ViewProgressActivity.this, StartRecordActivity.class);
            startActivity(intent);
        }
        //user clicked to go return to main activity
        else if (itemId == R.id.main) {
            Log.i(TAG, "Loading MainActivity");
            Intent intent = new Intent(ViewProgressActivity.this, MainActivity.class);
            startActivity(intent);
        }
        return true;
    };

}