package com.grf.database;


import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class SqliteDbHelper extends SQLiteOpenHelper {
    private static final String TAG = "SqliteDbHelper";

    private static final String DATABASE_NAME = "app_tasks.db";
    private static final int DATABASE_VERSION = 1;

    public SqliteDbHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Note: Task table is created by TaskDbHelper constructor (or you can exec SQL here).
        // Keeping this minimal as requested.
        try {
            // If you want all tables created here, add db.execSQL(...) calls.
        } catch (SQLException e) {
            Log.e(TAG, "onCreate: failed to create tables", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            // simple drop & recreate strategy (risky in production - data loss)
            db.execSQL("DROP TABLE IF EXISTS TaskList");
            onCreate(db);
        } catch (SQLException e) {
            Log.e(TAG, "onUpgrade: failed", e);
        }
    }
}
