package com.matvey.perelman.notepad2.executor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.matvey.perelman.notepad2.MainActivity;
import com.matvey.perelman.notepad2.R;

import java.util.Objects;

public class InputDialog extends DialogFragment {
    private TextInputLayout layout;
    private TextInputEditText input_text;
    private MainActivity activity;
    private String input_name;
    public static InputDialog createInstance(MainActivity activity){
        InputDialog dialog = new InputDialog();
        dialog.activity = activity;
        return dialog;
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_input, container, false);
        layout = view.findViewById(R.id.input_layout);
        input_text = layout.findViewById(R.id.input_text);

        input_text.setOnEditorActionListener((v, actionId, event) -> {
            super.dismiss();
            return true;
        });
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        layout.setHint(input_name);
        input_text.setText("");
    }

    @Override
    public void onStop() {
        super.onStop();
        activity.ui_barrier_wait();
    }

    public void start(String input_name) {
        this.input_name = input_name;
        super.show(activity.getSupportFragmentManager(), "input");
    }
    public String getString() {
        return Objects.requireNonNull(input_text.getText()).toString();
    }
}
