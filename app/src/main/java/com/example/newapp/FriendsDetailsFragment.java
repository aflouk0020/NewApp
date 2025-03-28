package com.example.newapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;

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

    private ProgressBar frndEnergyProgress, frndOveruseProgress;
    private TextView emailText, familySizeText, roomNumberText, energyUsageText,
            monthlyKWhText, pointsText, totalUsageText, totalFridgeText, totalHeatingText,
            totalWashingText, remainingEnergyText, estimatedDailyText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_friends_details, container, false);

        // UI Bindings
        emailText = view.findViewById(R.id.friend_email);
        familySizeText = view.findViewById(R.id.friend_family_size);
        roomNumberText = view.findViewById(R.id.friend_room_number);
        energyUsageText = view.findViewById(R.id.friend_energy_usage);
        monthlyKWhText = view.findViewById(R.id.friend_monthly_kwh);
        pointsText = view.findViewById(R.id.friend_points);
        totalUsageText = view.findViewById(R.id.text_total_usage);
        totalFridgeText = view.findViewById(R.id.text_total_fridge);
        totalHeatingText = view.findViewById(R.id.text_total_heating);
        totalWashingText = view.findViewById(R.id.text_total_washing_machine);
        remainingEnergyText = view.findViewById(R.id.friend_remaining_energy);
        estimatedDailyText = view.findViewById(R.id.friend_estimated_daily);
        frndEnergyProgress = view.findViewById(R.id.frnd_energy_progress_bar);
        frndOveruseProgress = view.findViewById(R.id.frnd_overuse_progress_bar);

        Button unfriendButton = view.findViewById(R.id.btn_unfriend);
        TextView cardTitle = view.findViewById(R.id.friend_card_title);
        View detailsLayout = view.findViewById(R.id.friend_card_details);
        TextView totalsTitle = view.findViewById(R.id.friend_totals_title);
        View totalsLayout = view.findViewById(R.id.friend_totals_content);

        cardTitle.setOnClickListener(v -> {
            boolean expanded = detailsLayout.getVisibility() == View.VISIBLE;
            detailsLayout.setVisibility(expanded ? View.GONE : View.VISIBLE);
            cardTitle.setText(expanded ? "Friend Energy Details ▼" : "Friend Energy Details ▲");
        });

        totalsTitle.setOnClickListener(v -> {
            boolean expanded = totalsLayout.getVisibility() == View.VISIBLE;
            totalsLayout.setVisibility(expanded ? View.GONE : View.VISIBLE);
            totalsTitle.setText(expanded ? "Totals ▼" : "Totals ▲");
        });

        if (getArguments() != null) {
            String friendUid = getArguments().getString(ARG_UID);
            String friendEmail = getArguments().getString(ARG_EMAIL);

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

                    if (monthlyKWh != null && email != null) {
                        fetchFriendTotals(email, monthlyKWh);
                    }

                    unfriendButton.setOnClickListener(v -> {
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Confirm Unfriend")
                                .setMessage("Are you sure you want to unfriend this user?")
                                .setPositiveButton("Yes", (dialog, which) -> {
                                    String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    DatabaseReference friendsRef = FirebaseDatabase.getInstance().getReference("friends");

                                    friendsRef.child(myUid).child(friendUid).removeValue();
                                    friendsRef.child(friendUid).child(myUid).removeValue();

                                    Toast.makeText(requireContext(), "Unfriended successfully", Toast.LENGTH_SHORT).show();

                                    FragmentTransaction ft = getParentFragmentManager().beginTransaction();
                                    ft.replace(R.id.fragment_container, new HomeFragment());
                                    ft.commit();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    });
                }
            }).addOnFailureListener(e -> {
                emailText.setText("Failed to load profile.");
                Log.e("FriendProfile", "Error loading friend profile", e);
            });
        }

        return view;
    }

    private void fetchFriendTotals(String email, long estimatedKWh) {
        String path = "totals/" + email + "/totals.json";
        StorageReference totalsRef = FirebaseStorage.getInstance()
                .getReference()
                .child(path);

        totalsRef.getBytes(1024 * 1024)
                .addOnSuccessListener(bytes -> {
                    try {
                        String json = new String(bytes, StandardCharsets.UTF_8);
                        JSONObject jsonObject = new JSONObject(json);

                        double washing = jsonObject.optDouble("washing_machine_total", 0);
                        double fridge = jsonObject.optDouble("fridge_total", 0);
                        double heating = jsonObject.optDouble("heating_system_total", 0);

                        double totalUsage = (washing + fridge + heating) / 1000.0;
                        float estimated = (float) estimatedKWh;

                        totalUsageText.setText("Total Usage: " + String.format("%.2f", totalUsage) + " kWh");
                        totalWashingText.setText("Washing Machine: " + String.format("%.2f", washing) + " Watts");
                        totalFridgeText.setText("Fridge: " + String.format("%.2f", fridge) + " Watts");
                        totalHeatingText.setText("Heating: " + String.format("%.2f", heating) + " Watts");

                        float remaining = estimated - (float) totalUsage;
                        remainingEnergyText.setText("Remaining: " + String.format("%.2f", remaining) + " kWh");

                        int daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
                        float estimatedDaily = estimated / daysInMonth;
                        estimatedDailyText.setText("Estimated Daily: " + String.format("%.2f", estimatedDaily) + " kWh");

                        if (totalUsage <= estimated) {
                            int progress = (int) ((totalUsage / estimated) * 100);
                            if (progress == 0 && totalUsage > 0) progress = 1;

                            frndEnergyProgress.setProgress(progress);
                            frndEnergyProgress.setVisibility(View.VISIBLE);
                            frndOveruseProgress.setVisibility(View.GONE);
                        } else {
                            double overUsed = totalUsage - estimated;
                            int redProgress = (int) ((overUsed / estimated) * 100);
                            if (redProgress == 0 && overUsed > 0) redProgress = 1;

                            frndOveruseProgress.setProgress(redProgress);
                            frndOveruseProgress.setVisibility(View.VISIBLE);
                            frndEnergyProgress.setVisibility(View.GONE);
                        }

                    } catch (Exception e) {
                        Log.e("StorageTotals", "Parse error", e);
                        Toast.makeText(requireContext(), "Error parsing totals", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("StorageTotals", "Failed to load: " + e.getMessage());
                    Toast.makeText(requireContext(), "No totals available for this friend", Toast.LENGTH_SHORT).show();
                });
    }
}
