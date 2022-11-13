package com.matvey.perelman.notepad2.database.connection;

import android.database.Cursor;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.matvey.perelman.notepad2.database.DatabaseElement;
import com.matvey.perelman.notepad2.utils.threads.Locker;

import java.util.concurrent.Exchanger;

public class DatabaseCursor implements IDListener {
    public final DatabaseConnection conn;
    private long path_id;

    public String path_t;

    private Cursor vis_files, new_data;
    private final ViewListener listener;
    private final AppCompatActivity context;
    private final Locker locker;

    private interface UIUpdate<T>{
        void run(T idx_from, T idx_to);
    }

    public DatabaseCursor(DatabaseConnection connection, ViewListener listener, AppCompatActivity context) {
        this.listener = listener;
        this.conn = connection;
        this.context = context;
        locker = new Locker();
    }

    public int length() {
        return vis_files.getCount();
    }

    public void getElement(DatabaseElement saveTo, int idx) {
        DatabaseConnection.getElement(vis_files, saveTo, path_id, idx);
    }
    private void uiEnterUI(long normal_id, long tmp){
        enterUI(normal_id);
    }
    public void enterUI(long normal_id) {
        path_id = normal_id;
        updatePath();
        reloadData();
        listener.onPathChanged();
    }

    public boolean backUI() {
        if (path_id == 0) {
            return true;
        } else {
            path_id = conn.getParent(path_id);
            updatePath();
            reloadData();
            listener.onPathChanged();
            return false;
        }
    }

    private void updatePath() {
        path_t = conn.buildPath(path_id);
    }
    private void load_new(){
        new_data = conn.getListFiles(path_id);
    }
    private void exchange(){
        close();
        vis_files = new_data;
        new_data = null;
    }

    private void reloadData(){
        load_new();
        exchange();
    }

    public void close() {
        if (vis_files != null && !vis_files.isClosed())
            vis_files.close();
    }

    private int indexOf(Cursor c, long id) {
        for (int i = 0; i < c.getCount(); ++i) {
            c.moveToPosition(i);
            if (c.getLong(0) == id)
                return i;
        }
        return -1;
    }

    @Override
    public void onNewItem(long id) {
        load_new();
        int idx_to = indexOf(new_data, id);
        if(idx_to == -1)
            return;
        uiUpdate(-1, idx_to, this::uiOnNewItem);
    }
    @Override
    public void onDeleteItem(long id) {
        load_new();
        int idx_from = indexOf(vis_files, id);
        uiUpdate(idx_from, -1, this::uiOnDeleteItem);
    }
    @Override
    public void onChangeItem(long id) {
        load_new();
        int idx_from = indexOf(vis_files, id);
        int idx_to = indexOf(new_data, id);
        if(idx_from != -1 && idx_to != -1)
            uiUpdate(idx_from, idx_to, this::uiOnChangeItem);
    }
    @Override
    public void onPathRenamed() {
        uiUpdate(-1, -1, this::uiUpdatePath);
    }
    @Override
    public void onPathChanged(long to_id) {
        uiUpdate(to_id, -1L, this::uiEnterUI);
    }

    private void uiUpdatePath(int tmp1, int tmp2){
        updatePath();
        listener.onPathRenamed();
    }

    private <T> void uiUpdate(T idx_from, T idx_to, UIUpdate<T> func){
        if(Thread.currentThread() == Looper.getMainLooper().getThread()){
            func.run(idx_from, idx_to);
        }else {
            context.runOnUiThread(() -> {
                func.run(idx_from, idx_to);
                locker.free();
            });
            locker.lock();
        }
    }

    private void uiOnNewItem(int idx_from, int idx_to){
        exchange();
        listener.onNewItem(idx_to);
    }

    private void uiOnDeleteItem(int idx_from, int idx_to){
        exchange();
        listener.onDeleteItem(idx_from);
    }

    private void uiOnChangeItem(int idx_from, int idx_to){
        exchange();
        if (idx_from == -1 && idx_to == -1)
            return;
        if (idx_to == -1) {
            listener.onDeleteItem(idx_from);
            return;
        }
        if (idx_from == -1) {
            listener.onNewItem(idx_to);
            return;
        }
        listener.onChangeItem(idx_from, idx_to);
    }
    @Override
    public long getPathID() {
        return path_id;
    }
}
