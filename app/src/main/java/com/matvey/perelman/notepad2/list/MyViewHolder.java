package com.matvey.perelman.notepad2.list;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.matvey.perelman.notepad2.R;
import com.matvey.perelman.notepad2.database.DatabaseElement;

public class MyViewHolder extends RecyclerView.ViewHolder {
    private final TextView name_tv;
    private final TextView content_tv;
    private final ImageView type_image;
    private final ImageButton action_btn;
    private boolean error_message;
    public final DatabaseElement element;
    public MyViewHolder(View itemView, Adapter adapter){
        super(itemView);
        element = new DatabaseElement();
        name_tv = itemView.findViewById(R.id.name_tv);
        content_tv = itemView.findViewById(R.id.content_tv);
        type_image = itemView.findViewById(R.id.type_image);
        action_btn = itemView.findViewById(R.id.action_btn);
        action_btn.setOnClickListener((view)->adapter.onClickSettings(element));
        itemView.setOnClickListener((view)->{
            if(!error_message)
                adapter.onClickField(element, getAdapterPosition());
        });
        itemView.setOnLongClickListener((view)->{
            adapter.onClickSettings(element);
            return true;
        });
    }
    public void setValues(){
        action_btn.setVisibility(View.VISIBLE);
        name_tv.setText(element.name);
        content_tv.setText(element.content);
        switch (element.type){
            case FOLDER:
                type_image.setImageResource(R.drawable.folder_image);
                break;
            case TEXT:
                type_image.setImageResource(R.drawable.text_image);
                break;
            case SCRIPT:
                type_image.setImageResource(R.drawable.executable_image);
                break;
        }
        error_message = false;
    }
    public void setError(String name, String description){
        action_btn.setVisibility(View.INVISIBLE);
        name_tv.setText(name);
        content_tv.setText(description);
        type_image.setImageResource(R.drawable.error_image);
        error_message = true;
    }
}
