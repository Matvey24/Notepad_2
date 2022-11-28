package com.matvey.perelman.notepad2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.matvey.perelman.notepad2.executor.InputDialog;
import com.matvey.perelman.notepad2.list.Adapter;
import com.matvey.perelman.notepad2.creator.CreatorDialog;
import com.matvey.perelman.notepad2.utils.threads.Locker;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {
    private static final String SP_NAME = "saved_state";
    private static final String SP_ASK_BEFORE_DELETE = "ask_before_delete";
    private static final String SP_ON_CREATE_ENABLED = "on_create_enabled";
    private static final String BUNDLE_VIEW_PATH = "view_path";

    public ConstraintLayout root_layout;
    public Adapter adapter;
    private TextView title;
    private Menu menu;
    public CreatorDialog creator_dialog;

    private boolean isStopped = true;

    private boolean menu_created;
    private TreeMap<String, String> menu_sets;

    private ActivityResultLauncher<Intent> editor_launcher;

    private final Object input_lock = new Object();
    private InputDialog input_dialog = null;
    private Locker lock, menu_lock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        menu_created = false;
        //preparing
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        root_layout = findViewById(R.id.root_layout);
        lock = new Locker();
        menu_lock = new Locker();
        super.setTitle(null);
        title = findViewById(R.id.toolbar_title);
        //load sp
        SharedPreferences sp = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        boolean ask_before_delete = sp.getBoolean(SP_ASK_BEFORE_DELETE, true);
        boolean onCreateEnabled = sp.getBoolean(SP_ON_CREATE_ENABLED, false);
        //menu
        menu_sets = new TreeMap<>();
        //recycler view
        long idt = 0;
        if (savedInstanceState != null)
            idt = savedInstanceState.getLong(BUNDLE_VIEW_PATH, 0);
        adapter = new Adapter(this, idt, onCreateEnabled);
        adapter.ask_before_delete = ask_before_delete;
        RecyclerView rv = findViewById(R.id.list_view);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        //creator_dialog
        creator_dialog = CreatorDialog.createInstance(this);
        //fab
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener((vie) -> {
            if (!isStopped)
                creator_dialog.startCreating(adapter.cursor.getPathID(), adapter.path_to_cut != null);
        });
        fab.setOnLongClickListener(v -> {
            adapter.quick_new_note();
            return true;
        });

        //editor launcher
        editor_launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    long id = result.getData().getLongExtra("id", -1);
                    adapter.cursor.onChangeItem(id);
                });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putLong(BUNDLE_VIEW_PATH, adapter.cursor.getPathID());
        super.onSaveInstanceState(outState);
    }

    @SuppressWarnings("UnusedDeclaration")
    private Menu getMenu() {
        if (!menu_created) {
            if(Thread.currentThread() == getMainLooper().getThread()){
                throw new RuntimeException(getString(R.string.error_wait_in_main_thread, "menu"));
            }else {
                menu_lock.lock();
            }
        }
        return menu;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void registerButton(String name, String path) {
        if(name == null || path == null)
            throw new NullPointerException("name|path is none");
        Menu menu = getMenu();
        runOnUiThread(() -> {
            if (!menu_sets.containsKey(name))
                menu.add(Menu.NONE, menu.getItem(menu.size() - 1).getItemId() + 1, Menu.NONE, name);
            menu_sets.put(name, path);
        });
    }

    @SuppressWarnings("UnusedDeclaration")
    public boolean unregisterButton(String name) {
        if(name == null)
            throw new NullPointerException("name is none");
        if (!menu_sets.containsKey(name))
            return false;
        menu_sets.remove(name);
        for(int i = 0; i < menu.size(); ++i){
            if(menu.getItem(i).getTitle().equals(name)){
                int id = menu.getItem(i).getItemId();
                runOnUiThread(()->menu.removeItem(id));
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onCreateEnabled(boolean createEnabled) {
        SharedPreferences.Editor editor = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).edit();
        editor.putBoolean(SP_ON_CREATE_ENABLED, createEnabled);
        editor.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        menu.getItem(0).setChecked(adapter.ask_before_delete);
        menu_created = true;
        menu_lock.free();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_ask_before_delete) {
            item.setChecked(!item.isChecked());
            adapter.ask_before_delete = item.isChecked();
            SharedPreferences.Editor editor = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE).edit();
            editor.putBoolean(SP_ASK_BEFORE_DELETE, adapter.ask_before_delete);
            editor.apply();
            return false;
        } else if (item.getItemId() == R.id.menu_btn_help) {
            adapter.goHelp();
            return false;
        }
        String path = menu_sets.get(item.getTitle().toString());
        if (path == null)
            return false;
        adapter.runPath(path, true);
        return false;
    }

    public void startEditor(long id, @NonNull String name) {
        Intent intent = new Intent(this, EditorActivity.class);
        intent.putExtra("id", id);
        intent.putExtra("name", name);
        editor_launcher.launch(intent);
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(root_layout.getRootView().getWindowToken(), 0);
        root_layout.getRootView().clearFocus();
    }
    public void makeToast(@NonNull String text, boolean len) {
        runOnUiThread(() -> Toast.makeText(this, text, len ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onStart() {
        super.onStart();
        isStopped = false;
        adapter.makeStarted();
    }

    @SuppressWarnings("UnusedDeclaration")
    public String showInputDialog(String input_name) {
        synchronized (input_lock) {
            if (isStopped)
                throw new RuntimeException(getString(R.string.error_stopped_input));
            if (Thread.currentThread() == getMainLooper().getThread())
                throw new RuntimeException(getString(R.string.error_wait_in_main_thread, "input"));
            runOnUiThread(() -> {
                if (input_dialog == null)
                    input_dialog = InputDialog.createInstance(this);
                input_dialog.start(input_name);
            });
            lock.lock();
            return input_dialog.getString();
        }
    }

    public void ui_barrier_wait() {
        lock.free();
    }

    public void th_barrier_await() {
        lock.lock();
    }

    @Override
    protected void onStop() {
        isStopped = true;
        super.onStop();
    }

    @Override
    public void setTitle(CharSequence title) {
        this.title.setText(title);
    }

    @Override
    public void onBackPressed() {
        if (adapter.back())
            super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        creator_dialog.onDestroy();
        if (input_dialog != null)
            input_dialog.onDestroy();
        adapter.close();
        super.onDestroy();
    }
}