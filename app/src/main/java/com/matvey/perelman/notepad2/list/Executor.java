package com.matvey.perelman.notepad2.list;

import com.matvey.perelman.notepad2.MainActivity;
import com.matvey.perelman.notepad2.creator.CreatorElement;
import com.matvey.perelman.notepad2.database.callback.CursorUpdatable;
import com.matvey.perelman.notepad2.database.DatabaseElement;

import java.util.ArrayList;
import java.util.Arrays;

public class Executor implements AutoCloseable {
    private final MainActivity act;
    private final CreatorElement celement;
    private final DatabaseElement delement;

    private CursorUpdatable cursor;

    public Executor(MainActivity act) {
        this.act = act;
        celement = new CreatorElement();
        delement = new DatabaseElement();
    }

    public void begin(CursorUpdatable path, int file_idx) {
        cursor = new CursorUpdatable(cursor.c.connection, null);
        cursor.c.copyPath(path.c);
        cursor.updatePath();
        cursor.reloadData();

        cursor.getElement(delement, file_idx);


    }

    @Override
    public void close() {
        cursor.close();
    }
}
