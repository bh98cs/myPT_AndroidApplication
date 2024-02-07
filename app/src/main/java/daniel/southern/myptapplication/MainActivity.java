package daniel.southern.myptapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener navListener = item -> {
        //retrieve id of button clicked on nav bar
        int itemId = item.getItemId();
        //user clicked to record new exercise
        if (itemId == R.id.record) {
             //TODO: Change to load StartRecordActivity
             Log.i(TAG, "Loading StartRecordActivity");
            Intent intent = new Intent(MainActivity.this, PoseEstimationActivity.class);
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