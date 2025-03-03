package com.example.newapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Set up Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Bottom Navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(navListener);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        // Get current user info from Firebase
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();  // Get logged-in user email

            // Save email to SharedPreferences
            SharedPreferences.Editor editor = getSharedPreferences("UserPrefs", MODE_PRIVATE).edit();
            editor.putString("USER_EMAIL", userEmail);
            editor.apply();
        }

        // Check if the user has set household info
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isFirstTime = sharedPreferences.getBoolean("FIRST_TIME", true);

        if (isFirstTime) {
            showHouseholdInputDialog(sharedPreferences);
        }
    }

    private void showHouseholdInputDialog(SharedPreferences sharedPreferences) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Household Information");

        // Layout for input fields
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText familySizeInput = new EditText(this);
        familySizeInput.setHint("Enter family size");
        layout.addView(familySizeInput);

        final EditText roomNumberInput = new EditText(this);
        roomNumberInput.setHint("Enter number of rooms");
        layout.addView(roomNumberInput);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                int familySize = Integer.parseInt(familySizeInput.getText().toString());
                int roomNumber = Integer.parseInt(roomNumberInput.getText().toString());

                // Save to SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("FAMILY_SIZE", familySize);
                editor.putInt("ROOM_NUMBER", roomNumber);
                editor.putBoolean("FIRST_TIME", false);
                editor.apply();
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.setCancelable(false);
        builder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logoutUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private final BottomNavigationView.OnItemSelectedListener navListener =
            item -> {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    selectedFragment = new HomeFragment();
                } else if (itemId == R.id.nav_assessment) {
                    selectedFragment = new AssessmentFragment();
                } else if (itemId == R.id.nav_profile) {
                    selectedFragment = new ProfileFragment();
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                }
                return true;
            };
}



//package com.example.newapp;
//
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.view.Menu;
//import android.view.MenuInflater;
//import android.view.MenuItem;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AlertDialog;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.fragment.app.Fragment;
//import com.google.android.material.bottomnavigation.BottomNavigationView;
//import com.google.firebase.auth.FirebaseAuth;
//import android.widget.EditText;
//import android.widget.LinearLayout;
//
//public class HomeActivity extends AppCompatActivity {
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_home);
//
//        // Set up the Toolbar as the Action Bar
//        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//
//        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
//        bottomNavigationView.setOnItemSelectedListener(navListener);
//
//        if (savedInstanceState == null) {
//            getSupportFragmentManager().beginTransaction()
//                    .replace(R.id.fragment_container, new HomeFragment())
//                    .commit();
//        }
//
//        // Check if user has already set family size and rooms
//        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
//        boolean isFirstTime = sharedPreferences.getBoolean("FIRST_TIME", true);
//
//        if (isFirstTime) {
//            showHouseholdInputDialog(sharedPreferences);
//        }
//    }
//
//    private void showHouseholdInputDialog(SharedPreferences sharedPreferences) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Household Information");
//
//        // Layout for input fields
//        LinearLayout layout = new LinearLayout(this);
//        layout.setOrientation(LinearLayout.VERTICAL);
//
//        // Input fields
//        final EditText familySizeInput = new EditText(this);
//        familySizeInput.setHint("Enter family size");
//        layout.addView(familySizeInput);
//
//        final EditText roomNumberInput = new EditText(this);
//        roomNumberInput.setHint("Enter number of rooms");
//        layout.addView(roomNumberInput);
//
//        builder.setView(layout);
//
//        builder.setPositiveButton("Save", (dialog, which) -> {
//            try {
//                int familySize = Integer.parseInt(familySizeInput.getText().toString());
//                int roomNumber = Integer.parseInt(roomNumberInput.getText().toString());
//
//                // Save to SharedPreferences
//                SharedPreferences.Editor editor = sharedPreferences.edit();
//                editor.putInt("FAMILY_SIZE", familySize);
//                editor.putInt("ROOM_NUMBER", roomNumber);
//                editor.putBoolean("FIRST_TIME", false);
//                editor.apply();
//            } catch (NumberFormatException e) {
//                e.printStackTrace();
//            }
//        });
//
//        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
//        builder.setCancelable(false);
//        builder.show();
//    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//        if (item.getItemId() == R.id.action_logout) {
//            logoutUser();
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//    private void logoutUser() {
//        FirebaseAuth.getInstance().signOut();
//        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        startActivity(intent);
//        finish();
//    }
//
//    private final BottomNavigationView.OnItemSelectedListener navListener =
//            item -> {
//                Fragment selectedFragment = null;
//                int itemId = item.getItemId();
//
//                if (itemId == R.id.nav_home) {
//                    selectedFragment = new HomeFragment();
//                } else if (itemId == R.id.nav_assessment) {
//                    selectedFragment = new AssessmentFragment();
//                } else if (itemId == R.id.nav_profile) {
//                    selectedFragment = new ProfileFragment();
//                }
//
//                if (selectedFragment != null) {
//                    getSupportFragmentManager().beginTransaction()
//                            .replace(R.id.fragment_container, selectedFragment)
//                            .commit();
//                }
//                return true;
//            };
//}
//
