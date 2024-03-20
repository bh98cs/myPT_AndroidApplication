package daniel.southern.myptapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    //for intenting Firebase ID of an item
    public static final String EXTRA_ITEM_FIREBASE_ID = "daniel.southern.danielsouthern_cet343assignment.ITEM_FIREBASE_ID";
    public static final String TAG = "MainActivity";
    private MyAdapter myAdapter;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private Toolbar toolbar;
    private ImageView logoutIcon;
    private Spinner spinner_selectedExercise;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private ExerciseLog deletedExercise;
    private final FirebaseFirestore database = FirebaseFirestore.getInstance();
    private final CollectionReference exerciseLogsRef = database.collection("exerciseLogs");
    //array list to hold the exercises this user has saved data for
    private ArrayList<String> exercises = new ArrayList<>();
    private String[] exercisesArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialize progress bar
        progressBar = findViewById(R.id.progressBar);
        Log.d(TAG, "onCreate: set progress bar to visible");
        progressBar.setVisibility(View.VISIBLE);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        //retrieve current user to check if they're already logged in
        currentUser = mAuth.getCurrentUser();
        //initialise spinner
        spinner_selectedExercise = findViewById(R.id.spinner_selectExercise);
        //update UI depending on whether user is logged in
        updateUI(currentUser);

        //create and initialise bottom navigation view
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(navListener);

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
                        progressBar.setVisibility(View.INVISIBLE);
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
                    loadData();
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
            loadData();
        }
    }
    private void loadData(){
        //retrieve exercise selected from the spinner
        String selectedExercise = (String)spinner_selectedExercise.getSelectedItem();
        Log.i(TAG, "Showing Exercises for " + selectedExercise);
        //create new query to retrieve exercises of the same type selected from spinner
        Query query = exerciseLogsRef.whereEqualTo("user", mAuth.getCurrentUser().getEmail())
                .whereEqualTo("exerciseType", selectedExercise)
                .orderBy("date", Query.Direction.DESCENDING);
        //update options for adapter with new query
        FirestoreRecyclerOptions<ExerciseLog> options = new FirestoreRecyclerOptions.Builder<ExerciseLog>().setQuery(query,
                ExerciseLog.class).build();
        //check if adapter has been created
        if(myAdapter != null){
            myAdapter.updateOptions(options);
        }
        else{
            //create adapter
            myAdapter = new MyAdapter(options);
            //attach adapter to recycler view
            recyclerView.setAdapter(myAdapter);
        }
    }

    private void setUpRecyclerView() {
        Log.i(TAG, "Setting up recycler view");
        //create recycler view
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT
                | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                //user swipes left to delete
                if (direction == 4) {
                    //store the position of the item in a local variable
                    int position = viewHolder.getAdapterPosition();
                    //store the deleted item incase user wants to undo delete
                    deletedExercise = myAdapter.getItem(position);
                    //delete item from recyclerview and Firebase
                    myAdapter.deleteItem(position);
                    //call method to give user the option to undo the delete
                    optionToUndoDelete();
                }
                //user swipes right to edit
                else if (direction == 8) {
                    //send user to activity to edit item
                    Intent intent = new Intent(MainActivity.this, EditExerciseLogActivity.class);
                    //get position of the item selected to edit
                    int position = viewHolder.getAdapterPosition();
                    //retrieve Firebase ID of item to edit using it's position
                    String itemFirebaseId = myAdapter.getItemFirebaseId(position);
                    //send Firebase ID of item to edit to new activity
                    intent.putExtra(EXTRA_ITEM_FIREBASE_ID, itemFirebaseId);
                    startActivity(intent);
                }
            }
            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                //create background colors and icons to display when swiping items using RecyclerViewSwipeDecorator library
                new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        //add background color and icon for deleting by swiping left or right
                        .addSwipeLeftBackgroundColor(MaterialColors.getColor(recyclerView,com.google.android.material.R.attr.colorError))
                        .addSwipeLeftActionIcon(R.drawable.delete_icon).setSwipeLeftActionIconTint(MaterialColors.getColor(recyclerView,
                                com.google.android.material.R.attr.colorOnError))
                        .addSwipeRightBackgroundColor(MaterialColors.getColor(recyclerView,
                                com.google.android.material.R.attr.colorTertiary))
                        .addSwipeRightActionIcon(R.drawable.edit_icon).setSwipeRightActionIconTint(MaterialColors.getColor(recyclerView,
                                com.google.android.material.R.attr.colorOnTertiary))
                        .create()
                        .decorate();
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }).attachToRecyclerView(recyclerView);
    }
    private void optionToUndoDelete() {
        RelativeLayout layout = findViewById(R.id.activity_main_layout);
        //create Snackbar to provide user feedback and give option to undo deletion
        Snackbar snackbar = Snackbar.make(layout, "Undo Delete", Snackbar.LENGTH_LONG)
                .setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //call method to undo the deletion if user clicks to Undo
                        undoDelete();
                    }
                });
        snackbar.show();
    }

    private void undoDelete() {
            //check if an item has been deleted in this session
            if(deletedExercise == null){
                //user feedback to advise nothing to undo
                Toast.makeText(this, "Unable to Undo. No items deleted in this session.", Toast.LENGTH_SHORT).show();
                //return out of method
                return;
            }

            //add details from the stored item to a hashmap
            Map<String, Object> exerciseLog = new HashMap<>();
            exerciseLog.put("exerciseType", deletedExercise.getExerciseType());
            exerciseLog.put("date", deletedExercise.getDate());
            exerciseLog.put("set1", deletedExercise.getSet1());
            exerciseLog.put("set2", deletedExercise.getSet2());
            exerciseLog.put("set3", deletedExercise.getSet3());
            exerciseLog.put("weight", deletedExercise.getWeight());
            exerciseLog.put("notes", deletedExercise.getNotes());
            exerciseLog.put("user", currentUser.getEmail());

            //re-upload deleted item to firebase
            database.collection("exerciseLogs")
                    .add(exerciseLog)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            //user feedback to advise item deletion has been undone
                            Toast.makeText(MainActivity.this, "Undo Successful", Toast.LENGTH_SHORT).show();
                            //set deleted item back to null so that it can not be added back again
                            deletedExercise = null;
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //advise was unable to undo deletion
                            Toast.makeText(MainActivity.this, "Error Undoing Delete", Toast.LENGTH_SHORT).show();
                        }
                    });
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
             Log.i(TAG, "Loading ProgressActivity");
             Intent intent = new Intent(MainActivity.this, ViewProgressActivity.class);
             startActivity(intent);
        }
        return true;
    };
}