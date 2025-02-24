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
import android.database.Cursor;

public class ProfileFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Get the username from SharedPreferences
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserPrefs", getActivity().MODE_PRIVATE);
        String username = sharedPreferences.getString("USERNAME", null);  // Ensure "USERNAME" is saved during login

        if (username != null) {
            // Fetch user details from the database
            DBAccess dbAccess = new DBAccess(getActivity());
            Cursor cursor = dbAccess.getUserDetails(username);

            if (cursor != null && cursor.moveToFirst()) {
                // Extract data from cursor
                String storedUsername = cursor.getString(cursor.getColumnIndex("username"));
                int familySize = cursor.getInt(cursor.getColumnIndex("family_size"));
                int roomNumber = cursor.getInt(cursor.getColumnIndex("room_number"));

                // Get the TextViews from the layout
                TextView usernameTextView = view.findViewById(R.id.user_id_text);
                usernameTextView.setText("User ID: " + storedUsername);

                TextView familySizeTextView = view.findViewById(R.id.family_size_text);
                familySizeTextView.setText("Family Size: " + familySize);

                TextView roomNumberTextView = view.findViewById(R.id.room_number_text);
                roomNumberTextView.setText("Room Number: " + roomNumber);
            } else {
                // Handle the case where user details are not found
                Toast.makeText(getActivity(), "User details not found", Toast.LENGTH_SHORT).show();
            }
            cursor.close();
        } else {
            // Handle the case where username is not found in SharedPreferences
            Toast.makeText(getActivity(), "No username found", Toast.LENGTH_SHORT).show();
        }

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


//package com.example.newapp;
//
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//import androidx.appcompat.app.AlertDialog;
//import androidx.fragment.app.Fragment;
//import android.database.Cursor;
//
//public class ProfileFragment extends Fragment {
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_profile, container, false);
//
//        // Get the username from SharedPreferences
//        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserPrefs", getActivity().MODE_PRIVATE);
//        String username = sharedPreferences.getString("USERNAME", null);  // Make sure "USERNAME" is set during login
//
//        if (username != null) {
//            // Fetch user details from the database
//            DBAccess dbAccess = new DBAccess(getActivity());
//            Cursor cursor = dbAccess.getUserDetails(username);
//
//            if (cursor != null && cursor.moveToFirst()) {
//                // Extract data from cursor
//                String storedUsername = cursor.getString(cursor.getColumnIndex("username"));
//                int familySize = cursor.getInt(cursor.getColumnIndex("family_size"));
//                int roomNumber = cursor.getInt(cursor.getColumnIndex("room_number"));
//
//                // Get the TextViews from the layout
//                TextView usernameTextView = view.findViewById(R.id.user_id_text);
//                usernameTextView.setText("User ID: " + storedUsername);
//
//                TextView familySizeTextView = view.findViewById(R.id.family_size_text);
//                familySizeTextView.setText("Family Size: " + familySize);
//
//                TextView roomNumberTextView = view.findViewById(R.id.room_number_text);
//                roomNumberTextView.setText("Room Number: " + roomNumber);
//            } else {
//                // Handle the case where user details are not found
//                Toast.makeText(getActivity(), "User details not found", Toast.LENGTH_SHORT).show();
//            }
//            cursor.close();
//        } else {
//            // Handle the case where username is not found in SharedPreferences
//            Toast.makeText(getActivity(), "No username found", Toast.LENGTH_SHORT).show();
//        }
//
//        // Set up the info icon to show rules
//        ImageView infoIcon = view.findViewById(R.id.info_icon);
//        infoIcon.setOnClickListener(v -> showRules());
//
//        return view;
//    }
//
//    private void showRules() {
//        // Display the rules in a dialog when the info icon is clicked
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


//  22

//package com.example.newapp;
//
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.TextView;
//import androidx.appcompat.app.AlertDialog;
//import androidx.fragment.app.Fragment;
//import android.database.Cursor;
//
//public class ProfileFragment extends Fragment {
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_profile, container, false);
//
//        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserPrefs", getActivity().MODE_PRIVATE);
//        String username = sharedPreferences.getString("USERNAME", null);  // Make sure "USERNAME" is set during login
//
//        if (username != null) {
//            DBAccess dbAccess = new DBAccess(getActivity());
//            Cursor cursor = dbAccess.getUserDetails(username);
//
//            if (cursor != null && cursor.moveToFirst()) {
//                String storedUsername = cursor.getString(cursor.getColumnIndex("username"));
//                int familySize = cursor.getInt(cursor.getColumnIndex("family_size"));
//                int roomNumber = cursor.getInt(cursor.getColumnIndex("room_number"));
//
//                // Set data in TextViews
//                TextView usernameTextView = view.findViewById(R.id.user_id_text);
//                usernameTextView.setText("Username: " + storedUsername);
//
//                TextView familySizeTextView = view.findViewById(R.id.family_size_text);
//                familySizeTextView.setText("Family Size: " + familySize);
//
//                TextView roomNumberTextView = view.findViewById(R.id.room_number_text);
//                roomNumberTextView.setText("Room Number: " + roomNumber);
//            }
//            cursor.close();
//        }
//
//        // Set up info icon click listener to call showRules()
//        ImageView infoIcon = view.findViewById(R.id.info_icon);
//        infoIcon.setOnClickListener(v -> showRules());
//
//        return view;
//    }
//
//    private void showRules() {
//        // Display the rules in a dialog when the info icon is clicked
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
//
//
//




//11

//
//package com.example.newapp;
//
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.TextView;
//import androidx.appcompat.app.AlertDialog;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import android.database.Cursor;
//
//public class ProfileFragment extends Fragment {
//
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_profile, container, false);
//
//        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserPrefs", getActivity().MODE_PRIVATE);
//        String username = sharedPreferences.getString("USERNAME", null);
//
//        if (username != null) {
//            DBAccess dbAccess = new DBAccess(getActivity());
//            Cursor cursor = dbAccess.getUserDetails(username);
//
//            if (cursor.moveToFirst()) {
//                String storedUsername = cursor.getString(cursor.getColumnIndex("username"));
//                String storedPassword = cursor.getString(cursor.getColumnIndex("password"));
//                int familySize = cursor.getInt(cursor.getColumnIndex("family_size"));
//                int roomNumber = cursor.getInt(cursor.getColumnIndex("room_number"));
//
//                // Set data in TextViews
//                TextView usernameTextView = view.findViewById(R.id.user_id_text);
//                usernameTextView.setText("Username: " + storedUsername);
//
//                TextView familySizeTextView = view.findViewById(R.id.family_size_text);
//                familySizeTextView.setText("Family Size: " + familySize);
//
//                TextView roomNumberTextView = view.findViewById(R.id.room_number_text);
//                roomNumberTextView.setText("Room Number: " + roomNumber);
//            }
//            cursor.close();
//        }
//
//        // Set up info icon click listener to call showRules()
//        ImageView infoIcon = view.findViewById(R.id.info_icon);
//        infoIcon.setOnClickListener(v -> showRules());
//
//        return view;
//    }
//
//    private void showRules() {
//        // Display the rules in a dialog when the info icon is clicked
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
//
//
