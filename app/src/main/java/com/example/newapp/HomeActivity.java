package com.example.newapp;
import android.widget.Toast;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;  // For AlertDialog
import androidx.fragment.app.Fragment;
import android.widget.EditText;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class HomeActivity extends AppCompatActivity {

    private DatabaseReference databaseReference;
    private TextView washingMachineTextView, fridgeTextView, heatingSystemTextView, lastUpdatedTextView, remainingEnergyText;
    private ProgressBar energyProgressBar;
    private static final String TAG = "HomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Set up Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize Bottom Navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(navListener);

        // Show HomeFragment by default
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        // Check if user is logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("USER_EMAIL", userEmail);
            editor.apply();

            // Only check if household data exists if profile is not complete
            boolean profileComplete = prefs.getBoolean("PROFILE_COMPLETE", false);
            if (!profileComplete) {
                checkIfHouseholdExists(currentUser.getUid(), bottomNavigationView);
            }
        }

        // (Other code such as fetchSensorData() can be placed here)
    }

    /**
     * Check if household.json exists in Firebase Storage.
     * If not found, switch to Profile tab and show a dialog
     * with input fields for family size and number of rooms.
     */
    private void checkIfHouseholdExists(String userId, BottomNavigationView bottomNav) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("users/" + userId + "/household.json");

        // Attempt to download 1 byte to see if the file exists.
        storageRef.getBytes(1)
                .addOnSuccessListener(bytes -> {
                    // File exists; nothing further needed.
                    Log.d(TAG, "household.json exists for userId: " + userId);
                })
                .addOnFailureListener(e -> {
                    // File doesn't exist => force user to fill in household details.
                    Log.d(TAG, "household.json not found for userId: " + userId + ", redirecting to Profile.");
                    bottomNav.setSelectedItemId(R.id.nav_profile);

                    // Inflate custom dialog view.
                    View dialogView = getLayoutInflater().inflate(R.layout.dialog_profile_input, null);
                    final EditText etFamilySize = dialogView.findViewById(R.id.etFamilySize);
                    final EditText etRoomNumber = dialogView.findViewById(R.id.etRoomNumber);

                    new AlertDialog.Builder(HomeActivity.this)
                            .setTitle("Complete Your Profile")
                            .setView(dialogView)
                            .setCancelable(false) // Force the user to interact.
                            .setPositiveButton("Save", (dialog, which) -> {
                                String familySizeStr = etFamilySize.getText().toString().trim();
                                String roomNumberStr = etRoomNumber.getText().toString().trim();
                                if (familySizeStr.isEmpty() || roomNumberStr.isEmpty()) {
                                    Toast.makeText(HomeActivity.this, "Please enter valid values", Toast.LENGTH_SHORT).show();
                                    // Optionally, you may call checkIfHouseholdExists() again.
                                } else {
                                    int familySize = Integer.parseInt(familySizeStr);
                                    int roomNumber = Integer.parseInt(roomNumberStr);
                                    saveHouseholdData(userId, familySize, roomNumber);
                                }
                            })
                            .show();
                });
    }

    /**
     * Save the household data for the user.
     * This method uploads household.json to Firebase Storage and sets a flag.
     */
    private void saveHouseholdData(String userId, int familySize, int roomNumber) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String email = user.getEmail();

        String energyUsage = calculateEnergyUsage(roomNumber, familySize);
        double kWhPerMonth = calculateKWhPerMonth(roomNumber, familySize);
        int points = 0;  // Initial points

        try {
            JSONObject householdData = new JSONObject();
            householdData.put("email", email);
            householdData.put("family_size", familySize);
            householdData.put("room_number", roomNumber);
            householdData.put("energy_usage", energyUsage);
            householdData.put("monthly_kWh", kWhPerMonth);
            householdData.put("points", points);

            byte[] data = householdData.toString().getBytes(StandardCharsets.UTF_8);
            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                    .child("users/" + userId + "/household.json");
            storageRef.putBytes(data)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Mark the profile as complete.
                        SharedPreferences.Editor editor = getSharedPreferences("UserPrefs", MODE_PRIVATE).edit();
                        editor.putString("USER_EMAIL", email);
                        editor.putInt("FAMILY_SIZE", familySize);
                        editor.putInt("ROOM_NUMBER", roomNumber);
                        editor.putFloat("ESTIMATED_KWH", (float) kWhPerMonth);
                        editor.putInt("POINTS", points);
                        editor.putBoolean("PROFILE_COMPLETE", true);
                        editor.apply();
                        Toast.makeText(HomeActivity.this, "Profile data saved successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to upload household.json", e);
                        Toast.makeText(HomeActivity.this, "Error saving profile data", Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error creating JSON for household data", e);
        }
    }

    // Sample implementations for calculating energy usage.
    private String calculateEnergyUsage(int rooms, int familySize) {
        if (rooms <= 2)
            return (familySize <= 3) ? "Low" : (familySize <= 5) ? "Slightly Higher Baseline" : "Higher Baseline";
        if (rooms <= 4)
            return (familySize <= 3) ? "Medium" : (familySize <= 5) ? "Medium-High" : "High";
        return (familySize <= 3) ? "High" : (familySize <= 5) ? "High" : "Very High";
    }

    private double calculateKWhPerMonth(int rooms, int familySize) {
        return (rooms * 80) + (familySize * 20);
    }

    /**
     * (Existing method to fetch sensor data remains unchanged.)
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

                    Log.d("Firebase", "Washing Machine: " + washingMachineUsage);
                    Log.d("Firebase", "Fridge: " + fridgeUsage);
                    Log.d("Firebase", "Heating System: " + heatingSystemUsage);
                    Log.d("Firebase", "Remaining Energy: " + remainingEnergy);
                    Log.d("Firebase", "Last Updated: " + lastUpdatedTimestamp);

                    // Update UI elements (assumed to be initialized)
                    washingMachineTextView.setText("Washing Machine: " + (washingMachineUsage != null ? washingMachineUsage : 0) + " Watts");
                    fridgeTextView.setText("Fridge: " + (fridgeUsage != null ? fridgeUsage : 0) + " Watts");
                    heatingSystemTextView.setText("Heating System: " + (heatingSystemUsage != null ? heatingSystemUsage : 0) + " Watts");

                    if (remainingEnergy != null) {
                        energyProgressBar.setProgress(remainingEnergy.intValue());
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
     * Convert a Firebase timestamp (in milliseconds) to a readable format.
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
