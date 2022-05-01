package com.matvey.perelman.notepad2.database.connection;

public interface ViewListener {
    void onNewItem(int idx);
    void onDeleteItem(int idx);
    void onChangeItem(int idx_start, int idx_end);
    void onPathRenamed();
    void onPathChanged();
}
