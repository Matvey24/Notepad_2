package com.matvey.perelman.notepad2.list;

import com.chaquo.python.PyObject;
import com.matvey.perelman.notepad2.creator.CreatorElement;
import com.matvey.perelman.notepad2.database.callback.CursorUpdatable;
import com.matvey.perelman.notepad2.database.DatabaseElement;

public class Executor implements AutoCloseable {
    private final CreatorElement celement;
    private final DatabaseElement delement;
    private PyObject pyapi;
    private CursorUpdatable cursor;

    public Executor() {
        celement = new CreatorElement();
        delement = new DatabaseElement();
    }

    public void setPython(PyObject p){
        this.pyapi = p;
    }
    public void begin(CursorUpdatable path, int file_idx) {
        cursor = new CursorUpdatable(path.c.connection, null);
        cursor.c.copyPath(path.c);
        cursor.updatePath();
        cursor.reloadData();

        cursor.getElement(delement, file_idx);
        try {
            pyapi.callAttr("run", delement.content);
        }catch (Throwable t){
            PythonAPI.toastL(t.getMessage());
        }
    }

    public String getPath(){
        return cursor.path;
    }
    private void defnewFile(String file, boolean exec){
        celement.setName(file);
        celement.setType(exec? ElementType.EXECUTABLE : ElementType.TEXT);
        cursor.newElement(celement);
        cursor.reloadData();
    }
    public void touch(String tfile){
        int idx = cursor.getElementIdx(tfile);
        if(idx == -1)
            defnewFile(tfile, false);
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
            throw new RuntimeException("File with the same name exists: " + dir);
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
            throw new RuntimeException("File " + file + " does not exist");
        cursor.getElement(delement, idx);
        return delement.content;
    }

    public void executable(String file, boolean mode){
        int idx = cursor.getElementIdx(file);
        if(idx == -1){
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

    public String cd(String dir){
        if(dir.equals("."))
            return ".";
        if(dir.equals("..")){
            if(cursor.layer() == 0){
                return ".";
            }else{
                String s = cursor.c.path.get(cursor.layer() - 1);
                cursor.back();
                return s;
            }
        }
        int idx = cursor.getElementIdx(dir);
        if(idx == -1){
            defnewDir(dir);
            idx = cursor.getElementIdx(dir);
        }
        if(!cursor.c.isFolder(idx))
            throw new RuntimeException("Couldn't enter file: " + dir);
        cursor.enter(idx);
        return "..";
    }

    public void listFiles(){

    }

    @Override
    public void close() {
        cursor.close();
    }
}
