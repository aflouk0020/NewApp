package com.example.newapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationActionReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String requesterUid = intent.getStringExtra("requesterUid");
        String requestKey = intent.getStringExtra("requestKey");

        Log.d(TAG, "Action: " + action + ", UID: " + requesterUid + ", Key: " + requestKey);

        FriendManager friendManager = new FriendManager(context);

        if ("ACTION_ACCEPT".equals(action)) {
            friendManager.acceptRequest(requestKey, requesterUid);
        } else if ("ACTION_DENY".equals(action)) {
            friendManager.denyRequest(requestKey);
        }
    }
}