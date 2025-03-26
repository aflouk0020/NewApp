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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        friendManager = new FriendManager(requireContext());
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
                    Context ctx = getContext();
                    if (ctx == null) return;

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
                    Context ctx = getContext();
                    if (ctx != null)
                        Toast.makeText(ctx, "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        loadFriends();

        circularProgressIcon.setOnClickListener(v -> {
            FragmentManager fragmentManager = getParentFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, new EnergyDetailsFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        startProgressSync();

        return view;
    }

    private void loadFriends() {
        friendManager.fetchFriends(new FriendManager.OnFriendsFetchedListener() {
            @Override
            public void onFriendsFetched(DataSnapshot snapshot) {
                friendsContainer.removeAllViews();
                Context ctx = getContext();
                if (ctx == null) return;

                if (!snapshot.exists()) {
                    TextView noFriends = new TextView(ctx);
                    noFriends.setText("No friends yet.");
                    friendsContainer.addView(noFriends);
                    return;
                }

                for (DataSnapshot friendSnap : snapshot.getChildren()) {
                    String friendUid = friendSnap.getKey();
                    Context innerCtx = getContext();
                    if (innerCtx == null) continue;

                    LinearLayout friendItem = new LinearLayout(innerCtx);
                    friendItem.setOrientation(LinearLayout.VERTICAL);
                    friendItem.setGravity(Gravity.CENTER_HORIZONTAL);
                    friendItem.setPadding(16, 16, 16, 16);

                    CardView circleCard = new CardView(innerCtx);
                    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(150, 150);
                    circleCard.setLayoutParams(cardParams);
                    circleCard.setRadius(75);
                    circleCard.setCardElevation(8);
                    circleCard.setCardBackgroundColor(ContextCompat.getColor(innerCtx, R.color.white));

                    FrameLayout circleContainer = new FrameLayout(innerCtx);
                    circleContainer.setLayoutParams(new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                    ));

                    View statusDot = new View(innerCtx);
                    FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(24, 24);
                    dotParams.gravity = Gravity.CENTER;
                    statusDot.setLayoutParams(dotParams);
                    statusDot.setBackground(ContextCompat.getDrawable(innerCtx, R.drawable.circle_background));

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




                    circleContainer.addView(statusDot);
                    circleCard.addView(circleContainer);
                    friendItem.addView(circleCard);

                    TextView emailLabel = new TextView(innerCtx);
                    emailLabel.setText("Loading...");
                    emailLabel.setTextSize(14);
                    emailLabel.setTypeface(null, Typeface.BOLD);
                    emailLabel.setGravity(Gravity.CENTER_HORIZONTAL);
                    emailLabel.setPadding(0, 8, 0, 0);
                    friendItem.addView(emailLabel);

                    friendsContainer.addView(friendItem);

                    usersRef.child(friendUid).get().addOnSuccessListener(userSnap -> {
                        String email = userSnap.child("email").getValue(String.class);
                        FirebaseDatabase.getInstance().getReference("onlineStatus")
                                .child(friendUid)
                                .get().addOnSuccessListener(statusSnap -> {
                                    boolean isOnline = Boolean.TRUE.equals(statusSnap.getValue(Boolean.class));
                                    int color = ContextCompat.getColor(innerCtx,
                                            isOnline ? R.color.green_progress : R.color.gray_progress);
                                    statusDot.getBackground().setTint(color);
                                    emailLabel.setText(email != null ? email : friendUid);
                                });
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                Context ctx = getContext();
                if (ctx != null)
                    Toast.makeText(ctx, "Failed to load friends: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
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
                circularProgressIcon.setIndicatorColor(
                        getResources().getColor(color.equals("green") ? R.color.green_progress : R.color.red_progress)
                );

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
