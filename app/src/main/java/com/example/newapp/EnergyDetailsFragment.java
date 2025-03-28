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
    private ProgressBar overuseProgressBar;
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
                    updateEstimatedTexts();
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

        bindUI(view);
        toggleCards(view);

        estimatedKWh = sharedPreferences.getFloat("ESTIMATED_KWH", 0);
        points = sharedPreferences.getInt("POINTS", 0);

        updateInitialUI();
        updateEstimatedTexts();

        sharedPreferences.registerOnSharedPreferenceChangeListener(prefsListener);

        fetchAggregatedTotals(view);
        fetchLatestDataFromFirebase();

        return view;
    }

    private void bindUI(View view) {
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
        overuseProgressBar = view.findViewById(R.id.overuse_progress_bar);
    }

    private void toggleCards(View view) {
        toggleCard(view, R.id.header_energy, R.id.content_energy, "Remaining Energy (kWh)");
        toggleCard(view, R.id.header_devices, R.id.content_devices, "Device Readings");
        toggleCard(view, R.id.header_points, R.id.content_points, "Points Earned");
        toggleCard(view, R.id.header_totals, R.id.content_totals, "Totals");
    }

    private void updateEstimatedTexts() {
        estimatedMonthlyText.setText("Estimated Monthly: " + String.format("%.2f", estimatedKWh) + " kWh");
        int daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
        double estimatedDailyUsage = estimatedKWh / daysInMonth;
        estimatedDailyText.setText(String.format("Estimated Daily: %.2f kWh", estimatedDailyUsage));
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
        washingMachineTextView.setText(sharedPreferences.getString("LIVE_WM", "Washing Machine: 0 Watts"));
        fridgeTextView.setText(sharedPreferences.getString("LIVE_FRIDGE", "Fridge: 0 Watts"));
        heatingSystemTextView.setText(sharedPreferences.getString("LIVE_HEAT", "Heating System: 0 Watts"));
        lastUpdatedTextView.setText(sharedPreferences.getString("LIVE_TIMESTAMP", "Last Updated: --"));
        pointsTextView.setText("Points: " + points);
        totalUsageText.setText(sharedPreferences.getString("TOTAL_USAGE_TEXT", "Total Usage: 0.00 Watts"));
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

    private void fetchLatestDataFromFirebase() {
        String userEmail = sharedPreferences.getString("USER_EMAIL", "default@gmail.com");
        String sanitizedEmail = sanitizeEmail(userEmail);

        databaseReference = FirebaseDatabase.getInstance().getReference("sensor_data").child(sanitizedEmail);

        databaseReference.orderByKey().limitToLast(1)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;
                        for (DataSnapshot data : snapshot.getChildren()) {
                            double wm = data.child("washing_machine").getValue(Double.class) != null ? data.child("washing_machine").getValue(Double.class) : 0;
                            double fridge = data.child("fridge").getValue(Double.class) != null ? data.child("fridge").getValue(Double.class) : 0;
                            double heating = data.child("heating_system").getValue(Double.class) != null ? data.child("heating_system").getValue(Double.class) : 0;
                            String rawTimestamp = data.child("timestamp").getValue(String.class);

                            String formattedTimestamp = formatTimestamp(rawTimestamp);

                            washingMachineTextView.setText("Washing Machine: " + String.format("%.2f", wm) + " Watts");
                            fridgeTextView.setText("Fridge: " + String.format("%.2f", fridge) + " Watts");
                            heatingSystemTextView.setText("Heating System: " + String.format("%.2f", heating) + " Watts");
                            lastUpdatedTextView.setText("Last Updated: " + formattedTimestamp);

                            sharedPreferences.edit()
                                    .putString("LIVE_WM", washingMachineTextView.getText().toString())
                                    .putString("LIVE_FRIDGE", fridgeTextView.getText().toString())
                                    .putString("LIVE_HEAT", heatingSystemTextView.getText().toString())
                                    .putString("LIVE_TIMESTAMP", lastUpdatedTextView.getText().toString())
                                    .apply();
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
                        double totalUsage = washingTotal + fridgeTotal + heatingTotal;

                        // Set UI
                        textTotalWashingMachine.setText("Total Washing Machine: " + String.format("%.2f", washingTotal) + " Watts");
                        textTotalFridge.setText("Total Fridge: " + String.format("%.2f", fridgeTotal) + " Watts");
                        textTotalHeatingSystem.setText("Total Heating System: " + String.format("%.2f", heatingTotal) + " Watts");
                        textTotalsTimestamp.setText("Last Updated: " + formattedTimestamp);
                        totalUsageText.setText("Total Usage: " + String.format("%.2f", totalUsage) + " kWh");

                        // Save to SharedPreferences
                        sharedPreferences.edit()
                                .putFloat("WASHING_TOTAL", (float) washingTotal)
                                .putFloat("FRIDGE_TOTAL", (float) fridgeTotal)
                                .putFloat("HEATING_TOTAL", (float) heatingTotal)
                                .putString("TOTALS_TIMESTAMP", formattedTimestamp)
                                .putFloat("TOTAL_USAGE_KWH", (float) totalUsage)
                                .apply();

                        float remaining = estimatedKWh - (float) totalUsage;
                        remainingEnergyText.setText("Remaining: " + String.format("%.2f", remaining) + " kWh");

                        if (totalUsage <= estimatedKWh) {
                            int progress = (int) ((totalUsage / estimatedKWh) * 100);
                            energyProgressBar.setProgress(progress);
                            energyProgressBar.setVisibility(View.VISIBLE);
                            overuseProgressBar.setVisibility(View.GONE);

                            sharedPreferences.edit()
                                    .putInt("PROGRESS_CIRCLE", progress)
                                    .putString("PROGRESS_COLOR", "green")
                                    .apply();
                        } else {
                            double overUsed = totalUsage - estimatedKWh;
                            int redProgress = (int) ((overUsed / estimatedKWh) * 100);
                            overuseProgressBar.setProgress(redProgress);
                            overuseProgressBar.setVisibility(View.VISIBLE);
                            energyProgressBar.setVisibility(View.GONE);

                            sharedPreferences.edit()
                                    .putInt("PROGRESS_CIRCLE", redProgress)
                                    .putString("PROGRESS_COLOR", "red")
                                    .apply();
                        }

                        calculateDailyPoints(totalUsage);

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing totals JSON", e);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch totals JSON", e);
                    // Fallback: Load from SharedPreferences
                    float washing = sharedPreferences.getFloat("WASHING_TOTAL", 0);
                    float fridge = sharedPreferences.getFloat("FRIDGE_TOTAL", 0);
                    float heating = sharedPreferences.getFloat("HEATING_TOTAL", 0);
                    String timestamp = sharedPreferences.getString("TOTALS_TIMESTAMP", "--");
                    float totalUsage = washing + fridge + heating;

                    textTotalWashingMachine.setText("Total Washing Machine: " + String.format("%.2f", washing) + " Watts");
                    textTotalFridge.setText("Total Fridge: " + String.format("%.2f", fridge) + " Watts");
                    textTotalHeatingSystem.setText("Total Heating System: " + String.format("%.2f", heating) + " Watts");
                    textTotalsTimestamp.setText("Last Updated: " + timestamp);
                    totalUsageText.setText("Total Usage: " + String.format("%.2f", totalUsage) + " kWh");

                    float remaining = estimatedKWh - totalUsage;
                    remainingEnergyText.setText("Remaining: " + String.format("%.2f", remaining) + " kWh");

                    calculateDailyPoints(totalUsage);
                });
    }



    private void calculateDailyPoints(double totalUsageToday) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastDate = sharedPreferences.getString("LAST_POINTS_DATE", "");
        if (lastDate.equals(today)) return;

        float dailyEstimate = estimatedKWh / Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
        int earned;
        if (totalUsageToday <= dailyEstimate * 0.8f) earned = 10;
        else if (totalUsageToday <= dailyEstimate) earned = 5;
        else if (totalUsageToday <= dailyEstimate * 1.2f) earned = -5;
        else earned = -10;

        int current = sharedPreferences.getInt("POINTS", 0);
        int newPoints = Math.max(0, current + earned);

        sharedPreferences.edit().putInt("POINTS", newPoints).putString("LAST_POINTS_DATE", today).apply();
        if (pointsTextView != null) pointsTextView.setText("Points: " + newPoints);
        if (pointsChangeTextView != null) pointsChangeTextView.setText("Points Today: " + earned);
    }

    private String sanitizeEmail(String email) {
        return email.replace(".", "_").replace("@", "_");
    }

    private String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return "--";
        try {
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());
            iso.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = iso.parse(timestamp);
            if (date == null) return "--";
            SimpleDateFormat readable = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            readable.setTimeZone(TimeZone.getDefault());
            return readable.format(date);
        } catch (ParseException e) {
            return "--";
        }
    }

    public int getProgress() {
        return energyProgressBar.getProgress();
    }
}
