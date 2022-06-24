package com.matvey.perelman.notepad2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.matvey.perelman.notepad2.executor.InputDialog;
import com.matvey.perelman.notepad2.list.Adapter;
import com.matvey.perelman.notepad2.creator.CreatorDialog;

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

import java.util.concurrent.CyclicBarrier;

public class MainActivity extends AppCompatActivity {
    public ConstraintLayout root_layout;
    public Adapter adapter;
    private TextView title;
    private Menu menu;
    public CreatorDialog creator_dialog;

    private boolean isStopped;

    private ActivityResultLauncher<Intent> editor_launcher;

    private InputDialog input_dialog = null;
    private CyclicBarrier barrier = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //preparing
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        root_layout = findViewById(R.id.root_layout);
        super.setTitle(null);
        title = findViewById(R.id.toolbar_title);
        //recycler view
        adapter = new Adapter(this, 0);
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
        //load delete info
        SharedPreferences sp = getSharedPreferences("saved_state", Context.MODE_PRIVATE);
        adapter.ask_before_delete = sp.getBoolean("ask_before_delete", true);

        //editor launcher
        editor_launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    long id = result.getData().getLongExtra("id", -1);
                    adapter.cursor.onChangeItem(id);
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        menu.getItem(0).setChecked(adapter.ask_before_delete);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item == menu.getItem(0)) {
            item.setChecked(!item.isChecked());
            adapter.ask_before_delete = item.isChecked();
            SharedPreferences.Editor editor = getSharedPreferences("saved_state", Context.MODE_PRIVATE).edit();
            editor.putBoolean("ask_before_delete", adapter.ask_before_delete);
            editor.apply();
        } else if (item == menu.getItem(1)) {
            adapter.goHelp();
        }
        return false;
    }

    public void start_editor(long id, String name) {
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

    public void makeToast(String text, boolean len) {
        runOnUiThread(() -> Toast.makeText(this, text, len ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onStart() {
        super.onStart();
        isStopped = false;
    }

    public synchronized String showInputDialog(String input_name) {
        if (isStopped)
            throw new RuntimeException(getString(R.string.error_stopped_input));
        if (barrier == null)
            barrier = new CyclicBarrier(2);
        runOnUiThread(() -> {
            if (input_dialog == null)
                input_dialog = InputDialog.createInstance(this);
            input_dialog.start(input_name);
        });
        th_barrier_await();
        return input_dialog.getString();
    }

    public void ui_barrier_wait() {
        if (barrier.getNumberWaiting() == 1)
            th_barrier_await();
    }

    public void th_barrier_await() {
        try {
            barrier.await();
        } catch (Exception ignored) {
        }
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