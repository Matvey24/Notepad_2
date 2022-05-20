package com.matvey.perelman.notepad2.executor;

import com.matvey.perelman.notepad2.MainActivity;
import com.matvey.perelman.notepad2.R;
import com.matvey.perelman.notepad2.database.DatabaseElement;
import com.matvey.perelman.notepad2.executor.Executor;
import com.matvey.perelman.notepad2.list.ElementType;

import java.util.ArrayList;

public class PythonAPI {
    public MainActivity activity;
    public Executor executor;

    public void toast(String text){
        activity.makeToast(text, false);
    }
    public void toast_l(String text){
        activity.makeToast(text, true);
    }
    public void touch(String tpath){
        String file = executor.cdGoEntry(executor.parsePath(tpath), true);
        executor.touch(file);
    }
    public String read(String fpath){
        String file = executor.cdGoEntry(executor.parsePath(fpath), false);
        return executor.read(file);
    }
    public void write(String fpath, String content){
        String file = executor.cdGoEntry(executor.parsePath(fpath), true);
        executor.write(file, content);
    }
    public void script(String fpath, boolean mode){
        String file = executor.cdGoEntry(executor.parsePath(fpath), true);
        executor.script(file, mode);
    }
    public void mkdir(String dpath){
        String dir = executor.cdGoEntry(executor.parsePath(dpath), true);
        executor.mkdir(dir);
    }
    public boolean delete(String epath){
        String entry = executor.cdGoEntry(executor.parsePath(epath), false);
        return executor.delete(entry);
    }
    public String path(){
        return executor.getPath();
    }
    public String script_name(){return executor.getScriptName();}
    public String get_name(String path){
        return executor.getName(path);
    }
    public void rename(String epath, String name){
        String entry = executor.cdGoEntry(executor.parsePath(epath), false);
        switch (executor.rename(entry, name)){
            case 1:
                throw new RuntimeException(activity.getString(R.string.error_bad_path));
            case 2:
                throw new RuntimeException(activity.getString(R.string.error_rename_exists));
        }
    }
    public String path_concat(String path1, String path2){
        return Executor.path_concat(path1, path2);
    }
    public boolean exists(String path){
        String entry = executor.cdGoEntry(executor.parsePath(path), false);
        return executor.exists(entry);
    }
    public boolean is_folder(String path){
        return get_type(path) == ElementType.FOLDER;
    }
    public boolean is_script(String path){
        return get_type(path) == ElementType.SCRIPT;
    }
    public ElementType get_type(String path){
        String entry = executor.cdGoEntry(executor.parsePath(path), false);
        return executor.getType(entry);
    }
    public ArrayList<DatabaseElement> list_files(String dpath){
        executor.cdGo(executor.parsePath(dpath), false);
        return executor.listFiles();
    }
    public void move(String entry_cut, String path_paste){
        executor.move(entry_cut, path_paste);
    }
    public String input(String input_name){
        return activity.showInputDialog(input_name);
    }

}
