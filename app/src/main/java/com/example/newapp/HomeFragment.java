package com.example.newapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomeFragment extends Fragment {

    private ProgressBar energyProgressBar;
    private TextView remainingEnergyText, washingMachineTextView, fridgeTextView, heatingSystemTextView;
    private DatabaseReference databaseReference;
    private float estimatedKWh, remainingKWh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        energyProgressBar = view.findViewById(R.id.energy_progress_bar);
        remainingEnergyText = view.findViewById(R.id.remaining_energy_text);
        washingMachineTextView = view.findViewById(R.id.text_washing_machine_reading);
        fridgeTextView = view.findViewById(R.id.text_fridge_reading);
        heatingSystemTextView = view.findViewById(R.id.text_heating_system_reading);

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", requireActivity().MODE_PRIVATE);
        estimatedKWh = sharedPreferences.getFloat("ESTIMATED_KWH", 0);
        remainingKWh = estimatedKWh;

        energyProgressBar.setMax((int) estimatedKWh);
        updateProgressBar(remainingKWh);

        databaseReference = FirebaseDatabase.getInstance("https://myapp2-75686-default-rtdb.firebaseio.com/")
                .getReference("sensor_data");

        fetchLatestDataFromFirebase();

        return view;
    }

    private void fetchLatestDataFromFirebase() {
        databaseReference.orderByKey().limitToLast(1)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                double wmUsage = data.child("washing_machine").getValue(Double.class) != null ?
                                        data.child("washing_machine").getValue(Double.class) : 0;

                                double fridgeUsage = data.child("fridge").getValue(Double.class) != null ?
                                        data.child("fridge").getValue(Double.class) : 0;

                                double heatingUsage = data.child("heating_system").getValue(Double.class) != null ?
                                        data.child("heating_system").getValue(Double.class) : 0;

                                washingMachineTextView.setText("Washing Machine: " + String.format("%.2f", wmUsage) + " Watts");
                                fridgeTextView.setText("Fridge: " + String.format("%.2f", fridgeUsage) + " Watts");
                                heatingSystemTextView.setText("Heating System: " + String.format("%.2f", heatingUsage) + " Watts");

                                float totalUsageKWh = (float) ((wmUsage + fridgeUsage + heatingUsage) / 1000.0);
                                remainingKWh = estimatedKWh - totalUsageKWh;
                                if (remainingKWh < 0) remainingKWh = 0;

                                updateProgressBar(remainingKWh);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("HomeFragment", "Error fetching data", error.toException());
                    }
                });
    }

    private void updateProgressBar(float remainingKWh) {
        energyProgressBar.setProgress((int) ((remainingKWh / estimatedKWh) * 100));
        remainingEnergyText.setText("Remaining: " + String.format("%.2f", remainingKWh) + " kWh");
    }
}

