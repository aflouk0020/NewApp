package com.example.newapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.content.BroadcastReceiver;
import android.content.Context;
import androidx.core.app.NotificationCompat;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "friend_requests_channel";


    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseDatabase.getInstance().getReference("users")
                    .child(user.getUid())
                    .child("fcmToken")
                    .setValue(token);
        }
    }


    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getData().size() > 0) {
            createNotificationChannel(); // 🔴 MAKE SURE THIS IS CALLED FIRST

            String requesterUid = remoteMessage.getData().get("requesterUid");

            // Build Accept/Deny Intents
            Intent acceptIntent = new Intent(this, NotificationActionReceiver.class);
            acceptIntent.setAction("ACTION_ACCEPT");
            acceptIntent.putExtra("requesterUid", requesterUid);
            PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(this, 0, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Intent denyIntent = new Intent(this, NotificationActionReceiver.class);
            denyIntent.setAction("ACTION_DENY");
            denyIntent.putExtra("requesterUid", requesterUid);
            PendingIntent denyPendingIntent = PendingIntent.getBroadcast(this, 1, denyIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Friend Request")
                    .setContentText("You received a friend request")
                    .addAction(0, "Accept", acceptPendingIntent)
                    .addAction(0, "Deny", denyPendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            NotificationManagerCompat.from(this).notify(1001, builder.build());

        }
    }




    public class NotificationActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String requesterUid = intent.getStringExtra("requesterUid");

            FriendManager friendManager = new FriendManager(context);

            if ("ACTION_ACCEPT".equals(action)) {
                friendManager.acceptRequest(null, requesterUid);
            } else if ("ACTION_DENY".equals(action)) {
                friendManager.denyRequest(null);
            }
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
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

}
