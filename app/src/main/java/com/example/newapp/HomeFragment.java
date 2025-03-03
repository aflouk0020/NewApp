package com.example.newapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    private TextView washingMachineTextView;
    private TextView fridgeTextView;
    private TextView heatingSystemTextView;

    private DatabaseReference databaseReference;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        washingMachineTextView = view.findViewById(R.id.text_washing_machine_reading);
        fridgeTextView = view.findViewById(R.id.text_fridge_reading);
        heatingSystemTextView = view.findViewById(R.id.text_heating_system_reading);

        // Initialize Firebase Database Reference
        databaseReference = FirebaseDatabase.getInstance("https://myapp2-75686-default-rtdb.firebaseio.com/")
                .getReference("devices");

        fetchDataFromFirebase();

        return view;
    }

    private void fetchDataFromFirebase() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Retrieve values for each device
                    Double washingMachineReading = snapshot.child("washing_machine").getValue(Double.class);
                    Double fridgeReading = snapshot.child("fridge").getValue(Double.class);
                    Double heatingSystemReading = snapshot.child("heating_system").getValue(Double.class);

                    // Update UI with real-time data
                    washingMachineTextView.setText("Washing Machine: " +
                            (washingMachineReading != null ? String.format("%.2f", washingMachineReading) + " Watts" : "Not Connected"));

                    fridgeTextView.setText("Fridge: " +
                            (fridgeReading != null ? String.format("%.2f", fridgeReading) + " Watts" : "Not Connected"));

                    heatingSystemTextView.setText("Heating System: " +
                            (heatingSystemReading != null ? String.format("%.2f", heatingSystemReading) + " Watts" : "Not Connected"));
                } else {
                    // Show that devices are off or not connected
                    washingMachineTextView.setText("Washing Machine: Not Connected");
                    fridgeTextView.setText("Fridge: Not Connected");
                    heatingSystemTextView.setText("Heating System: Not Connected");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                washingMachineTextView.setText("Washing Machine: Error");
                fridgeTextView.setText("Fridge: Error");
                heatingSystemTextView.setText("Heating System: Error");
            }
        });
    }
}
