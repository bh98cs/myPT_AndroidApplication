package daniel.southern.myptapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class CreateAccountActivity extends AppCompatActivity implements View.OnClickListener {

    //constant variable for sending contents of email address field to LoginActivity
    public static final String EXTRA_EMAIL_ADDRESS = "daniel.southern.myptapplication.EXTRA_EMAIL_ADDRESS";
    //tag for logs
    public static final String TAG = "CreateAccountActivity";
    private FirebaseAuth mAuth;
    private EditText userEmail;
    private EditText userPassword;
    private Button createAccBtn;
    private Button loginBtn;
    private EditText confirmPassword;
    private Toolbar toolbar;
    private ImageView homepageIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        //set toolbar as activity's actionbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        //set on click listener for HomePage icon
        homepageIcon = findViewById(R.id.imageView_homepageIcon);
        homepageIcon.setOnClickListener(this);

        //assign variables to EditText views and Buttons
        userEmail = findViewById(R.id.editText_UserEmail);
        userPassword = findViewById(R.id.editText_Password);
        confirmPassword = findViewById(R.id.editText_confirmPassword);
        createAccBtn = findViewById(R.id.button_CreateAccount);
        loginBtn = findViewById(R.id.button_Login);

        //initialise firebase auth
        mAuth = FirebaseAuth.getInstance();

        //set onClick listeners for buttons
        loginBtn.setOnClickListener(this);
        createAccBtn.setOnClickListener(this);

        //get intent that started activity
        Intent intent = getIntent();
        //store any text from the intent in a local variable
        String email = intent.getStringExtra(CreateAccountActivity.EXTRA_EMAIL_ADDRESS);
        //prepopulate email address for user if filled out in LoginActivity
        userEmail.setText(email);
    }
    @Override
    public void onClick(View v) {
        //get user email outside of if statement as needed for both scenarios
        String email = userEmail.getText().toString().trim();
        //user clicks to create account
        if(v.getId() == R.id.button_CreateAccount){
            //try-catch block for getting string values from user input
            try{
                //store contents of the two password boxes in variables
                String password = userPassword.getText().toString();
                String password2 = confirmPassword.getText().toString();
                //check if the two passwords match (to make sure user hasn't miss typed)
                if(passwordsMatch(password, password2)){
                    //create new user if passwords match
                    createNewUser(email, password);
                }
                else {
                    //user feedback to advise two password boxes do not match
                    Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_LONG).show();
                }
            }
            //catch exception if null or empty string is provided
            catch(IllegalArgumentException e){
                //user feedback to advise all fields must be provided.
                Toast.makeText(this, "Please provide an email address and two matching passwords.", Toast.LENGTH_SHORT).show();
            }

        }
        //login button has been clicked
        else if (v.getId() == R.id.button_Login) {
            //send user to login page
            Intent intent = new Intent(this, LoginActivity.class);
            //send contents of email address box to login page
            intent.putExtra(EXTRA_EMAIL_ADDRESS, email);
            startActivity(intent);
        }
        //homepage icon clicked
        else if (v.getId() == R.id.imageView_homepageIcon) {
            //send user back to homepage
            Intent intent = new Intent(this, HomePageActivity.class);
            startActivity(intent);
        }
    }

    /**
     * Create a new account using Firebase Auth
     * @param email email address for account
     * @param password password for account
     */
    private void createNewUser(String email, String password){
        //create new user using email and password
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        //check if task was successful
                        if(task.isSuccessful()){
                            //sign up success, go to main page
                            Log.d(TAG, "createUserWithEmail:success");
                            Intent intent = new Intent(CreateAccountActivity.this, MainActivity.class);
                            startActivity(intent);
                        }else {
                            //sign up fails print error in the log
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            //user feedback to advise could not create account
                            Toast.makeText(CreateAccountActivity.this, "Unable to Create Account.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Check that the user has provided two matching passwords
     * @param pw1 first password
     * @param pw2 second password
     * @return bool indicating whether passwords match
     */
    private boolean passwordsMatch(String pw1, String pw2){
        return pw1.equals(pw2);
    }

}