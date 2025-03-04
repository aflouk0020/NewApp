//package com.example.newapp;
//
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.storage.FirebaseStorage;
//import com.google.firebase.storage.StorageReference;
//import com.google.firebase.storage.UploadTask;
//import org.json.JSONObject;
//import java.nio.charset.StandardCharsets;
//
//public class ProfileFragment extends Fragment {
//
//    private static final String TAG = "ProfileFragment";
//    private TextView usernameTextView, energyUsageTextView, kWhTextView;
//    private EditText familySizeEditText, roomNumberEditText;
//    private Button editButton;
//    private SharedPreferences sharedPreferences;
//    private ImageView infoIcon;
//    private FirebaseUser currentUser;
//    private FirebaseStorage storage;
//    private boolean isEditing = false; // Track editing state
//
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_profile, container, false);
//
//        sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", requireActivity().MODE_PRIVATE);
//        storage = FirebaseStorage.getInstance();
//
//        usernameTextView = view.findViewById(R.id.user_id_text);
//        familySizeEditText = view.findViewById(R.id.family_size_text);
//        roomNumberEditText = view.findViewById(R.id.room_number_text);
//        energyUsageTextView = view.findViewById(R.id.energy_usage_text);
//        kWhTextView = view.findViewById(R.id.kwh_text);
//        editButton = view.findViewById(R.id.edit_household_button);
//        infoIcon = view.findViewById(R.id.info_icon);
//
//        currentUser = FirebaseAuth.getInstance().getCurrentUser();
//
//        // Initially disable editing
//        setEditingEnabled(false);
//
//        loadUserData();
//
//        editButton.setOnClickListener(v -> toggleEditing());
//
//        infoIcon.setOnClickListener(v -> showRules());
//
//        return view;
//    }
//
//    private void loadUserData() {
//        if (getActivity() == null) return;
//
//        String email = sharedPreferences.getString("USER_EMAIL", "Guest");
//        int familySize = sharedPreferences.getInt("FAMILY_SIZE", 0);
//        int roomNumber = sharedPreferences.getInt("ROOM_NUMBER", 0);
//
//        usernameTextView.setText("User Email: " + email);
//        familySizeEditText.setText(String.valueOf(familySize));
//        roomNumberEditText.setText(String.valueOf(roomNumber));
//
//        String energyUsageCategory = calculateEnergyUsage(roomNumber, familySize);
//        double kWhPerMonth = calculateKWhPerMonth(roomNumber, familySize);
//
//        energyUsageTextView.setText("Energy Usage: " + energyUsageCategory);
//        kWhTextView.setText("Estimated Monthly kWh: " + kWhPerMonth + " kWh");
//    }
//
//    private String calculateEnergyUsage(int rooms, int familySize) {
//        if (rooms <= 2) {
//            if (familySize <= 3) return "Low";
//            else if (familySize <= 5) return "Slightly Higher Baseline";
//            else return "Higher Baseline";
//        } else if (rooms <= 4) {
//            if (familySize <= 3) return "Medium";
//            else if (familySize <= 5) return "Medium-High";
//            else return "High";
//        } else {
//            if (familySize <= 3) return "High";
//            else if (familySize <= 5) return "High";
//            else return "Very High";
//        }
//    }
//
//    private double calculateKWhPerMonth(int rooms, int familySize) {
//        int R = 80; // Room weight
//        int F = 20; // Family size weight
//        return (rooms * R) + (familySize * F);
//    }
//
//    private void toggleEditing() {
//        if (isEditing) {
//            saveUserData();
//            setEditingEnabled(false);
//            editButton.setText("Edit Household Info");
//        } else {
//            setEditingEnabled(true);
//            editButton.setText("Save");
//        }
//        isEditing = !isEditing;
//    }
//
//    private void setEditingEnabled(boolean enabled) {
//        familySizeEditText.setEnabled(enabled);
//        roomNumberEditText.setEnabled(enabled);
//    }
//
//    private void saveUserData() {
//        try {
//            int familySize = Integer.parseInt(familySizeEditText.getText().toString());
//            int roomNumber = Integer.parseInt(roomNumberEditText.getText().toString());
//
//            double estimatedKWh = calculateKWhPerMonth(roomNumber, familySize); // Calculate kWh
//
//            SharedPreferences.Editor editor = sharedPreferences.edit();
//            editor.putInt("FAMILY_SIZE", familySize);
//            editor.putInt("ROOM_NUMBER", roomNumber);
//            editor.putFloat("ESTIMATED_KWH", (float) estimatedKWh); // Save estimated kWh
//            editor.apply();
//
//            loadUserData();
//            uploadHouseholdDataToStorage(familySize, roomNumber);
//
//            Toast.makeText(getActivity(), "Household info updated!", Toast.LENGTH_SHORT).show();
//        } catch (Exception e) {
//            Log.e(TAG, "Error saving user data", e);
//            Toast.makeText(getActivity(), "Invalid input!", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    private void uploadHouseholdDataToStorage(int familySize, int roomNumber) {
//        if (currentUser == null || getActivity() == null) {
//            Log.e(TAG, "User is not logged in or Activity is null.");
//            return;
//        }
//
//        String userId = currentUser.getUid();
//        StorageReference storageRef = storage.getReference().child("users/" + userId + "/household.json");
//
//        try {
//            JSONObject householdData = new JSONObject();
//            householdData.put("email", sharedPreferences.getString("USER_EMAIL", "Guest"));
//            householdData.put("family_size", familySize);
//            householdData.put("room_number", roomNumber);
//            householdData.put("energy_usage", calculateEnergyUsage(roomNumber, familySize));
//            householdData.put("monthly_kWh", calculateKWhPerMonth(roomNumber, familySize));
//
//            byte[] jsonData = householdData.toString().getBytes(StandardCharsets.UTF_8);
//            UploadTask uploadTask = storageRef.putBytes(jsonData);
//
//            uploadTask.addOnSuccessListener(taskSnapshot -> {
//                if (getActivity() != null) {
//                    Toast.makeText(getActivity(), "Data uploaded to Firebase Storage!", Toast.LENGTH_SHORT).show();
//                }
//            }).addOnFailureListener(e -> {
//                Log.e(TAG, "Firebase Upload Failed", e);
//                if (getActivity() != null) {
//                    Toast.makeText(getActivity(), "Upload failed!", Toast.LENGTH_SHORT).show();
//                }
//            });
//
//        } catch (Exception e) {
//            Log.e(TAG, "Error preparing data for upload", e);
//            if (getActivity() != null) {
//                Toast.makeText(getActivity(), "Error preparing data for upload!", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//
//    private void showRules() {
//        if (getActivity() == null) return;
//
//        new androidx.appcompat.app.AlertDialog.Builder(getActivity())
//                .setTitle("App Rules")
//                .setMessage("1. Complete tasks...\n2. Earn points...\n3. Compete with friends...")
//                .setPositiveButton("OK", null)
//                .show();
//    }
//}
package com.example.newapp;

import androidx.appcompat.app.AlertDialog;
import android.content.SharedPreferences;
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

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private TextView usernameTextView, energyUsageTextView, kWhTextView;
    private EditText familySizeEditText, roomNumberEditText;
    private Button editButton;
    private SharedPreferences sharedPreferences;
    private ImageView infoIcon;
    private FirebaseUser currentUser;
    private boolean isEditing = false;

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
        editButton = view.findViewById(R.id.edit_household_button);
        infoIcon = view.findViewById(R.id.info_icon);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        setEditingEnabled(false);
        loadUserData();

        editButton.setOnClickListener(v -> toggleEditing());
        infoIcon.setOnClickListener(v -> showRules()); // Fixed: Method now exists

        return view;
    }

    private void showRules() {
        // Display an alert dialog with the app rules
        new AlertDialog.Builder(requireContext())
                .setTitle("Energy Usage Guidelines")
                .setMessage("1. Reduce unnecessary power usage.\n" +
                        "2. Turn off lights when not needed.\n" +
                        "3. Use energy-efficient appliances.\n" +
                        "4. Regularly check energy consumption.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void loadUserData() {
        String email = sharedPreferences.getString("USER_EMAIL", "Guest");
        int familySize = sharedPreferences.getInt("FAMILY_SIZE", 0);
        int roomNumber = sharedPreferences.getInt("ROOM_NUMBER", 0);

        usernameTextView.setText("User Email: " + email);
        familySizeEditText.setText(String.valueOf(familySize));
        roomNumberEditText.setText(String.valueOf(roomNumber));

        String energyUsageCategory = calculateEnergyUsage(roomNumber, familySize);
        double kWhPerMonth = calculateKWhPerMonth(roomNumber, familySize);

        energyUsageTextView.setText("Energy Usage: " + energyUsageCategory);
        kWhTextView.setText("Estimated Monthly kWh: " + kWhPerMonth + " kWh");

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat("ESTIMATED_KWH", (float) kWhPerMonth);
        editor.apply();
    }

    private String calculateEnergyUsage(int rooms, int familySize) {
        if (rooms <= 2) return (familySize <= 3) ? "Low" : (familySize <= 5) ? "Slightly Higher Baseline" : "Higher Baseline";
        if (rooms <= 4) return (familySize <= 3) ? "Medium" : (familySize <= 5) ? "Medium-High" : "High";
        return (familySize <= 3) ? "High" : (familySize <= 5) ? "High" : "Very High";
    }

    private double calculateKWhPerMonth(int rooms, int familySize) {
        return (rooms * 80) + (familySize * 20);
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
        // Get user input from EditText fields
        String familySizeStr = familySizeEditText.getText().toString();
        String roomNumberStr = roomNumberEditText.getText().toString();

        if (familySizeStr.isEmpty() || roomNumberStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            return;
        }

        int familySize = Integer.parseInt(familySizeStr);
        int roomNumber = Integer.parseInt(roomNumberStr);

        // Save to SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("FAMILY_SIZE", familySize);
        editor.putInt("ROOM_NUMBER", roomNumber);
        editor.apply();

        // Recalculate energy usage
        String energyUsageCategory = calculateEnergyUsage(roomNumber, familySize);
        double kWhPerMonth = calculateKWhPerMonth(roomNumber, familySize);

        // Update UI
        energyUsageTextView.setText("Energy Usage: " + energyUsageCategory);
        kWhTextView.setText("Estimated Monthly kWh: " + kWhPerMonth + " kWh");

        // Save estimated kWh in SharedPreferences
        editor.putFloat("ESTIMATED_KWH", (float) kWhPerMonth);
        editor.apply();

        Toast.makeText(requireContext(), "User data saved successfully", Toast.LENGTH_SHORT).show();
    }

}
