
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

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private ProgressBar energyProgressBar;
    private TextView remainingEnergyText, washingMachineTextView, fridgeTextView, heatingSystemTextView, lastUpdatedTextView;
    private DatabaseReference databaseReference;
    private float estimatedKWh, totalUsageKWh; // Track total usage for remaining energy

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        energyProgressBar = view.findViewById(R.id.energy_progress_bar);
        remainingEnergyText = view.findViewById(R.id.remaining_energy_text);
        washingMachineTextView = view.findViewById(R.id.text_washing_machine_reading);
        fridgeTextView = view.findViewById(R.id.text_fridge_reading);
        heatingSystemTextView = view.findViewById(R.id.text_heating_system_reading);
        lastUpdatedTextView = view.findViewById(R.id.text_last_updated);

        // Verify views are not null (debugging step)
        if (energyProgressBar == null || remainingEnergyText == null || washingMachineTextView == null ||
                fridgeTextView == null || heatingSystemTextView == null || lastUpdatedTextView == null) {
            Log.e(TAG, "One or more views are null. Check fragment_home.xml IDs.");
            return view; // Early return to avoid further crashes
        }

        // Load estimated kWh from SharedPreferences
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", requireActivity().MODE_PRIVATE);
        estimatedKWh = sharedPreferences.getFloat("ESTIMATED_KWH", 0);

        // Start ProgressBar empty (progress = 0) with gray color (handled by drawable)
        energyProgressBar.setMax(100);
        energyProgressBar.setProgress(0);

        // Set initial UI values
        remainingEnergyText.setText("Remaining: " + String.format("%.2f", estimatedKWh) + " kWh");
        washingMachineTextView.setText("Washing Machine: 0 Watts");
        fridgeTextView.setText("Fridge: 0 Watts");
        heatingSystemTextView.setText("Heating System: 0 Watts");
        lastUpdatedTextView.setText("Last Updated: --");

        // Initialize Firebase reference
        databaseReference = FirebaseDatabase.getInstance("https://myapp2-75686-default-rtdb.firebaseio.com/")
                .getReference("sensor_data");

        // Start Firebase listener for TextView updates
        fetchLatestDataFromFirebase();

        // Start the test animation for the ProgressBar
        startProgressBarTest();

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

                                // Calculate total usage in kWh
                                totalUsageKWh = (float) ((wm + fridge + heating) / 1000.0);

                                // Calculate and display remaining energy
                                float remainingKWh = estimatedKWh - totalUsageKWh;
                                if (remainingKWh < 0) remainingKWh = 0;
                                remainingEnergyText.setText("Remaining: " + String.format("%.2f", remainingKWh) + " kWh");
                            }
                        } else {
                            Log.d(TAG, "No data found in snapshot");
                            lastUpdatedTextView.setText("Last Updated: No data");
                            washingMachineTextView.setText("Washing Machine: 0 Watts");
                            fridgeTextView.setText("Fridge: 0 Watts");
                            heatingSystemTextView.setText("Heating System: 0 Watts");
                            remainingEnergyText.setText("Remaining: " + String.format("%.2f", estimatedKWh) + " kWh");
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
}