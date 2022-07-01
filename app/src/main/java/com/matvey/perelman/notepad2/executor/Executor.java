package com.matvey.perelman.notepad2.executor;

import android.database.Cursor;

import androidx.annotation.StringRes;

import com.chaquo.python.PyObject;
import com.matvey.perelman.notepad2.MainActivity;
import com.matvey.perelman.notepad2.R;
import com.matvey.perelman.notepad2.creator.CreatorElement;
import com.matvey.perelman.notepad2.database.DatabaseElement;
import com.matvey.perelman.notepad2.database.connection.DatabaseConnection;
import com.matvey.perelman.notepad2.list.ElementType;

import java.util.ArrayList;

public class Executor implements AutoCloseable {
    private final MainActivity activity;
    private final PyObject py_executor;
    private final DatabaseConnection conn;

    private final CreatorElement celement;
    private final DatabaseElement delement;

    private String filepath;
    private long vis_folder;
    private long curr_path;

    public Executor(DatabaseConnection connection, MainActivity activity, PyObject api, PyObject space) {
        this.conn = connection;
        this.activity = activity;
        celement = new CreatorElement();
        delement = new DatabaseElement();
        py_executor = api.callAttr("__java_api_make_executor", this, activity, space);
    }

    public void begin(String path){
        vis_folder = 0;
        String entry = cdGoEntry(path, parsePath(path), false);
        long id = conn.getID(curr_path, entry);
        if(id != -1 && conn.getType(id) != ElementType.FOLDER) {
            begin(path, id, curr_path);
        }else
            throw new RuntimeException(getString(R.string.error_run_nofile, path));
    }
    public void begin(String filepath, long id, long parent) {
        String content = conn.getContent(id);
        this.filepath = filepath;
        vis_folder = parent;
        py_executor.callAttr("run_code", filepath, content);
    }

    public void makeDatabase(String json) {
        vis_folder = 0;
        py_executor.callAttr("from_json", json, "/");
    }
    @SuppressWarnings("UnusedDeclaration")
    public String getScriptPath() {
        return filepath;
    }
    public String getPath() {
        return conn.buildPath(vis_folder);
    }
    @SuppressWarnings("UnusedDeclaration")
    public String getScriptName(){
        return getName(filepath);
    }
    public static ArrayList<String> parsePath(String path) {
        if (path.trim().equals("/")) {
            ArrayList<String> list = new ArrayList<>();
            list.add(path);
            return list;
        }
        String[] arr = path.split("/");
        ArrayList<String> list = new ArrayList<>();
        if (arr.length == 0)
            return list;
        int i = 0;
        if (arr[0].trim().isEmpty()) {
            list.add("/");
            i = 1;
        }
        for (; i < arr.length; ++i) {
            String s = arr[i].trim();
            switch (s) {
                case "":
                case ".":
                    break;
                case "..":
                    if (list.isEmpty() || list.get(list.size() - 1).equals("..")) {
                        list.add("..");
                        break;
                    }
                    if (list.size() != 1 || !list.get(0).equals("/"))
                        list.remove(list.size() - 1);
                    break;
                default:
                    list.add(s);
                    break;
            }
        }
        return list;
    }

    //get name of entry, go to folder, that contains this entry
    public String cdGoEntry(String path, ArrayList<String> arr, boolean make_dir) {
        if(arr.size() == 0){
            curr_path = conn.getParent(vis_folder);
            return conn.getName(vis_folder);
        }
        int i = 0;
        if(arr.get(0).equals("/")){
            curr_path = 0;
            i = 1;
            if(arr.size() == 1)
                return "/";
        }else
            curr_path = vis_folder;

        for(; i < arr.size() - 1; ++i){
            if ("..".equals(arr.get(i)))
                curr_path = conn.getParent(curr_path);
            else {
                long new_path = conn.getID(curr_path, arr.get(i));
                if(new_path == -1) {
                    if(make_dir)
                        curr_path = defnewDir(arr.get(i));
                    else
                        throw new RuntimeException(getString(R.string.error_bad_path, path));
                }else if(conn.getType(new_path) != ElementType.FOLDER)
                    throw new RuntimeException(activity.getString(R.string.error_file2folder, arr.get(i)));
                else
                    curr_path = new_path;
            }
        }

        String last = arr.get(arr.size() - 1);
        if(last.equals("..")){
            curr_path = conn.getParent(curr_path);
            String name = conn.getName(curr_path);
            curr_path = conn.getParent(curr_path);
            return name;
        }
        return last;
    }
    //go to the folder
    public void cdGo(String path, ArrayList<String> arr, boolean make_dir){
        if(arr.size() == 0){
            curr_path = vis_folder;
            return;
        }
        int i = 0;
        if(arr.get(0).equals("/")){
            curr_path = 0;
            i = 1;
        }else
            curr_path = vis_folder;

        for(; i < arr.size(); ++i){
            if("..".equals(arr.get(i)))
                curr_path = conn.getParent(curr_path);
            else{
                long new_path = conn.getID(curr_path, arr.get(i));
                if(new_path == -1){
                    if(make_dir)
                        curr_path = defnewDir(arr.get(i));
                    else
                        throw new RuntimeException(getString(R.string.error_bad_path, path));
                }else if(conn.getType(new_path) != ElementType.FOLDER)
                    throw new RuntimeException(activity.getString(R.string.error_file2folder, arr.get(i)));
                else
                    curr_path = new_path;
            }
        }
    }

    private long defnewFile(String file, boolean exec) {
        celement.setName(file);
        celement.setType(exec ? ElementType.SCRIPT : ElementType.TEXT);
        celement.parent = curr_path;
        return conn.newElement(celement);
    }

    private long defnewDir(String dir) {
        celement.setName(dir);
        celement.setType(ElementType.FOLDER);
        celement.parent = curr_path;
        return conn.newElement(celement);
    }
    @SuppressWarnings("UnusedDeclaration")
    public void touch(String tpath) {
        String entry = cdGoEntry(tpath, Executor.parsePath(tpath), true);
        long id = conn.getID(curr_path, entry);
        if (id == -1)
            defnewFile(entry, false);
    }

    public void mkdir(String dpath) {
        String dir = cdGoEntry(dpath, Executor.parsePath(dpath), true);
        long id = conn.getID(curr_path, dir);
        if (id == -1)
            defnewDir(dir);
        else if (conn.getType(id) != ElementType.FOLDER)
            throw new RuntimeException(activity.getString(R.string.error_file2folder, dir));
    }
    @SuppressWarnings("UnusedDeclaration")
    public boolean delete(String path) {
        String entry = cdGoEntry(path, Executor.parsePath(path), false);
        long id = conn.getID(curr_path, entry);
        if (id < 1) {
            return false;
        } else {
            conn.deleteElement(curr_path, id);
            return true;
        }
    }
    @SuppressWarnings("UnusedDeclaration")
    public void write(String fpath, String content) {
        String file = cdGoEntry(fpath, Executor.parsePath(fpath), true);
        long id = conn.getID(curr_path, file);
        if (id == -1) {
            id = defnewFile(file, false);
        } else if (conn.getType(id) == ElementType.FOLDER) {
            throw new RuntimeException(activity.getString(R.string.error_folder2file, file));
        }
        delement.id = id;
        delement.parent = curr_path;
        delement.content = content;
        conn.updateTextData(delement);
    }
    @SuppressWarnings("UnusedDeclaration")
    public String read(String fpath) {
        String file = cdGoEntry(fpath, Executor.parsePath(fpath), false);
        long id = conn.getID(curr_path, file);
        if (id == -1 || conn.getType(id) == ElementType.FOLDER)
            throw new RuntimeException(activity.getString(R.string.error_read_existence, file));
        return conn.getContent(id);
    }
    @SuppressWarnings("UnusedDeclaration")
    public void script(String fpath, boolean mode) {
        String file = cdGoEntry(fpath, Executor.parsePath(fpath), true);
        long id = conn.getID(curr_path, file);
        if (id == -1) {
            defnewFile(file, mode);
        } else if (conn.getType(id) == ElementType.FOLDER) {
            throw new RuntimeException(activity.getString(R.string.error_folder_to_script, file));
        } else {
            conn.getElement(id, delement);
            celement.id = delement.id;
            celement.parent = delement.parent;
            celement.setName(file);
            celement.setType(delement.type);
            celement.updateType(mode ? ElementType.SCRIPT : ElementType.TEXT);

            conn.updateElement(celement);
        }
    }
    @SuppressWarnings("UnusedDeclaration")
    public Cursor listFiles(String dpath) {
        cdGo(dpath, Executor.parsePath(dpath), false);
        return conn.getListFiles(curr_path);
    }

    @SuppressWarnings("UnusedDeclaration")
    public boolean exists(String path) {
        String entry = cdGoEntry(path, Executor.parsePath(path), false);
        return conn.getID(curr_path, entry) != -1;
    }
    @SuppressWarnings("UnusedDeclaration")
    public ElementType getType(String path) {
        try {
            String entry = cdGoEntry(path, Executor.parsePath(path), false);
            return conn.getType(conn.getID(curr_path, entry));
        }catch (RuntimeException e){
            return null;
        }
    }

    public static String path_concat(String path1, String path2) {
        path1 = path1.trim();
        path2 = path2.trim();
        if (path1.endsWith("/")) {
            if (path2.startsWith("/")) {
                return path1 + path2.substring(1);
            } else {
                return path1 + path2;
            }
        } else {
            if (path2.startsWith("/")) {
                return path1 + path2;
            } else {
                return path1 + "/" + path2;
            }
        }
    }
    @SuppressWarnings("UnusedDeclaration")
    public void rename(String epath, String name2) {
        String name1 = cdGoEntry(epath, Executor.parsePath(epath), false);

        long id = conn.getID(curr_path, name1);
        if (id < 1)
            throw new RuntimeException(activity.getString(R.string.error_bad_path, epath));
        long id2 = conn.getID(curr_path, name2);
        if (id2 != -1)
            throw new RuntimeException(activity.getString(R.string.error_rename_exists, epath));
        celement.id = id;
        celement.parent = curr_path;
        celement.setType(conn.getType(id));
        celement.setName(name1);
        celement.updateName(name2);
        conn.updateElement(celement);
    }

    public String getName(String path) {
        ArrayList<String> p = parsePath(path);
        if (p.size() == 0 || p.get(p.size() - 1).equals(".."))
            return cdGoEntry(path, p, false);
        else
            return p.get(p.size() - 1);
    }

    public void move(String entry_cut, String path_paste) {
        ArrayList<String> path_from = parsePath(entry_cut);
        String name = cdGoEntry(entry_cut, path_from, false);

        long from_dir = curr_path;
        long from_id = conn.getID(curr_path, name);
        if(from_id < 1)//ничего не найдено по данному пути
            throw new RuntimeException(getString(R.string.error_bad_path, entry_cut));

        ArrayList<String> path_to = parsePath(path_paste);
        cdGo(path_paste, path_to, true);
        long to_dir = curr_path;

        if (from_id == to_dir || conn.isParentFor(from_id, to_dir))//перемещение внутрь себя
            throw new RuntimeException(activity.getString(R.string.error_move_dest, entry_cut, path_paste));

        if (from_dir == to_dir)//пункт назначения == пункту отправления
            return;

        long err_id = conn.getID(to_dir, name);
        if(err_id != -1)
            throw new RuntimeException(getString(R.string.error_rename_exists, path_concat(path_paste, name)));
        conn.updateParent(from_id, from_dir, to_dir);
    }
    @SuppressWarnings("UnusedDeclaration")
    public void run(String path){
        String entry = cdGoEntry(path, parsePath(path), false);
        long id = conn.getID(curr_path, entry);
        if(id != -1 && conn.getType(id) != ElementType.FOLDER)
            activity.adapter.runFile(conn.buildPath(id), curr_path, id);
        else
            throw new RuntimeException(getString(R.string.error_run_nofile, path));
    }
    @SuppressWarnings("UnusedDeclaration")
    public void cd(String dpath){
        cdGo(dpath, parsePath(dpath), true);
        vis_folder = curr_path;
    }
    @SuppressWarnings("UnusedDeclaration")
    public void view(String dpath){
        cdGo(dpath, parsePath(dpath), false);
        activity.adapter.view(curr_path);
    }
    public String getString(@StringRes int id, String text){
        return activity.getString(id, text);
    }
    @Override
    public void close() {
        conn.close();
    }
}
