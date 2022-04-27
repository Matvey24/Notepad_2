package com.matvey.perelman.notepad2.database;

import com.matvey.perelman.notepad2.list.ElementType;

public class DatabaseElement {
    public int id;
    public String name;
    public String content;
    public ElementType type;

    @Override
    public String toString() {
        return "DatabaseElement{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", content='" + content + '\'' +
                ", type=" + type +
                '}';
    }
}
