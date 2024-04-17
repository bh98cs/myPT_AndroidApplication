package daniel.southern.myptapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class HomePageActivity extends AppCompatActivity implements View.OnClickListener{
    //declare buttons for login and create account
    Button loginBtn;
    Button createAccBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        //set on click listeners for buttons
        loginBtn = findViewById(R.id.button_login);
        createAccBtn = findViewById(R.id.button_createAccount);

        loginBtn.setOnClickListener(this);
        createAccBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        //login button is clicked
        if(v.getId() == R.id.button_login){
            //call method to handle users action
            goToLoginPage();
        }
        //create account button is clicked
        else if (v.getId() == R.id.button_createAccount) {
            //call method to handle users action
            goToCreateAccountPage();
        }
    }

    /**
     * Sends user to {@link CreateAccountActivity}
     */
    private void goToCreateAccountPage() {
        //send user to create account page
        Intent intent = new Intent(HomePageActivity.this, CreateAccountActivity.class);
        startActivity(intent);
    }

    /**
     * Sends user to {@link LoginActivity}
     */
    private void goToLoginPage() {
        //send user to login page
        Intent intent = new Intent(HomePageActivity.this, LoginActivity.class);
        startActivity(intent);
    }

}