package daniel.southern.myptapplication;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.video.VideoRecordEvent;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class StartRecordActivity extends AppCompatActivity implements View.OnClickListener{

    public static final String TAG = "StartRecordActivity";
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private TextClock clock;
    private TextView currentDate;
    private Toolbar toolbar;
    private ImageView logoutIcon;
    private Button cancelBtn;
    private Button startRecBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_record);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        //retrieve current user to check if they're already logged in
        currentUser = mAuth.getCurrentUser();

        clock = findViewById(R.id.textClock);
        //set clock to 24 hour format
        clock.setFormat24Hour("HH:mm:ss");
        currentDate = findViewById(R.id.textView_currentDate);

        //set textview to show current date by calling method
        currentDate.setText(getTodayDate());

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
        bottomNavigationView.setSelectedItemId(R.id.record);


        startRecBtn = findViewById(R.id.button_start);
        startRecBtn.setOnClickListener(this);
    }
    private void checkLoggedIn(FirebaseUser currentUser) {
        //send user to homepage if not already logged in
        if(currentUser == null){
            //current user is null therefore they are not logged in. Send them to homepage
            Intent intent = new Intent(this, HomePageActivity.class);
            startActivity(intent);
        }
    }
    //method to retrieve current date
    private String getTodayDate() {
        //get current date
        Date today = Calendar.getInstance().getTime();
        //create simple format for the date to be displayed in
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
        //return current date in the specified format
        return dateFormat.format(today);
    }
    @ExperimentalGetImage
    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.button_start) {
            startRecButtonClicked();
        } else if (v.getId() == R.id.imageView_logoutIcon) {
            logout();
        }
    }

    private void logout() {
        //request confirmation to sign out
        AlertDialog.Builder builder = new AlertDialog.Builder(StartRecordActivity.this);
        builder.setTitle("Confirm Logout")
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //sign user out
                        mAuth.signOut();
                        Log.i(TAG, "User Signed out");
                        //send user back to home page
                        Intent intent = new Intent(StartRecordActivity.this, HomePageActivity.class);
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


    @ExperimentalGetImage
    private void startRecButtonClicked() {
        Intent intent = new Intent(this, PoseEstimationActivity.class);
        startActivity(intent);
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener navListener = item -> {
        //retrieve id of button clicked on nav bar
        int itemId = item.getItemId();
        //user clicked to record new exercise
        if (itemId == R.id.progress) {
            Log.i(TAG, "Loading StartRecordActivity");
            Intent intent = new Intent(StartRecordActivity.this, ViewProgressActivity.class);
            startActivity(intent);
        }
        //user clicked to go return to main activity
        else if (itemId == R.id.main) {
            Log.i(TAG, "Loading MainActivity");
            Intent intent = new Intent(StartRecordActivity.this, MainActivity.class);
            startActivity(intent);
        }
        return true;
    };
}