package com.matvey.perelman.notepad2.list;

import com.chaquo.python.PyObject;
import com.matvey.perelman.notepad2.creator.CreatorElement;
import com.matvey.perelman.notepad2.database.callback.CursorUpdatable;
import com.matvey.perelman.notepad2.database.DatabaseElement;

import java.util.ArrayList;

public class Executor implements AutoCloseable {
    private final CreatorElement celement;
    private final DatabaseElement delement;
    private PyObject pyapi;
    private CursorUpdatable cursor;
    private CursorUpdatable path;
    public Executor() {
        celement = new CreatorElement();
        delement = new DatabaseElement();
    }

    public void setPython(PyObject p){
        this.pyapi = p;
    }
    public void begin(CursorUpdatable path, int file_idx) {
        cursor = new CursorUpdatable(path.c.connection, null);
        this.path = path;

        path.getElement(delement, file_idx);
        try {
            pyapi.callAttr("run", delement.content);
        }catch (Throwable t){
            PythonAPI.toast_l(t.getMessage());
        }
    }

    public String getPath(){
        return path.path;
    }

    public String cdGo(String path){
        String[] arr = path.split("/");
        int i = 0;
        if(arr[0].trim().isEmpty()) {
            cursor.c.setRootPath();
            i = 1;
        }else
            cursor.c.copyPath(this.path.c);

        cursor.updatePath();
        cursor.reloadData();

        int len = arr.length - 1;
        if(arr[len].trim().isEmpty())
            len--;

        for(; i < len; ++i){
            String s = arr[i].trim();
            if(cd(cursor, s))
                return null;
        }
        return arr[len].trim();
    }
    private void defnewFile(String file, boolean exec){
        celement.setName(file);
        celement.setType(exec? ElementType.EXECUTABLE : ElementType.TEXT);
        cursor.newElement(celement);
        cursor.reloadData();
    }
    public void touch(String path){
        int idx = cursor.getElementIdx(path);
        if(idx == -1) {
            if(cursor.layer() == 0)
                throw new RuntimeException("Could not make file in root directory");
            defnewFile(path, false);
        }
    }
    private void defnewDir(String dir){
        celement.setName(dir);
        celement.setType(ElementType.FOLDER);
        cursor.newElement(celement);
        cursor.reloadData();
    }
    public void mkdir(String dir){
        int idx = cursor.getElementIdx(dir);
        if(idx == -1)
            defnewDir(dir);
        else if(!cursor.c.isFolder(idx))
            throw new RuntimeException("Could not mkdir, file with the same name exists: " + dir);
    }

    public boolean delete(String entry){
        int idx = cursor.getElementIdx(entry);
        if(idx == -1){
            return false;
        }else {
            cursor.deleteElement(cursor.getElementId(idx));
            cursor.reloadData();
            return true;
        }
    }

    public void write(String file, String content){
        int idx = cursor.getElementIdx(file);
        if(idx == -1){
            if(cursor.layer() == 0)
                throw new RuntimeException("Could not make file in root directory");
            defnewFile(file, false);
            idx = cursor.getElementIdx(file);
        }else if(cursor.c.isFolder(idx)){
            throw new RuntimeException("Could not write to folder: " + file);
        }
        delement.id = cursor.getElementId(idx);
        delement.content = content;
        cursor.updateTextData(delement);
        cursor.reloadData();
    }

    public String read(String file){
        int idx = cursor.getElementIdx(file);
        if(idx == -1 || cursor.c.isFolder(idx))
            throw new RuntimeException("Could not read: file " + file + " does not exist");
        cursor.getElement(delement, idx);
        return delement.content;
    }

    public void executable(String file, boolean mode){
        int idx = cursor.getElementIdx(file);
        if(idx == -1){
            if(cursor.layer() == 0)
                throw new RuntimeException("Could not make file in root directory");
            defnewFile(file, mode);
        }else{
            celement.id = cursor.getElementId(idx);
            celement.setName(file);
            celement.setType(cursor.c.getType(idx));
            celement.updateType(mode?ElementType.EXECUTABLE:ElementType.TEXT);
            cursor.updateElement(celement);
            cursor.reloadData();
        }
    }

    public boolean cd(CursorUpdatable curs, String dir){
        if(dir.equals("."))
            return false;
        if(dir.equals("..")){
            if (curs.layer() != 0)
                curs.back();
            return false;
        }
        int idx = curs.getElementIdx(dir);
        if(idx == -1){
            defnewDir(dir);
            idx = curs.getElementIdx(dir);
        }
        if(!curs.c.isFolder(idx))
            return true;
        curs.enter(idx);
        return false;
    }

    public ArrayList<DatabaseElement> listFiles(String dir){
        if(cd(cursor, dir))
            return null;
        ArrayList<DatabaseElement> list = new ArrayList<>();
        for(int i = 0; i < cursor.length(); ++i){
            DatabaseElement de = new DatabaseElement();
            cursor.getElement(de, i);
            list.add(de);
        }
        return list;
    }

    @Override
    public void close() {
        cursor.close();
    }
}
