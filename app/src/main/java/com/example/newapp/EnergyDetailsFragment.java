package com.example.newapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class EnergyDetailsFragment extends Fragment {

    private static final String TAG = "EnergyDetailsFragment";
    // This totalsFilePath will be built dynamically in onCreateView using the user's email.
    private String totalsFilePath;

    private ProgressBar energyProgressBar;
    private TextView remainingEnergyText, usedEnergyText;
    private TextView washingMachineTextView, fridgeTextView, heatingSystemTextView;
    private TextView lastUpdatedTextView, pointsTextView;
    private TextView textTotalWashingMachine, textTotalFridge, textTotalHeatingSystem, textTotalsTimestamp;
    private TextView estimatedMonthlyText;

    private DatabaseReference databaseReference;
    private SharedPreferences sharedPreferences;

    private float estimatedKWh;
    private int points;

    private Handler totalsUpdateHandler = new Handler();
    private Runnable totalsUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (getView() != null) {
                fetchAggregatedTotals(getView());
            }
            // Schedule next update after 15 seconds.
            totalsUpdateHandler.postDelayed(this, 15000);
        }
    };

    // Listener to update estimated monthly kWh if it changes in SharedPreferences.
    private SharedPreferences.OnSharedPreferenceChangeListener prefsListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    if (key.equals("ESTIMATED_KWH")) {
                        estimatedKWh = sharedPreferences.getFloat("ESTIMATED_KWH", 0);
                        if (estimatedMonthlyText != null) {
                            estimatedMonthlyText.setText("Estimated Monthly: " +
                                    String.format("%.2f", estimatedKWh) + " kWh");
                        }
                    }
                }
            };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_energy_details, container, false);

        // Initialize SharedPreferences and retrieve the user email.
        sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userEmail = sharedPreferences.getString("USER_EMAIL", "default@gmail.com");
        Log.d(TAG, "EnergyDetailsFragment - userEmail from SharedPrefs: " + userEmail);
        totalsFilePath = "totals/" + userEmail + "/totals.json";
        Log.d(TAG, "EnergyDetailsFragment - totalsFilePath: " + totalsFilePath);

        // Initialize Totals Card UI elements.
        textTotalWashingMachine = view.findViewById(R.id.text_total_washing_machine);
        textTotalFridge = view.findViewById(R.id.text_total_fridge);
        textTotalHeatingSystem = view.findViewById(R.id.text_total_heating_system);

        // Initialize other UI elements.
        energyProgressBar = view.findViewById(R.id.energy_progress_bar);
        remainingEnergyText = view.findViewById(R.id.remaining_energy_text);
        usedEnergyText = view.findViewById(R.id.used_energy_text);
        washingMachineTextView = view.findViewById(R.id.text_washing_machine_reading);
        fridgeTextView = view.findViewById(R.id.text_fridge_reading);
        heatingSystemTextView = view.findViewById(R.id.text_heating_system_reading);
        lastUpdatedTextView = view.findViewById(R.id.text_last_updated);
        pointsTextView = view.findViewById(R.id.points_text);
        estimatedMonthlyText = view.findViewById(R.id.estimated_monthly_text);

        // Fetch aggregated totals from Firebase Storage using the dynamically built totalsFilePath.
        fetchAggregatedTotals(view);

        // Register SharedPreferences listener.
        sharedPreferences.registerOnSharedPreferenceChangeListener(prefsListener);
        // Load current values.
        estimatedKWh = sharedPreferences.getFloat("ESTIMATED_KWH", 0);
        points = sharedPreferences.getInt("POINTS", 0);

        // Initialize UI with default values.
        updateInitialUI();

        // Set the monthly estimate immediately.
        estimatedMonthlyText.setText("Estimated Monthly: " +
                String.format("%.2f", estimatedKWh) + " kWh");

        // Fetch real-time sensor data from Firebase Database.
        fetchLatestDataFromFirebase();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        totalsUpdateHandler.post(totalsUpdateRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        totalsUpdateHandler.removeCallbacks(totalsUpdateRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(prefsListener);
        }
    }

    private void updateInitialUI() {
        energyProgressBar.setMax(100);
        energyProgressBar.setProgress(0);
        // Set default fixed values.
        remainingEnergyText.setText("Remaining: 0.00 kWh");
        usedEnergyText.setText("Used: 0.00 kWh");

        washingMachineTextView.setText("Washing Machine: 0 Watts");
        fridgeTextView.setText("Fridge: 0 Watts");
        heatingSystemTextView.setText("Heating System: 0 Watts");
        lastUpdatedTextView.setText("Last Updated: --");
        pointsTextView.setText("Points: " + points);
    }

    private String sanitizeEmail(String email) {
        return email.replace(".", "_").replace("@", "_");
    }

    private void fetchLatestDataFromFirebase() {
        // 1) Get the raw email from SharedPreferences
        String userEmail = sharedPreferences.getString("USER_EMAIL", "default@gmail.com");

        // 2) Sanitize it
        String sanitizedEmail = sanitizeEmail(userEmail);

        // 3) Build the reference to the user's sensor data node
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("sensor_data")
                .child(sanitizedEmail);

        // 4) Query the last sensor reading under that user node
        databaseReference.orderByKey().limitToLast(1)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Log.d(TAG, "No data found in snapshot");
                            updateInitialUI();
                            return;
                        }

                        for (DataSnapshot data : snapshot.getChildren()) {
                            double wm = data.child("washing_machine").getValue(Double.class) != null
                                    ? data.child("washing_machine").getValue(Double.class) : 0;
                            double fridge = data.child("fridge").getValue(Double.class) != null
                                    ? data.child("fridge").getValue(Double.class) : 0;
                            double heating = data.child("heating_system").getValue(Double.class) != null
                                    ? data.child("heating_system").getValue(Double.class) : 0;
                            String rawTimestamp = data.child("timestamp").getValue(String.class);

                            Log.d(TAG, "Raw Timestamp from Firebase: " + rawTimestamp);

                            String formattedTimestamp = formatTimestamp(rawTimestamp);
                            Log.d(TAG, "Formatted Timestamp: " + formattedTimestamp);

                            // Update sensor reading UI
                            washingMachineTextView.setText(
                                    "Washing Machine: " + String.format("%.2f", wm) + " Watts");
                            fridgeTextView.setText(
                                    "Fridge: " + String.format("%.2f", fridge) + " Watts");
                            heatingSystemTextView.setText(
                                    "Heating System: " + String.format("%.2f", heating) + " Watts");
                            lastUpdatedTextView.setText("Last Updated: " + formattedTimestamp);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error fetching data: " + error.getMessage());
                        updateInitialUI();
                    }
                });
    }

    private String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return "--";
        }
        try {
            // Parse timestamp assuming ISO 8601 with microseconds.
            SimpleDateFormat isoFormat =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = isoFormat.parse(timestamp);
            if (date == null) return "--";
            SimpleDateFormat readableFormat =
                    new SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault());
            readableFormat.setTimeZone(TimeZone.getDefault());
            return readableFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing timestamp: " + timestamp, e);
            return "--";
        }
    }

    // Optional getter if you need to share the progress value with other fragments.
    public int getProgress() {
        return energyProgressBar.getProgress();
    }

    private void fetchAggregatedTotals(View rootView) {
        // Use the dynamically constructed totalsFilePath.
        StorageReference totalsRef = FirebaseStorage.getInstance().getReference()
                .child(totalsFilePath);

        totalsRef.getBytes(1024 * 1024)
                .addOnSuccessListener(bytes -> {
                    try {
                        String json = new String(bytes, StandardCharsets.UTF_8);
                        JSONObject jsonObject = new JSONObject(json);

                        double washingTotal = jsonObject.optDouble("washing_machine_total", 0);
                        double fridgeTotal = jsonObject.optDouble("fridge_total", 0);
                        double heatingTotal = jsonObject.optDouble("heating_system_total", 0);
                        String rawTimestamp = jsonObject.optString("timestamp", "--");

                        String formattedTimestamp = formatTimestamp(rawTimestamp);

                        TextView washingTotalText = rootView.findViewById(R.id.text_total_washing_machine);
                        TextView fridgeTotalText = rootView.findViewById(R.id.text_total_fridge);
                        TextView heatingTotalText = rootView.findViewById(R.id.text_total_heating_system);
                        TextView totalsTimestampText = rootView.findViewById(R.id.text_totals_timestamp);

                        washingTotalText.setText("Total Washing Machine: " + String.format("%.2f", washingTotal) + " Watts");
                        fridgeTotalText.setText("Total Fridge: " + String.format("%.2f", fridgeTotal) + " Watts");
                        heatingTotalText.setText("Total Heating System: " + String.format("%.2f", heatingTotal) + " Watts");
                        totalsTimestampText.setText("Last Updated: " + formattedTimestamp);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing totals JSON", e);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch totals JSON", e));
    }
}
