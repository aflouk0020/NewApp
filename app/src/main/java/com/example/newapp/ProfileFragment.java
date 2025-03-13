package com.example.newapp;

import androidx.appcompat.app.AlertDialog;

import android.content.SharedPreferences;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private TextView usernameTextView, energyUsageTextView, kWhTextView, pointsTextView;
    private EditText familySizeEditText, roomNumberEditText;
    private Button editButton;
    private SharedPreferences sharedPreferences;
    private ImageView infoIcon;
    private FirebaseUser currentUser;
    private boolean isEditing = false;
    private int points;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", requireActivity().MODE_PRIVATE);

        usernameTextView = view.findViewById(R.id.user_id_text);
        familySizeEditText = view.findViewById(R.id.family_size_text);
        roomNumberEditText = view.findViewById(R.id.room_number_text);
        energyUsageTextView = view.findViewById(R.id.energy_usage_text);
        kWhTextView = view.findViewById(R.id.kwh_text);
        pointsTextView = view.findViewById(R.id.points_text);
        editButton = view.findViewById(R.id.edit_household_button);
        infoIcon = view.findViewById(R.id.info_icon);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        setEditingEnabled(false);
        loadUserData();

        // Listen for changes to points and update Firebase
        sharedPreferences.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
            if (key.equals("POINTS")) {
                int updatedPoints = sharedPreferences.getInt("POINTS", 0);
                updatePointsInFirebase(updatedPoints);
            }
        });

        editButton.setOnClickListener(v -> toggleEditing());
        infoIcon.setOnClickListener(v -> showRules());

        return view;
    }

    private void updatePointsInFirebase(int newPoints) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "No authenticated user found.");
            return;
        }

        String userId = user.getUid();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("users/" + userId + "/household.json");

        // Retrieve existing user data, update points, and re-upload JSON
        storageRef.getBytes(1024 * 1024)
                .addOnSuccessListener(bytes -> {
                    try {
                        String json = new String(bytes, StandardCharsets.UTF_8);
                        JSONObject jsonObject = new JSONObject(json);

                        jsonObject.put("points", newPoints); // Update points value

                        byte[] updatedData = jsonObject.toString().getBytes(StandardCharsets.UTF_8);
                        storageRef.putBytes(updatedData)
                                .addOnSuccessListener(taskSnapshot -> Log.d(TAG, "Points updated in Firebase Storage"))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to update points in Firebase Storage", e));

                    } catch (Exception e) {
                        Log.e(TAG, "Error updating points in JSON", e);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch household.json for updating points", e));
    }

    private void showRules() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Energy Usage Guidelines")
                .setMessage("1. Reduce unnecessary power usage.\n" +
                        "2. Turn off lights when not needed.\n" +
                        "3. Use energy-efficient appliances.\n" +
                        "4. Regularly check energy consumption.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

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

    private void loadUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "No authenticated user found.");
            return;
        }

        String userId = user.getUid();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("users/" + userId + "/household.json");

        storageRef.getBytes(1024 * 1024)
                .addOnSuccessListener(bytes -> {
                    try {
                        String json = new String(bytes, StandardCharsets.UTF_8);
                        JSONObject jsonObject = new JSONObject(json);

                        String email = jsonObject.optString("email", "Guest");
                        Log.d(TAG, "Email from JSON: " + email);

                        int familySize = jsonObject.optInt("family_size", 0);
                        int roomNumber = jsonObject.optInt("room_number", 0);
                        String energyUsage = jsonObject.optString("energy_usage", "Unknown");
                        double kWhPerMonth = jsonObject.optDouble("monthly_kWh", 0);
                        points = jsonObject.optInt("points", 0);

                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("USER_EMAIL", email);
                        editor.putInt("FAMILY_SIZE", familySize);
                        editor.putInt("ROOM_NUMBER", roomNumber);
                        editor.putFloat("ESTIMATED_KWH", (float) kWhPerMonth);
                        editor.putInt("POINTS", points);
                        editor.apply();

                        usernameTextView.setText("User Email: " + email);
                        familySizeEditText.setText(String.valueOf(familySize));
                        roomNumberEditText.setText(String.valueOf(roomNumber));
                        energyUsageTextView.setText("Energy Usage: " + energyUsage);
                        kWhTextView.setText("Estimated Monthly kWh: " + kWhPerMonth + " kWh");
                        pointsTextView.setText("Points: " + points);

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing household.json", e);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch household.json", e));
    }

    private void toggleEditing() {
        if (isEditing) {
            saveUserData();
            setEditingEnabled(false);
            editButton.setText("Edit Household Info");
        } else {
            setEditingEnabled(true);
            editButton.setText("Save");
        }
        isEditing = !isEditing;
    }

    private void setEditingEnabled(boolean enabled) {
        familySizeEditText.setEnabled(enabled);
        roomNumberEditText.setEnabled(enabled);
    }





    private void saveUserData() {
        String familySizeStr = familySizeEditText.getText().toString();
        String roomNumberStr = roomNumberEditText.getText().toString();

        if (familySizeStr.isEmpty() || roomNumberStr.isEmpty() || familySizeStr.equals("Loading...") || roomNumberStr.equals("Loading...")) {
            Toast.makeText(requireContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            return;
        }

        int familySize = Integer.parseInt(familySizeStr);
        int roomNumber = Integer.parseInt(roomNumberStr);
        String energyUsageCategory = calculateEnergyUsage(roomNumber, familySize);
        double kWhPerMonth = calculateKWhPerMonth(roomNumber, familySize);
        points = sharedPreferences.getInt("POINTS", 0);

        JSONObject userData = new JSONObject();
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;
            String userId = user.getUid();
            userData.put("email", user.getEmail());
            userData.put("family_size", familySize);
            userData.put("room_number", roomNumber);
            userData.put("energy_usage", energyUsageCategory);
            userData.put("monthly_kWh", kWhPerMonth);
            userData.put("points", points);

            byte[] data = userData.toString().getBytes(StandardCharsets.UTF_8);
            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                    .child("users/" + userId + "/household.json");

            storageRef.putBytes(data)
                    .addOnSuccessListener(taskSnapshot -> {
                        if (!isAdded()) {
                            return;
                        }
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("FAMILY_SIZE", familySize);
                        editor.putInt("ROOM_NUMBER", roomNumber);
                        editor.putFloat("ESTIMATED_KWH", (float) kWhPerMonth);
                        editor.putInt("POINTS", points);
                        // Mark profile as complete.
                        editor.putBoolean("PROFILE_COMPLETE", true);
                        editor.apply();

                        energyUsageTextView.setText("Energy Usage: " + energyUsageCategory);
                        kWhTextView.setText("Estimated Monthly kWh: " + kWhPerMonth + " kWh");
                        pointsTextView.setText("Points: " + points);

                        Toast.makeText(requireContext(), "User data saved successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to upload household.json", e));
        } catch (Exception e) {
            Log.e(TAG, "Error creating JSON for household.json", e);
        }
    }

}
