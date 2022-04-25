package com.matvey.perelman.notepad2.database.callback;

import com.matvey.perelman.notepad2.creator.CreatorElement;
import com.matvey.perelman.notepad2.database.connection.DatabaseConnection;
import com.matvey.perelman.notepad2.database.connection.DatabaseCursor;
import com.matvey.perelman.notepad2.database.DatabaseElement;

public class CursorUpdatable {
    public final DatabaseCursor c;
    private final ViewListener listener;
    private final ViewUpdater updater;
    private final StringBuilder sb;
    public String path;
    public CursorUpdatable(DatabaseConnection connection, ViewListener listener) {
        this.listener = listener;
        this.updater = connection.updater;
        updater.updatables.add(this);
        c = connection.makeCursor();
        reloadData();
        sb = new StringBuilder();
    }

    public void updatePath() {
        sb.setLength(0);
        for (String name : c.path) {
            sb.append("/");
            sb.append(name);
        }
        path = sb.toString();
        if(listener != null) {
            listener.onPathChanged();
        }
    }
    public int length(){
        return c.length();
    }
    public int layer(){
        return c.layer();
    }
    public void close() {
        updater.updatables.remove(this);
        c.connection.deleteCursor(c);
    }

    public void getElement(DatabaseElement saveTo, int idx) {
        c.getElement(saveTo, idx);
    }
    public void searchParamChanged(String search_param){
        c.search_param = search_param;
        c.reloadData();
        listener.onPathChanged();
    }
    public void reloadData() {
        c.reloadData();
    }

    public void enter(int idx) {
        c.enter(idx);
        c.reloadData();
        updatePath();
    }

    public boolean back() {
        boolean b = c.back();
        if(!b) {
            c.reloadData();
            updatePath();
        }
        return b;
    }

    public void newElement(CreatorElement element) {
        int id = c.newElement(element);
        updater.onNewItem(this, id);
    }
    public void updateElement(CreatorElement element) {
        c.updateElement(element);
        updater.onChangeItem(this, element.id);
    }
    public void deleteElement(int id) {
        c.deleteElement(id);
        updater.onDeleteItem(this, id);
    }

    void onNewItem(int id){
        if(listener == null)
            return;
        c.reloadData();
        int idx = c.indexOf(id);
        if(idx != -1)
            listener.onNewItem(idx);
    }
    void onChangeItem(int id){
        if(listener == null)
            return;
        int idx1 = c.indexOf(id);
        c.reloadData();
        int idx2 = c.indexOf(id);
        if(idx1 == -1 && idx2 == -1)
            return;
        if(idx1 == -1) {
            listener.onNewItem(idx2);
            return;
        }
        if(idx2 == -1) {
            listener.onDeleteItem(idx1);
            return;
        }
        listener.onChangeItem(idx1, idx2);
    }
    void onDeleteItem(int id){
        if(listener == null)
            return;
        int idx = c.indexOf(id);
        if(idx != -1) {
            reloadData();
            listener.onDeleteItem(idx);
        }
    }
}
