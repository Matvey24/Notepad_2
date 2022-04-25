package com.matvey.perelman.notepad2.creator;

import com.matvey.perelman.notepad2.database.DatabaseElement;
import com.matvey.perelman.notepad2.list.ElementType;

public class CreatorElement {
    public int id;
    private ElementType type_start, type_end;
    private int idx_start, idx_end;
    private String name_start, name_end;
    public void set(DatabaseElement other, int idx){
        id = other.id;
        setName(other.name);
        setType(other.type);
        setIdx(idx);
    }
    public void setType(ElementType type){
        type_start = type;
        type_end = type;
    }
    public void updateType(ElementType type){
        type_end = type;
    }
    public void setName(String name){
        name_start = name;
        name_end = name;
    }
    public void updateName(String name){
        this.name_end = name;
    }
    public void setIdx(int idx){
        idx_start = idx;
        idx_end = idx;
    }
    public void updateIdx(int idx){
        idx_end = idx;
    }
    public ElementType getType(){
        return type_end;
    }
    public String getName(){
        return name_end;
    }
    public String getNameStart(){
        return name_start;
    }
    public int getIdx(){
        return idx_end;
    }
    public int getIdxStart(){
        return idx_start;
    }
    public boolean isChanged(){
        return isNameChanged() || isTypeChanged() || isIdxChanged();
    }
    public boolean isTypeChanged(){
        return !type_end.equals(type_start);
    }
    public boolean isNameChanged(){
        return !name_end.equals(name_start);
    }
    public boolean isIdxChanged(){
        return idx_start != idx_end;
    }
}
