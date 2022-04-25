package com.matvey.perelman.notepad2.database.helpers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseFileHelper extends SQLiteOpenHelper {
    public DatabaseFileHelper(Context context, String enc_name){
        super(context, enc_name, null, 9);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table files (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "content TEXT NOT NULL, " +
                "type INTEGER NOT NULL, " +
                "parent INTEGER)");
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE files");
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
