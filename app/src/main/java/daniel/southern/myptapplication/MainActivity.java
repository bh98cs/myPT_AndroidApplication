package daniel.southern.myptapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private MyAdapter myAdapter;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private Toolbar toolbar;
    private ImageView logoutIcon;
    private Spinner spinner_selectedExercise;
    private final FirebaseFirestore database = FirebaseFirestore.getInstance();
    private final CollectionReference exerciseLogsRef = database.collection("exerciseLogs");
    //array list to hold the exercises this user has saved data for
    private ArrayList<String> exercises = new ArrayList<>();
    private String[] exercisesArray;

    public static final String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        //retrieve current user to check if they're already logged in
        currentUser = mAuth.getCurrentUser();
        //update UI depending on whether user is logged in
        updateUI(currentUser);

        //create and initialise bottom navigation view
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(navListener);

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
        Log.i(TAG, "setUpSpinner: " + exercisesArray.length);
        if(exercisesArray.length == 0){
            spinner_selectedExercise.setVisibility(View.INVISIBLE);
            //no saved exercises therefore display user feedback
            TextView textView = findViewById(R.id.textView_previousLogs);
            textView.setText(R.string.no_exercises);
        }
        else{
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, exercisesArray);
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
            spinner_selectedExercise.setAdapter(arrayAdapter);
            spinner_selectedExercise.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    //retrieve exercise selected from the spinner
                    String selectedItem = (String)parent.getSelectedItem();
                    Log.i(TAG, "Showing Exercises for " + selectedItem);
                    //create new query to retrieve exercises of the same type selected from spinner
                    Query query = exerciseLogsRef.whereEqualTo("user", mAuth.getCurrentUser().getEmail())
                            .whereEqualTo("exerciseType", selectedItem);
                    //update options for adapter with new query
                    FirestoreRecyclerOptions<ExerciseLog> options = new FirestoreRecyclerOptions.Builder<ExerciseLog>().setQuery(query,
                            ExerciseLog.class).build();
                    myAdapter.updateOptions(options);
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    Log.i(TAG, "Nothing Selected");
                }
            });
        }
    }


    private void updateUI(FirebaseUser currentUser) {
        //send user to homepage if not already logged in
        if(currentUser == null){
            //current user is null therefore they are not logged in. Send them to homepage
            Intent intent = new Intent(this, HomePageActivity.class);
            startActivity(intent);
        }
        else{
            //user is logged in - call method to set up Recycler view
            setUpRecyclerView();
        }
    }

    private void setUpRecyclerView() {
        Log.i(TAG, "Setting up recycler view");
        //TODO: May need to change if allowing user to signup through other accounts
        Query query = exerciseLogsRef.whereEqualTo("user", mAuth.getCurrentUser().getEmail());
        //set options for adapter
        FirestoreRecyclerOptions<ExerciseLog> options = new FirestoreRecyclerOptions.Builder<ExerciseLog>().setQuery(query,
                ExerciseLog.class).build();

        //create adapter
        myAdapter = new MyAdapter(options);
        //create recycler view
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(myAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        myAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        myAdapter.stopListening();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.imageView_logoutIcon) {
            //call logout method
            logout();
        }
    }

    private void logout() {
        //request confirmation to sign out
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Confirm Logout")
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //sign user out
                        mAuth.signOut();
                        Log.i(TAG, "User Signed out");
                        //send user back to home page
                        Intent intent = new Intent(MainActivity.this, HomePageActivity.class);
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
            Intent intent = new Intent(MainActivity.this, StartRecordActivity.class);
            startActivity(intent);
        }
        //user clicked to view progress
        else if (itemId == R.id.progress) {
             //TODO: Change to load Progress Activity
             Log.i(TAG, "Loading ProgressActivity");
        }
        return true;
    };
}