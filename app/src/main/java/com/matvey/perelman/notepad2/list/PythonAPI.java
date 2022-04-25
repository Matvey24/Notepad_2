package com.matvey.perelman.notepad2.list;

import com.chaquo.python.PyObject;
import com.matvey.perelman.notepad2.MainActivity;

public class PythonAPI {
    public static MainActivity activity;
    public static void makeToast(PyObject text){
        activity.makeToast(text.toString(), true);
    }
}
