package com.example.newapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.security.MessageDigest;
import java.util.Base64;

public class ProfileFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Retrieve User ID from SharedPreferences
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserPrefs", getActivity().MODE_PRIVATE);
        String userId = sharedPreferences.getString("USER_ID", "N/A");

        // Convert UUID to a short, shareable format
        String shortUserId = generateShortId(userId);

        // Display Shortened User ID in TextView
        TextView userIdTextView = view.findViewById(R.id.user_id_text);
        userIdTextView.setText("User ID: " + shortUserId);

        return view;
    }

    // Function to generate a short user ID from UUID
    private String generateShortId(String uuid) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(uuid.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 8); // Shorten to 8 chars
        } catch (Exception e) {
            return "ERROR"; // Fallback in case of an issue
        }
    }
}


//package com.example.newapp;
//
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//
//import java.security.MessageDigest;
//import java.util.Base64;
//
//public class ProfileFragment extends Fragment {
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_profile, container, false);
//
//        // Retrieve User ID
//        Bundle bundle = getArguments();
//        String userId = (bundle != null) ? bundle.getString("USER_ID", "N/A") : "N/A";
//
//        // Convert UUID to a short, shareable format
//        String shortUserId = generateShortId(userId);
//
//        // Display Shortened User ID in TextView
//        TextView userIdTextView = view.findViewById(R.id.user_id_text);
//        userIdTextView.setText("User ID: " + shortUserId);
//
//        return view;
//    }
//
//    // Function to generate a short user ID from UUID
//    private String generateShortId(String uuid) {
//        try {
//            MessageDigest digest = MessageDigest.getInstance("SHA-256");
//            byte[] hash = digest.digest(uuid.getBytes());
//            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 8); // Shorten to 8 chars
//        } catch (Exception e) {
//            return "ERROR"; // Fallback in case of an issue
//        }
//    }
//}
//
