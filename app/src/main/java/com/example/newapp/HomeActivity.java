package com.example.newapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class HomeActivity extends AppCompatActivity {

    private DatabaseReference databaseReference;
    private TextView washingMachineTextView, fridgeTextView, heatingSystemTextView, lastUpdatedTextView, remainingEnergyText;
    private ProgressBar energyProgressBar;  // ✅ Re-added progress bar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Set up Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize UI elements
        washingMachineTextView = findViewById(R.id.text_washing_machine_reading);
        fridgeTextView = findViewById(R.id.text_fridge_reading);
        heatingSystemTextView = findViewById(R.id.text_heating_system_reading);
        lastUpdatedTextView = findViewById(R.id.text_last_updated);
        remainingEnergyText = findViewById(R.id.remaining_energy_text);
        energyProgressBar = findViewById(R.id.energy_progress_bar);  // ✅ Properly initialized

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
            String userEmail = currentUser.getEmail();
            SharedPreferences.Editor editor = getSharedPreferences("UserPrefs", MODE_PRIVATE).edit();
            editor.putString("USER_EMAIL", userEmail);
            editor.apply();
        }

        // Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("energy_usage");
        fetchSensorData();  // Fetch sensor values from Firebase
    }

    /**
     * Fetch sensor readings and last updated timestamp from Firebase.
     */
    private void fetchSensorData() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Log.d("Firebase", "Data received: " + snapshot.getValue().toString());

                    Long washingMachineUsage = snapshot.child("washing_machine").getValue(Long.class);
                    Long fridgeUsage = snapshot.child("fridge").getValue(Long.class);
                    Long heatingSystemUsage = snapshot.child("heating_system").getValue(Long.class);
                    Long lastUpdatedTimestamp = snapshot.child("last_updated").getValue(Long.class);
                    Long remainingEnergy = snapshot.child("remaining_energy").getValue(Long.class);

                    // Log each value for debugging
                    Log.d("Firebase", "Washing Machine: " + washingMachineUsage);
                    Log.d("Firebase", "Fridge: " + fridgeUsage);
                    Log.d("Firebase", "Heating System: " + heatingSystemUsage);
                    Log.d("Firebase", "Remaining Energy: " + remainingEnergy);
                    Log.d("Firebase", "Last Updated: " + lastUpdatedTimestamp);

                    // Update UI
                    washingMachineTextView.setText("Washing Machine: " + (washingMachineUsage != null ? washingMachineUsage : 0) + " Watts");
                    fridgeTextView.setText("Fridge: " + (fridgeUsage != null ? fridgeUsage : 0) + " Watts");
                    heatingSystemTextView.setText("Heating System: " + (heatingSystemUsage != null ? heatingSystemUsage : 0) + " Watts");

                    if (remainingEnergy != null) {
                        energyProgressBar.setProgress(remainingEnergy.intValue());  // ✅ Fixed progress bar
                        remainingEnergyText.setText("Remaining: " + remainingEnergy + " kWh");
                    }

                    if (lastUpdatedTimestamp != null) {
                        lastUpdatedTextView.setText("Last Updated: " + formatTimestamp(lastUpdatedTimestamp));
                    } else {
                        lastUpdatedTextView.setText("Last Updated: --");
                    }
                } else {
                    Log.d("Firebase", "No data found in database.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error fetching data", error.toException());
            }
        });
    }

    /**
     * Convert Firebase timestamp (milliseconds) to readable format.
     */
    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date(timestamp));
    }

    /**
     * Handles Bottom Navigation clicks.
     */
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
}
