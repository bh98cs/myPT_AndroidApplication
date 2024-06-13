package daniel.southern.myptapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

public class ViewProgressActivity extends AppCompatActivity implements View.OnClickListener {

    //tag for class
    public static final String TAG = "ViewProgressActivity";
    private LineChart lineChart;
    private Spinner spinner_selectedExercise;
    private String selectedExercise;
    private Toolbar toolbar;
    private ImageView logoutIcon;
    private CardView loadingGif;
    private String[] exercisesArray;
    private ArrayList<ExerciseLog> exerciseLogs;
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

        //initialise progress bar and set to visible
        loadingGif = findViewById(R.id.loadingGif);
        showLoadingGif(true);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        //retrieve current user to check if they're already logged in
        currentUser = mAuth.getCurrentUser();

        lineChart = findViewById(R.id.line_chart);
        //set line chart to invisible whilst loading
        lineChart.setVisibility(View.INVISIBLE);

        //initialise spinner
        spinner_selectedExercise = findViewById(R.id.spinner_selectExercise);
        //initialise array for spinner options
        initList();
        // set tool bar as the action bar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //set logout icon on click listener
        logoutIcon = findViewById(R.id.imageView_logoutIcon);
        logoutIcon.setOnClickListener(this);
        //check whether user is logged in
        checkLoggedIn(currentUser);
        //create and initialise bottom navigation view
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(navListener);
        bottomNavigationView.setSelectedItemId(R.id.progress);

    }

    /**
     * toggles whether the loading animation is displayed
     * @param b boolean to indicate whether the loading animation should be displayed
     */
    private void showLoadingGif(boolean b) {
        if(b){
            loadingGif.setVisibility(View.VISIBLE);
            loadingGif.bringToFront();
        }
        else{
            //disable spinner whilst loading gif is displayed
            spinner_selectedExercise.setEnabled(false);
            //add a delay so user can see loading icon (looks better than it disappearing straight away)
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadingGif.setVisibility(View.INVISIBLE);
                    spinner_selectedExercise.setEnabled(true);
                    //set line chart to visible
                    lineChart.setVisibility(View.VISIBLE);

                }
            }, 2000);//short delay so user can see loading icon (looks better than it disappearing straight away)
        }

    }

    /**
     * Check whether the user has been authenticated
     * @param currentUser current firebase user
     */
    private void checkLoggedIn(FirebaseUser currentUser) {
        //send user to homepage if not already logged in
        if(currentUser == null){
            //current user is null therefore they are not logged in. Send them to homepage
            Intent intent = new Intent(this, HomePageActivity.class);
            startActivity(intent);
        }
    }

    /**
     * Loads names of exercises saved in the cloud into the spinner for the user to select from
     */
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

    /**
     * Creates spinner from list of exercises saved to cloud
     */
    private void setUpSpinner() {
        Log.i(TAG, "setUpSpinner: length = " + exercisesArray.length);
        if(exercisesArray.length == 0){
            noExerciseLogs();
        }
        else{
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(ViewProgressActivity.this, android.R.layout.simple_spinner_item, exercisesArray);
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
            spinner_selectedExercise.setAdapter(arrayAdapter);
            spinner_selectedExercise.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG, "onItemSelected: item selected");
                    //set progress bar to visible whilst creating new graph
                    showLoadingGif(true);
                    //set line chart to invisible whilst loading
                    lineChart.setVisibility(View.INVISIBLE);
                    createLineChart();
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    Log.i(TAG, "Nothing Selected");
                }
            });
        }
    }

    /**
     * Called when no exercise data has been saved to the cloud by the user. Updates the UI to indicate this
     */
    private void noExerciseLogs() {
        CardView noLogsGif = findViewById(R.id.noLogsGif);
        showLoadingGif(false);
        spinner_selectedExercise.setVisibility(View.INVISIBLE);
        //no saved exercises therefore display user feedback
        TextView textView = findViewById(R.id.textView_noExercises);
        textView.setVisibility(View.VISIBLE);
        noLogsGif.setVisibility(View.VISIBLE);
    }

    /**
     * Creates dataset for a line chart from most recent 6 entries of the selected
     * exercise saved to the database.
     */
    private void createLineChart() {
        //create list to hold data for visualisation
        ArrayList<Entry> dataset = new ArrayList<>();
        //get exercise selected from spinner
        selectedExercise = (String) spinner_selectedExercise.getSelectedItem();
        //retrieve 6 most recent data entries for selected exercise
        exerciseLogsRef.whereEqualTo("user", mAuth.getCurrentUser().getEmail())
                .whereEqualTo("exerciseType", selectedExercise)
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(6)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            exerciseLogs = new ArrayList<>();
                            Log.d(TAG, "Retrieved data successfully.");
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                //add data to an exerciseLog object
                                ExerciseLog exerciseLog = document.toObject(ExerciseLog.class);
                                //add exerciseLog to list so it can be sorted by date (ascending)
                                exerciseLogs.add(exerciseLog);
                            }
                            Collections.sort(exerciseLogs, new SortExerciseLogs());
                            int i = 0;
                            for(ExerciseLog e : exerciseLogs){
                                //get sets from exercise
                                int set1 = e.getSet1();
                                int set2 = e.getSet2();
                                int set3 = e.getSet3();
                                Log.d(TAG, "onComplete: sets retrieved: " + set1 + " " + set2 + " " + set3);
                                //add sets to an array
                                int[] sets = {set1, set2, set3};
                                //sort array into ascending order
                                Arrays.sort(sets);
                                //retrieve max reps done in this exercise
                                int maxReps = sets[sets.length - 1];
                                //call method to calculate est 1 rep max using most reps performed and weight
                                int estOneRM = calculateOneRM(e.getWeight(), maxReps);
                                dataset.add(new Entry(i, estOneRM));
                                i++;
                            }
                            showChart(dataset);
                        } else {
                            Log.d(TAG, "Unable to retrieve data", task.getException());
                        }
                    }
                });
    }

    /**
     * Creates a line chart using the dataset given
     * @param dataset data to be plotted within the line chart
     */
    private void showChart(ArrayList<Entry> dataset) {
        lineDataSet.setValues(dataset);
        Log.d(TAG, "showChart: Create chart with values: " + dataset);
        lineDataSet.setLabel("Est. One Rep Max (Kg)");
        lineDataSet.setLineWidth(3);
        lineDataSet.setColor(R.color.md_theme_primary);
        lineDataSet.setCircleRadius(5);
        lineDataSet.setCircleColor(R.color.md_theme_primary);
        lineDataSet.setValueTextSize(15);
        iLineDataSets.clear();
        iLineDataSets.add(lineDataSet);
        lineData = new LineData(iLineDataSets);
        lineChart.clear();


        final String[] dateLabels = new String[6];
        int i = 0;
        for (ExerciseLog e : exerciseLogs){
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy", Locale.getDefault());
            dateLabels[i] = dateFormat.format(e.getDate());
            i++;
        }
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new MyXAxisValueFormatter(dateLabels));
        lineChart.setData(lineData);
        lineChart.setDescription(null);
        lineChart.invalidate();
        //hide loading gif
        showLoadingGif(false);
    }

    /**
     * Calculates the estimated one rep max according to Epley's equation
     * @param weight the weight used for the exercise
     * @param maxReps maximum number of reps performed for the exercise
     * @return the estimated one rep max based on Epley's equation
     */
    private int calculateOneRM(int weight, int maxReps) {
        //calculate estimated one rep max using Epley's equation
        return (int) ((0.033 * maxReps * weight) + weight);
    }

    /**
     * Logs the user out of their account
     */
    private void logout() {
        //request confirmation to sign out
        AlertDialog.Builder builder = new AlertDialog.Builder(ViewProgressActivity.this);
        builder.setTitle("Confirm Logout")
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //sign user out
                        mAuth.signOut();
                        Log.i(TAG, "User Signed out");
                        //send user back to home page
                        Intent intent = new Intent(ViewProgressActivity.this, HomePageActivity.class);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing as user does not want to log out
                    }
                });

        AlertDialog dialog = builder.create();
        //show alert dialog to request confirmation of user logging out
        dialog.show();
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

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.imageView_logoutIcon) {
            //call logout method
            logout();
        }
    }
}