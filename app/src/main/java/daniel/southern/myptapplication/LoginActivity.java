package daniel.southern.myptapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.BeginSignInResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    //TODO: make use of Firebase SDKs to implement password reset

    //tag for logs for this activity
    public static final String TAG = "LoginActivity";
    //for intenting email address to another activity
    public static final String EXTRA_EMAIL_ADDRESS = "daniel.southern.myptapplication.EXTRA_EMAIL_ADDRESS";

    //declare instance of firebase auth
    private FirebaseAuth mAuth;
    private BeginSignInRequest beginSignInRequest;
    private SignInClient oneTapClient;
    private static final int REQ_ONE_TAP = 2;  // Can be any integer unique to the Activity.
    private boolean showOneTapUI = true;

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

        oneTapClient = Identity.getSignInClient(this);
        beginSignInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        // Your server's client ID, not your Android client ID.
                        .setServerClientId(getString(R.string.default_web_client_id))
                        // Only show accounts previously used to sign in.
                        .setFilterByAuthorizedAccounts(true)
                        .build())
                .build();

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

        oneTapClient.beginSignIn(beginSignInRequest)
                .addOnSuccessListener(new OnSuccessListener<BeginSignInResult>() {
                    @Override
                    public void onSuccess(BeginSignInResult beginSignInResult) {
                        try{
                            startIntentSenderForResult(beginSignInResult.getPendingIntent().getIntentSender(),
                                    REQ_ONE_TAP, null, 0, 0, 0);
                        }catch(IntentSender.SendIntentException e){
                            Log.e(TAG, "Couldn't start One Tap UI: " + e.getLocalizedMessage());
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: No saved credentials found", e);
                    }
                });

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

        switch (requestCode) {
            case REQ_ONE_TAP:
                try {
                    SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(data);
                    String idToken = credential.getGoogleIdToken();
                    String username = credential.getId();
                    String password = credential.getPassword();
                    if (idToken !=  null) {
                        // Got an ID token from Google. Use it to authenticate
                        // with your backend.
                        Log.d(TAG, "Got ID token.");
                    } else if (password != null) {
                        // Got a saved username and password. Use them to authenticate
                        // with your backend.
                        Log.d(TAG, "Got password.");
                    }
                } catch (ApiException e) {
                    switch (e.getStatusCode()) {
                        case CommonStatusCodes.CANCELED:
                            Log.d(TAG, "One-tap dialog was closed.");
                            // Don't re-prompt the user.
                            showOneTapUI = false;
                            break;
                        case CommonStatusCodes.NETWORK_ERROR:
                            Log.d(TAG, "One-tap encountered a network error.");
                            // Try again or just ignore.
                            break;
                        default:
                            Log.d(TAG, "Couldn't get credential from result."
                                    + e.getLocalizedMessage());
                            break;
                    }
                }
                break;
        }
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