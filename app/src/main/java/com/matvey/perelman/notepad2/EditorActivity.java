package com.matvey.perelman.notepad2;

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.matvey.perelman.notepad2.database.connection.DatabaseConnection;
import com.matvey.perelman.notepad2.database.helpers.DatabaseHelper;

import java.util.ArrayList;

public class EditorActivity extends AppCompatActivity {
    private SQLiteDatabase database;

    private EditText text_editor;
    private MenuItem btn_save, btn_rollback, btn_rollforward, btn_kso;

    private long id;

    private ArrayList<String> versions;
    private int position;
    private boolean save_changed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        versions = new ArrayList<>();
        position = 0;
        save_changed = false;
        text_editor = findViewById(R.id.editor_text);
        
        Intent intent = getIntent();
        setTitle(intent.getStringExtra("name"));
        id = intent.getLongExtra("id", -1);


        database = new DatabaseHelper(this).getWritableDatabase();
        versions.add(DatabaseConnection.getContent(database, id));
        text_editor.setText(versions.get(0));

        text_editor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                save_changed = true;
            }
        });
        setResult(Activity.RESULT_OK, intent);
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(text_editor.getRootView().getWindowToken(), 0);
        text_editor.getRootView().clearFocus();
    }

    private void makeVersion() {
        if(save_changed) {
            String text = text_editor.getText().toString();
            while (versions.size() - 1 > position)
                versions.remove(versions.size() - 1);
            if(!versions.get(position).equals(text)) {
                versions.add(text);
                position = versions.size() - 1;
            }
            btn_save.setTitle(getString(R.string.action_save) + " (" + position + ")");
            btn_rollback.setTitle(getString(R.string.action_rollback) + " (" + position + ")");
            save_changed = false;
        }
    }

    private void rollback() {
        makeVersion();
        if(position == 0)
            return;
        text_editor.setText(versions.get(--position));
        btn_rollback.setTitle(getString(R.string.action_rollback) + " (" + position + ")");
        save_changed = false;
    }

    private void rollForward() {
        if(position != versions.size() - 1){
            text_editor.setText(versions.get(++position));
            save_changed = false;
            btn_rollback.setTitle(getString(R.string.action_rollback) + " (" + position + ")");
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        makeVersion();
        DatabaseConnection.resetTextData(database, id, versions.get(position));
        hideKeyboard();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item == btn_save) {
            makeVersion();
        } else if (item == btn_rollback) {
            rollback();
        } else if (item == btn_rollforward) {
            rollForward();
        } else if (item == btn_kso){
            btn_kso.setChecked(!btn_kso.isChecked());
            getWindow().setFlags(btn_kso.isChecked()?128:0, 128);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        btn_save = menu.getItem(0);
        btn_rollback = menu.getItem(1);
        btn_rollforward = menu.getItem(2);
        btn_kso = menu.getItem(3);
        return true;
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }
}
