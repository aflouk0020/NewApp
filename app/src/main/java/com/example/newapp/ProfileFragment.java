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

public class ProfileFragment extends Fragment {

    private TextView usernameTextView, familySizeTextView, roomNumberTextView;
    private SharedPreferences sharedPreferences;
    private ImageView infoIcon;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        sharedPreferences = getActivity().getSharedPreferences("UserPrefs", getActivity().MODE_PRIVATE);

        usernameTextView = view.findViewById(R.id.user_id_text);
        familySizeTextView = view.findViewById(R.id.family_size_text);
        roomNumberTextView = view.findViewById(R.id.room_number_text);
        Button editButton = view.findViewById(R.id.edit_household_button);
        infoIcon = view.findViewById(R.id.info_icon);

        loadUserData();

        editButton.setOnClickListener(v -> showEditDialog());
        infoIcon.setOnClickListener(v -> showRules());

        return view;
    }

    private void loadUserData() {
        String email = sharedPreferences.getString("USER_EMAIL", "Guest"); // Get email
        int familySize = sharedPreferences.getInt("FAMILY_SIZE", 0);
        int roomNumber = sharedPreferences.getInt("ROOM_NUMBER", 0);

        usernameTextView.setText("User Email: " + email); // Display email
        familySizeTextView.setText("Family Size: " + familySize);
        roomNumberTextView.setText("Room Number: " + roomNumber);
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
                Toast.makeText(getActivity(), "Household info updated!", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), "Invalid input!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showRules() {
        new AlertDialog.Builder(getActivity())
                .setTitle("App Rules")
                .setMessage("1. Complete tasks...\n2. Earn points...\n3. Compete with friends...")
                .setPositiveButton("OK", null)
                .show();
    }
}



//package com.example.newapp;
//
//import android.content.SharedPreferences;
//import android.os.Bundle;
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
//import androidx.appcompat.app.AlertDialog;
//import androidx.fragment.app.Fragment;
//
//public class ProfileFragment extends Fragment {
//
//    private TextView usernameTextView, familySizeTextView, roomNumberTextView;
//    private SharedPreferences sharedPreferences;
//    private ImageView infoIcon; // Declare infoIcon to fix the rules button issue
//
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_profile, container, false);
//
//        // Initialize SharedPreferences
//        sharedPreferences = getActivity().getSharedPreferences("UserPrefs", getActivity().MODE_PRIVATE);
//
//        usernameTextView = view.findViewById(R.id.user_id_text);
//        familySizeTextView = view.findViewById(R.id.family_size_text);
//        roomNumberTextView = view.findViewById(R.id.room_number_text);
//        Button editButton = view.findViewById(R.id.edit_household_button);
//        infoIcon = view.findViewById(R.id.info_icon); // Fix missing reference for Rules button
//
//        loadUserData();
//
//        // Fix: Set click listener for the "Edit Household Info" button
//        editButton.setOnClickListener(v -> showEditDialog());
//
//        // Fix: Set click listener for the "Rules" button
//        infoIcon.setOnClickListener(v -> showRules());
//
//        return view;
//    }
//
//    private void loadUserData() {
//        String username = sharedPreferences.getString("USERNAME", "Guest");
//        int familySize = sharedPreferences.getInt("FAMILY_SIZE", 0);
//        int roomNumber = sharedPreferences.getInt("ROOM_NUMBER", 0);
//
//        usernameTextView.setText("User ID: " + username);
//        familySizeTextView.setText("Family Size: " + familySize);
//        roomNumberTextView.setText("Room Number: " + roomNumber);
//    }
//
//    private void showEditDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//        builder.setTitle("Update Household Info");
//
//        // Layout for input fields
//        View view = getLayoutInflater().inflate(R.layout.dialog_edit_household, null);
//        final EditText familySizeInput = view.findViewById(R.id.edit_family_size);
//        final EditText roomNumberInput = view.findViewById(R.id.edit_room_number);
//
//        familySizeInput.setText(String.valueOf(sharedPreferences.getInt("FAMILY_SIZE", 0)));
//        roomNumberInput.setText(String.valueOf(sharedPreferences.getInt("ROOM_NUMBER", 0)));
//
//        builder.setView(view);
//
//        builder.setPositiveButton("Save", (dialog, which) -> {
//            try {
//                int familySize = Integer.parseInt(familySizeInput.getText().toString());
//                int roomNumber = Integer.parseInt(roomNumberInput.getText().toString());
//
//                // Save to SharedPreferences
//                SharedPreferences.Editor editor = sharedPreferences.edit();
//                editor.putInt("FAMILY_SIZE", familySize);
//                editor.putInt("ROOM_NUMBER", roomNumber);
//                editor.apply();
//
//                loadUserData(); // Refresh UI
//                Toast.makeText(getActivity(), "Household info updated!", Toast.LENGTH_SHORT).show();
//            } catch (NumberFormatException e) {
//                Toast.makeText(getActivity(), "Invalid input!", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
//        builder.show();
//    }
//
//    private void showRules() {
//        // Fix: Add rules back to the Rules button
//        String rules = "1. Task Completion: Complete tasks on time to earn points.\n" +
//                "2. Points & Rewards: Earn points for completing tasks.\n" +
//                "3. Friends & Challenges: Add friends and compete with them.\n" +
//                "4. Energy Savings Predictions: Compare your appliances for savings.\n" +
//                "5. Notifications: Get task reminders and energy tips.\n" +
//                "6. Fairness & Privacy: Data is private, and fair play is expected.\n" +
//                "7. Account Suspensions: Violations may lead to suspension.";
//
//        new AlertDialog.Builder(getActivity())
//                .setTitle("App Rules")
//                .setMessage(rules)
//                .setPositiveButton("OK", null)
//                .show();
//    }
//}
