package daniel.southern.myptapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    //tag for logs for this activity
    public static final String TAG = "LoginActivity";
    //for intenting email address to another activity
    public static final String EXTRA_EMAIL_ADDRESS = "daniel.southern.myptapplication.EXTRA_EMAIL_ADDRESS";

    //declare instance of firebase auth
    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private static final int REQ_SIGN_IN = 20;

    //declare functional views on Activity
    private Button loginBtn;
    private Button createAccBtn;
    private EditText userEmail;
    private EditText userPassword;
    private Toolbar toolbar;
    private ImageView homepageIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();


        //create toolbar
        toolbar =  findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //hide title as custom one created in layout file
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        //initialize homepage icon and set onclick listener
        homepageIcon = findViewById(R.id.imageView_homepageIcon);
        homepageIcon.setOnClickListener(this);

        //Initialize views
        loginBtn = findViewById(R.id.button_Login);
        createAccBtn = findViewById(R.id.button_CreateAccount);
        userEmail = findViewById(R.id.editText_UserEmail);
        userPassword = findViewById(R.id.editText_Password);

        //set onclick listener for buttons
        loginBtn.setOnClickListener(this);
        createAccBtn.setOnClickListener(this);

        //get intent that started activity
        Intent intent = getIntent();
        //store any text from the intent in a local variable
        String email = intent.getStringExtra(CreateAccountActivity.EXTRA_EMAIL_ADDRESS);
        //prepopulate email address for user if filled out in CreateAccountActivity
        userEmail.setText(email);

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);

    }
    @Override
    public void onClick(View v) {
        //login button clicked
        if(v.getId() == R.id.button_Login){
            //try-catch block for getting string values from user input
            try{
                //store user input in string variables
                String email = userEmail.getText().toString().trim();
                String password = userPassword.getText().toString().trim();

                //call method to sign in user with email and password given
                signIn(email, password);
            }
            //catch exception if null or empty string is provided
            catch(IllegalArgumentException e){
                //user feedback to advise both fields need to be given
                Toast.makeText(LoginActivity.this, "Please provide an email address and password.", Toast.LENGTH_SHORT).show();
            }


        }
        //create account button clicked
        else if (v.getId() == R.id.button_CreateAccount) {
            //get contents of email input box
            String email = userEmail.getText().toString().trim();
            //send user to create account page
            Intent intent = new Intent(this, CreateAccountActivity.class);
            //send contents of email input to CreateAccountActivity
            intent.putExtra(EXTRA_EMAIL_ADDRESS, email);
            startActivity(intent);
        } else if (v.getId() == R.id.imageView_homepageIcon) {
            //send user to homepage
            Intent intent = new Intent(this, HomePageActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQ_SIGN_IN){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try{
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuth(account.getIdToken());
            }catch (Exception e){
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuth(String idToken) {
        AuthCredential authCredential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(authCredential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            FirebaseUser user = mAuth.getCurrentUser();
                            Log.i(TAG, "onComplete: Logged in using Google");
                            //send user to their main page
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                        }
                    }
                });
    }

    private void signIn(String email, String password) {

        //sign user in with their email and password
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        //check if login was successful
                        if(task.isSuccessful()){
                            //sign in successful
                            Log.d(TAG, "signInWithEmail:success");
                            //send user to their main page
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                        }else{
                            //sign in unsuccessful
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Email or Password is incorrect.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //if attempt to sign in failed
                        Log.e(TAG, "Unable to attempt sign in.", e);
                        //user feedback
                        Toast.makeText(LoginActivity.this, "Unable to Sign in.", Toast.LENGTH_SHORT).show();
                    }
                });



    }


}