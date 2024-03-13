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
import com.google.firebase.firestore.Query;
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
    private LineDataSet lineDataSet = new LineDataSet(null, null);
    private ArrayList<ILineDataSet> iLineDataSets = new ArrayList<>();
    private LineData lineData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_progress);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        //retrieve current user to check if they're already logged in
        currentUser = mAuth.getCurrentUser();

        lineChart = findViewById(R.id.line_chart);

        //initialise spinner
        spinner_selectedExercise = findViewById(R.id.spinner_selectExercise);
        //initialise array for spinner options
        initList();
        //check whether user is logged in
        checkLoggedIn(currentUser);
        //create and initialise bottom navigation view
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(navListener);

    }

    private void checkLoggedIn(FirebaseUser currentUser) {
        //send user to homepage if not already logged in
        if(currentUser == null){
            //current user is null therefore they are not logged in. Send them to homepage
            Intent intent = new Intent(this, HomePageActivity.class);
            startActivity(intent);
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
        //create list to hold data for visualisation
        ArrayList<Entry> dataset = new ArrayList<>();
        //get exercise selected from spinner
        selectedExercise = (String) spinner_selectedExercise.getSelectedItem();
        //retrieve data for exercises of the same type as selected
        exerciseLogsRef.whereEqualTo("user", mAuth.getCurrentUser().getEmail())
                .whereEqualTo("exerciseType", selectedExercise)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Retrieved data successfully.");
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                //TODO: add this to a method to make code simpler
                                //get sets from document
                                int set1 = Math.toIntExact((long) document.get("set1"));
                                int set2 = Math.toIntExact((long) document.get("set2"));
                                int set3 = Math.toIntExact((long) document.get("set3"));
                                Log.d(TAG, "onComplete: sets retrieved: " + set1 + " " + set2 + " " + set3);
                                //add sets to an array
                                int[] sets = {set1, set2, set3};
                                //retrieve weight data from exercise log
                                int weight = Math.toIntExact((long) document.get("weight"));
                                //retrieve date from exercise log
                                Date date = document.getTimestamp("date").toDate();
                                //convert date to just provide day
                                int numericDate = date.getDate();
                                //sort array into ascending order
                                Arrays.sort(sets);
                                //retrieve max reps done in this exercise
                                int maxReps = sets[sets.length - 1];
                                //call method to calculate est 1 rep max
                                int estOneRM = calculateOneRM(weight, maxReps);
                                Log.d(TAG, "onComplete: adding " + numericDate + " " + estOneRM + " to line graph");
                                dataset.add(new Entry(numericDate, estOneRM));
                            }
                            showChart(dataset);
                        } else {
                            Log.d(TAG, "Unable to retrieve data", task.getException());
                        }
                    }
                });
    }

    private void showChart(ArrayList<Entry> dataset) {
        lineDataSet.setValues(dataset);
        Log.d(TAG, "showChart: Create chart with values: " + dataset);
        lineDataSet.setLabel("Est. One Rep Max");
        iLineDataSets.clear();
        iLineDataSets.add(lineDataSet);
        lineData = new LineData(iLineDataSets);
        lineChart.clear();
        lineChart.setData(lineData);
        lineChart.invalidate();
    }

    private int calculateOneRM(int weight, int maxReps) {
        //calculate estimated one rep max using Epley's equation
        return (int) ((0.033 * maxReps * weight) + weight);
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