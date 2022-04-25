package com.matvey.perelman.notepad2.database.connection;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.matvey.perelman.notepad2.database.callback.ViewUpdater;
import com.matvey.perelman.notepad2.database.helpers.DatabaseCollectionHelper;
import com.matvey.perelman.notepad2.database.helpers.DatabaseFileHelper;

import java.io.File;
import java.util.ArrayList;

public class DatabaseConnection {
    public final SQLiteDatabase db_collection;
    private final ArrayList<Integer> file_ids;
    public final ArrayList<SQLiteDatabase> db_file_list;
    private final ArrayList<DatabaseCursor> cursors;
    public final Context context;
    public final ViewUpdater updater;
    public DatabaseConnection(Context context, ViewUpdater updater){
        this.context = context;
        this.updater = updater;
        db_collection = new DatabaseCollectionHelper(context).getReadableDatabase();
        db_file_list = new ArrayList<>();
        cursors = new ArrayList<>();
        file_ids = new ArrayList<>();
    }
    public DatabaseCursor makeCursor(){
        DatabaseCursor cursor = new DatabaseCursor(this);
        cursors.add(cursor);
        return cursor;
    }
    public int enter_file(int id, String name_enc){
        int idx = file_ids.indexOf(id);
        if(idx != -1)
            return idx;
        db_file_list.add(new DatabaseFileHelper(context, name_enc).getReadableDatabase());
        file_ids.add(id);
        return db_file_list.size() - 1;
    }
    public void exitFile(int index){
        for(DatabaseCursor c: cursors){
            if(c.db_file_index == index)
                return;
        }
        file_ids.remove(index);
        db_file_list.remove(index);
        for(DatabaseCursor c:cursors){
            if(c.db_file_index > index)
                c.db_file_index--;
        }
    }
    public void deleteCursor(DatabaseCursor c){
        if(c.db_file_index != -1){
            int idx = c.db_file_index;
            c.db_file_index = -1;
            exitFile(idx);
        }
        cursors.remove(c);
        c.close();
    }
    public void renameDatabase(int id, String from_enc, String to_enc){
        int idx = file_ids.indexOf(id);
        if(idx != -1)
            db_file_list.get(idx).close();
        File databaseFile = context.getDatabasePath(from_enc);
        databaseFile.renameTo(context.getDatabasePath(to_enc));
        if(idx != -1)
            db_file_list.set(idx, new DatabaseFileHelper(context, to_enc).getReadableDatabase());
    }
    public void deleteDatabase(String name_enc, int id){
        db_collection.execSQL("delete from info where id == " + id);
        context.deleteDatabase(name_enc);
    }

    public void close(){
        db_collection.close();
        for(DatabaseCursor c: cursors)
            c.close();
    }
}
