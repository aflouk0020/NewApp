package com.example.newapp;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import android.widget.Toast;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.HashMap;
import java.util.Map;

public class FriendManager {
    private static final String TAG = "FriendManager";
    private Context context;
    //private final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private final DatabaseReference friendRequestsRef = FirebaseDatabase.getInstance().getReference("friendRequests");
    private final DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

    public FriendManager(Context context) {
        this.context = context;
    }

    // Interface for search result callbacks
    public interface OnUserSearchResultListener {
        void onSearchCompleted(DataSnapshot snapshot);
        void onSearchFailed(Exception e);
    }

    // Method to search users by email
    public void searchUsersByEmail(String searchTerm, OnUserSearchResultListener listener) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            Log.e(TAG, "searchUsersByEmail: Search term is null or empty");
            if (listener != null) {
                listener.onSearchFailed(new IllegalArgumentException("Search term cannot be empty"));
            }
            return;
        }

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        Query query = usersRef.orderByChild("email").equalTo(searchTerm);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (listener != null) {
                    listener.onSearchCompleted(snapshot);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (listener != null) {
                    listener.onSearchFailed(error.toException());
                }
            }
        });
    }

    public void acceptRequest(String requestKey, String requesterUid) {
        if (currentUser == null) return;

        String currentUid = currentUser.getUid();

        DatabaseReference friendsRef = FirebaseDatabase.getInstance().getReference("friends");

        // Step 1: Check if already friends
        friendsRef.child(currentUid).child(requesterUid).get().addOnSuccessListener(friendSnapshot -> {
            if (friendSnapshot.exists()) {
                Toast.makeText(context, "You are already friends!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Step 2: Add to friends
            friendsRef.child(currentUid).child(requesterUid).setValue(true);
            friendsRef.child(requesterUid).child(currentUid).setValue(true);

            // Step 3: Update the friend request status
            friendRequestsRef.child(currentUid).child(requestKey).child("status").setValue("accepted")
                    .addOnSuccessListener(aVoid -> {
                        // Step 4: Update sender's sentFriendRequests status
                        DatabaseReference sentRef = FirebaseDatabase.getInstance()
                                .getReference("sentFriendRequests")
                                .child(requesterUid)
                                .child(currentUid);
                        sentRef.child("status").setValue("accepted");

                        Toast.makeText(context, "Friend request accepted", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to accept request", Toast.LENGTH_SHORT).show();
                    });

        }).addOnFailureListener(e -> {
            Toast.makeText(context, "Failed to check friendship", Toast.LENGTH_SHORT).show();
        });
    }


    public void denyRequest(String requestKey) {
        if (currentUser == null) return;

        String currentUid = currentUser.getUid();

        friendRequestsRef.child(currentUid).child(requestKey).get().addOnSuccessListener(snapshot -> {
            String requesterUid = snapshot.child("requesterUid").getValue(String.class);

            if (requesterUid == null) {
                Toast.makeText(context, "Invalid request data.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. Remove from friendRequests
            friendRequestsRef.child(currentUid).child(requestKey).removeValue()
                    .addOnSuccessListener(aVoid1 -> {
                        Log.d(TAG, "Removed from friendRequests.");

                        // 2. Then remove from sentFriendRequests
                        DatabaseReference sentRef = FirebaseDatabase.getInstance()
                                .getReference("sentFriendRequests")
                                .child(requesterUid)
                                .child(currentUid);

                        sentRef.removeValue()
                                .addOnSuccessListener(aVoid2 -> {
                                    Log.d(TAG, "Removed from sentFriendRequests.");
                                    Toast.makeText(context, "Friend request denied", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to remove from sentFriendRequests: " + e.getMessage());
                                    Toast.makeText(context, "Cleanup failed", Toast.LENGTH_SHORT).show();
                                });

                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to remove from friendRequests: " + e.getMessage());
                        Toast.makeText(context, "Failed to deny request", Toast.LENGTH_SHORT).show();
                    });

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to fetch request: " + e.getMessage());
            Toast.makeText(context, "Error loading request", Toast.LENGTH_SHORT).show();
        });
    }
    public void sendFriendRequest(String targetUid) {
        if (currentUser == null) {
            Log.e(TAG, "No authenticated user.");
            return;
        }

        String currentUid = currentUser.getUid();

        if (currentUid.equals(targetUid)) {
            Toast.makeText(context, "Cannot send friend request to yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        // Step 1: Check if already friends
        DatabaseReference friendsRef = FirebaseDatabase.getInstance()
                .getReference("friends")
                .child(currentUid)
                .child(targetUid);

        friendsRef.get().addOnSuccessListener(friendSnapshot -> {
            if (friendSnapshot.exists()) {
                Toast.makeText(context, "You are already friends.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Step 2: Check if a pending request already exists
            DatabaseReference sentRequestsRef = FirebaseDatabase.getInstance()
                    .getReference("sentFriendRequests")
                    .child(currentUid)
                    .child(targetUid);

            sentRequestsRef.get().addOnSuccessListener(requestSnapshot -> {
                String status = requestSnapshot.child("status").getValue(String.class);
                if ("pending".equals(status)) {
                    Toast.makeText(context, "Friend request already sent.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Step 3: Proceed to send request
                DatabaseReference targetRequestsRef = friendRequestsRef.child(targetUid);
                String requestKey = targetRequestsRef.push().getKey();
                if (requestKey == null) {
                    Toast.makeText(context, "Failed to generate request key", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> requestData = new HashMap<>();
                requestData.put("requesterUid", currentUid);
                requestData.put("status", "pending");
                requestData.put("timestamp", System.currentTimeMillis());

                targetRequestsRef.child(requestKey).setValue(requestData)
                        .addOnSuccessListener(aVoid -> {
                            Map<String, Object> sentData = new HashMap<>();
                            sentData.put("requestKey", requestKey);
                            sentData.put("status", "pending");
                            sentData.put("timestamp", System.currentTimeMillis());

                            sentRequestsRef.setValue(sentData)
                                    .addOnSuccessListener(aVoid2 -> {
                                        Toast.makeText(context, "Friend request sent!", Toast.LENGTH_SHORT).show();
                                        Log.d(TAG, "Friend request sent successfully!");

                                        usersRef.child(targetUid).child("fcmToken").get()
                                                .addOnSuccessListener(tokenSnap -> {
                                                    String token = tokenSnap.getValue(String.class);
                                                    if (token != null) {
                                                        sendFCMNotification(token, requestKey);
                                                    } else {
                                                        Log.w(TAG, "FCM token for user " + targetUid + " is null.");
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "Failed to retrieve FCM token: " + e.getMessage());
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to update sentFriendRequests: " + e.getMessage());
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "Failed to send request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

            }).addOnFailureListener(e -> {
                Toast.makeText(context, "Failed to check existing request", Toast.LENGTH_SHORT).show();
            });

        }).addOnFailureListener(e -> {
            Toast.makeText(context, "Failed to check friendship", Toast.LENGTH_SHORT).show();
        });
    }


    private void sendFCMNotification(String recipientToken, String requestKey) {
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        org.json.JSONObject json = new org.json.JSONObject();

        try {
            org.json.JSONObject data = new org.json.JSONObject();
            data.put("requesterUid", currentUser.getUid());
            data.put("requestKey", requestKey);

            json.put("to", recipientToken);
            json.put("data", data);

            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                    json.toString(),
                    okhttp3.MediaType.parse("application/json; charset=utf-8")
            );

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url("https://fcm.googleapis.com/fcm/send")
                    .addHeader("Authorization", "key=YOUR_SERVER_KEY_HERE") // <-- Replace with your actual FCM server key
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(@NonNull okhttp3.Call call, @NonNull java.io.IOException e) {
                    Log.e(TAG, "Failed to send notification", e);
                }

                @Override
                public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Notification sent successfully.");
                    } else {
                        Log.e(TAG, "Failed to send notification: " + response.message());
                    }
                }
            });

        } catch (org.json.JSONException e) {
            Log.e(TAG, "JSON error", e);
        }
    }
    public interface OnFriendRequestListener {
        void onRequestsChanged(DataSnapshot snapshot);
        void onRequestError(Exception e);
    }
    public void listenForIncomingRequests(OnFriendRequestListener listener) {
        if (currentUser == null) {
            Log.e(TAG, "No authenticated user found.");
            return;
        }

        DatabaseReference incomingRef = FirebaseDatabase.getInstance()
                .getReference("friendRequests")
                .child(currentUser.getUid());

        incomingRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listener.onRequestsChanged(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onRequestError(error.toException());
            }
        });
    }
}
