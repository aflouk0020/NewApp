package com.example.newapp;

import android.content.Context;
import android.content.SharedPreferences;



public class UserManager {
    private static final String PREF_NAME = "UserPrefs";

    public static String getUserId(Context context, String username) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(username + "_id", "UNKNOWN_USER");
    }
}
