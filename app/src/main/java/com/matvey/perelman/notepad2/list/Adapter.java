package com.matvey.perelman.notepad2.list;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.material.snackbar.Snackbar;
import com.matvey.perelman.notepad2.MainActivity;
import com.matvey.perelman.notepad2.R;
import com.matvey.perelman.notepad2.database.connection.DatabaseConnection;
import com.matvey.perelman.notepad2.database.DatabaseElement;
import com.matvey.perelman.notepad2.creator.CreatorDialog;
import com.matvey.perelman.notepad2.database.connection.DatabaseCursor;
import com.matvey.perelman.notepad2.database.connection.ViewListener;
import com.matvey.perelman.notepad2.utils.threads.Tasks;

public class Adapter extends RecyclerView.Adapter<MyViewHolder> {
    public boolean ask_before_delete = true;
    public final DatabaseConnection connection;
    public final DatabaseCursor cursor;
    private final MainActivity main_activity;

    private final Executor executor;

    private final Tasks tasks;

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
                else {
                    String s = cursor.path_t;
                    if(s.length() > 25)
                        s = s.substring(s.length() - 25);
                    main_activity.setTitle(s);

                }
            }

            @Override
            public void onPathChanged() {
                onPathRenamed();
                notifyDataSetChanged();
            }

        }, main_activity);

        this.main_activity = main_activity;
        tasks = new Tasks(Runtime.getRuntime().availableProcessors());
        executor = new Executor(connection);
        tasks.runTask(() -> {
            if (!Python.isStarted())
                Python.start(new AndroidPlatform(main_activity));

            PythonAPI.activity = main_activity;
            PythonAPI.executor = executor;
            Python p = Python.getInstance();
            executor.setPython(p.getModule("python_api"));
        });
    }

    public void onClickDelete(String name, long parent, long id) {
        if (ask_before_delete) {
            Snackbar.make(main_activity.root_layout,
                    String.format(main_activity.getString(R.string.ask_delete), name),
                    Snackbar.LENGTH_SHORT)
                    .setAction(R.string.action_delete, (view) -> tasks.runTask(()->connection.deleteElement(parent, id)))
                    .show();
        } else
            tasks.runTask(()->connection.deleteElement(parent, id));
    }

    public void onClickSettings(DatabaseElement element) {
        CreatorDialog d = main_activity.creator_dialog;
        d.element.set(element);
        d.startEditing();
    }

    public boolean moveHere(String entry_path) {
        try {
            executor.move(entry_path, cursor.path_t);
            return true;
        } catch (RuntimeException e) {
            main_activity.makeToast(e.getMessage(), true);
            return false;
        }
    }

    public boolean back() {
        return cursor.backUI();
    }

    public void runFile(long parent, long id) {
        tasks.runTask(() -> {
            executor.begin(id, parent);
        });
    }

    public void onClickField(DatabaseElement element, int position) {
        if (element.type == ElementType.FOLDER) {
            cursor.enterUI(element.id);
        } else if (element.type == ElementType.TEXT) {
            String content = connection.getContent(element.id);
            main_activity.start_editor(element.id, position, element.name, content);
        } else if (element.type == ElementType.EXECUTABLE) {
            runFile(element.parent, element.id);
        }
    }

    public void goHelp() {
        tasks.runTask(()-> {
            String json = main_activity.getString(R.string.help_text);
            executor.makeDatabase(json);
            long id = connection.getID(0, "Help");
            main_activity.runOnUiThread(()->cursor.enterUI(id));
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
        return executor.path_concat(path1, path2);
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