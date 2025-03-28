package com.example.newapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.storage.FirebaseStorage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class HomeFragment extends Fragment {
    private FriendManager friendManager;
    private CircularProgressIndicator circularProgressIcon;
    private Handler handler;
    private Runnable progressSyncRunnable;

    private EditText etSearchFriend;
    private Button btnSearch;
    private LinearLayout searchResultsContainer;
    private LinearLayout friendsContainer;
    private DatabaseReference usersRef;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        Context ctx = requireContext();
        prefs = ctx.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        friendManager = new FriendManager(ctx);
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        etSearchFriend = view.findViewById(R.id.et_search_friend);
        btnSearch = view.findViewById(R.id.btn_search);
        searchResultsContainer = view.findViewById(R.id.search_results_container);
        friendsContainer = view.findViewById(R.id.friends_container);
        circularProgressIcon = view.findViewById(R.id.circular_progress_icon);

        btnSearch.setOnClickListener(v -> {
            String searchTerm = etSearchFriend.getText().toString().trim();
            friendManager.searchUsersByEmail(searchTerm, new FriendManager.OnUserSearchResultListener() {
                @Override
                public void onSearchCompleted(DataSnapshot snapshot) {
                    searchResultsContainer.removeAllViews();
                    if (!snapshot.exists()) {
                        TextView noResults = new TextView(ctx);
                        noResults.setText("No users found.");
                        searchResultsContainer.addView(noResults);
                        return;
                    }
                    for (DataSnapshot userSnap : snapshot.getChildren()) {
                        String foundUid = userSnap.getKey();
                        String foundEmail = userSnap.child("email").getValue(String.class);

                        Button addFriendButton = new Button(ctx);
                        addFriendButton.setText("Add: " + foundEmail);
                        addFriendButton.setOnClickListener(v1 -> friendManager.sendFriendRequest(foundUid));

                        searchResultsContainer.addView(addFriendButton);
                    }
                }

                @Override
                public void onSearchFailed(Exception e) {
                    Toast.makeText(ctx, "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        circularProgressIcon.setOnClickListener(v -> {
            FragmentManager fragmentManager = getParentFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, new EnergyDetailsFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        startProgressSync();
        loadCachedFriends();
        loadFriendsFromFirebase();

        return view;
    }

    private void loadCachedFriends() {
        try {
            String json = prefs.getString("cached_friends", null);
            if (json == null) return;
            JSONArray arr = new JSONArray(json);
            Set<String> displayed = new HashSet<>();
            for (int i = 0; i < arr.length(); i++) {
                String uid = arr.getString(i);
                if (!displayed.contains(uid)) {
                    displayFriendUI(uid);
                    displayed.add(uid);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadFriendsFromFirebase() {
        friendManager.fetchFriends(new FriendManager.OnFriendsFetchedListener() {
            @Override
            public void onFriendsFetched(DataSnapshot snapshot) {
                if (getContext() == null) return;
                Set<String> uids = new HashSet<>();
                friendsContainer.removeAllViews();

                for (DataSnapshot friendSnap : snapshot.getChildren()) {
                    String uid = friendSnap.getKey();
                    if (uid != null && !uids.contains(uid)) {
                        uids.add(uid);
                        displayFriendUI(uid);
                    }
                }

                // Cache UIDs
                JSONArray arr = new JSONArray();
                for (String uid : uids) arr.put(uid);
                prefs.edit().putString("cached_friends", arr.toString()).apply();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Failed to load friends: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayFriendUI(String friendUid) {
        Context ctx = getContext();
        if (ctx == null) return;

        LinearLayout friendItem = new LinearLayout(ctx);
        friendItem.setOrientation(LinearLayout.VERTICAL);
        friendItem.setGravity(Gravity.CENTER_HORIZONTAL);
        friendItem.setPadding(16, 16, 16, 16);

        CardView circleCard = new CardView(ctx);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(150, 150);
        circleCard.setLayoutParams(cardParams);
        circleCard.setRadius(75);
        circleCard.setCardElevation(8);
        circleCard.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.white));

        FrameLayout circleContainer = new FrameLayout(ctx);
        circleContainer.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        CircularProgressIndicator progressCircle = new CircularProgressIndicator(ctx);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        progressParams.gravity = Gravity.CENTER;
        progressCircle.setLayoutParams(progressParams);
        progressCircle.setIndeterminate(false);
        progressCircle.setTrackThickness(6);
        progressCircle.setTrackColor(ContextCompat.getColor(ctx, R.color.gray_progress));
        progressCircle.setIndicatorColor(ContextCompat.getColor(ctx, R.color.gray_progress));
        progressCircle.setProgressCompat(0, false);

        ImageView statusDot = new ImageView(ctx);
        FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(20, 20);
        dotParams.gravity = Gravity.CENTER;
        statusDot.setLayoutParams(dotParams);
        statusDot.setVisibility(View.GONE);
        statusDot.setElevation(10);

        circleContainer.addView(progressCircle);
        circleContainer.addView(statusDot);
        circleCard.addView(circleContainer);
        friendItem.addView(circleCard);

        TextView emailLabel = new TextView(ctx);
        emailLabel.setText("Loading...");
        emailLabel.setTextSize(14);
        emailLabel.setTypeface(null, Typeface.BOLD);
        emailLabel.setGravity(Gravity.CENTER_HORIZONTAL);
        emailLabel.setPadding(0, 8, 0, 0);
        friendItem.addView(emailLabel);

        friendsContainer.addView(friendItem);

        FirebaseDatabase.getInstance().getReference("userProfiles")
                .child(friendUid).child("household")
                .get().addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String email = snapshot.child("email").getValue(String.class);
                        Long monthlyKWh = snapshot.child("monthly_kWh").getValue(Long.class);
                        emailLabel.setText(email != null ? email : friendUid);

                        if (email != null && monthlyKWh != null) {
                            FirebaseStorage.getInstance().getReference("totals/" + email + "/totals.json")
                                    .getBytes(1024 * 1024)
                                    .addOnSuccessListener(bytes -> {
                                        try {
                                            String json = new String(bytes, StandardCharsets.UTF_8);
                                            JSONObject totals = new JSONObject(json);

                                            double total = totals.optDouble("washing_machine_total", 0)
                                                    + totals.optDouble("fridge_total", 0)
                                                    + totals.optDouble("heating_system_total", 0);

                                            int progress = (int) ((total / monthlyKWh) * 100);
                                            progressCircle.setProgressCompat(progress, true);
                                            int colorRes = (total <= monthlyKWh)
                                                    ? R.color.green_progress : R.color.red_progress;
                                            progressCircle.setIndicatorColor(ContextCompat.getColor(ctx, colorRes));
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    });
                        }
                    }
                });

        FirebaseDatabase.getInstance().getReference("onlineStatus")
                .child(friendUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Boolean isOnline = snapshot.getValue(Boolean.class);
                        if (Boolean.TRUE.equals(isOnline)) {
                            statusDot.setImageResource(R.drawable.green_dot);
                            statusDot.setVisibility(View.VISIBLE);
                        } else {
                            statusDot.setImageResource(R.drawable.red_dot);
                            statusDot.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        statusDot.setVisibility(View.GONE);
                    }
                });

        circleCard.setOnClickListener(v -> {
            usersRef.child(friendUid).get().addOnSuccessListener(userSnap -> {
                String email = userSnap.child("email").getValue(String.class);
                FragmentManager fragmentManager = getParentFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.fragment_container, FriendsDetailsFragment.newInstance(friendUid, email));
                transaction.addToBackStack(null);
                transaction.commit();
            });
        });
    }

    private void startProgressSync() {
        handler = new Handler();
        progressSyncRunnable = new Runnable() {
            @Override
            public void run() {
                Context ctx = getContext();
                if (ctx == null) return;

                SharedPreferences prefs = ctx.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                int progress = prefs.getInt("PROGRESS_CIRCLE", 0);
                String color = prefs.getString("PROGRESS_COLOR", "green");

                circularProgressIcon.setProgressCompat(progress, true);
                int colorRes = color.equals("green") ? R.color.green_progress : R.color.red_progress;
                circularProgressIcon.setIndicatorColor(ContextCompat.getColor(ctx, colorRes));

                handler.postDelayed(this, 100);
            }
        };
        handler.post(progressSyncRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (handler != null && progressSyncRunnable != null) {
            handler.removeCallbacks(progressSyncRunnable);
        }
    }
}