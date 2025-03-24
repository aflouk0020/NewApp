package com.example.newapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String requesterUid = intent.getStringExtra("requesterUid");

        Log.d("NotificationReceiver", "Action: " + action + ", UID: " + requesterUid);

        FriendManager friendManager = new FriendManager(context);

        if ("ACTION_ACCEPT".equals(action)) {
            friendManager.acceptRequest(null, requesterUid);
        } else if ("ACTION_DENY".equals(action)) {
            friendManager.denyRequest(null);
        }
    }
}
