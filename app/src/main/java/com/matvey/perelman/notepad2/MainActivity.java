package com.matvey.perelman.notepad2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.matvey.perelman.notepad2.creator.CreatorElement;
import com.matvey.perelman.notepad2.list.ActionType;
import com.matvey.perelman.notepad2.list.Adapter;
import com.matvey.perelman.notepad2.creator.CreatorDialog;
import com.matvey.perelman.notepad2.list.ElementType;
import com.matvey.perelman.notepad2.utils.StringEncoder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    public ConstraintLayout root_layout;
    private Adapter adapter;
    private Menu menu;
    public TextInputEditText text_search;
    public CreatorDialog creator_dialog;
    public int to_update_index;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //preparing
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        root_layout = findViewById(R.id.root_layout);
        //recycler view
        adapter = new Adapter(this);
        RecyclerView rv = findViewById(R.id.list_view);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        //search edit text
        text_search = findViewById(R.id.search_input_et);
        text_search.setOnEditorActionListener((v, actionId, event) -> {
            adapter.cursor.searchParamChanged((text_search.getText() == null) ? "" : text_search.getText().toString());
            return true;
        });
        //creator_dialog
        creator_dialog = new CreatorDialog(this);
        //fab
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener((vie) -> {
            creator_dialog.element.setType((adapter.cursor.layer() > 0) ? ElementType.TEXT : ElementType.FOLDER);
            creator_dialog.startCreating();
        });
        //load state
        loadState();
        to_update_index = -1;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        menu.getItem(adapter.actionType.ordinal()).setChecked(true);
        menu.getItem(3).setChecked(adapter.ask_before_delete);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        item.setChecked(!item.isChecked());
        if (item == menu.getItem(0)) {
            adapter.setActionType(ActionType.DISABLED);
        } else if (item == menu.getItem(1)) {
            adapter.setActionType(ActionType.DELETE);
        } else if (item == menu.getItem(2)) {
            adapter.setActionType(ActionType.SETTINGS);
        } else if (item == menu.getItem(3)) {
            adapter.ask_before_delete = item.isChecked();
        }
        return false;
    }

    public void start_editor(int id, int idx, String name, String content) {
        Intent intent = new Intent(this, EditorActivity.class);
        intent.putExtra("id", id);
        to_update_index = idx;
        intent.putExtra("name", name);
        intent.putExtra("content", content);
        intent.putExtra("database_name", StringEncoder.encode(adapter.cursor.c.path.get(0)));
        intent.setAction(Intent.ACTION_VIEW);
        startActivity(intent);
    }

    public void update_element(CreatorElement element, boolean editing) {
        if (editing)
            adapter.cursor.updateElement(element);
        else
            adapter.cursor.newElement(element);
    }

    public void loadState() {
        SharedPreferences sp = getSharedPreferences("saved_state", Context.MODE_PRIVATE);
        adapter.actionType = ActionType.values()[sp.getInt("action_type", 0)];
        adapter.ask_before_delete = sp.getBoolean("ask_before_delete", true);
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(root_layout.getRootView().getWindowToken(), 0);
        root_layout.getRootView().clearFocus();
    }

    public void saveState() {
        SharedPreferences.Editor editor = getSharedPreferences("saved_state", Context.MODE_PRIVATE).edit();
        editor.putInt("action_type", adapter.actionType.ordinal());
        editor.putBoolean("ask_before_delete", adapter.ask_before_delete);
        editor.apply();
    }
    public void makeToast(String text, boolean lon){
        runOnUiThread(()-> Toast.makeText(this, text, lon?Toast.LENGTH_LONG:Toast.LENGTH_SHORT).show());
    }
    @Override
    protected void onStart() {
        super.onStart();
        if (to_update_index != -1) {
            adapter.cursor.reloadData();
            adapter.notifyItemChanged(to_update_index);
        }
    }

    @Override
    public void onBackPressed() {
        if (adapter.cursor.back())
            super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        saveState();
        adapter.onClose();
        creator_dialog.hide();
    }
}