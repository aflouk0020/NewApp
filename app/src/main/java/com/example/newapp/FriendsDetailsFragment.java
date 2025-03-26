//package com.example.newapp;
//
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//
//public class FriendsDetailsFragment extends Fragment {
//    private static final String ARG_UID = "uid";
//    private static final String ARG_EMAIL = "email";
//
//    public static FriendsDetailsFragment newInstance(String uid, String email) {
//        FriendsDetailsFragment fragment = new FriendsDetailsFragment();
//        Bundle args = new Bundle();
//        args.putString(ARG_UID, uid);
//        args.putString(ARG_EMAIL, email);
//        fragment.setArguments(args);
//        return fragment;
//    }
//
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater,
//                             @Nullable ViewGroup container,
//                             @Nullable Bundle savedInstanceState) {
//
//        View view = inflater.inflate(R.layout.fragment_friends_details, container, false);
//
//        // View bindings
//        TextView emailText = view.findViewById(R.id.friend_email);
//        TextView familySizeText = view.findViewById(R.id.friend_family_size);
//        TextView roomNumberText = view.findViewById(R.id.friend_room_number);
//        TextView energyUsageText = view.findViewById(R.id.friend_energy_usage);
//        TextView monthlyKWhText = view.findViewById(R.id.friend_monthly_kwh);
//        TextView pointsText = view.findViewById(R.id.friend_points);
//        Button unfriendButton = view.findViewById(R.id.btn_unfriend);
//
//        TextView cardTitle = view.findViewById(R.id.friend_card_title);
//        View detailsLayout = view.findViewById(R.id.friend_card_details);
//
//        // Expand/Collapse
//        cardTitle.setOnClickListener(v -> {
//            if (detailsLayout.getVisibility() == View.VISIBLE) {
//                detailsLayout.animate().alpha(0f).setDuration(150).withEndAction(() -> {
//                    detailsLayout.setVisibility(View.GONE);
//                    detailsLayout.animate().alpha(1f).setDuration(150).start();
//                }).start();
//                cardTitle.setText("Friend Energy Details ▼");
//            } else {
//                detailsLayout.animate().alpha(0f).setDuration(150).withEndAction(() -> {
//                    detailsLayout.setVisibility(View.VISIBLE);
//                    detailsLayout.animate().alpha(1f).setDuration(150).start();
//                }).start();
//                cardTitle.setText("Friend Energy Details ▲");
//            }
//        });
//
//        // Load data
//        if (getArguments() != null) {
//            String friendUid = getArguments().getString(ARG_UID);
//
//            DatabaseReference profileRef = FirebaseDatabase.getInstance()
//                    .getReference("userProfiles")
//                    .child(friendUid)
//                    .child("household");
//
//            profileRef.get().addOnSuccessListener(snapshot -> {
//                if (snapshot.exists()) {
//                    String email = snapshot.child("email").getValue(String.class);
//                    Long familySize = snapshot.child("family_size").getValue(Long.class);
//                    Long roomNumber = snapshot.child("room_number").getValue(Long.class);
//                    String energyUsage = snapshot.child("energy_usage").getValue(String.class);
//                    Long monthlyKWh = snapshot.child("monthly_kWh").getValue(Long.class);
//                    Long points = snapshot.child("points").getValue(Long.class);
//
//                    emailText.setText("Email: " + email);
//                    familySizeText.setText("Family Size: " + familySize);
//                    roomNumberText.setText("Room Number: " + roomNumber);
//                    energyUsageText.setText("Energy Usage: " + energyUsage);
//                    monthlyKWhText.setText("Monthly kWh: " + monthlyKWh);
//                    pointsText.setText("Points: " + points);
//                } else {
//                    emailText.setText("No profile data found.");
//                }
//            }).addOnFailureListener(e -> {
//                emailText.setText("Failed to load friend data.");
//                e.printStackTrace();
//            });
//
//            // ✅ Unfriend logic
//            unfriendButton.setOnClickListener(v -> {
//                String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
//                DatabaseReference friendsRef = FirebaseDatabase.getInstance().getReference("friends");
//
//                friendsRef.child(myUid).child(friendUid).removeValue();
//                friendsRef.child(friendUid).child(myUid).removeValue();
//
//                Toast.makeText(requireContext(), "Unfriended successfully.", Toast.LENGTH_SHORT).show();
//            });
//        }
//
//        return view;
//    }
//}
package com.example.newapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FriendsDetailsFragment extends Fragment {
    private static final String ARG_UID = "uid";
    private static final String ARG_EMAIL = "email";

    public static FriendsDetailsFragment newInstance(String uid, String email) {
        FriendsDetailsFragment fragment = new FriendsDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_UID, uid);
        args.putString(ARG_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_friends_details, container, false);

        TextView emailText = view.findViewById(R.id.friend_email);
        TextView familySizeText = view.findViewById(R.id.friend_family_size);
        TextView roomNumberText = view.findViewById(R.id.friend_room_number);
        TextView energyUsageText = view.findViewById(R.id.friend_energy_usage);
        TextView monthlyKWhText = view.findViewById(R.id.friend_monthly_kwh);
        TextView pointsText = view.findViewById(R.id.friend_points);
        Button unfriendButton = view.findViewById(R.id.btn_unfriend);

        TextView cardTitle = view.findViewById(R.id.friend_card_title);
        View detailsLayout = view.findViewById(R.id.friend_card_details);

        // Expand/collapse logic
        cardTitle.setOnClickListener(v -> {
            if (detailsLayout.getVisibility() == View.VISIBLE) {
                detailsLayout.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                    detailsLayout.setVisibility(View.GONE);
                    detailsLayout.animate().alpha(1f).setDuration(150).start();
                }).start();
                cardTitle.setText("Friend Energy Details ▼");
            } else {
                detailsLayout.animate().alpha(0f).setDuration(150).withEndAction(() -> {
                    detailsLayout.setVisibility(View.VISIBLE);
                    detailsLayout.animate().alpha(1f).setDuration(150).start();
                }).start();
                cardTitle.setText("Friend Energy Details ▲");
            }
        });

        // Load and display profile data
        if (getArguments() != null) {
            String friendUid = getArguments().getString(ARG_UID);

            DatabaseReference profileRef = FirebaseDatabase.getInstance()
                    .getReference("userProfiles")
                    .child(friendUid)
                    .child("household");

            profileRef.get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    String email = snapshot.child("email").getValue(String.class);
                    Long familySize = snapshot.child("family_size").getValue(Long.class);
                    Long roomNumber = snapshot.child("room_number").getValue(Long.class);
                    String energyUsage = snapshot.child("energy_usage").getValue(String.class);
                    Long monthlyKWh = snapshot.child("monthly_kWh").getValue(Long.class);
                    Long points = snapshot.child("points").getValue(Long.class);

                    emailText.setText("Email: " + email);
                    familySizeText.setText("Family Size: " + familySize);
                    roomNumberText.setText("Room Number: " + roomNumber);
                    energyUsageText.setText("Energy Usage: " + energyUsage);
                    monthlyKWhText.setText("Monthly kWh: " + monthlyKWh);
                    pointsText.setText("Points: " + points);
                } else {
                    emailText.setText("No profile data found.");
                }
            }).addOnFailureListener(e -> {
                emailText.setText("Failed to load friend data.");
                e.printStackTrace();
            });

            // ✅ Confirm before unfriending
            unfriendButton.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Unfriend Confirmation")
                        .setMessage("Are you sure you want to unfriend this person?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            DatabaseReference friendsRef = FirebaseDatabase.getInstance().getReference("friends");

                            friendsRef.child(myUid).child(friendUid).removeValue();
                            friendsRef.child(friendUid).child(myUid).removeValue();

                            Toast.makeText(requireContext(), "Unfriended successfully.", Toast.LENGTH_SHORT).show();

                            // 👇 Navigate back to HomeFragment
                            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                            transaction.replace(R.id.fragment_container, new HomeFragment());
                            transaction.commit();
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .show();
            });
        }

        return view;
    }
}
