package com.matvey.perelman.notepad2.database.connection;

import android.database.Cursor;

import androidx.appcompat.app.AppCompatActivity;

import com.matvey.perelman.notepad2.database.DatabaseElement;
import com.matvey.perelman.notepad2.list.ElementType;

import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;

public class DatabaseCursor implements IDListener {
    public final DatabaseConnection conn;
    private long path_id;

    public String path_t;

    private Cursor vis_files;
    private final ViewListener listener;
    private final AppCompatActivity context;
    private final CyclicBarrier cb;

    public DatabaseCursor(DatabaseConnection connection, ViewListener listener, AppCompatActivity context) {
        this.listener = listener;
        this.conn = connection;
        this.context = context;
        cb = new CyclicBarrier(2);
        setRootPath();
    }

    public int length() {
        return vis_files.getCount();
    }

    public void getElement(DatabaseElement saveTo, int idx) {
        conn.getElement(vis_files, saveTo, path_id, idx);
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

    public void reloadData() {
        close();
        vis_files = conn.getListFiles(path_id);
    }

    public void close() {
        if (vis_files != null && !vis_files.isClosed())
            vis_files.close();
    }

    public int indexOf(long id) {
        for (int i = 0; i < vis_files.getCount(); ++i) {
            vis_files.moveToPosition(i);
            if (vis_files.getLong(0) == id)
                return i;
        }
        return -1;
    }

    public boolean equalPath(DatabaseCursor other) {
        return this == other || path_id == other.path_id;
    }

    public void setRootPath() {
        path_id = 0;
        updatePath();
        reloadData();
    }

    @Override
    public void onNewItem(long id) {
        context.runOnUiThread(() -> {
            reloadData();
            int idx = indexOf(id);
            if (idx != -1)
                listener.onNewItem(idx);
            await();
        });
        await();
    }

    @Override
    public void onDeleteItem(long id) {
        context.runOnUiThread(() -> {
            int idx = indexOf(id);
            if (idx == -1)
                return;
            reloadData();
            if (indexOf(id) == -1)
                listener.onDeleteItem(idx);
            await();
        });
        await();
    }

    @Override
    public void onChangeItem(long id) {
        context.runOnUiThread(() -> {
            int idx1 = indexOf(id);
            reloadData();
            int idx2 = indexOf(id);
            if (idx1 == -1 && idx2 == -1)
                return;
            if (idx2 == -1) {
                listener.onDeleteItem(idx1);
                return;
            }
            if (idx1 == -1) {
                listener.onNewItem(idx2);
                return;
            }
            listener.onChangeItem(idx1, idx2);
            await();
        });
        await();
    }

    @Override
    public void onPathRenamed() {
        context.runOnUiThread(() -> {
            updatePath();
            listener.onPathRenamed();
        });
    }

    private void await() {
        try {
            cb.await();
        } catch (Exception ignored) {
        }
    }

    @Override
    public long getPathID() {
        return path_id;
    }
}
