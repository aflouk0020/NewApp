package com.example.newapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    EditText usernameEditText, passwordEditText;
    Button signInButton, createAccountButton;
    ProgressBar progressBar;
    Handler handler = new Handler();
    int counter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        signInButton = findViewById(R.id.but_sign_in);
        createAccountButton = findViewById(R.id.but_create_account);
        progressBar = findViewById(R.id.progressBar);

        progressBar.setVisibility(View.GONE); // Hide progress bar initially

        signInButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (validateInput(username, password)) {
                if (checkCredentials(username, password)) {
                    startProgress(username);
                } else {
                    Toast.makeText(MainActivity.this, "Invalid credentials. Please create an account.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "Please enter valid credentials.", Toast.LENGTH_SHORT).show();
            }
        });

        createAccountButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void startProgress(String username) {
        // Hide input fields and buttons
        usernameEditText.setVisibility(View.GONE);
        passwordEditText.setVisibility(View.GONE);
        signInButton.setVisibility(View.GONE);
        createAccountButton.setVisibility(View.GONE);

        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString(username + "_ID", "UNKNOWN"); // Retrieve the stored ID

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (counter < 100) {
                    counter++;
                    progressBar.setProgress(counter);
                    handler.postDelayed(this, 50); // 50ms delay for smooth progress
                } else {
                    progressBar.setVisibility(View.GONE);

                    // ✅ Pass User ID to HomeActivity
                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                    intent.putExtra("USERNAME", username);
                    intent.putExtra("USER_ID", userId);  // Pass User ID to the next screen
                    startActivity(intent);
                    finish(); // Close MainActivity
                }
            }
        };
        handler.post(runnable);
    }


//    private void startProgress(String username) {
//        usernameEditText.setVisibility(View.GONE);
//        passwordEditText.setVisibility(View.GONE);
//        signInButton.setVisibility(View.GONE);
//        createAccountButton.setVisibility(View.GONE);
//
//        progressBar.setVisibility(View.VISIBLE);
//        progressBar.setProgress(0);
//
//        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
//        String userId = sharedPreferences.getString(username + "_id", "UNKNOWN_USER");
//
//        Runnable runnable = new Runnable() {
//            @Override
//            public void run() {
//                if (counter < 100) {
//                    counter++;
//                    progressBar.setProgress(counter);
//                    handler.postDelayed(this, 50);
//                } else {
//                    progressBar.setVisibility(View.GONE);
//
//                    // Pass User ID to HomeActivity
//                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
//                    intent.putExtra("USER_ID", userId);
//                    intent.putExtra("USERNAME", username);
//                    startActivity(intent);
//                    finish();
//                }
//            }
//        };
//        handler.post(runnable);
//    }

    private boolean validateInput(String username, String password) {
        return !username.isEmpty() && !password.isEmpty();
    }

    private boolean checkCredentials(String username, String password) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String storedPassword = sharedPreferences.getString(username, null);
        return storedPassword != null && storedPassword.equals(password);
    }
}
