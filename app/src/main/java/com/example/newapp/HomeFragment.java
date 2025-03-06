package com.example.newapp;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class HomeFragment extends Fragment {

    private ProgressBar circularProgressIcon;
    private Handler handler;
    private Runnable progressSyncRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        circularProgressIcon = view.findViewById(R.id.circular_progress_icon);

        // Make the circular progress icon clickable
        circularProgressIcon.setOnClickListener(v -> {
            // Navigate to EnergyDetailsFragment
            FragmentManager fragmentManager = getParentFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, new EnergyDetailsFragment());
            transaction.addToBackStack(null); // Allow back navigation
            transaction.commit();
        });

        // Start syncing the circular progress with EnergyDetailsFragment's progress
        startProgressSync();

        return view;
    }

    private void startProgressSync() {
        handler = new Handler();
        progressSyncRunnable = new Runnable() {
            @Override
            public void run() {
                // Check if EnergyDetailsFragment is active and get its progress
                FragmentManager fragmentManager = getParentFragmentManager();
                Fragment fragment = fragmentManager.findFragmentById(R.id.fragment_container);
                if (fragment instanceof EnergyDetailsFragment) {
                    int progress = ((EnergyDetailsFragment) fragment).getProgress();
                    circularProgressIcon.setProgress(progress);
                }

                // Sync every 100ms to keep the circular progress in sync
                handler.postDelayed(this, 100);
            }
        };
        handler.post(progressSyncRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up the handler to prevent memory leaks
        if (handler != null && progressSyncRunnable != null) {
            handler.removeCallbacks(progressSyncRunnable);
        }
    }
}