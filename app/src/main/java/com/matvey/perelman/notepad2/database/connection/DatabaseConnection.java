package com.matvey.perelman.notepad2.database.connection;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import androidx.appcompat.app.AppCompatActivity;

import com.matvey.perelman.notepad2.creator.CreatorElement;
import com.matvey.perelman.notepad2.database.DatabaseElement;
import com.matvey.perelman.notepad2.database.helpers.DatabaseHelper;
import com.matvey.perelman.notepad2.list.ElementType;

import java.util.ArrayList;

public class DatabaseConnection {
    public final SQLiteDatabase db;
    private final ArrayList<DatabaseCursor> cursors;

    private final String[] single_param;
    private final String[] double_param;
    private final StringBuilder sb;

    public DatabaseConnection(Context context){
        cursors = new ArrayList<>();
        single_param = new String[1];
        double_param = new String[2];
        sb = new StringBuilder();

        db = new DatabaseHelper(context).getWritableDatabase();
    }
    public DatabaseCursor makeCursor(ViewListener listener, AppCompatActivity activity){
        DatabaseCursor cursor = new DatabaseCursor(this, listener, activity);
        cursors.add(cursor);
        return cursor;
    }
    public void close(){
        for(DatabaseCursor c: cursors)
            c.close();
        cursors.clear();
        db.close();
    }

    private void onNewItem(long parent, long id){
        for(IDListener listener: cursors){
            if(parent == listener.getPathID())
                listener.onNewItem(id);
        }
    }
    private void onChangeItem(long parent, long id){
        for(IDListener listener: cursors){
            if(parent == listener.getPathID())
                listener.onChangeItem(id);
        }
    }
    private void onChangeName(long parent){
        for(IDListener listener: cursors){
            if(isParentFor(parent, listener.getPathID()))
                listener.onPathRenamed();
        }
    }
    private void onDeleteView(long parent, long id){
        for(IDListener listener: cursors){
            if(parent == listener.getPathID())
                listener.onDeleteItem(id);
        }
    }
    private void onDeleteItem(long parent, long id){
        onDeleteView(parent, id);
        for(IDListener listener: cursors){
            if(!exists(listener.getPathID()))
                listener.onPathChanged(parent);
        }
    }

    public void getElement(long id, DatabaseElement save_to){
        single_param[0] = String.valueOf(id);
        Cursor c = db.rawQuery("SELECT parent, name, type, substr(content, 0, 64) FROM main WHERE ID == ?", single_param);
        if(c.getCount() != 0){
            c.moveToPosition(0);
            save_to.id = id;
            save_to.parent = c.getLong(0);

            save_to.name = c.getString(1);
            save_to.type = ElementType.values()[c.getInt(2)];

            save_to.content = c.getString(3);
        }else
            save_to.id = -1;
        c.close();
    }
    public void getElement(Cursor list, DatabaseElement save_to, long parent, int idx){
        list.moveToPosition(idx);
        save_to.id = list.getLong(0);
        save_to.parent = parent;

        save_to.name = list.getString(1);
        save_to.type = ElementType.values()[list.getInt(2)];
        save_to.content = list.getString(3);
    }
    public Cursor getListFiles(long folder_id){
        single_param[0] = String.valueOf(folder_id);
//        return db.rawQuery("SELECT ID, name, type, substr(content, 0, 64) FROM main WHERE parent == ? AND ID != 0 ORDER BY name", single_param);
        return db.rawQuery("SELECT ID, name, type, substr(content, 0, 64) FROM main WHERE ID != 0 ORDER BY name", null);
    }
    public long getID(long parent, String item){
        double_param[0] = String.valueOf(parent);
        double_param[1] = item;
        Cursor c = db.rawQuery("SELECT ID FROM main WHERE parent == ? AND name == ?", double_param);
        long id;
        if(c.getCount() != 0) {
            c.moveToPosition(0);
            id = c.getLong(0);
        }else
            id = -1;
        c.close();
        return id;
    }
    private boolean exists(long id){
        single_param[0] = String.valueOf(id);
        Cursor c = db.rawQuery("SELECT ID FROM main WHERE ID = ?", single_param);
        boolean b = c.getCount() != 0;
        c.close();
        return b;
    }
    public String getName(long id){
        single_param[0] = String.valueOf(id);
        Cursor c = db.rawQuery("SELECT name FROM main WHERE ID == ?", single_param);
        String name;
        if(c.getCount() != 0) {
            c.moveToPosition(0);
            name = c.getString(0);
        }else
            name = null;
        c.close();
        return name;
    }
    public long getParent(long id){
        single_param[0] = String.valueOf(id);
        Cursor c = db.rawQuery("SELECT parent FROM main WHERE ID == ?", single_param);
        long parent;
        if(c.getCount() != 0) {
            c.moveToPosition(0);
            parent = c.getLong(0);
        }else
            parent = -1;
        c.close();
        return parent;
    }
    public ElementType getType(long id){
        if(id == -1)
            return null;
        single_param[0] = String.valueOf(id);
        Cursor c = db.rawQuery("SELECT type FROM main WHERE ID == ?", single_param);
        int type;
        if(c.getCount() != 0) {
            c.moveToPosition(0);
            type = c.getInt(0);
        }else
            type = -1;
        c.close();
        if(type >= 0)
            return ElementType.values()[type];
        return null;
    }
    public String getContent(long id){
        single_param[0] = String.valueOf(id);
        Cursor c = db.rawQuery("SELECT content FROM main WHERE ID == ?", single_param);
        String content;
        if(c.getCount() != 0) {
            c.moveToPosition(0);
            content = c.getString(0);
        }else
            content = null;
        c.close();
        return content;
    }

    public synchronized long newElement(CreatorElement element) {
        //needs set: parent, name, type
        String enc_name = getFreeName(element.parent, element.getName());
        SQLiteStatement st = db.compileStatement("INSERT INTO main(parent, name, type, content) values(?, ?, ?, ?)");
        st.bindLong(1, element.parent);
        st.bindString(2, enc_name);
        st.bindLong(3, element.getType().ordinal());
        if(element.getType() == ElementType.FOLDER)
            st.bindNull(4);
        else
            st.bindString(4, "");
        long id = st.executeInsert();
        onNewItem(element.parent, id);
        return id;
    }
    public synchronized void updateElement(CreatorElement element) {
        //needs set: id, parent, name, type
        boolean updatedName = false;
        boolean updatedType = false;
        if (element.isNameChanged()) {
            SQLiteStatement st = db.compileStatement("UPDATE main SET name = ? WHERE ID == ?");
            st.bindString(1, getFreeName(element.parent, element.getName()));
            st.bindLong(2, element.id);
            st.execute();
            updatedName = true;
        }
        if (element.isTypeChanged()){
            SQLiteStatement st = db.compileStatement("UPDATE main SET type = ? WHERE ID = ?");
            st.bindLong(1, element.getType().ordinal());
            st.bindLong(2, element.id);
            st.execute();
            updatedType = true;
        }
        if(updatedName && element.getType() == ElementType.FOLDER) {
            onChangeItem(element.parent, element.id);
            onChangeName(element.parent);
        }else if(updatedName || updatedType) {
            onChangeItem(element.parent, element.id);
        }
    }
    public synchronized void updateParent(long id, long old_parent, long new_parent){
        SQLiteStatement st = db.compileStatement("UPDATE main SET parent = ? WHERE ID = ?");
        st.bindLong(1, new_parent);
        st.bindLong(2, id);
        st.execute();
        onDeleteView(old_parent, id);
        onNewItem(new_parent, id);
        onChangeName(new_parent);
    }
    public synchronized void deleteElement(long parent, long id) {
        SQLiteStatement st = db.compileStatement("DELETE FROM main WHERE ID == ?");
        st.bindLong(1, id);
        st.execute();
        onDeleteItem(parent, id);
    }
    private String getFreeName(long parent, String name_encoded) {
        String free_name = name_encoded;
        int idx = 0;
        while (true) {
            Cursor cursor = db.rawQuery("SELECT name FROM main WHERE parent == ? AND name == ?", new String[]{String.valueOf(parent), free_name});
            int count = cursor.getCount();
            cursor.close();
            if (count == 0)
                break;
            idx++;
            free_name = name_encoded + " (" + idx + ")";
        }
        return free_name;
    }
    public boolean isParentFor(long parent, long element){
        while(element != 0){
            single_param[0] = String.valueOf(element);
            Cursor c = db.rawQuery("SELECT parent FROM main WHERE ID == ?", single_param);
            if(c.getCount() == 0)
                return false;
            c.moveToPosition(0);
            element = c.getLong(0);
            c.close();
            if(element == parent)
                return true;
        }
        return false;
    }
    public String buildPath(long id){
        sb.setLength(0);
        while(id != 0) {
            single_param[0] = String.valueOf(id);
            Cursor c = db.rawQuery("SELECT name, parent FROM main WHERE ID == ?", single_param);
            int count = c.getCount();
            if(count == 0)
                return null;
            c.moveToPosition(0);
            sb.insert(0, "/" + c.getString(0));
            id = c.getLong(1);
            c.close();
        }
        return sb.toString();
    }
    public synchronized void updateTextData(DatabaseElement element){
        resetTextData(db, element.id, element.content);
        onChangeItem(element.parent, element.id);
    }
    public static void resetTextData(SQLiteDatabase database, long id, String content) {
        SQLiteStatement st = database.compileStatement("UPDATE main SET content = ? WHERE ID == ?");
        st.bindString(1, content);
        st.bindLong(2, id);
        st.execute();
    }
}
