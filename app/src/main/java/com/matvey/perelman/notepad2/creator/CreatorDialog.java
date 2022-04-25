package com.matvey.perelman.notepad2.creator;

import android.app.Dialog;
import android.widget.Button;
import android.widget.RadioButton;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.matvey.perelman.notepad2.MainActivity;
import com.matvey.perelman.notepad2.R;
import com.matvey.perelman.notepad2.list.ElementType;

import static com.matvey.perelman.notepad2.list.ElementType.EXECUTABLE;
import static com.matvey.perelman.notepad2.list.ElementType.FOLDER;
import static com.matvey.perelman.notepad2.list.ElementType.TEXT;

public class CreatorDialog extends Dialog {
    private final RadioButton btn_file;
    private final RadioButton btn_folder;
    private final RadioButton btn_executable;
    private final TextInputLayout layout;
    private final TextInputEditText name_text;
    private final Button btn_create;
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
        btn_create = findViewById(R.id.btn_create);
        btn_create.setOnClickListener((view)->{
            String name = (name_text.getText() == null) ? "" : name_text.getText().toString();

            if(editing){
                if(!name.isEmpty()) {
                    element.updateName(name);
                    context.update_element(element, true);
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
                context.update_element(element, false);
            }
            hide();
            context.hideKeyboard();
        });
    }
    public void startCreating(){
        editing = false;
        setChecked();
        element.setName("");
        name_text.setText("");
        btn_create.setText(R.string.action_create);
        show();
    }
    public void startEditing(){
        editing = true;
        setChecked();
        name_text.setText(element.getName());
        btn_create.setText(R.string.action_apply);
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
