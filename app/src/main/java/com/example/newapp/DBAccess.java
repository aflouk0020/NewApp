package com.example.newapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DBAccess extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "mydatabase.db";
    public static final int DATABASE_VERSION = 1;

    public DBAccess(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (username TEXT PRIMARY KEY, password TEXT, family_size INTEGER, room_number INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }

    public boolean insertUser(String username, String password, int familySize, int roomNumber) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "INSERT INTO users (username, password, family_size, room_number) VALUES (?, ?, ?, ?)";
        db.execSQL(query, new Object[]{username, password, familySize, roomNumber});
        db.close();
        return true;
    }

    public Cursor getUserDetails(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Modified query to ensure correct data retrieval
        return db.rawQuery("SELECT * FROM users WHERE username = ?", new String[]{username});
    }

    public boolean checkUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM users WHERE username = ?";
        Cursor cursor = db.rawQuery(query, new String[]{username});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public boolean checkUser(String username, String password) {
        SQLiteDatabase myDB = this.getWritableDatabase();
        Cursor cursor = myDB.rawQuery("SELECT * FROM users WHERE username = ? AND password = ?", new String[]{username, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public void saveUserIdToSharedPreferences(Context context, String username, String userId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(username + "_ID", userId);  // Save user ID with the username key
        editor.apply();
    }
}
//package com.example.newapp;
//
//import android.content.Context;
//import android.content.SharedPreferences;
//import android.database.Cursor;
//import android.database.sqlite.SQLiteDatabase;
//import android.database.sqlite.SQLiteOpenHelper;
//
//import androidx.annotation.Nullable;
//
//public class DBAccess extends SQLiteOpenHelper {
//
//    public static final String DATABASE_NAME = "mydatabase.db";
//    public static final int DATABASE_VERSION = 1;
//
//    public DBAccess(@Nullable Context context) {
//        super(context, DATABASE_NAME, null, DATABASE_VERSION);
//    }
//
//    @Override
//    public void onCreate(SQLiteDatabase db) {
//        db.execSQL("CREATE TABLE users (username TEXT PRIMARY KEY, password TEXT, family_size INTEGER, room_number INTEGER)");
//    }
//
//    @Override
//    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        db.execSQL("DROP TABLE IF EXISTS users");
//        onCreate(db);
//    }
//
//    public boolean insertUser(String username, String password, int familySize, int roomNumber) {
//        SQLiteDatabase db = this.getWritableDatabase();
//        String query = "INSERT INTO users (username, password, family_size, room_number) VALUES (?, ?, ?, ?)";
//        db.execSQL(query, new Object[]{username, password, familySize, roomNumber});
//        db.close();
//        return true;
//    }
//
//    public Cursor getUserDetails(String username) {
//        SQLiteDatabase db = this.getReadableDatabase();
//        return db.rawQuery("SELECT * FROM users WHERE username = ?", new String[]{username});
//    }
//
//    public boolean checkUsername(String username) {
//        SQLiteDatabase db = this.getReadableDatabase();
//        String query = "SELECT * FROM users WHERE username = ?";
//        Cursor cursor = db.rawQuery(query, new String[]{username});
//        boolean exists = cursor.getCount() > 0;
//        cursor.close();
//        return exists;
//    }
//
//    public boolean checkUser(String username, String password) {
//        SQLiteDatabase myDB = this.getWritableDatabase();
//        Cursor cursor = myDB.rawQuery("SELECT * FROM users WHERE username = ? AND password = ?", new String[]{username, password});
//        boolean exists = cursor.getCount() > 0;
//        cursor.close();
//        return exists;
//    }
//
//    public void saveUsernameToSharedPreferences(Context context, String username) {
//        SharedPreferences sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putString("USERNAME", username);  // Save the username to SharedPreferences
//        editor.apply();
//    }
//}


//package com.example.newapp;
//
//import android.content.Context;
//import android.content.SharedPreferences;  // Add this import
//import android.database.Cursor;
//import android.database.sqlite.SQLiteDatabase;
//import android.database.sqlite.SQLiteOpenHelper;
//
//import androidx.annotation.Nullable;
//
//public class DBAccess extends SQLiteOpenHelper {
//
//    public static final String DATABASE_NAME = "mydatabase.db";
//    public static final int DATABASE_VERSION = 1;
//
//    public DBAccess(@Nullable Context context) {
//        super(context, DATABASE_NAME, null, DATABASE_VERSION);
//    }
//
//    @Override
//    public void onCreate(SQLiteDatabase db) {
//        db.execSQL("CREATE TABLE users (username TEXT PRIMARY KEY, password TEXT, family_size INTEGER, room_number INTEGER)");
//    }
//
//    @Override
//    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        db.execSQL("DROP TABLE IF EXISTS users");
//        onCreate(db);
//    }
//
//    public boolean insertUser(String username, String password, int familySize, int roomNumber) {
//        SQLiteDatabase db = this.getWritableDatabase();
//        String query = "INSERT INTO users (username, password, family_size, room_number) VALUES (?, ?, ?, ?)";
//        db.execSQL(query, new Object[]{username, password, familySize, roomNumber});
//        db.close();
//        return true;
//    }
//
//    public Cursor getUserDetails(String username) {
//        SQLiteDatabase db = this.getReadableDatabase();
//        return db.rawQuery("SELECT * FROM users WHERE username = ?", new String[]{username});
//    }
//
//    public boolean checkUsername(String username) {
//        SQLiteDatabase db = this.getReadableDatabase();
//        String query = "SELECT * FROM users WHERE username = ?";
//        Cursor cursor = db.rawQuery(query, new String[]{username});
//        boolean exists = cursor.getCount() > 0;
//        cursor.close();
//        return exists;
//    }
//
//    public boolean checkUser(String username, String password) {
//        SQLiteDatabase myDB = this.getWritableDatabase();
//        Cursor cursor = myDB.rawQuery("SELECT * FROM users WHERE username = ? AND password = ?", new String[]{username, password});
//        boolean exists = cursor.getCount() > 0;
//        cursor.close();
//        return exists;
//    }
//
//    public void saveUserIdToSharedPreferences(Context context, String username, String userId) {
//        SharedPreferences sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);  // Now it works because of the import
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putString(username + "_ID", userId);  // Save user ID with the username key
//        editor.apply();
//    }
//}
