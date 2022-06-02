package com.matvey.perelman.notepad2.database.connection;

public interface IDListener {
    void onNewItem(long id);
    void onDeleteItem(long id);
    void onChangeItem(long id);
    void onPathRenamed();
    void onPathChanged(long to_id);
    long getPathID();
}
