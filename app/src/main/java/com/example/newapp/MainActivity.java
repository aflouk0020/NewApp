
package com.example.newapp;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // ActivityResultLauncher for FirebaseUI sign-in intent
    private final ActivityResultLauncher<Intent> signInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                IdpResponse response = IdpResponse.fromResultIntent(result.getData());
                if (result.getResultCode() == RESULT_OK) {
                    // Sign-in successful; fetch current user and navigate to HomeActivity
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    Log.d(TAG, "Sign in successful for user: " + (user != null ? user.getEmail() : "unknown"));
                    startActivity(new Intent(MainActivity.this, HomeActivity.class));
                    finish();
                } else {
                    // Sign-in failed: distinguish between cancellation and an error
                    if (response == null) {
                        Log.w(TAG, "Sign in cancelled by user");
                    } else {
                        int errorCode = response.getError().getErrorCode();
                        String errorMessage = response.getError().getMessage();
                        Log.e(TAG, "Sign in error. Error code: " + errorCode + ", message: " + errorMessage);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already signed in and skip sign-in if so
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already signed in: " + currentUser.getEmail());
            startActivity(new Intent(MainActivity.this, HomeActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
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
}

