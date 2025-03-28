package com.example.newapp;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "friend_request_channel";
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getData().size() > 0) {
            createNotificationChannel();

            String requesterUid = remoteMessage.getData().get("requesterUid");
            String requestKey = remoteMessage.getData().get("requestKey");

            // 🔔 Intent to open RequestsFragment
            Intent contentIntent = new Intent(this, HomeActivity.class);
            contentIntent.putExtra("navigate_to", "requests");  // You'll check for this in HomeActivity
            contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent contentPendingIntent = PendingIntent.getActivity(
                    this,
                    2,
                    contentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Accept & Deny actions
            Intent acceptIntent = new Intent(this, NotificationActionReceiver.class);
            acceptIntent.setAction("ACTION_ACCEPT");
            acceptIntent.putExtra("requesterUid", requesterUid);
            acceptIntent.putExtra("requestKey", requestKey);
            PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(this, 0, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Intent denyIntent = new Intent(this, NotificationActionReceiver.class);
            denyIntent.setAction("ACTION_DENY");
            denyIntent.putExtra("requesterUid", requesterUid);
            denyIntent.putExtra("requestKey", requestKey);
            PendingIntent denyPendingIntent = PendingIntent.getBroadcast(this, 1, denyIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification) // Replace with your icon
                    .setContentTitle("Friend Request")
                    .setContentText("You received a friend request")
                    .setContentIntent(contentPendingIntent) // 👈 THIS MAKES TAPPING THE NOTIFICATION OPEN RequestsFragment
                    .addAction(0, "Accept", acceptPendingIntent)
                    .addAction(0, "Deny", denyPendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            NotificationManagerCompat.from(this).notify(1001, builder.build());
        }
    }



    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Friend Requests";
            String description = "Notifications for friend requests";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}