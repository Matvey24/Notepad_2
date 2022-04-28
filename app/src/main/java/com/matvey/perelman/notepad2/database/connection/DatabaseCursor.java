package com.matvey.perelman.notepad2.database.connection;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.matvey.perelman.notepad2.creator.CreatorElement;
import com.matvey.perelman.notepad2.database.DatabaseElement;
import com.matvey.perelman.notepad2.list.ElementType;

import java.util.ArrayList;

import static com.matvey.perelman.notepad2.utils.StringEncoder.decode;
import static com.matvey.perelman.notepad2.utils.StringEncoder.encode;

public class DatabaseCursor {
    public final DatabaseConnection connection;
    public int db_file_index;
    private Cursor cursor;
    public String search_param;
    public final ArrayList<String> path;
    private final ArrayList<Integer> ids;

    public DatabaseCursor(DatabaseConnection connection) {
        this.connection = connection;
        path = new ArrayList<>();
        ids = new ArrayList<>();
        db_file_index = -1;
        search_param = "";
    }

    public int layer() {
        return ids.size();
    }

    public int length() {
        return cursor.getCount();
    }

    public void enter(int idx) {
        cursor.moveToPosition(idx);
        String name = cursor.getString(1);
        int element_id = cursor.getInt(0);
        path.add(name);
        if (layer() == 0) {
            db_file_index = connection.enter_file(element_id, encode(name));
            ids.add(0);
        } else
            ids.add(element_id);
    }

    public boolean back() {
        if (layer() == 0) {
            return true;
        } else {
            path.remove(layer() - 1);
            ids.remove(layer() - 1);
        }
        if (layer() == 0) {
            int idx = db_file_index;
            db_file_index = -1;
            connection.exitFile(idx);
        }
        return false;
    }

    public void getElement(DatabaseElement saveTo, int idx) {
        cursor.moveToPosition(idx);
        saveTo.id = cursor.getInt(0);
        saveTo.name = decode(cursor.getString(1));
        if (layer() == 0) {
            saveTo.type = ElementType.FOLDER;
            saveTo.content = "";
        } else {
            saveTo.type = ElementType.values()[cursor.getInt(3)];
            saveTo.content = decode(cursor.getString(2));
        }
    }
    public int getElementIdx(String name) {
        for (int i = 0; i < cursor.getCount(); ++i) {
            cursor.moveToPosition(i);
            if (decode(cursor.getString(1)).equals(name)) {
                return i;
            }
        }
        return -1;
    }
    public int getElementId(int idx){
        cursor.moveToPosition(idx);
        return cursor.getInt(0);
    }
    public ElementType getType(int idx){
        if(layer() == 0)
            return ElementType.FOLDER;
        cursor.moveToPosition(idx);
        return ElementType.values()[cursor.getInt(3)];
    }
    public boolean isFolder(int idx){
        return getType(idx) == ElementType.FOLDER;
    }
    public void reloadData() {
        close();
        if (layer() == 0) {
            cursor = connection.db_collection.rawQuery(
                    String.format(
                            "select * from info where name like '%s%%' order by name",
                            encode(search_param)), null);
        } else {
            cursor = db_file().rawQuery(
                    String.format(
                            "select * from files where (parent %s) and (name like '%s%%') order by name",
                            getSelectionParentId(),
                            encode(search_param)), null);
        }
    }

    public void updateElement(CreatorElement element) {
        if (!element.isChanged())
            return;
        String name_enc;
        if (element.isNameChanged()) {
            name_enc = getFreeName(encode(element.getName()));
            if (layer() == 0) {
                connection.db_collection.execSQL(String.format("UPDATE info set name = '%s' where id == %s", name_enc, "" + element.id));
                connection.renameDatabase(element.id, encode(element.getNameStart()), name_enc);
            } else {
                db_file().execSQL(String.format("UPDATE files set name = '%s' where id == %s", name_enc, "" + element.id));
            }
        }
        if (element.isTypeChanged())
            db_file().execSQL(String.format("UPDATE files set type = %s where id == %s", "" + element.getType().ordinal(), "" + element.id));
    }
    public void updateTextData(DatabaseElement element){
        resetTextData(db_file(), element.id, element.content);
    }
    public static void resetTextData(SQLiteDatabase database, int id, String content) {
        database.execSQL(String.format("UPDATE files set content = '%s' where id = %s", encode(content), id));
    }

    public int newElement(CreatorElement element) {
        String enc_name = getFreeName(encode(element.getName()));
        int id;
        Cursor c;
        if (layer() == 0) {
            connection.db_collection.execSQL(String.format("insert into info (name) values ('%s')", enc_name));
            c = connection.db_collection.rawQuery(String.format("select id from info where name == '%s'", enc_name), null);
        } else {
            db_file().execSQL(String.format("insert into files (name, content, type, parent) values ('%s', '', %s, %s)", enc_name, "" + element.getType().ordinal(), getParentID()));
            c = db_file().rawQuery(String.format("select id from files where name == '%s' and parent %s", enc_name, getSelectionParentId()), null);
        }
        c.moveToPosition(0);
        id = c.getInt(0);
        c.close();
        return id;
    }
    public void update_parent(int id, int new_parent_id){
        if(new_parent_id == -1)
            db_file().execSQL(String.format("UPDATE files set parent = NULL where id = %s", id));
        else
            db_file().execSQL(String.format("UPDATE files set parent = %s where id = %s", new_parent_id, id));
    }
    public void deleteElement(int id) {
        if (layer() == 0) {
            Cursor c = connection.db_collection.rawQuery("select * from info where id == " + id, null);
            c.moveToPosition(0);
            connection.deleteDatabase(encode(c.getString(1)), id);
            c.close();
        } else {
            deleteFile(id);
        }
    }

    private void deleteFile(int id) {
        ids.add(id);
        Cursor c = db_file().rawQuery(
                String.format(
                        "select * from files where (parent %s)",
                        getSelectionParentId()), null);
        if (c.getCount() != 0)
            for (int i = c.getCount() - 1; i >= 0; --i) {
                c.moveToPosition(i);
                deleteFile(c.getInt(0));
            }
        c.close();
        ids.remove(ids.size() - 1);
        db_file().execSQL("delete from files where id == " + id);
    }

    public void close() {
        if (cursor != null && !cursor.isClosed())
            cursor.close();
    }

    private String getFreeName(String name_encoded) {
        String free_name = name_encoded;
        int idx = 0;
        while (true) {
            Cursor cursor;
            if (layer() == 0)
                cursor = connection.db_collection.rawQuery(String.format("select name from info where name == '%s'", free_name), null);
            else
                cursor = db_file().rawQuery(String.format("select name from files where ((parent %s) and (name == '%s'))", getSelectionParentId(), free_name), null);
            int count = cursor.getCount();
            cursor.close();
            if (count == 0)
                break;
            idx++;
            free_name = name_encoded + " (" + idx + ")";
        }
        return free_name;
    }

    private String getSelectionParentId() {
        if (ids.size() <= 1)
            return "is NULL";
        return "== " + ids.get(ids.size() - 1);
    }

    private String getParentID() {
        if (ids.size() <= 1)
            return "NULL";
        return "" + ids.get(ids.size() - 1);
    }

    private SQLiteDatabase db_file() {
        return connection.db_file_list.get(db_file_index);
    }

    public int indexOf(int id) {
        for (int i = 0; i < cursor.getCount(); ++i) {
            cursor.moveToPosition(i);
            if (cursor.getInt(0) == id) {
                return i;
            }
        }
        return -1;
    }

    public boolean equalPath(DatabaseCursor other) {
        return this == other || path.equals(other.path);
    }

    public void copyPath(DatabaseCursor other) {
        path.clear();
        path.addAll(other.path);
        ids.clear();
        ids.addAll(other.ids);
        int idx = db_file_index;
        db_file_index = other.db_file_index;
        if (idx != db_file_index && idx != -1)
            connection.exitFile(idx);
    }
    public void setRootPath(){
        path.clear();
        ids.clear();
        int idx = db_file_index;
        db_file_index = -1;
        if(idx != -1)
            connection.exitFile(idx);
    }
}
