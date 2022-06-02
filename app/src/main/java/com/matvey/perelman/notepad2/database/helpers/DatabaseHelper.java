package com.matvey.perelman.notepad2.database.helpers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

public class DatabaseHelper extends SQLiteOpenHelper {
    public DatabaseHelper(Context context) {
        super(context, "main_db", null, 4);
        getWritableDatabase().execSQL("PRAGMA foreign_keys = ON");
    }

    private void createTable(SQLiteDatabase sqLiteDatabase){
        sqLiteDatabase.execSQL(
                "CREATE TABLE main(" +
                        "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "parent INTEGER," +
                        "name TEXT NOT NULL," +
                        "type INTEGER NOT NULL," +
                        "content TEXT," +

                        "UNIQUE(parent, name)," +
                        "CHECK (type >= 0 and type < 3)," +
                        "CHECK (type == 0 <> NOT (content is NULL))," +
                        "FOREIGN KEY(parent)" +
                        " REFERENCES main(ID)" +
                        " ON DELETE CASCADE" +
                        ");");
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        createTable(sqLiteDatabase);
        sqLiteDatabase.execSQL("INSERT INTO main(ID, parent, name, type, content) values(0, 0, '/', 0, NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        SQLiteStatement statement = sqLiteDatabase.compileStatement("DELETE from main WHERE NOT EXISTS(SELECT ID from main as m WHERE m.ID = main.parent)");
        int v;
        do{
            v = statement.executeUpdateDelete();
        }while(v > 0);

        sqLiteDatabase.execSQL("ALTER TABLE main RENAME TO main_tmp");
        createTable(sqLiteDatabase);
        sqLiteDatabase.execSQL("INSERT INTO main SELECT * FROM main_tmp");
        sqLiteDatabase.execSQL("DROP TABLE main_tmp");
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE main");
        onCreate(db);
    }
}
