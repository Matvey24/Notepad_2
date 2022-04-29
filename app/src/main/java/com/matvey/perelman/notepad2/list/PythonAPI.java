package com.matvey.perelman.notepad2.list;

import com.matvey.perelman.notepad2.MainActivity;
import com.matvey.perelman.notepad2.database.DatabaseElement;

import java.util.ArrayList;

public class PythonAPI {
    public static MainActivity activity;
    public static Executor executor;

    public static void toast(String text){
        activity.makeToast(text, false);
    }
    public static void toast_l(String text){
        activity.makeToast(text, true);
    }
    private static void checkPath(String end_path){
        if(end_path == null)
            throw new RuntimeException("Nonexistent path");
    }
    public static void touch(String tpath){
        String file = executor.cdGo(executor.parsePath(tpath), true);
        checkPath(file);
        executor.touch(file);
    }
    public static void mkdir(String dpath){
        String dir = executor.cdGo(executor.parsePath(dpath), true);
        checkPath(dir);
        executor.mkdir(dir);
    }
    public static boolean delete(String epath){
        String entry = executor.cdGo(executor.parsePath(epath), false);
        checkPath(entry);
        return executor.delete(entry);
    }
    public static void write(String fpath, String content){
        String file = executor.cdGo(executor.parsePath(fpath), true);
        checkPath(file);
        executor.write(file, content);
    }
    public static String read(String fpath){
        String file = executor.cdGo(executor.parsePath(fpath), false);
        checkPath(file);
        return executor.read(file);
    }
    public static void executable(String fpath, boolean mode){
        String file = executor.cdGo(executor.parsePath(fpath), true);
        checkPath(file);
        executor.executable(file, mode);
    }
    public static ArrayList<DatabaseElement> list_files(String dpath){
        String dir = executor.cdGo(executor.parsePath(dpath), false);
        checkPath(dir);
        return executor.listFiles(dir);
    }
    public static String path_concat(String path1, String path2){
        return executor.path_concat(path1, path2);
    }
    public static boolean exists(String path){
        String entry = executor.cdGo(executor.parsePath(path), false);
        checkPath(entry);
        return executor.exists(entry);
    }
    public static boolean is_folder(String path){
        String entry = executor.cdGo(executor.parsePath(path), false);
        checkPath(entry);
        return executor.isDir(entry);
    }
    public static boolean is_executable(String path){
        String entry = executor.cdGo(executor.parsePath(path), false);
        checkPath(entry);
        return executor.isExecutable(entry);
    }
    public static String get_path(){
        return executor.getPath();
    }
    public static String get_name(String path){
        return executor.getName(path);
    }
    public static void move(String entry_cut, String path_paste){
        executor.move(entry_cut, path_paste);
    }
}
