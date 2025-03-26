package com.example.newapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";



    private final ActivityResultLauncher<Intent> signInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                IdpResponse response = IdpResponse.fromResultIntent(result.getData());
                if (result.getResultCode() == RESULT_OK) {
                    // Sign-in successful
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    Log.d(TAG, "Sign in successful for user: " + (user != null ? user.getEmail() : "unknown"));

                    if (user != null) {
                        // Store the user's profile in Realtime Database
                        storeUserProfileInDatabase(user);

                        // Retrieve and store the FCM token
                        FirebaseMessaging.getInstance().getToken()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        String token = task.getResult();
                                        // Use the existing 'user' from the outer scope
                                        if (user != null) {
                                            DatabaseReference userRef = FirebaseDatabase.getInstance()
                                                    .getReference("users")
                                                    .child(user.getUid());
                                            userRef.child("fcmToken").setValue(token);
                                            Log.d("FCM", "Token updated on startup: " + token);
                                        }
                                    } else {
                                        Log.e("FCM", "Failed to get token", task.getException());
                                    }
                                    // Remove redundant and potentially problematic lines (see notes below)
                                });
                    }

                    // Go to HomeActivity
                    startActivity(new Intent(MainActivity.this, HomeActivity.class));
                    finish();
                } else {
                    // Sign-in failed
                    if (response == null) {
                        Log.w(TAG, "Sign in cancelled by user");
                    } else {
                        int errorCode = response.getError().getErrorCode();
                        String errorMessage = response.getError().getMessage();
                        Log.e(TAG, "Sign in error. Code: " + errorCode + ", message: " + errorMessage);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main); // ✅ First, set the content view

        // Check if user is already signed in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already signed in: " + currentUser.getEmail());
            String uid = currentUser.getUid();
            DatabaseReference onlineRef = FirebaseDatabase.getInstance().getReference("onlineStatus").child(uid);

            // ✅ Mark user online when app launches
            onlineRef.setValue(true);

            // ✅ Automatically mark user offline if they disconnect (lose connection or quit app)
            onlineRef.onDisconnect().setValue(false);
            startActivity(new Intent(MainActivity.this, HomeActivity.class));
            finish();
            return;
        }

        // Retrieve and log FCM token
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token);
                });

    }

    // Called when the user clicks the sign-in button
    public void signIn(View view) {
        // Define sign-in providers (Google, Email, Phone)
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build()
        );

        // Build and launch sign-in intent
        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)  // Disable SmartLock for testing
                .build();

        signInLauncher.launch(signInIntent);
    }

    /**
     * Write user profile to the "users" node in Realtime Database.
     */
    private void storeUserProfileInDatabase(FirebaseUser user) {
        String uid = user.getUid();
        // Some providers (like PhoneAuth) may return null for getEmail().
        String email = (user.getEmail() != null) ? user.getEmail() : "NoEmail";
        String displayName = (user.getDisplayName() != null) ? user.getDisplayName() : "NoName";

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid);

        // Write the user's email and displayName
        userRef.child("email").setValue(email);
        userRef.child("displayName").setValue(displayName)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "User profile stored for " + email))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to store user profile", e));
    }
}