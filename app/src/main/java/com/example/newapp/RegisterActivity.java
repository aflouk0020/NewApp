package com.example.newapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.UUID;
import android.widget.NumberPicker;

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

        NumberPicker familySizePicker = findViewById(R.id.family_size_picker);
        NumberPicker numberOfRoomsPicker = findViewById(R.id.number_of_rooms_picker);

        // ✅ Set the min and max values
        familySizePicker.setMinValue(1);
        familySizePicker.setMaxValue(20);
        familySizePicker.setValue(1); // Default value
        familySizePicker.setWrapSelectorWheel(true);

        numberOfRoomsPicker.setMinValue(1);
        numberOfRoomsPicker.setMaxValue(10);
        numberOfRoomsPicker.setValue(1); // Default value
        numberOfRoomsPicker.setWrapSelectorWheel(true);

        // Optional: Set a listener to check changes in the values
        familySizePicker.setOnValueChangedListener((picker, oldVal, newVal) ->
                Toast.makeText(RegisterActivity.this, "Selected Family Size: " + newVal, Toast.LENGTH_SHORT).show()
        );

        numberOfRoomsPicker.setOnValueChangedListener((picker, oldVal, newVal) ->
                Toast.makeText(RegisterActivity.this, "Selected Rooms: " + newVal, Toast.LENGTH_SHORT).show()
        );

        // Button click listeners
//        butRegister.setOnClickListener(v -> {
//            String username = etUsername.getText().toString().trim();
//            String password = etPassword.getText().toString().trim();
//            String repeatPassword = etRepeatPassword.getText().toString().trim();
//
//            if (username.isEmpty() || password.isEmpty() || repeatPassword.isEmpty()) {
//                Toast.makeText(RegisterActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            if (!password.equals(repeatPassword)) {
//                Toast.makeText(RegisterActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            // ✅ Get selected values from NumberPickers
//            int selectedFamilySize = familySizePicker.getValue();
//            int selectedNumberOfRooms = numberOfRoomsPicker.getValue();
//
//            Toast.makeText(RegisterActivity.this, "Family Size: " + selectedFamilySize + ", Rooms: " + selectedNumberOfRooms, Toast.LENGTH_SHORT).show();
//        });

        butRegister.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String repeatPassword = etRepeatPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty() || repeatPassword.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(repeatPassword)) {
                Toast.makeText(RegisterActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get selected values from NumberPickers
            int selectedFamilySize = familySizePicker.getValue();
            int selectedNumberOfRooms = numberOfRoomsPicker.getValue();

            // Insert the user data into the database
            DBAccess dbAccess = new DBAccess(RegisterActivity.this);
            boolean isInserted = dbAccess.insertUser(username, password, selectedFamilySize, selectedNumberOfRooms);

            if (isInserted) {
                Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                // Redirect to the login page or main activity
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(RegisterActivity.this, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });






        butSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

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
//                Toast.makeText(RegisterActivity.this, "Username must be at least 3 Characters", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            if (password.length() < 8) {
//                Toast.makeText(RegisterActivity.this, "Password must be at least 8 Characters", Toast.LENGTH_SHORT).show();
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
//            // ✅ Generate a unique user ID only ONCE
//            String userId = UUID.randomUUID().toString();
//
//            // ✅ Save user ID and password in SharedPreferences
//            editor.putString(username, password);
//            editor.putString(username + "_ID", userId); // Store ID under "<username>_ID"
//            boolean commit = editor.commit();
//
//            if (commit) {
//                Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
//                etUsername.setText("");
//                etPassword.setText("");
//                etRepeatPassword.setText("");
//            } else {
//                Toast.makeText(RegisterActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
//            }
//        });
//
////        butRegister.setOnClickListener(v -> {
////            String username = etUsername.getText().toString().trim();
////            String password = etPassword.getText().toString().trim();
////            String repeatPassword = etRepeatPassword.getText().toString().trim();
////
////            if (username.length() < 3) {
////                Toast.makeText(RegisterActivity.this, "Username must be at least 3 Characters", Toast.LENGTH_SHORT).show();
////                return;
////            }
////
////            if (password.length() < 8) {
////                Toast.makeText(RegisterActivity.this, "Password must be at least 8 Characters", Toast.LENGTH_SHORT).show();
////                return;
////            }
////
////            if (username.isEmpty() || password.isEmpty() || repeatPassword.isEmpty()) {
////                Toast.makeText(RegisterActivity.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
////                return;
////            }
////
////            if (!password.equals(repeatPassword)) {
////                Toast.makeText(RegisterActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
////                return;
////            }
////
////            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
////            SharedPreferences.Editor editor = sharedPreferences.edit();
////
////            if (sharedPreferences.contains(username)) {
////                Toast.makeText(RegisterActivity.this, "Username already exists", Toast.LENGTH_SHORT).show();
////                return;
////            }
////
////            // ✅ Generate a unique user ID only ONCE
////            String userId = UUID.randomUUID().toString();
////
////            // ✅ Save user ID and password in SharedPreferences
////            editor.putString(username, password);
////            editor.putString(username + "_ID", userId); // Store ID under "<username>_ID"
////            editor.apply();
////
////            Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
////            etUsername.setText("");
////            etPassword.setText("");
////            etRepeatPassword.setText("");
////        });
//
//    }
}









//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.NumberPicker;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//public class RegisterActivity extends AppCompatActivity {
//
//    private EditText etUsername, etPassword, etRepeatPassword;
//    private NumberPicker familySizePicker, numberOfRoomsPicker;
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
//        familySizePicker = findViewById(R.id.family_size_picker);
//        numberOfRoomsPicker = findViewById(R.id.number_of_rooms_picker);
//        butRegister = findViewById(R.id.but_register);
//        butSignIn = findViewById(R.id.but_sign_in);
//
//        // Set up the NumberPickers
//        familySizePicker.setMinValue(1);
//        familySizePicker.setMaxValue(20);
//        familySizePicker.setValue(1); // Default value
//
//        numberOfRoomsPicker.setMinValue(1);
//        numberOfRoomsPicker.setMaxValue(10);
//        numberOfRoomsPicker.setValue(1); // Default value
//
//        // Register button click listener
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
//            // Get selected values from the NumberPickers
//            int familySize = familySizePicker.getValue();
//            int numberOfRooms = numberOfRoomsPicker.getValue();
//
//            // Do something with the values (e.g., save them to SharedPreferences or pass them to the next activity)
//            Toast.makeText(RegisterActivity.this, "Family Size: " + familySize + ", Rooms: " + numberOfRooms, Toast.LENGTH_SHORT).show();
//
//            // You can proceed with registration logic here...
//        });
//
//        // Sign in button click listener
//        butSignIn.setOnClickListener(v -> {
//            // Navigate to sign-in activity if needed
//            // Example:
//            // Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
//            // startActivity(intent);
//        });
//    }
//}
