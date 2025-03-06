package com.example.newapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class EnergyDetailsFragment extends Fragment {

    private static final String TAG = "EnergyDetailsFragment";
    private ProgressBar energyProgressBar;
    private TextView remainingEnergyText, usedEnergyText, washingMachineTextView, fridgeTextView, heatingSystemTextView, lastUpdatedTextView, pointsTextView;
    private DatabaseReference databaseReference;
    private float estimatedKWh, totalUsageKWh; // Track total usage for remaining energy
    private int points; // Track points
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_energy_details, container, false);

        // Initialize views
        energyProgressBar = view.findViewById(R.id.energy_progress_bar);
        remainingEnergyText = view.findViewById(R.id.remaining_energy_text);
        usedEnergyText = view.findViewById(R.id.used_energy_text); // New TextView for used energy
        washingMachineTextView = view.findViewById(R.id.text_washing_machine_reading);
        fridgeTextView = view.findViewById(R.id.text_fridge_reading);
        heatingSystemTextView = view.findViewById(R.id.text_heating_system_reading);
        lastUpdatedTextView = view.findViewById(R.id.text_last_updated);
        pointsTextView = view.findViewById(R.id.points_text);

        // Verify views are not null (debugging step)
        if (energyProgressBar == null || remainingEnergyText == null || usedEnergyText == null ||
                washingMachineTextView == null || fridgeTextView == null || heatingSystemTextView == null ||
                lastUpdatedTextView == null || pointsTextView == null) {
            Log.e(TAG, "One or more views are null. Check fragment_energy_details.xml IDs.");
            return view; // Early return to avoid further crashes
        }

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", requireActivity().MODE_PRIVATE);

        // Load estimated kWh and points from SharedPreferences
        estimatedKWh = sharedPreferences.getFloat("ESTIMATED_KWH", 0);
        points = sharedPreferences.getInt("POINTS", 0); // Initialize points to 0 if not set
        Log.d(TAG, "Estimated KWh loaded from SharedPreferences: " + estimatedKWh);

        // Start ProgressBar empty (progress = 0) with gray color (handled by drawable)
        energyProgressBar.setMax(100);
        energyProgressBar.setProgress(0);

        // Set initial UI values
        remainingEnergyText.setText("Remaining: " + String.format("%.2f", estimatedKWh) + " kWh");
        usedEnergyText.setText("Used: 0.00 kWh");
        washingMachineTextView.setText("Washing Machine: 0 Watts");
        fridgeTextView.setText("Fridge: 0 Watts");
        heatingSystemTextView.setText("Heating System: 0 Watts");
        lastUpdatedTextView.setText("Last Updated: --");
        pointsTextView.setText("Points: " + points);

        // Initialize Firebase reference
        databaseReference = FirebaseDatabase.getInstance("https://myapp2-75686-default-rtdb.firebaseio.com/")
                .getReference("sensor_data");

        // Start Firebase listener for TextView updates
        fetchLatestDataFromFirebase();

        // Start the test animation for the ProgressBar (comment out if using usage-based progress)
        startProgressBarTest();

        // Start the test animation for points (for demonstration)
        startPointsTest();

        return view;
    }

    private void fetchLatestDataFromFirebase() {
        databaseReference.orderByKey().limitToLast(1)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                Double wmUsage = data.child("washing_machine").getValue(Double.class);
                                Double fridgeUsage = data.child("fridge").getValue(Double.class);
                                Double heatingUsage = data.child("heating_system").getValue(Double.class);

                                // Default to 0 if null
                                double wm = wmUsage != null ? wmUsage : 0;
                                double fridge = fridgeUsage != null ? fridgeUsage : 0;
                                double heating = heatingUsage != null ? heatingUsage : 0;

                                // Update TextViews for device readings
                                washingMachineTextView.setText("Washing Machine: " + String.format("%.2f", wm) + " Watts");
                                fridgeTextView.setText("Fridge: " + String.format("%.2f", fridge) + " Watts");
                                heatingSystemTextView.setText("Heating System: " + String.format("%.2f", heating) + " Watts");

                                // Get and display the timestamp
                                String timestamp = data.child("timestamp").getValue(String.class);
                                if (timestamp != null) {
                                    lastUpdatedTextView.setText("Last Updated: " + timestamp);
                                } else {
                                    lastUpdatedTextView.setText("Last Updated: Unknown");
                                    Log.w(TAG, "Timestamp is null in snapshot");
                                }

                                // Calculate total usage in kWh (adjust hoursOfUsage as needed)
                                double hoursOfUsage = 1.0; // Assuming 1 hour for now; adjust based on your data
                                totalUsageKWh = (float) (((wm + fridge + heating) / 1000.0) * hoursOfUsage);

                                // Log for debugging
                                Log.d(TAG, "Sensor Values - WM: " + wm + "W, Fridge: " + fridge + "W, Heating: " + heating + "W");
                                Log.d(TAG, "Total Usage (kWh): " + totalUsageKWh + ", Estimated (kWh): " + estimatedKWh);

                                // Calculate and display remaining energy
                                float remainingKWh = estimatedKWh - totalUsageKWh;
                                if (remainingKWh < 0) remainingKWh = 0;
                                Log.d(TAG, "Remaining KWh: " + remainingKWh);
                                remainingEnergyText.setText("Remaining: " + String.format("%.2f", remainingKWh) + " kWh");

                                // Display the used energy
                                usedEnergyText.setText("Used: " + String.format("%.2f", totalUsageKWh) + " kWh");

                                // Optional: Tie progress bar to actual usage (comment out startProgressBarTest() if using this)
                                /*
                                if (estimatedKWh > 0) {
                                    float usagePercentage = (totalUsageKWh / estimatedKWh) * 100;
                                    energyProgressBar.setProgress((int) usagePercentage);
                                    Log.d(TAG, "Usage Percentage: " + usagePercentage + "%");
                                }
                                */
                            }
                        } else {
                            Log.d(TAG, "No data found in snapshot");
                            lastUpdatedTextView.setText("Last Updated: No data");
                            washingMachineTextView.setText("Washing Machine: 0 Watts");
                            fridgeTextView.setText("Fridge: 0 Watts");
                            heatingSystemTextView.setText("Heating System: 0 Watts");
                            remainingEnergyText.setText("Remaining: " + String.format("%.2f", estimatedKWh) + " kWh");
                            usedEnergyText.setText("Used: 0.00 kWh");
                            totalUsageKWh = 0;
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error fetching data: " + error.getMessage());
                        lastUpdatedTextView.setText("Last Updated: Error");
                        washingMachineTextView.setText("Washing Machine: 0 Watts");
                        fridgeTextView.setText("Fridge: 0 Watts");
                        heatingSystemTextView.setText("Heating System: 0 Watts");
                        remainingEnergyText.setText("Remaining: " + String.format("%.2f", estimatedKWh) + " kWh");
                        usedEnergyText.setText("Used: 0.00 kWh");
                    }
                });
    }

    private void startProgressBarTest() {
        final Handler handler = new Handler();
        final int[] progress = {0};

        // Simulate progress increasing from 0 to 100, then reset to 0
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (progress[0] <= 100) {
                    energyProgressBar.setProgress(progress[0]);
                    progress[0]++;
                    handler.postDelayed(this, 50); // Update every 50ms (20 updates per second)
                } else {
                    // Reset to 0 after reaching 100
                    progress[0] = 0;
                    energyProgressBar.setProgress(0);
                    handler.postDelayed(this, 50); // Start over
                }
            }
        };

        handler.post(runnable); // Start the animation
    }

    // Test method to simulate points increment (replace with your own logic later)
    private void startPointsTest() {
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                points++; // Increment points (for testing)
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("POINTS", points);
                editor.apply(); // Save to SharedPreferences
                pointsTextView.setText("Points: " + points); // Update UI
                handler.postDelayed(this, 5000); // Update every 5 seconds (for testing)
            }
        };
        handler.post(runnable); // Start the points increment
    }

    // Getter for progress to share with HomeFragment
    public int getProgress() {
        return energyProgressBar.getProgress();
    }
}