package com.example.newapp;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import okhttp3.*;

public class FriendManager {
    private static final String TAG = "FriendManager";
    private final Context context;
    private final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private final DatabaseReference friendRequestsRef = FirebaseDatabase.getInstance().getReference("friendRequests");
    private final DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

    public FriendManager(Context context) {
        this.context = context;
    }

    public interface OnUserSearchResultListener {
        void onSearchCompleted(DataSnapshot snapshot);
        void onSearchFailed(Exception e);
    }

    public void searchUsersByEmail(String searchTerm, OnUserSearchResultListener listener) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            if (listener != null) {
                listener.onSearchFailed(new IllegalArgumentException("Search term cannot be empty"));
            }
            return;
        }

        Query query = usersRef.orderByChild("email").equalTo(searchTerm);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (listener != null) listener.onSearchCompleted(snapshot);
            }

            public void onCancelled(@NonNull DatabaseError error) {
                if (listener != null) listener.onSearchFailed(error.toException());
            }
        });
    }

    // ✅ Accept friend request and clean up
    public void acceptRequest(String requestKey, String requesterUid) {
        if (currentUser == null) return;

        String currentUid = currentUser.getUid();
        DatabaseReference friendsRef = FirebaseDatabase.getInstance().getReference("friends");

        friendsRef.child(currentUid).child(requesterUid).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                Toast.makeText(context, "You are already friends!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Add each other as friends
            friendsRef.child(currentUid).child(requesterUid).setValue(true);
            friendsRef.child(requesterUid).child(currentUid).setValue(true);

            // Update status to accepted
            friendRequestsRef.child(currentUid).child(requestKey).child("status").setValue("accepted")
                    .addOnSuccessListener(aVoid -> {
                        DatabaseReference sentRef = FirebaseDatabase.getInstance()
                                .getReference("sentFriendRequests")
                                .child(requesterUid)
                                .child(currentUid);

                        sentRef.child("status").setValue("accepted");

                        // ✅ CLEAN UP: Remove both sides of request (so fragment refreshes)
                        friendRequestsRef.child(currentUid).child(requestKey).removeValue();
                        sentRef.removeValue();

                        Toast.makeText(context, "Friend request accepted", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(context, "Failed to accept request", Toast.LENGTH_SHORT).show());

        }).addOnFailureListener(e ->
                Toast.makeText(context, "Failed to check friendship", Toast.LENGTH_SHORT).show());
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

            friendRequestsRef.child(currentUid).child(requestKey).removeValue()
                    .addOnSuccessListener(aVoid1 -> {
                        DatabaseReference sentRef = FirebaseDatabase.getInstance()
                                .getReference("sentFriendRequests")
                                .child(requesterUid)
                                .child(currentUid);
                        sentRef.removeValue()
                                .addOnSuccessListener(aVoid2 ->
                                        Toast.makeText(context, "Friend request denied", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(context, "Cleanup failed", Toast.LENGTH_SHORT).show());
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(context, "Failed to deny request", Toast.LENGTH_SHORT).show());
        }).addOnFailureListener(e ->
                Toast.makeText(context, "Error loading request", Toast.LENGTH_SHORT).show());
    }




    public void sendFriendRequest(String targetUid) {
        if (currentUser == null || currentUser.getUid().equals(targetUid)) {
            Toast.makeText(context, "Invalid operation", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUid = currentUser.getUid();
        DatabaseReference friendsRef = FirebaseDatabase.getInstance().getReference("friends")
                .child(currentUid).child(targetUid);

        friendsRef.get().addOnSuccessListener(friendSnapshot -> {
            if (friendSnapshot.exists()) {
                Toast.makeText(context, "You are already friends.", Toast.LENGTH_SHORT).show();
                return;
            }

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
                                        usersRef.child(targetUid).child("fcmToken").get()
                                                .addOnSuccessListener(tokenSnap -> {
                                                    String token = tokenSnap.getValue(String.class);
                                                    if (token != null) sendFCMNotification(token, requestKey);
                                                });
                                    });
                        });
            });
        });
    }

    private void sendFCMNotification(String recipientToken, String requestKey) {
        OkHttpClient client = new OkHttpClient();
        JSONObject json = new JSONObject();

        try {
            JSONObject data = new JSONObject();
            data.put("requesterUid", currentUser.getUid());
            data.put("requestKey", requestKey);

            json.put("to", recipientToken);
            json.put("data", data);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url("https://fcm.googleapis.com/fcm/send")
                    .addHeader("Authorization", "key=YOUR_SERVER_KEY_HERE") // Replace this!
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                public void onFailure(@NonNull Call call, @NonNull java.io.IOException e) {
                    Log.e(TAG, "Failed to send notification", e);
                }

                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    Log.d(TAG, "Notification response: " + response.message());
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "JSON error", e);
        }
    }

    public interface OnFriendRequestListener {
        void onRequestsChanged(DataSnapshot snapshot);
        void onRequestError(Exception e);
    }

    public void listenForIncomingRequests(OnFriendRequestListener listener) {
        if (currentUser == null) return;

        DatabaseReference incomingRef = friendRequestsRef.child(currentUser.getUid());
        incomingRef.addValueEventListener(new ValueEventListener() {
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listener.onRequestsChanged(snapshot);
            }

            public void onCancelled(@NonNull DatabaseError error) {
                listener.onRequestError(error.toException());
            }
        });
    }

    public interface OnFriendsFetchedListener {
        void onFriendsFetched(DataSnapshot snapshot);
        void onError(Exception e);
    }

    public void fetchFriends(OnFriendsFetchedListener listener) {
        if (currentUser == null) return;

        DatabaseReference friendsRef = FirebaseDatabase.getInstance()
                .getReference("friends")
                .child(currentUser.getUid());

        friendsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listener.onFriendsFetched(snapshot);
            }

            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.toException());
            }
        });
    }

    public void unfriend(String friendUid, @NonNull Runnable onSuccess, @NonNull Runnable onFailure) {
        if (currentUser == null) return;

        String currentUid = currentUser.getUid();
        DatabaseReference friendsRef = FirebaseDatabase.getInstance().getReference("friends");

        friendsRef.child(currentUid).child(friendUid).removeValue()
                .addOnSuccessListener(aVoid -> {
                    friendsRef.child(friendUid).child(currentUid).removeValue()
                            .addOnSuccessListener(aVoid2 -> onSuccess.run())
                            .addOnFailureListener(e -> onFailure.run());
                })
                .addOnFailureListener(e -> onFailure.run());
    }



    public void listenToFriends(OnFriendsFetchedListener listener) {
        if (currentUser == null) return;

        DatabaseReference friendsRef = FirebaseDatabase.getInstance()
                .getReference("friends")
                .child(currentUser.getUid());

        friendsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listener.onFriendsFetched(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.toException());
            }
        });
    }

}