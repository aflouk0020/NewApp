package com.example.newapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.UUID;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etPassword, etRepeatPassword;
    private Button butRegister, butSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etUsername = findViewById(R.id.username);
        etPassword = findViewById(R.id.password);
        etRepeatPassword = findViewById(R.id.et_repeat_password);
        butRegister = findViewById(R.id.but_register);
        butSignIn = findViewById(R.id.but_sign_in);

        butSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            startActivity(intent);
        });

        butRegister.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String repeatPassword = etRepeatPassword.getText().toString().trim();

            if (username.length() < 3) {
                Toast.makeText(RegisterActivity.this, "Username must be at least 3 Characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 8) {
                Toast.makeText(RegisterActivity.this, "Password must be at least 8 Characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (username.isEmpty() || password.isEmpty() || repeatPassword.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(repeatPassword)) {
                Toast.makeText(RegisterActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            if (sharedPreferences.contains(username)) {
                Toast.makeText(RegisterActivity.this, "Username already exists", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ Generate a unique user ID only ONCE
            String userId = UUID.randomUUID().toString();

            // ✅ Save user ID and password in SharedPreferences
            editor.putString(username, password);
            editor.putString(username + "_ID", userId); // Store ID under "<username>_ID"
            boolean commit = editor.commit();

            if (commit) {
                Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                etUsername.setText("");
                etPassword.setText("");
                etRepeatPassword.setText("");
            } else {
                Toast.makeText(RegisterActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
//package com.example.newapp;
//
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.widget.EditText;
//import android.widget.Button;
//import android.widget.Toast;
//import androidx.appcompat.app.AppCompatActivity;
//import java.util.UUID;  // Import UUID for generating unique ID
//
//public class RegisterActivity extends AppCompatActivity {
//    private EditText etUsername, etPassword, etRepeatPassword;
//    private Button butRegister, butSignIn;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_register);
//
//        etUsername = findViewById(R.id.username);
//        etPassword = findViewById(R.id.password);
//        etRepeatPassword = findViewById(R.id.et_repeat_password);
//        butRegister = findViewById(R.id.but_register);
//        butSignIn = findViewById(R.id.but_sign_in);
//
//        butSignIn.setOnClickListener(v -> {
//            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
//            startActivity(intent);
//        });
//
//        butRegister.setOnClickListener(v -> {
//            String username = etUsername.getText().toString().trim();
//            String password = etPassword.getText().toString().trim();
//            String repeatPassword = etRepeatPassword.getText().toString().trim();
//
//            if (username.length() < 3) {
//                Toast.makeText(RegisterActivity.this, "Username must be at least 3 characters", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            if (password.length() < 8) {
//                Toast.makeText(RegisterActivity.this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            if (username.isEmpty() || password.isEmpty() || repeatPassword.isEmpty()) {
//                Toast.makeText(RegisterActivity.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            if (!password.equals(repeatPassword)) {
//                Toast.makeText(RegisterActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
//            SharedPreferences.Editor editor = sharedPreferences.edit();
//
//            if (sharedPreferences.contains(username)) {
//                Toast.makeText(RegisterActivity.this, "Username already exists", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            // Generate and store a unique user ID
//            String userId = UUID.randomUUID().toString();
//            editor.putString(username, password);
//            editor.putString(username + "_id", userId);  // Store user ID
//            boolean commit = editor.commit();
//
//            if (commit) {
//                Toast.makeText(RegisterActivity.this, "Registration successful! Your ID: " + userId, Toast.LENGTH_SHORT).show();
//                etUsername.setText("");
//                etPassword.setText("");
//                etRepeatPassword.setText("");
//            } else {
//                Toast.makeText(RegisterActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//}
