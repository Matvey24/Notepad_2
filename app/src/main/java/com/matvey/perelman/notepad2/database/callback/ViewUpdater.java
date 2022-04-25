package com.matvey.perelman.notepad2.database.callback;

import java.util.ArrayList;

public class ViewUpdater {
    public ArrayList<CursorUpdatable> updatables;
    public ViewUpdater(){
        updatables = new ArrayList<>();
    }
    public void onNewItem(CursorUpdatable updatable, int id){
        for(CursorUpdatable c: updatables){
            if(c.c.equalPath(updatable.c))
                c.onNewItem(id);
        }
    }
    public void onChangeItem(CursorUpdatable updatable, int id){
        for(CursorUpdatable c: updatables){
            if(c.c.equalPath(updatable.c))
                c.onChangeItem(id);
        }
    }
    public void onDeleteItem(CursorUpdatable updatable, int id){
        for(CursorUpdatable c: updatables){
            if(c.c.equalPath(updatable.c))
                c.onDeleteItem(id);
        }
    }
}
