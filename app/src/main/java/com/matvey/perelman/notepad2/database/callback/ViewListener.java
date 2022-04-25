package com.matvey.perelman.notepad2.database.callback;

public interface ViewListener {
    void onNewItem(int idx);
    void onDeleteItem(int idx);
    void onChangeItem(int idx_start, int idx_end);
    void onPathChanged();
}
