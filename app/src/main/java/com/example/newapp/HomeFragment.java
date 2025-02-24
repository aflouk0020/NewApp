package com.example.newapp;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    private TextView washingMachineTextView;
    private TextView fridgeTextView;
    private TextView heatingSystemTextView;

    // Starting values for each device in Watts
    private double washingMachineReading = 50.0;
    private double fridgeReading = 150.0;
    private double heatingSystemReading = 200.0;

    private Handler handler = new Handler();
    private Runnable updateRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        washingMachineTextView = view.findViewById(R.id.text_washing_machine_reading);
        fridgeTextView = view.findViewById(R.id.text_fridge_reading);
        heatingSystemTextView = view.findViewById(R.id.text_heating_system_reading);

        startSimulation();

        return view;
    }

    private void startSimulation() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                // Update Washing Machine reading
                double wmVariation = (Math.random() * 10) - 5; // variation between -5 and +5
                washingMachineReading = Math.max(0, washingMachineReading + wmVariation);
                washingMachineTextView.setText("Washing Machine: " +
                        String.format("%.2f", washingMachineReading) + " Watts");

                // Update Fridge reading
                double fridgeVariation = (Math.random() * 10) - 5; // variation between -5 and +5
                fridgeReading = Math.max(0, fridgeReading + fridgeVariation);
                fridgeTextView.setText("Fridge: " +
                        String.format("%.2f", fridgeReading) + " Watts");

                // Update Heating System reading
                double heatVariation = (Math.random() * 10) - 5; // variation between -5 and +5
                heatingSystemReading = Math.max(0, heatingSystemReading + heatVariation);
                heatingSystemTextView.setText("Heating System: " +
                        String.format("%.2f", heatingSystemReading) + " Watts");

                // Schedule next update in 1 second (1000ms)
                handler.postDelayed(this, 1000);
            }
        };

        // Start the simulation after a 1-second delay
        handler.postDelayed(updateRunnable, 1000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove callbacks to prevent memory leaks
        handler.removeCallbacks(updateRunnable);
    }
}
