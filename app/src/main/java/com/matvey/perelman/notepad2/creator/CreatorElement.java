package com.matvey.perelman.notepad2.creator;

import com.matvey.perelman.notepad2.database.DatabaseElement;
import com.matvey.perelman.notepad2.list.ElementType;

public class CreatorElement {
    public long id, parent;
    private ElementType type_start, type_end;
    private String name_start, name_end;
    public void set(DatabaseElement element){
        id = element.id;
        parent = element.parent;
        setName(element.name);
        setType(element.type);
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
    public ElementType getType(){
        return type_end;
    }
    public String getName(){
        return name_end;
    }
    public String getNameStart(){
        return name_start;
    }
    public boolean isTypeChanged(){
        return !type_end.equals(type_start);
    }
    public boolean isNameChanged(){
        return !name_end.equals(name_start);
    }
}
