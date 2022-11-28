package com.matvey.perelman.notepad2.executor;

import android.database.Cursor;

import androidx.annotation.NonNull;
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
    public final PyObject py_executor;
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
    public void begin(String path, boolean check_empty) {
        vis_folder = 0;
        String entry = cdGoEntry(path, parsePath(path), false);
        long id = conn.getID(curr_path, entry);
        if(id == -1)
            throw new RuntimeException(activity.getString(R.string.error_bad_path, path));
        if (conn.getType(id) == ElementType.FOLDER)
            throw new RuntimeException(activity.getString(R.string.error_run_nofile, path));
        begin(path, id, curr_path, check_empty);
    }

    public void begin(String filepath, long id, long parent, boolean check_empty) {
        String content = conn.getContent(id);
        this.filepath = filepath;
        vis_folder = parent;
        if (check_empty && content.equals(""))
            activity.makeToast(activity.getString(R.string.warn_script_empty, filepath), true);
        else
            py_executor.callAttr("execute", filepath, content);
    }

    public void makeDatabase(String json) {
        vis_folder = 0;
        py_executor.callAttr("from_json", json, "/");
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getScriptPath() {
        return filepath;
    }
    @SuppressWarnings("UnusedDeclaration")
    public String getPath(@NonNull String path) {
        if(".".equals(path)){
            return conn.buildPath(vis_folder);
        }else{
            ArrayList<String> p = parsePath(path);
            if(p.size() > 0 && p.get(0).equals("/"))
                return concatPath(p);
            return path_concat(conn.buildPath(vis_folder), concatPath(p));
        }
    }
    private static String concatPath(ArrayList<String> path){
        if(path.size() == 0)
            return "";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        if(path.get(0).equals("/")) {
            sb.append("/");
            i = 1;
        }
        for(; i < path.size(); ++i){
            sb.append(path.get(i));
            if(i + 1 != path.size())
                sb.append("/");
        }
        return sb.toString();
    }
    @SuppressWarnings("UnusedDeclaration")
    public String getScriptName() {
        return getName(filepath);
    }

    public ArrayList<String> parsePath(String path) {
        requireNonNull(path);
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
    private String cdGoEntry(@NonNull String path, ArrayList<String> arr, boolean make_dir) {
        if (arr.size() == 0) {
            curr_path = conn.getParent(vis_folder);
            return conn.getName(vis_folder);
        }
        int i = 0;
        if (arr.get(0).equals("/")) {
            curr_path = 0;
            i = 1;
            if (arr.size() == 1)
                return "/";
        } else
            curr_path = vis_folder;

        for (; i < arr.size() - 1; ++i) {
            if ("..".equals(arr.get(i)))
                curr_path = conn.getParent(curr_path);
            else {
                long new_path = conn.getID(curr_path, arr.get(i));
                if (new_path == -1) {
                    if (make_dir)
                        curr_path = defnewDir(arr.get(i));
                    else
                        throw new RuntimeException(getString(R.string.error_bad_path, path));
                } else if (conn.getType(new_path) != ElementType.FOLDER)
                    throw new RuntimeException(activity.getString(R.string.error_file2folder, arr.get(i)));
                else
                    curr_path = new_path;
            }
        }

        String last = arr.get(arr.size() - 1);
        if (last.equals("..")) {
            curr_path = conn.getParent(curr_path);
            String name = conn.getName(curr_path);
            curr_path = conn.getParent(curr_path);
            return name;
        }
        return last;
    }

    //go to the folder
    private void cdGo(@NonNull String path, ArrayList<String> arr, boolean make_dir) {
        if (arr.size() == 0) {
            curr_path = vis_folder;
            return;
        }
        int i = 0;
        if (arr.get(0).equals("/")) {
            curr_path = 0;
            i = 1;
        } else
            curr_path = vis_folder;

        for (; i < arr.size(); ++i) {
            if ("..".equals(arr.get(i)))
                curr_path = conn.getParent(curr_path);
            else {
                long new_path = conn.getID(curr_path, arr.get(i));
                if (new_path == -1) {
                    if (make_dir)
                        curr_path = defnewDir(arr.get(i));
                    else
                        throw new RuntimeException(getString(R.string.error_bad_path, path));
                } else if (conn.getType(new_path) != ElementType.FOLDER)
                    throw new RuntimeException(activity.getString(R.string.error_file2folder, arr.get(i)));
                else
                    curr_path = new_path;
            }
        }
    }

    private long defnewFile(@NonNull String file, boolean exec) {
        celement.setName(file);
        celement.setType(exec ? ElementType.SCRIPT : ElementType.TEXT);
        celement.parent = curr_path;
        return conn.newElement(celement);
    }

    private long defnewDir(@NonNull String dir) {
        celement.setName(dir);
        celement.setType(ElementType.FOLDER);
        celement.parent = curr_path;
        return conn.newElement(celement);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void touch(@NonNull String tpath) {
        String entry = cdGoEntry(tpath, parsePath(tpath), true);
        long id = conn.getID(curr_path, entry);
        if (id == -1)
            defnewFile(entry, false);
    }

    public void mkdir(@NonNull String dpath) {
        String dir = cdGoEntry(dpath, parsePath(dpath), true);
        long id = conn.getID(curr_path, dir);
        if (id == -1)
            defnewDir(dir);
        else if (conn.getType(id) != ElementType.FOLDER)
            throw new RuntimeException(activity.getString(R.string.error_file2folder, dir));
    }

    @SuppressWarnings("UnusedDeclaration")
    public boolean delete(@NonNull String path) {
        String entry = cdGoEntry(path, parsePath(path), false);
        long id = conn.getID(curr_path, entry);
        if (id < 1) {
            return false;
        } else {
            conn.deleteElement(curr_path, id);
            return true;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public void write(@NonNull String fpath, @NonNull String content) {
        String file = cdGoEntry(fpath, parsePath(fpath), true);
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
    public String read(@NonNull String fpath) {
        String file = cdGoEntry(fpath, parsePath(fpath), false);
        long id = conn.getID(curr_path, file);
        if (id == -1 || conn.getType(id) == ElementType.FOLDER)
            throw new RuntimeException(activity.getString(R.string.error_read_existence, file));
        return conn.getContent(id);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void script(@NonNull String fpath, boolean mode) {
        String file = cdGoEntry(fpath, parsePath(fpath), true);
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
    public Cursor listFiles(@NonNull String dpath) {
        cdGo(dpath, parsePath(dpath), false);
        return conn.getListFiles(curr_path);
    }

    @SuppressWarnings("UnusedDeclaration")
    public boolean exists(@NonNull String path) {
        String entry = cdGoEntry(path, parsePath(path), false);
        return conn.getID(curr_path, entry) != -1;
    }

    @SuppressWarnings("UnusedDeclaration")
    public ElementType getType(@NonNull String path) {
        try {
            String entry = cdGoEntry(path, parsePath(path), false);
            return conn.getType(conn.getID(curr_path, entry));
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static String path_concat(String path1, String path2) {
        if(path1 == null)
            return path2;
        if(path2 == null)
            return path1;
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
    public void rename(@NonNull String epath, String name2) {
        requireNonNull(name2);
        String name1 = cdGoEntry(epath, parsePath(epath), false);

        long id = conn.getID(curr_path, name1);
        if (id < 1)
            throw new RuntimeException(activity.getString(R.string.error_bad_path, epath));
        long id2 = conn.getID(curr_path, name2);
        if (id2 != -1)
            throw new RuntimeException(activity.getString(R.string.error_rename_exists, name2));
        celement.id = id;
        celement.parent = curr_path;
        celement.setType(conn.getType(id));
        celement.setName(name1);
        celement.updateName(name2);
        conn.updateElement(celement);
    }

    public String getName(@NonNull String path) {
        ArrayList<String> p = parsePath(path);
        if (p.size() == 0 || p.get(p.size() - 1).equals(".."))
            return cdGoEntry(path, p, false);
        else
            return p.get(p.size() - 1);
    }

    public void move(@NonNull String entry_cut, @NonNull String path_paste) {
        ArrayList<String> path_from = parsePath(entry_cut);
        String name = cdGoEntry(entry_cut, path_from, false);

        long from_dir = curr_path;
        long from_id = conn.getID(curr_path, name);
        if (from_id < 1)//ничего не найдено по данному пути
            throw new RuntimeException(getString(R.string.error_bad_path, entry_cut));

        ArrayList<String> path_to = parsePath(path_paste);
        cdGo(path_paste, path_to, true);
        long to_dir = curr_path;

        if (from_id == to_dir || conn.isParentFor(from_id, to_dir))//перемещение внутрь себя
            throw new RuntimeException(activity.getString(R.string.error_move_dest, entry_cut, path_paste));

        if (from_dir == to_dir)//пункт назначения == пункту отправления
            return;

        long err_id = conn.getID(to_dir, name);
        if (err_id != -1)
            throw new RuntimeException(getString(R.string.error_rename_exists, path_concat(path_paste, name)));
        conn.updateParent(from_id, from_dir, to_dir);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void run(@NonNull String path) {
        String entry = cdGoEntry(path, parsePath(path), false);
        long id = conn.getID(curr_path, entry);
        if (id == -1)
            throw new RuntimeException(activity.getString(R.string.error_bad_path, path));
        if (conn.getType(id) != ElementType.FOLDER)
            activity.adapter.runFile(conn.buildPath(id), curr_path, id, false);
        else
            throw new RuntimeException(getString(R.string.error_run_nofile, path));
    }
    @SuppressWarnings("UnusedDeclaration")
    public void reset_params(PyObject list, @NonNull String path){
        String entry = cdGoEntry(path, parsePath(path), false);
        long id = conn.getID(curr_path, entry);
        if (id == -1)
            throw new RuntimeException(activity.getString(R.string.error_bad_path, path));
        if (conn.getType(id) == ElementType.FOLDER)
            throw new RuntimeException(getString(R.string.error_run_nofile, path));
        list.callAttr("append", this.filepath);
        list.callAttr("append", this.vis_folder);
        list.callAttr("append", conn.getContent(id));
        String new_path = conn.buildPath(id);
        list.callAttr("append", new_path);
        list.callAttr("append", entry);
        this.filepath = new_path;
        this.vis_folder = curr_path;
    }
    @SuppressWarnings("UnusedDeclaration")
    public void return_params(PyObject list){
        this.filepath = list.callAttr("__getitem__", 0).toString();
        this.vis_folder = list.callAttr("__getitem__", 1).toLong();
    }
    @SuppressWarnings("UnusedDeclaration")
    public void cd(@NonNull String dpath) {
        cdGo(dpath, parsePath(dpath), true);
        vis_folder = curr_path;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void view(@NonNull String dpath) {
        cdGo(dpath, parsePath(dpath), false);
        activity.adapter.view(curr_path);
    }
    @SuppressWarnings("UnusedDeclaration")
    public String getViewPath(){
        return activity.adapter.cursor.path_t;
    }
    public String getString(@StringRes int id, Object... text) {
        return activity.getString(id, text);
    }
    private void requireNonNull(String obj){
        if(obj == null)
            throw new NullPointerException(activity.getString(R.string.error_path_none));
    }
    @Override
    public void close() {
        conn.close();
    }
}
