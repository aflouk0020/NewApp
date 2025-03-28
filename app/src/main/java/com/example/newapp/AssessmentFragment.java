package com.example.newapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AssessmentFragment extends Fragment {
    private LinearLayout comparisonContainer;
    private int cardCounter = 1;
    private final List<UserStats> allUsers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_assessment, container, false);
        comparisonContainer = view.findViewById(R.id.comparison_container);
        loadComparisonData();
        return view;
    }

    private void loadComparisonData() {
        Context context = requireContext();
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);

        int myPoints = prefs.getInt("POINTS", 0);
        float myUsage = prefs.getFloat("TOTAL_USAGE_KWH", 0);
        allUsers.add(new UserStats("ME", myPoints, myUsage));

        String json = prefs.getString("cached_friends", null);
        if (json == null) {
            renderAllUserCards(); // no friends
            return;
        }

        try {
            JSONArray arr = new JSONArray(json);
            if (arr.length() == 0) {
                renderAllUserCards();
                return;
            }

            final int[] remaining = {arr.length()};

            for (int i = 0; i < arr.length(); i++) {
                String uid = arr.getString(i);
                FirebaseDatabase.getInstance().getReference("userProfiles")
                        .child(uid).child("household")
                        .get().addOnSuccessListener(snapshot -> {
                            if (snapshot.exists()) {
                                String email = snapshot.child("email").getValue(String.class);
                                Long points = snapshot.child("points").getValue(Long.class);

                                if (email != null) {
                                    FirebaseStorage.getInstance()
                                            .getReference("totals/" + email + "/totals.json")
                                            .getBytes(1024 * 1024)
                                            .addOnSuccessListener(bytes -> {
                                                try {
                                                    String jsonStr = new String(bytes, StandardCharsets.UTF_8);
                                                    JSONObject totals = new JSONObject(jsonStr);
                                                    double usage = totals.optDouble("washing_machine_total", 0)
                                                            + totals.optDouble("fridge_total", 0)
                                                            + totals.optDouble("heating_system_total", 0);
                                                    double usageKWh = usage / 1000.0;

                                                    allUsers.add(new UserStats(email, points != null ? points.intValue() : 0, usageKWh));
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                                if (--remaining[0] == 0) {
                                                    renderAllUserCards();
                                                }
                                            });
                                } else {
                                    if (--remaining[0] == 0) {
                                        renderAllUserCards();
                                    }
                                }
                            } else {
                                if (--remaining[0] == 0) {
                                    renderAllUserCards();
                                }
                            }
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
            renderAllUserCards();
        }
    }

    private void renderAllUserCards() {
        if (getContext() == null) return;

        // Find top scorer
        UserStats top = null;
        for (UserStats user : allUsers) {
            if (top == null || user.points > top.points) {
                top = user;
            }
        }

        cardCounter = 1;
        for (UserStats user : allUsers) {
            boolean isTop = user == top;
            addUserCard(user.label, user.points, user.usageKWh, isTop);
        }
    }

    private void addUserCard(String label, int points, double totalUsageKWh, boolean isTop) {
        Context context = requireContext();

        CardView card = new CardView(context);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 12, 0, 12);
        card.setLayoutParams(cardParams);
        card.setCardElevation(8f);
        card.setRadius(16f);
        card.setUseCompatPadding(true);
        card.setCardBackgroundColor(getResources().getColor(android.R.color.white));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(24, 24, 24, 24);

        TextView title = new TextView(context);
        String topIcon = isTop ? " 🏃‍♂️" : "";
        title.setText(cardCounter++ + ". " + label + topIcon);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        container.addView(title);

        LinearLayout detailsLayout = new LinearLayout(context);
        detailsLayout.setOrientation(LinearLayout.VERTICAL);
        detailsLayout.setVisibility(View.GONE);

        TextView pointsText = new TextView(context);
        pointsText.setText("Points: " + points);
        pointsText.setTextSize(16);
        detailsLayout.addView(pointsText);

        TextView usageText = new TextView(context);
        usageText.setText(String.format("Total Usage: %.2f kWh", totalUsageKWh));
        usageText.setTextSize(16);
        detailsLayout.addView(usageText);

        container.addView(detailsLayout);
        card.addView(container);
        comparisonContainer.addView(card);

        title.setOnClickListener(v -> {
            boolean expanded = detailsLayout.getVisibility() == View.VISIBLE;
            detailsLayout.setVisibility(expanded ? View.GONE : View.VISIBLE);
        });
    }

    private static class UserStats {
        String label;
        int points;
        double usageKWh;

        UserStats(String label, int points, double usageKWh) {
            this.label = label;
            this.points = points;
            this.usageKWh = usageKWh;
        }
    }
}
