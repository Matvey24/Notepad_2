package com.matvey.perelman.notepad2.list;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.material.snackbar.Snackbar;
import com.matvey.perelman.notepad2.MainActivity;
import com.matvey.perelman.notepad2.R;
import com.matvey.perelman.notepad2.creator.CreatorElement;
import com.matvey.perelman.notepad2.database.connection.DatabaseConnection;
import com.matvey.perelman.notepad2.database.DatabaseElement;
import com.matvey.perelman.notepad2.database.connection.DatabaseCursor;
import com.matvey.perelman.notepad2.database.connection.ViewListener;
import com.matvey.perelman.notepad2.executor.Executor;
import com.matvey.perelman.notepad2.utils.threads.Tasks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

public class Adapter extends RecyclerView.Adapter<MyViewHolder> {
    public boolean ask_before_delete = true;
    public final DatabaseConnection connection;
    public final DatabaseCursor cursor;
    private final MainActivity main_activity;

    private final Stack<Executor> executors;

    private final Tasks tasks;
    private PyObject space;
    public String path_to_cut;

    public Adapter(MainActivity main_activity) {
        connection = new DatabaseConnection(main_activity);
        cursor = connection.makeCursor(new ViewListener() {
            @Override
            public void onNewItem(int idx) {
                if (cursor.length() == 1)
                    notifyItemRemoved(0);
                notifyItemInserted(idx);
            }

            @Override
            public void onDeleteItem(int idx) {
                notifyItemRemoved(idx);
                if (cursor.length() == 0)
                    notifyItemInserted(0);
            }

            @Override
            public void onChangeItem(int idx_start, int idx_end) {
                if (idx_start != idx_end)
                    notifyItemMoved(idx_start, idx_end);
                notifyItemChanged(idx_end);
            }

            @Override
            public void onPathRenamed() {
                if (cursor.getPathID() == 0)
                    main_activity.setTitle(R.string.app_name);
                else
                    main_activity.setTitle(cursor.path_t);
            }

            @Override
            public void onPathChanged() {
                onPathRenamed();
                notifyDataSetChanged();
            }

        }, main_activity);

        this.main_activity = main_activity;
        tasks = new Tasks(Integer.MAX_VALUE);
        executors = new Stack<>();
        tasks.runTask(() -> {
            if (!Python.isStarted())
                Python.start(new AndroidPlatform(main_activity));
            space = Python.getInstance().getModule("python_api").callAttr("__java_api_make_dict");
        });
    }

    public void onClickDelete(String name, long parent, long id) {
        if (ask_before_delete) {
            Snackbar.make(main_activity.root_layout,
                    main_activity.getString(R.string.ask_delete, name),
                    Snackbar.LENGTH_SHORT)
                    .setAction(R.string.action_delete, (view) -> connection.deleteElement(parent, id))
                    .show();
        } else
            tasks.runTask(() -> connection.deleteElement(parent, id));
    }

    public void onClickSettings(DatabaseElement element) {
        main_activity.creator_dialog.startEditing(element);
    }
    public void cut_element(CreatorElement element){
        path_to_cut = path_concat(cursor.path_t, element.getNameStart());
    }
    public void moveHere() {
        Executor e = allocExecutor();
        try {
            e.move(path_to_cut, cursor.path_t);
            path_to_cut = null;
        }catch (RuntimeException ex){
            main_activity.makeToast(ex.toString(), true);
        }
        freeExecutor(e);
    }
    public boolean back() {
        return cursor.backUI();
    }

    private Executor allocExecutor() {
        synchronized (executors) {
            if (executors.isEmpty())
                return new Executor(connection, main_activity, Python.getInstance().getModule("python_api"), space);
            else
                return executors.pop();
        }
    }

    private void freeExecutor(Executor ex) {
        synchronized (executors) {
            executors.push(ex);
        }
    }

    public void runFile(long parent, long id) {
        tasks.runTask(() -> {
            Executor e = allocExecutor();
            e.begin(id, parent);
            freeExecutor(e);
        });
    }

    public void quick_new_note() {
        CreatorElement celement = new CreatorElement();
        celement.setType(ElementType.TEXT);
        celement.setName(main_activity.getString(R.string.new_file_text));
        celement.parent = cursor.getPathID();
        long id = connection.newElement(celement);
        main_activity.start_editor(id, cursor.indexOf(id), connection.getName(id), connection.getContent(id));
    }

    public void onClickField(DatabaseElement element, int position) {
        if (element.type == ElementType.FOLDER) {
            cursor.enterUI(element.id);
        } else if (element.type == ElementType.TEXT) {
            String content = connection.getContent(element.id);
            main_activity.start_editor(element.id, position, element.name, content);
        } else if (element.type == ElementType.SCRIPT) {
            runFile(element.parent, element.id);
        }
    }

    public void goHelp() {
        tasks.runTask(() -> {
            String help_name = main_activity.getString(R.string.folder_help_name);
            long id = connection.getID(0, help_name);
            if (id == -1) {
                String json;
                try{
                    Resources r = main_activity.getResources();
                    InputStream is = r.openRawResource(R.raw.help_text);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int i = is.read();
                    while(i != -1){
                        baos.write(i);
                        i = is.read();
                    }
                    json = baos.toString();
                }catch (IOException e){
                    e.printStackTrace();
                    return;
                }
                Executor e = allocExecutor();
                e.mkdir("/" + help_name);
                long ui_pos = connection.getID(0, help_name);
                main_activity.runOnUiThread(()->{
                    cursor.enterUI(ui_pos);
                    tasks.runTask(()->{
                        e.makeDatabase(json);
                        freeExecutor(e);
                    });
                });
                return;
            }
            ElementType type = connection.getType(id);
            if(type != ElementType.FOLDER)
                id = 0;
            long ui_pos = id;
            main_activity.runOnUiThread(() -> cursor.enterUI(ui_pos));
        });
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.view_element, parent, false), this);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        if (cursor.length() != 0) {
            cursor.getElement(holder.element, position);
            holder.setValues();
        } else {
            String name;
            if (cursor.getPathID() == 0)
                name = main_activity.getString(R.string.app_name);
            else
                name = connection.getName(cursor.getPathID());
            holder.setError(String.format(main_activity.getString(R.string.empty_folder_view), name), "");
        }
    }

    public String path_concat(String path1, String path2) {
        return Executor.path_concat(path1, path2);
    }

    @Override
    public int getItemCount() {
        int count = cursor.length();
        if (count == 0)
            return 1;
        return count;
    }

    public void onClose() {
        tasks.runTask(connection::close);
        tasks.disposeOnFinish();
    }
}