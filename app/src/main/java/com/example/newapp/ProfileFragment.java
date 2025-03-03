package com.example.newapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Get the username from SharedPreferences
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserPrefs", getActivity().MODE_PRIVATE);
        String username = sharedPreferences.getString("USERNAME", "Guest"); // Default to "Guest" if not found
        int familySize = sharedPreferences.getInt("FAMILY_SIZE", 0); // Default 0 if not set
        int roomNumber = sharedPreferences.getInt("ROOM_NUMBER", 0); // Default 0 if not set

        // Display the user details
        TextView usernameTextView = view.findViewById(R.id.user_id_text);
        usernameTextView.setText("User ID: " + username);

        TextView familySizeTextView = view.findViewById(R.id.family_size_text);
        familySizeTextView.setText("Family Size: " + familySize);

        TextView roomNumberTextView = view.findViewById(R.id.room_number_text);
        roomNumberTextView.setText("Room Number: " + roomNumber);

        // Set up the info icon to show rules
        ImageView infoIcon = view.findViewById(R.id.info_icon);
        infoIcon.setOnClickListener(v -> showRules());

        return view;
    }

    private void showRules() {
        // Display the rules in a dialog when the info icon is clicked
        String rules = "1. Task Completion: Complete tasks on time to earn points.\n" +
                "2. Points & Rewards: Earn points for completing tasks.\n" +
                "3. Friends & Challenges: Add friends and compete with them.\n" +
                "4. Energy Savings Predictions: Compare your appliances for savings.\n" +
                "5. Notifications: Get task reminders and energy tips.\n" +
                "6. Fairness & Privacy: Data is private, and fair play is expected.\n" +
                "7. Account Suspensions: Violations may lead to suspension.";

        new AlertDialog.Builder(getActivity())
                .setTitle("App Rules")
                .setMessage(rules)
                .setPositiveButton("OK", null)
                .show();
    }
}
