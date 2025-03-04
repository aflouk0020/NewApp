package com.example.newapp;

import android.content.SharedPreferences;
import android.os.Bundle;
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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;

public class ProfileFragment extends Fragment {

    private TextView usernameTextView, familySizeTextView, roomNumberTextView, energyUsageTextView, kWhTextView;
    private SharedPreferences sharedPreferences;
    private ImageView infoIcon;
    private FirebaseUser currentUser;
    private FirebaseStorage storage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        sharedPreferences = getActivity().getSharedPreferences("UserPrefs", getActivity().MODE_PRIVATE);
        storage = FirebaseStorage.getInstance();

        usernameTextView = view.findViewById(R.id.user_id_text);
        familySizeTextView = view.findViewById(R.id.family_size_text);
        roomNumberTextView = view.findViewById(R.id.room_number_text);
        energyUsageTextView = view.findViewById(R.id.energy_usage_text);
        kWhTextView = view.findViewById(R.id.kwh_text); // New text field for kWh/month
        Button editButton = view.findViewById(R.id.edit_household_button);
        infoIcon = view.findViewById(R.id.info_icon);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        loadUserData();

        editButton.setOnClickListener(v -> showEditDialog());
        infoIcon.setOnClickListener(v -> showRules());

        return view;
    }

    private void loadUserData() {
        String email = sharedPreferences.getString("USER_EMAIL", "Guest");
        int familySize = sharedPreferences.getInt("FAMILY_SIZE", 0);
        int roomNumber = sharedPreferences.getInt("ROOM_NUMBER", 0);

        usernameTextView.setText("User Email: " + email);
        familySizeTextView.setText("Family Size: " + familySize);
        roomNumberTextView.setText("Room Number: " + roomNumber);

        String energyUsageCategory = calculateEnergyUsage(roomNumber, familySize);
        double kWhPerMonth = calculateKWhPerMonth(roomNumber, familySize);

        energyUsageTextView.setText("Energy Usage: " + energyUsageCategory);
        kWhTextView.setText("Estimated Monthly kWh: " + kWhPerMonth + " kWh");

    }

    private String calculateEnergyUsage(int rooms, int familySize) {
        if (rooms <= 2) {
            if (familySize <= 3) return "Low";
            else if (familySize <= 5) return "Slightly Higher Baseline";
            else return "Higher Baseline";
        } else if (rooms <= 4) {
            if (familySize <= 3) return "Medium";
            else if (familySize <= 5) return "Medium-High";
            else return "High";
        } else {
            if (familySize <= 3) return "High";
            else if (familySize <= 5) return "High";
            else return "Very High";
        }
    }

    private double calculateKWhPerMonth(int rooms, int familySize) {
        int R = 80; // Room weight
        int F = 20; // Family size weight
        return (rooms * R) + (familySize * F);
    }

    private void showEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Update Household Info");

        View view = getLayoutInflater().inflate(R.layout.dialog_edit_household, null);
        final EditText familySizeInput = view.findViewById(R.id.edit_family_size);
        final EditText roomNumberInput = view.findViewById(R.id.edit_room_number);

        familySizeInput.setText(String.valueOf(sharedPreferences.getInt("FAMILY_SIZE", 0)));
        roomNumberInput.setText(String.valueOf(sharedPreferences.getInt("ROOM_NUMBER", 0)));

        builder.setView(view);

        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                int familySize = Integer.parseInt(familySizeInput.getText().toString());
                int roomNumber = Integer.parseInt(roomNumberInput.getText().toString());

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("FAMILY_SIZE", familySize);
                editor.putInt("ROOM_NUMBER", roomNumber);
                editor.apply();

                loadUserData();
                uploadHouseholdDataToStorage(familySize, roomNumber);

                Toast.makeText(getActivity(), "Household info updated!", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), "Invalid input!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void uploadHouseholdDataToStorage(int familySize, int roomNumber) {
        if (currentUser == null) {
            Toast.makeText(getActivity(), "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        StorageReference storageRef = storage.getReference().child("users/" + userId + "/household.json");

        try {
            JSONObject householdData = new JSONObject();
            householdData.put("email", sharedPreferences.getString("USER_EMAIL", "Guest"));
            householdData.put("family_size", familySize);
            householdData.put("room_number", roomNumber);
            householdData.put("energy_usage", calculateEnergyUsage(roomNumber, familySize));
            householdData.put("monthly_kWh", calculateKWhPerMonth(roomNumber, familySize));

            byte[] jsonData = householdData.toString().getBytes(StandardCharsets.UTF_8);
            UploadTask uploadTask = storageRef.putBytes(jsonData);

            uploadTask.addOnSuccessListener(taskSnapshot ->
                    Toast.makeText(getActivity(), "Data uploaded to Firebase Storage!", Toast.LENGTH_SHORT).show()
            ).addOnFailureListener(e ->
                    Toast.makeText(getActivity(), "Upload failed!", Toast.LENGTH_SHORT).show()
            );

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Error preparing data for upload!", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRules() {
        new AlertDialog.Builder(getActivity())
                .setTitle("App Rules")
                .setMessage("1. Complete tasks...\n2. Earn points...\n3. Compete with friends...")
                .setPositiveButton("OK", null)
                .show();
    }
}
