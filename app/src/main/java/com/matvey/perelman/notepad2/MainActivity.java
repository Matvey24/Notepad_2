package com.matvey.perelman.notepad2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.matvey.perelman.notepad2.creator.CreatorElement;
import com.matvey.perelman.notepad2.executor.InputDialog;
import com.matvey.perelman.notepad2.list.Adapter;
import com.matvey.perelman.notepad2.creator.CreatorDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.util.concurrent.CyclicBarrier;

public class MainActivity extends AppCompatActivity {
    public ConstraintLayout root_layout;
    private Adapter adapter;
    private Menu menu;
    public CreatorDialog creator_dialog;

    public int to_update_index;

    private String path_to_cut;

    private boolean isStopped;

    private InputDialog input_dialog = null;
    private CyclicBarrier barrier = null;

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
        //creator_dialog
        creator_dialog = CreatorDialog.createInstance(this);
        //fab
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener((vie) -> {
            if(!isStopped)
                creator_dialog.startCreating(adapter.cursor.getPathID(),  path_to_cut != null);
        });
        fab.setOnLongClickListener(v -> {
            adapter.quick_new_note();
            return true;
        });
        //load state
        loadState();
        to_update_index = -1;
        path_to_cut = null;
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
        item.setChecked(!item.isChecked());
        if (item == menu.getItem(0)) {
            adapter.ask_before_delete = item.isChecked();
        } else if (item == menu.getItem(1)) {
            adapter.goHelp();
        }
        return false;
    }

    public void start_editor(long id, int idx, String name, String content) {
        Intent intent = new Intent(this, EditorActivity.class);
        intent.putExtra("id", id);
        to_update_index = idx;
        intent.putExtra("name", name);
        intent.putExtra("content", content);
        intent.setAction(Intent.ACTION_VIEW);
        startActivity(intent);
    }
    public void create_element(CreatorElement element){
        adapter.connection.newElement(element);
    }
    public void update_element(CreatorElement element) {
        adapter.connection.updateElement(element);
    }
    public void delete_element(CreatorElement element){
        adapter.onClickDelete(element.getNameStart(), element.parent, element.id);
    }
    public void cut_element(CreatorElement element){
        path_to_cut = adapter.path_concat(adapter.cursor.path_t, element.getNameStart());
    }
    public void paste_element(){
        if(adapter.moveHere(path_to_cut))
            path_to_cut = null;
    }
    public void loadState() {
        SharedPreferences sp = getSharedPreferences("saved_state", Context.MODE_PRIVATE);
        adapter.ask_before_delete = sp.getBoolean("ask_before_delete", true);
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(root_layout.getRootView().getWindowToken(), 0);
        root_layout.getRootView().clearFocus();
    }

    public void saveState() {
        SharedPreferences.Editor editor = getSharedPreferences("saved_state", Context.MODE_PRIVATE).edit();
        editor.putBoolean("ask_before_delete", adapter.ask_before_delete);
        editor.apply();
    }
    public void makeToast(String text, boolean lon){
        runOnUiThread(()-> Toast.makeText(this, text, lon?Toast.LENGTH_LONG:Toast.LENGTH_SHORT).show());
    }
    @Override
    protected void onStart() {
        super.onStart();
        isStopped = false;
        if (to_update_index != -1) {
            adapter.cursor.reloadData();
            adapter.notifyItemChanged(to_update_index);
        }
    }

    public synchronized String showInputDialog(String input_name){
        if(barrier == null)
            barrier = new CyclicBarrier(2);
        runOnUiThread(()->{
            if(input_dialog == null)
                input_dialog = InputDialog.createInstance(this);
            input_dialog.start(input_name);
        });
        th_barrier_await();
        return input_dialog.getString();
    }
    public void ui_barrier_wait(){
        if(barrier.getNumberWaiting() == 1)
            th_barrier_await();
    }
    public void th_barrier_await(){
        try{
            barrier.await();
        }catch (Exception ignored) {}
    }
    @Override
    protected void onStop() {
        super.onStop();
        isStopped = true;
    }

    @Override
    public void onBackPressed() {
        if (adapter.back())
            super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        creator_dialog.onDestroy();
        if(input_dialog != null) {
            input_dialog.onDestroy();
        }
        saveState();
        adapter.onClose();
        super.onDestroy();
    }
}