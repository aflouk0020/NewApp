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
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.*;
import com.google.firebase.storage.*;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class EnergyDetailsFragment extends Fragment {

    private static final String TAG = "EnergyDetailsFragment";
    private String totalsFilePath;

    private ProgressBar energyProgressBar;
    private TextView remainingEnergyText;
    private TextView washingMachineTextView, fridgeTextView, heatingSystemTextView;
    private TextView lastUpdatedTextView, pointsTextView, pointsChangeTextView;
    private TextView textTotalWashingMachine, textTotalFridge, textTotalHeatingSystem, textTotalsTimestamp;
    private TextView estimatedMonthlyText, estimatedDailyText, totalUsageText;

    private DatabaseReference databaseReference;
    private SharedPreferences sharedPreferences;

    private float estimatedKWh;
    private int points;

    private final Handler totalsUpdateHandler = new Handler();
    private final Runnable totalsUpdateRunnable = () -> {
        if (getView() != null) {
            fetchAggregatedTotals(getView());
        }
        totalsUpdateHandler.postDelayed(this.totalsUpdateRunnable, 15000);
    };

    private final SharedPreferences.OnSharedPreferenceChangeListener prefsListener =
            (sharedPreferences, key) -> {
                if (key.equals("ESTIMATED_KWH")) {
                    estimatedKWh = sharedPreferences.getFloat("ESTIMATED_KWH", 0);
                    if (estimatedMonthlyText != null) {
                        estimatedMonthlyText.setText("Estimated Monthly: " +
                                String.format("%.2f", estimatedKWh) + " kWh");

                        int daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
                        double estimatedDailyUsage = estimatedKWh / daysInMonth;
                        if (estimatedDailyText != null) {
                            estimatedDailyText.setText(String.format("Estimated Daily: %.2f kWh", estimatedDailyUsage));
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

        sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userEmail = sharedPreferences.getString("USER_EMAIL", "default@gmail.com");
        totalsFilePath = "totals/" + userEmail + "/totals.json";

        // Bind UI
        textTotalWashingMachine = view.findViewById(R.id.text_total_washing_machine);
        textTotalFridge = view.findViewById(R.id.text_total_fridge);
        textTotalHeatingSystem = view.findViewById(R.id.text_total_heating_system);
        textTotalsTimestamp = view.findViewById(R.id.text_totals_timestamp);
        totalUsageText = view.findViewById(R.id.text_total_usage);
        energyProgressBar = view.findViewById(R.id.energy_progress_bar);
        remainingEnergyText = view.findViewById(R.id.remaining_energy_text);
        washingMachineTextView = view.findViewById(R.id.text_washing_machine_reading);
        fridgeTextView = view.findViewById(R.id.text_fridge_reading);
        heatingSystemTextView = view.findViewById(R.id.text_heating_system_reading);
        lastUpdatedTextView = view.findViewById(R.id.text_last_updated);
        pointsTextView = view.findViewById(R.id.points_text);
        estimatedMonthlyText = view.findViewById(R.id.estimated_monthly_text);
        estimatedDailyText = view.findViewById(R.id.estimated_daily_text);
        pointsChangeTextView = view.findViewById(R.id.text_points_change);

        // Toggle Cards
        toggleCard(view, R.id.header_energy, R.id.content_energy, "Remaining Energy (kWh)");
        toggleCard(view, R.id.header_devices, R.id.content_devices, "Device Readings");
        toggleCard(view, R.id.header_points, R.id.content_points, "Points Earned");
        toggleCard(view, R.id.header_totals, R.id.content_totals, "Totals");

        estimatedKWh = sharedPreferences.getFloat("ESTIMATED_KWH", 0);
        points = sharedPreferences.getInt("POINTS", 0);

        updateInitialUI();

        estimatedMonthlyText.setText("Estimated Monthly: " +
                String.format("%.2f", estimatedKWh) + " kWh");

        int daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
        double estimatedDailyUsage = estimatedKWh / daysInMonth;
        estimatedDailyText.setText(String.format("Estimated Daily: %.2f kWh", estimatedDailyUsage));

        sharedPreferences.registerOnSharedPreferenceChangeListener(prefsListener);

        fetchAggregatedTotals(view);
        fetchLatestDataFromFirebase();

        return view;
    }

    private void toggleCard(View rootView, int headerId, int contentId, String title) {
        TextView header = rootView.findViewById(headerId);
        LinearLayout content = rootView.findViewById(contentId);

        header.setOnClickListener(v -> {
            if (content.getVisibility() == View.GONE) {
                content.setVisibility(View.VISIBLE);
                header.setText(title + " ▲");
            } else {
                content.setVisibility(View.GONE);
                header.setText(title + " ▼");
            }
        });
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
        washingMachineTextView.setText("Washing Machine: 0 Watts");
        fridgeTextView.setText("Fridge: 0 Watts");
        heatingSystemTextView.setText("Heating System: 0 Watts");
        lastUpdatedTextView.setText("Last Updated: --");
        pointsTextView.setText("Points: " + points);
        totalUsageText.setText("Total Usage: 0.00 Watts");
    }

    private String sanitizeEmail(String email) {
        return email.replace(".", "_").replace("@", "_");
    }

    private void fetchLatestDataFromFirebase() {
        String userEmail = sharedPreferences.getString("USER_EMAIL", "default@gmail.com");
        String sanitizedEmail = sanitizeEmail(userEmail);

        databaseReference = FirebaseDatabase.getInstance()
                .getReference("sensor_data")
                .child(sanitizedEmail);

        databaseReference.orderByKey().limitToLast(1)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
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

                            String formattedTimestamp = formatTimestamp(rawTimestamp);

                            washingMachineTextView.setText("Washing Machine: " + String.format("%.2f", wm) + " Watts");
                            fridgeTextView.setText("Fridge: " + String.format("%.2f", fridge) + " Watts");
                            heatingSystemTextView.setText("Heating System: " + String.format("%.2f", heating) + " Watts");
                            lastUpdatedTextView.setText("Last Updated: " + formattedTimestamp);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        updateInitialUI();
                    }
                });
    }

    private void fetchAggregatedTotals(View rootView) {
        StorageReference totalsRef = FirebaseStorage.getInstance().getReference().child(totalsFilePath);

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

                        textTotalWashingMachine.setText("Total Washing Machine: " + String.format("%.2f", washingTotal) + " Watts");
                        textTotalFridge.setText("Total Fridge: " + String.format("%.2f", fridgeTotal) + " Watts");
                        textTotalHeatingSystem.setText("Total Heating System: " + String.format("%.2f", heatingTotal) + " Watts");
                        textTotalsTimestamp.setText("Last Updated: " + formattedTimestamp);

                        double totalUsage = washingTotal + fridgeTotal + heatingTotal;
                        totalUsageText.setText("Total Usage: " + String.format("%.2f", totalUsage) + " kWh");

                        sharedPreferences.edit().putFloat("TOTAL_USAGE_KWH", (float) totalUsage).apply();

                        float remaining = estimatedKWh - (float) totalUsage;
                        if (remaining < 0) remaining = 0;
                        if (remainingEnergyText != null) {
                            remainingEnergyText.setText("Remaining: " + String.format("%.2f", remaining) + " kWh");
                        }

                        calculateDailyPoints(totalUsage);

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing totals JSON", e);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch totals JSON", e);

                    float fallbackUsage = sharedPreferences.getFloat("TOTAL_USAGE_KWH", 0);
                    Log.d(TAG, "Using fallback total usage for points: " + fallbackUsage);
                    calculateDailyPoints(fallbackUsage);
                });
    }

    private void calculateDailyPoints(double totalUsageToday) {
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String lastAwardedDate = prefs.getString("LAST_POINTS_DATE", "");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (lastAwardedDate.equals(today)) return;

        float dailyEstimate = estimatedKWh / Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
        int earnedPoints;

        if (totalUsageToday <= dailyEstimate * 0.8f) {
            earnedPoints = 10;
        } else if (totalUsageToday <= dailyEstimate) {
            earnedPoints = 5;
        } else if (totalUsageToday <= dailyEstimate * 1.2f) {
            earnedPoints = -5;
        } else {
            earnedPoints = -10;
        }

        int currentPoints = prefs.getInt("POINTS", 0);
        int newPoints = Math.max(0, currentPoints + earnedPoints);

        prefs.edit()
                .putInt("POINTS", newPoints)
                .putString("LAST_POINTS_DATE", today)
                .apply();

        if (pointsTextView != null) {
            pointsTextView.setText("Points: " + newPoints);
        }
        if (pointsChangeTextView != null) {
            pointsChangeTextView.setText("Points Today: " + earnedPoints);
        }

        Log.d(TAG, "Daily Points Calculated — Usage: " + totalUsageToday + " → Points: " + earnedPoints);
    }

    private String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return "--";
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = isoFormat.parse(timestamp);
            if (date == null) return "--";
            SimpleDateFormat readableFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            readableFormat.setTimeZone(TimeZone.getDefault());
            return readableFormat.format(date);
        } catch (ParseException e) {
            return "--";
        }
    }

    public int getProgress() {
        return energyProgressBar.getProgress();
    }
}
