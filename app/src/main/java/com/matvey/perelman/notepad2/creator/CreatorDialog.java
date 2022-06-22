package com.matvey.perelman.notepad2.creator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.matvey.perelman.notepad2.MainActivity;
import com.matvey.perelman.notepad2.R;
import com.matvey.perelman.notepad2.database.DatabaseElement;
import com.matvey.perelman.notepad2.list.ElementType;

import static com.matvey.perelman.notepad2.list.ElementType.SCRIPT;
import static com.matvey.perelman.notepad2.list.ElementType.FOLDER;
import static com.matvey.perelman.notepad2.list.ElementType.TEXT;

public class CreatorDialog extends DialogFragment {
    private TextView tv_dialog_name;
    private RadioButton btn_file;
    private RadioButton btn_folder;
    private RadioButton btn_executable;
    private TextInputLayout layout;
    private TextInputEditText name_text;
    private ImageButton btn_delete;
    private ImageButton btn_cut;
    private MainActivity activity;
    public CreatorElement element;
    private boolean editing;
    private boolean just_started;
    private long parent;
    private boolean paste_available;

    public static CreatorDialog createInstance(MainActivity activity){
        CreatorDialog dialog = new CreatorDialog();
        dialog.activity = activity;
        dialog.element = new CreatorElement();
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_creator, container, false);
        btn_file = view.findViewById(R.id.rbtn_file);
        btn_folder = view.findViewById(R.id.rbtn_folder);
        btn_executable = view.findViewById(R.id.rbtn_executable);
        layout = view.findViewById(R.id.name_layout);
        name_text = layout.findViewById(R.id.name_text);
        Button btn_create = view.findViewById(R.id.btn_create);
        btn_delete = view.findViewById(R.id.btn_delete);
        btn_cut = view.findViewById(R.id.btn_cut);
        tv_dialog_name = view.findViewById(R.id.tv_dialog_name);

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
                element.updateType(SCRIPT);
            }
        });

        btn_create.setOnClickListener((v)->{
            String name = (name_text.getText() == null) ? "" : name_text.getText().toString().trim();
            if(editing){
                if(!name.isEmpty()) {
                    element.updateName(name);
                    activity.update_element(element);
                }
            }else {
                if (name.isEmpty()) {
                    switch (element.getType()) {
                        case TEXT:
                            name = activity.getString(R.string.new_file_text);
                            break;
                        case FOLDER:
                            name = activity.getString(R.string.new_folder_text);
                            break;
                        case SCRIPT:
                            name = activity.getString(R.string.new_executable_text);
                            break;
                    }
                }
                element.updateName(name);
                activity.create_element(element);
            }
            dismiss();
            activity.hideKeyboard();
        });
        btn_delete.setOnClickListener((v)->{
            dismiss();
            activity.delete_element(element);
        });
        btn_cut.setOnClickListener((v)->{
            dismiss();
            if(editing){
                activity.cut_element(element);
            }else{
                activity.paste_element();
            }
        });
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        just_started = true;
    }

    @Override
    public void onStart() {
        super.onStart();
        if(!just_started)
            return;
        just_started = false;
        if(editing){
            setChecked();
            tv_dialog_name.setText(R.string.edit_item_text);
            name_text.setText(element.getName());
            btn_cut.setImageResource(R.drawable.cut_image);
            btn_cut.setEnabled(true);
            btn_delete.setVisibility(View.VISIBLE);
        }else{
            element.setType(TEXT);
            element.parent = parent;
            setChecked();
            tv_dialog_name.setText(R.string.create_item_text);
            element.setName("");
            name_text.setText("");
            btn_cut.setImageResource(R.drawable.paste_image);
            btn_cut.setEnabled(paste_available);
            btn_delete.setVisibility(View.INVISIBLE);
        }
    }

    public void startCreating(long parent, boolean paste_available){
        if(!isAdded()) {
            editing = false;
            this.parent = parent;
            this.paste_available = paste_available;
            show(activity.getSupportFragmentManager(), "creator");
        }
    }
    public void startEditing(DatabaseElement element){
        if(!isAdded()) {
            this.element.set(element);
            editing = true;
            show(activity.getSupportFragmentManager(), "creator");
        }
    }
    private void setChecked(){
        ElementType type = element.getTypeStart();
        switch (type){
            case TEXT:
                btn_file.setChecked(true);
                break;
            case FOLDER:
                btn_folder.setChecked(true);
                break;
            case SCRIPT:
                btn_executable.setChecked(true);
                break;
        }
        btn_file.setEnabled(type != FOLDER);
        btn_folder.setEnabled(type == FOLDER || !editing);
        btn_executable.setEnabled(type != FOLDER);
    }
}
