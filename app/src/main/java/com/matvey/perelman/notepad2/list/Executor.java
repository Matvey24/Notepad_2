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
    private final CursorUpdatable path;

    public Executor(CursorUpdatable path) {
        this.path = path;
        celement = new CreatorElement();
        delement = new DatabaseElement();
        cursor = new CursorUpdatable(path.c.connection, null);
    }

    public void setPython(PyObject p) {
        this.pyapi = p;
    }

    public void begin(int file_idx) {
        path.getElement(delement, file_idx);
        try {
            pyapi.callAttrThrows("__java_api_run", delement.content);
        } catch (Throwable t) {
            PythonAPI.toast_l(t.getMessage());
        }
    }

    public void makeDatabase(String name, String json) {
        cursor = new CursorUpdatable(path.c.connection, null);
        pyapi.callAttr("__java_api_from_json", "/" + name, json);
    }

    public String getPath() {
        return path.path;
    }

    public ArrayList<String> parsePath(String path) {
        if(path.trim().equals("/")){
            ArrayList<String> list = new ArrayList<>();
            list.add(path);
            return list;
        }
        String[] arr = path.split("/");
        ArrayList<String> list = new ArrayList<>();
        if (arr.length == 0) {
            list.add(".");
            return list;
        }
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
        if (list.isEmpty())
            list.add(".");
        return list;
    }

    public boolean cdGoFull(ArrayList<String> arr, int len) {
        int i = 0;
        if (arr.get(0).equals("/")) {
            cursor.c.setRootPath();
            i = 1;
        } else
            cursor.c.copyPath(this.path.c);

        cursor.updatePath();
        cursor.reloadData();

        for (; i < len; ++i) {
            String s = arr.get(i);
            if (cd(cursor, s))
                return true;
        }
        return false;
    }

    public String cdGo(ArrayList<String> arr) {
        if (cdGoFull(arr, arr.size() - 1))
            return null;
        else
            return arr.get(arr.size() - 1);
    }

    private void defnewFile(String file, boolean exec) {
        celement.setName(file);
        celement.setType(exec ? ElementType.EXECUTABLE : ElementType.TEXT);
        cursor.newElement(celement);
        cursor.reloadData();
    }

    public void touch(String path) {
        int idx = cursor.getElementIdx(path);
        if (idx == -1) {
            if (cursor.layer() == 0)
                throw new RuntimeException("Could not make file in root directory");
            defnewFile(path, false);
        }
    }

    private void defnewDir(String dir) {
        celement.setName(dir);
        celement.setType(ElementType.FOLDER);
        cursor.newElement(celement);
        cursor.reloadData();
    }

    public void mkdir(String dir) {
        int idx = cursor.getElementIdx(dir);
        if (idx == -1)
            defnewDir(dir);
        else if (!cursor.c.isFolder(idx))
            throw new RuntimeException("Could not mkdir, file with the same name exists: " + dir);
    }

    public boolean delete(String entry) {
        int idx = cursor.getElementIdx(entry);
        if (idx == -1) {
            return false;
        } else {
            cursor.deleteElement(cursor.getElementId(idx));
            cursor.reloadData();
            return true;
        }
    }

    public void write(String file, String content) {
        int idx = cursor.getElementIdx(file);
        if (idx == -1) {
            if (cursor.layer() == 0)
                throw new RuntimeException("Could not make file in root directory");
            defnewFile(file, false);
            idx = cursor.getElementIdx(file);
        } else if (cursor.c.isFolder(idx)) {
            throw new RuntimeException("Could not write to folder: " + file);
        }
        delement.id = cursor.getElementId(idx);
        delement.content = content;
        cursor.updateTextData(delement);
        cursor.reloadData();
    }

    public String read(String file) {
        int idx = cursor.getElementIdx(file);
        if (idx == -1 || cursor.c.isFolder(idx))
            throw new RuntimeException("Could not read: file " + file + " does not exist");
        cursor.getElement(delement, idx);
        return delement.content;
    }

    public void executable(String file, boolean mode) {
        int idx = cursor.getElementIdx(file);
        if (idx == -1) {
            if (cursor.layer() == 0)
                throw new RuntimeException("Could not make file in root directory");
            defnewFile(file, mode);
        } else {
            celement.id = cursor.getElementId(idx);
            celement.setName(file);
            celement.setType(cursor.c.getType(idx));
            celement.updateType(mode ? ElementType.EXECUTABLE : ElementType.TEXT);
            cursor.updateElement(celement);
            cursor.reloadData();
        }
    }

    public boolean cd(CursorUpdatable curs, String dir) {
        if (dir.equals("..")) {
            curs.back();
            return false;
        }
        int idx = curs.getElementIdx(dir);
        if (idx == -1) {
            defnewDir(dir);
            idx = curs.getElementIdx(dir);
        }
        if (!curs.c.isFolder(idx))
            return true;
        curs.enter(idx);
        return false;
    }

    public ArrayList<DatabaseElement> listFiles(String dir) {
        if (cd(cursor, dir))
            return null;
        ArrayList<DatabaseElement> list = new ArrayList<>();
        for (int i = 0; i < cursor.length(); ++i) {
            DatabaseElement de = new DatabaseElement();
            cursor.getElement(de, i);
            list.add(de);
        }
        return list;
    }

    public boolean exists(String entry) {
        int idx = cursor.getElementIdx(entry);
        return idx != -1;
    }

    public boolean isDir(String entry) {
        ElementType type = getType(entry);
        return type == ElementType.FOLDER;
    }

    public boolean isExecutable(String entry) {
        ElementType type = getType(entry);
        return type == ElementType.EXECUTABLE;
    }

    public ElementType getType(String entry) {
        int idx = cursor.getElementIdx(entry);
        if (idx == -1)
            return null;
        return cursor.c.getType(idx);
    }

    public String path_concat(String path1, String path2) {
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

    public String getName(String path) {
        ArrayList<String> p = parsePath(path);
        if (p.get(p.size() - 1).equals("..")) {
            String s = cdGo(p);
            cursor.back();
            if (cursor.c.path.size() == 0)
                return "/";
            return cursor.c.path.get(cursor.c.path.size() - 1);
        } else
            return p.get(p.size() - 1);
    }
    public void move(String entry_cut, String path_paste) {
        ArrayList<String> path_from = parsePath(entry_cut);
        if (!path_from.get(0).equals("/")) {
            entry_cut = path_concat(path.path, entry_cut);
            path_from = parsePath(entry_cut);
        }
        ArrayList<String> path_to = parsePath(path_paste);
        if (!path_to.get(0).equals("/")) {
            path_paste = path_concat(path.path, path_paste);
            path_to = parsePath(path_paste);
        }
        if(path_from.size() <= path_to.size()) {
            boolean copying_in = true;
            for (int i = 0; i < path_from.size(); ++i) {
                if (!path_to.get(i).equals(path_from.get(i))) {
                    copying_in = false;
                    break;
                }
            }
            if (copying_in)
                throw new RuntimeException("move: destination " + entry_cut + " is in " + path_paste);
        }
        boolean can_change_parent = (path_from.size() != 1 && path_to.size() != 1 && path_from.get(1).equals(path_to.get(1)));
        if (!can_change_parent) {
            pyapi.callAttr("__java_api_copying_move", entry_cut, path_paste);
            return;
        }
        String from_name = cdGo(path_from);
        int idx = cursor.getElementIdx(from_name);
        if (idx == -1)
            throw new RuntimeException("move: No to cut found at path " + entry_cut);
        int id;
        if(cursor.layer() == 0)
            id = -1;
        else
            id = cursor.getElementId(idx);

        String to_name = cdGo(path_to);
        int parent_idx = cursor.getElementIdx(to_name);
        if (parent_idx == -1) {
            defnewDir(to_name);
            parent_idx = cursor.getElementIdx(to_name);
        }
        int parent_id;
        if(cursor.layer() == 0)
            parent_id = -1;
        else
            parent_id = cursor.getElementId(parent_idx);
        if(cd(cursor, to_name))
            throw new RuntimeException("move: destination is file");
        cursor.updateParent(id, parent_id);
    }

    @Override
    public void close() {
        cursor.close();
    }
}
