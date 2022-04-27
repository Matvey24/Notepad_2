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
            throw new RuntimeException("Path contains file instead of folder");
    }
    public static void touch(String tpath){
        String file = executor.cdGo(tpath);
        checkPath(file);
        executor.touch(file);
    }
    public static void mkdir(String dpath){
        String dir = executor.cdGo(dpath);
        checkPath(dir);
        executor.mkdir(dir);
    }
    public static boolean delete(String epath){
        String entry = executor.cdGo(epath);
        checkPath(entry);
        return executor.delete(entry);
    }
    public static void write(String fpath, String content){
        String file = executor.cdGo(fpath);
        checkPath(file);
        executor.write(file, content);
    }
    public static String read(String fpath){
        String file = executor.cdGo(fpath);
        checkPath(file);
        return executor.read(file);
    }
    public static void executable(String fpath, boolean mode){
        String file = executor.cdGo(fpath);
        checkPath(file);
        executor.executable(file, mode);
    }
    public static ArrayList<DatabaseElement> list_files(String dpath){
        String dir = executor.cdGo(dpath);
        checkPath(dir);
        return executor.listFiles(dir);
    }
    public static String path_concat(String path1, String path2){
        path1 = path1.trim();
        path2 = path2.trim();
        if(path1.endsWith("/")){
            if(path2.startsWith("/")){
                return path1 + path2.substring(1);
            }else{
                return path1 + path2;
            }
        }else{
            if(path2.startsWith("/")){
                return path1 + path2;
            }else{
                return path1 + "/" + path2;
            }
        }
    }
    public static String get_path(){
        return executor.getPath();
    }
}
