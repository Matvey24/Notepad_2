package com.matvey.perelman.notepad2.list;

import com.matvey.perelman.notepad2.MainActivity;

import java.util.ArrayList;

public class PythonAPI {
    public static MainActivity activity;
    public static Executor executor;

    public static void toast(String text){
        activity.makeToast(text, true);
    }
    private static ArrayList<String> normPath(String path){
        String[] arr = path.split("/");
        ArrayList<String> list = new ArrayList<>();
        for(String s: arr)
            list.add(s.replaceAll("[ \n\t\r]", ""));
        return list;
    }
    private static void cdGoReverse(ArrayList<String> path){
        for(int i = 0; i < path.size() - 1; ++i)
            path.set(i, executor.cd(path.get(i)));
    }
    public static void touch(String tpath){
        ArrayList<String> path = normPath(tpath);
        cdGoReverse(path);
        executor.touch(path.get(path.size() - 1));
        cdGoReverse(path);
    }
    public static void mkdir(String dpath){
        ArrayList<String> path = normPath(dpath);
        cdGoReverse(path);
        executor.mkdir(path.get(path.size() - 1));
        cdGoReverse(path);
    }
    public static void delete(String epath){
        ArrayList<String> path = normPath(epath);
        cdGoReverse(path);
        executor.delete(path.get(path.size() - 1));
        cdGoReverse(path);
    }
    public static void write(String fpath, String content){
        ArrayList<String> path = normPath(fpath);
        cdGoReverse(path);
        executor.write(path.get(path.size() - 1), content);
        cdGoReverse(path);
    }
    public static String read(String fpath){
        ArrayList<String> path = normPath(fpath);
        cdGoReverse(path);
        String content = executor.read(path.get(path.size() - 1));
        cdGoReverse(path);
        return content;
    }
    public static void executable(String fpath, boolean mode){
        ArrayList<String> path = normPath(fpath);
        cdGoReverse(path);
        executor.executable(path.get(path.size() - 1), mode);
        cdGoReverse(path);
    }
    public static void cd(String dpath){
        cdGoReverse(normPath(dpath));
    }
    public static void listFiles(){

    }

}
