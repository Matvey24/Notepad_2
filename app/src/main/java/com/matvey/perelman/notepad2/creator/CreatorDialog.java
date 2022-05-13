package com.matvey.perelman.notepad2.creator;

import android.app.Dialog;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.matvey.perelman.notepad2.MainActivity;
import com.matvey.perelman.notepad2.R;
import com.matvey.perelman.notepad2.list.ElementType;

import static com.matvey.perelman.notepad2.list.ElementType.EXECUTABLE;
import static com.matvey.perelman.notepad2.list.ElementType.FOLDER;
import static com.matvey.perelman.notepad2.list.ElementType.TEXT;

public class CreatorDialog extends Dialog {
    private final TextView tv_dialog_name;
    private final RadioButton btn_file;
    private final RadioButton btn_folder;
    private final RadioButton btn_executable;
    private final TextInputLayout layout;
    private final TextInputEditText name_text;
    private final Button btn_create;
    private final ImageButton btn_delete;
    private final ImageButton btn_cut;
    public final CreatorElement element;
    private boolean editing;
    public CreatorDialog(MainActivity context){
        super(context);
        setContentView(R.layout.dialog_creator);
        element = new CreatorElement();
        btn_file = findViewById(R.id.rbtn_file);
        btn_folder = findViewById(R.id.rbtn_folder);
        btn_executable = findViewById(R.id.rbtn_executable);
        layout = findViewById(R.id.name_layout);
        name_text = layout.findViewById(R.id.name_text);
        btn_create = findViewById(R.id.btn_create);
        btn_delete = findViewById(R.id.btn_delete);
        btn_cut = findViewById(R.id.btn_cut);
        tv_dialog_name = findViewById(R.id.tv_dialog_name);

        btn_file.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked){
                layout.setHint(R.string.new_file_text);
                element.updateType(TEXT);
            }
        });
        btn_folder.setOnCheckedChangeListener((buttonView, isChecked)->{
            if(isChecked){
                layout.setHint(R.string.new_folder_text);
                element.updateType(FOLDER);
            }
        });
        btn_executable.setOnCheckedChangeListener((buttonView, isChecked)->{
            if(isChecked){
                layout.setHint(R.string.new_executable_text);
                element.updateType(EXECUTABLE);
            }
        });

        btn_create.setOnClickListener((view)->{
            String name = (name_text.getText() == null) ? "" : name_text.getText().toString().trim();
            if(editing){
                if(!name.isEmpty()) {
                    element.updateName(name);
                    context.update_element(element);
                }
            }else {
                if (name.isEmpty()) {
                    switch (element.getType()) {
                        case TEXT:
                            name = context.getString(R.string.new_file_text);
                            break;
                        case FOLDER:
                            name = context.getString(R.string.new_folder_text);
                            break;
                        case EXECUTABLE:
                            name = context.getString(R.string.new_executable_text);
                            break;
                    }
                }
                element.updateName(name);
                context.create_element(element);
            }
            hide();
            context.hideKeyboard();
        });
        btn_delete.setOnClickListener((view)->{
            hide();
            context.hideKeyboard();
            context.delete_element(element);
        });
        btn_cut.setOnClickListener((view)->{
           hide();
           context.hideKeyboard();
           if(editing){
               context.cut_element(element);
           }else{
               context.paste_element();
           }
        });
    }
    public void startCreating(long parent, boolean paste_available){
        editing = false;
        element.setType(TEXT);
        element.parent = parent;
        setChecked();
        tv_dialog_name.setText(R.string.create_item_text);
        element.setName("");
        name_text.setText("");
        btn_create.setText(R.string.action_create);
        btn_cut.setImageResource(R.drawable.paste_image);
        btn_cut.setEnabled(paste_available);
        btn_delete.setVisibility(View.INVISIBLE);
        show();
    }
    public void startEditing(){
        editing = true;
        setChecked();
        tv_dialog_name.setText(R.string.edit_item_text);
        name_text.setText(element.getName());
        btn_create.setText(R.string.action_apply);
        btn_cut.setImageResource(R.drawable.cut_image);
        btn_cut.setEnabled(true);
        btn_delete.setVisibility(View.VISIBLE);
        show();
    }
    private void setChecked(){
        ElementType type = element.getType();
        switch (type){
            case TEXT:
                btn_file.setChecked(true);
                break;
            case FOLDER:
                btn_folder.setChecked(true);
                break;
            case EXECUTABLE:
                btn_executable.setChecked(true);
                break;
        }
        btn_file.setEnabled(type != FOLDER);
        btn_folder.setEnabled(type == FOLDER || !editing);
        btn_executable.setEnabled(type != FOLDER);
    }
}
