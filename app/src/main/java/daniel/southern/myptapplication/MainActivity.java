package daniel.southern.myptapplication;

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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private MyAdapter myAdapter;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private Toolbar toolbar;
    private ImageView logoutIcon;

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
        Spinner spinner_selectedExercise = findViewById(R.id.spinner_selectExercise);
        //TODO: follow tutorial https://www.youtube.com/watch?v=GeO5F0nnzAw to only give option
        // for exercises which have been saved to cloud
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.Exercises, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinner_selectedExercise.setAdapter(adapter);
        spinner_selectedExercise.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //retrieve exercise selected from the spinner
                String selectedItem = parent.getItemAtPosition(position).toString();
                Log.i(TAG, "Showing Exercises for " + selectedItem);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // set tool bar as the action bar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //set logout icon on click listener
        logoutIcon = findViewById(R.id.imageView_logoutIcon);
        logoutIcon.setOnClickListener(this);
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
        //create recycler view
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(myAdapter);
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