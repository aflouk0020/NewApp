package com.example.newapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RequestsFragment extends Fragment {
    private LinearLayout yourContainer;
    private FriendManager friendManager;
    private DatabaseReference usersRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_requests, container, false);
        yourContainer = view.findViewById(R.id.requests_container);
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        friendManager = new FriendManager(requireContext());

        friendManager.listenForIncomingRequests(new FriendManager.OnFriendRequestListener() {
            @Override
            public void onRequestsChanged(@NonNull DataSnapshot snapshot) {
                yourContainer.removeAllViews();
                Log.d("RequestsFragment", "Requests changed");

                if (!snapshot.exists()) return;

                for (DataSnapshot requestSnap : snapshot.getChildren()) {
                    String requestKey = requestSnap.getKey();
                    String requesterUid = requestSnap.child("requesterUid").getValue(String.class);
                    String status = requestSnap.child("status").getValue(String.class);

                    if (!"pending".equals(status)) continue; // ✅ Only show pending

                    View requestView = getLayoutInflater().inflate(R.layout.friend_request_item, yourContainer, false);
                    TextView emailText = requestView.findViewById(R.id.requesterEmail);
                    Button acceptButton = requestView.findViewById(R.id.acceptButton);
                    Button denyButton = requestView.findViewById(R.id.denyButton);

                    usersRef.child(requesterUid).child("email").get().addOnSuccessListener(snapshot1 -> {
                        String requesterEmail = snapshot1.getValue(String.class);
                        emailText.setText("From: " + (requesterEmail != null ? requesterEmail : requesterUid));
                    }).addOnFailureListener(e -> {
                        emailText.setText("From: " + requesterUid);
                    });

                    acceptButton.setOnClickListener(v -> friendManager.acceptRequest(requestKey, requesterUid));
                    denyButton.setOnClickListener(v -> friendManager.denyRequest(requestKey));

                    yourContainer.addView(requestView);
                }
            }

            @Override
            public void onRequestError(Exception e) {
                Toast.makeText(getContext(), "Failed to load requests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}
