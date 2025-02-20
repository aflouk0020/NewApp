package com.example.newapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Set up the Toolbar as the Action Bar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);  // Important: This makes the menu appear

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(navListener);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_home);
//
//        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
//        bottomNavigationView.setOnItemSelectedListener(navListener);
//
//        // Debugging
//        Log.d("Debug", "HomeActivity Loaded");
//
//        if (savedInstanceState == null) {
//            getSupportFragmentManager().beginTransaction()
//                    .replace(R.id.fragment_container, new HomeFragment())
//                    .commit();
//        }
    }

    // Create the menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    // Handle menu item clicks
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logoutUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Logout function
    private void logoutUser() {
        // Clear SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("USER_ID");  // Remove user session
        editor.apply();

        // Redirect to login
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);  // Prevent going back
        startActivity(intent);
        finish();
    }

    private final BottomNavigationView.OnItemSelectedListener navListener =
            new BottomNavigationView.OnItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
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
                }
            };
}



//package com.example.newapp;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.util.Log;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.fragment.app.Fragment;
//import com.google.android.material.bottomnavigation.BottomNavigationView;
//import android.view.MenuItem;
//
//public class HomeActivity extends AppCompatActivity {
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_home);
//
//        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
//
//        // Retrieve User ID and Username from Intent
//        String userId = getIntent().getStringExtra("USER_ID");
//        String username = getIntent().getStringExtra("USERNAME");
//
//        Log.d("UserInfo", "User ID: " + userId);
//        Log.d("UserInfo", "Username: " + username);
//
//        bottomNavigationView.setOnItemSelectedListener(navListener);
//
//        // Load the HomeFragment by default
//        if (savedInstanceState == null) {
//            getSupportFragmentManager().beginTransaction()
//                    .replace(R.id.fragment_container, new HomeFragment())
//                    .commit();
//        }
//    }
//
//    private final BottomNavigationView.OnItemSelectedListener navListener =
//            new BottomNavigationView.OnItemSelectedListener() {
//                @Override
//                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
//                    Fragment selectedFragment = null;
//                    int itemId = item.getItemId();
//
//                    if (itemId == R.id.nav_home) {
//                        selectedFragment = new HomeFragment();
//                    } else if (itemId == R.id.nav_assessment) {
//                        selectedFragment = new AssessmentFragment();
//                    } else if (itemId == R.id.nav_profile) {
//                        // Pass User ID to ProfileFragment
//                        selectedFragment = new ProfileFragment();
//                        Bundle bundle = new Bundle();
//                        bundle.putString("USER_ID", getIntent().getStringExtra("USER_ID"));
//                        selectedFragment.setArguments(bundle);
//                    }
//
//                    if (selectedFragment != null) {
//                        getSupportFragmentManager().beginTransaction()
//                                .replace(R.id.fragment_container, selectedFragment)
//                                .commit();
//                    }
//                    return true;
//                }
//            };
//}
