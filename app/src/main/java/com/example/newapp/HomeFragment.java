package com.example.newapp;
import android.widget.TextView;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class HomeFragment extends Fragment {
    private FriendManager friendManager;
    private ProgressBar circularProgressIcon;
    private Handler handler;
    private Runnable progressSyncRunnable;

    // Class-level variables for search functionality
    private EditText etSearchFriend;
    private Button btnSearch;
    private LinearLayout searchResultsContainer;
    private DatabaseReference usersRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize friendManager
        friendManager = new FriendManager(requireContext());

        // Initialize class-level search views (do not declare new local variables)
        etSearchFriend = view.findViewById(R.id.et_search_friend);
        btnSearch = view.findViewById(R.id.btn_search);
        searchResultsContainer = view.findViewById(R.id.search_results_container);
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Set up the search button listener
        btnSearch.setOnClickListener(v -> {
            String searchTerm = etSearchFriend.getText().toString().trim();
            friendManager.searchUsersByEmail(searchTerm, new FriendManager.OnUserSearchResultListener() {
                @Override
                public void onSearchCompleted(DataSnapshot snapshot) {
                    searchResultsContainer.removeAllViews();

                    if (!snapshot.exists()) {
                        TextView noResults = new TextView(getContext());
                        noResults.setText("No users found.");
                        searchResultsContainer.addView(noResults);
                        return;
                    }

                    for (DataSnapshot userSnap : snapshot.getChildren()) {
                        String foundUid = userSnap.getKey();
                        String foundEmail = userSnap.child("email").getValue(String.class);

                        Button addFriendButton = new Button(getContext());
                        addFriendButton.setText("Add: " + foundEmail);
                        addFriendButton.setOnClickListener(v1 ->
                                friendManager.sendFriendRequest(foundUid)
                        );

                        searchResultsContainer.addView(addFriendButton);
                    }
                }


                @Override
                public void onSearchFailed(Exception e) {
                    Toast.makeText(getContext(), "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Initialize the circular progress icon and set its click listener.
        circularProgressIcon = view.findViewById(R.id.circular_progress_icon);
        circularProgressIcon.setOnClickListener(v -> {
            // Navigate to EnergyDetailsFragment
            FragmentManager fragmentManager = getParentFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, new EnergyDetailsFragment());
            transaction.addToBackStack(null); // Allow back navigation
            transaction.commit();
        });

        // Start syncing the circular progress with EnergyDetailsFragment's progress.
        startProgressSync();

        return view;
    }

    private void startProgressSync() {
        handler = new Handler();
        progressSyncRunnable = new Runnable() {
            @Override
            public void run() {
                // Check if EnergyDetailsFragment is active and get its progress.
                FragmentManager fragmentManager = getParentFragmentManager();
                Fragment fragment = fragmentManager.findFragmentById(R.id.fragment_container);
                if (fragment instanceof EnergyDetailsFragment) {
                    int progress = ((EnergyDetailsFragment) fragment).getProgress();
                    circularProgressIcon.setProgress(progress);
                }
                // Sync every 100ms.
                handler.postDelayed(this, 100);
            }
        };
        handler.post(progressSyncRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up the handler to prevent memory leaks.
        if (handler != null && progressSyncRunnable != null) {
            handler.removeCallbacks(progressSyncRunnable);
        }
    }
}
