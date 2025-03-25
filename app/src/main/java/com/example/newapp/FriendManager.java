package com.example.newapp;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


public class FriendManager {

    private static final String TAG = "FriendManager";

    private final Context context;
    private final DatabaseReference usersRef;
    private final DatabaseReference friendRequestsRef;
    private final DatabaseReference friendsRef;
    private final FirebaseUser currentUser;

    public FriendManager(Context context) {
        this.context = context;
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        this.usersRef = database.getReference("users");
        this.friendRequestsRef = database.getReference("friendRequests");
        this.friendsRef = database.getReference("friends");
        this.currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    public void searchUsersByEmail(String searchTerm, OnUserSearchResultListener listener) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            Toast.makeText(context, "Enter a search term", Toast.LENGTH_SHORT).show();
            return;
        }

        Query query = usersRef.orderByChild("email")
                .startAt(searchTerm)
                .endAt(searchTerm + "\uf8ff");

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listener.onSearchCompleted(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onSearchFailed(error.toException());
            }
        });
    }




    public void sendFriendRequest(String targetUid) {
        if (currentUser == null) {
            Log.e(TAG, "No authenticated user.");
            return;
        }

        if (currentUser.getUid().equals(targetUid)) {
            Toast.makeText(context, "Cannot send friend request to yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference targetRequestsRef = friendRequestsRef.child(targetUid);

        // 🔍 Check if a request from this user already exists
        targetRequestsRef.orderByChild("requesterUid")
                .equalTo(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Toast.makeText(context, "Friend request already sent.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // 🆕 No existing request, so send one
                        String requestKey = targetRequestsRef.push().getKey();
                        if (requestKey == null) {
                            Toast.makeText(context, "Failed to generate request key", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Map<String, Object> requestData = new HashMap<>();
                        requestData.put("requesterUid", currentUser.getUid());
                        requestData.put("status", "pending");
                        requestData.put("timestamp", System.currentTimeMillis());

                        targetRequestsRef.child(requestKey).setValue(requestData)
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(context, "Friend request sent!", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(context, "Failed to send request: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "sendFriendRequest check failed: " + error.getMessage());
                    }
                });
    }

//public void sendFriendRequest(String targetUid) {
//    if (currentUser == null) {
//        Log.e(TAG, "No authenticated user.");
//        return;
//    }
//
//    if (currentUser.getUid().equals(targetUid)) {
//        Toast.makeText(context, "Cannot send friend request to yourself", Toast.LENGTH_SHORT).show();
//        return;
//    }
//
//    DatabaseReference targetRequestsRef = friendRequestsRef.child(targetUid);
//    String requestKey = targetRequestsRef.push().getKey();
//    if (requestKey == null) {
//        Toast.makeText(context, "Failed to generate request key", Toast.LENGTH_SHORT).show();
//        return;
//    }
//
//    Map<String, Object> requestData = new HashMap<>();
//    requestData.put("requesterUid", currentUser.getUid());
//    requestData.put("status", "pending");
//    requestData.put("timestamp", System.currentTimeMillis());
//
//    targetRequestsRef.child(requestKey).setValue(requestData)
//            .addOnSuccessListener(aVoid -> {
//                Toast.makeText(context, "Friend request sent!", Toast.LENGTH_SHORT).show();
//
//            })
//            .addOnFailureListener(e ->
//                    Toast.makeText(context, "Failed to send request: " + e.getMessage(), Toast.LENGTH_SHORT).show());
//}


    private void sendFCMNotification(String recipientToken) {
        OkHttpClient client = new OkHttpClient();
        JSONObject json = new JSONObject();
        JSONObject notificationJson = new JSONObject();

        try {
            notificationJson.put("title", "New Friend Request");
            notificationJson.put("body", currentUser.getEmail() + " sent you a friend request.");

            json.put("to", recipientToken);
            //json.put("notification", notificationJson);
            json.put("to", recipientToken);

            JSONObject data = new JSONObject();
            data.put("requesterUid", currentUser.getUid());

            json.put("data", data);


            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json; charset=utf-8"));

            Request request = new Request.Builder()
                    .url("https://fcm.googleapis.com/fcm/send")
                    .addHeader("Authorization", "key=YOUR_SERVER_KEY_HERE")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Failed to send notification", e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Notification sent successfully.");
                    } else {
                        Log.e(TAG, "Failed to send notification: " + response.message());
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "JSON error", e);
        }
    }





    public void acceptRequest(String requestKey, String requesterUid) {
        if (currentUser == null) return;

        String currentUid = currentUser.getUid();
        friendsRef.child(currentUid).child(requesterUid).setValue(true);
        friendsRef.child(requesterUid).child(currentUid).setValue(true);

        friendRequestsRef.child(currentUid).child(requestKey).child("status").setValue("accepted")
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(context, "Friend request accepted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Failed to accept request", Toast.LENGTH_SHORT).show());
    }

    public void denyRequest(String requestKey) {
        if (currentUser == null) return;

        String currentUid = currentUser.getUid();
        friendRequestsRef.child(currentUid).child(requestKey).removeValue()
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(context, "Friend request denied", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Failed to deny request", Toast.LENGTH_SHORT).show());
    }

    public interface OnUserSearchResultListener {
        void onSearchCompleted(DataSnapshot snapshot);
        void onSearchFailed(Exception e);
    }

    // Clearly add this:
    public interface OnFriendRequestListener {
        void onRequestsChanged(DataSnapshot snapshot);
        void onRequestError(Exception e);
    }

    public void listenForIncomingRequests(OnFriendRequestListener listener) {
        if (currentUser == null) {
            Log.e(TAG, "No authenticated user found.");
            return;
        }


        friendRequestsRef.child(currentUser.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d("RequestsFragment", "Requests changed: " + snapshot.toString());
                        listener.onRequestsChanged(snapshot);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        listener.onRequestError(error.toException());
                    }
                });

    }

}
