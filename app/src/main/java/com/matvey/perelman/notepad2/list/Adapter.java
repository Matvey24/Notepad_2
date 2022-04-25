package com.matvey.perelman.notepad2.list;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.material.snackbar.Snackbar;
import com.matvey.perelman.notepad2.MainActivity;
import com.matvey.perelman.notepad2.R;
import com.matvey.perelman.notepad2.database.callback.ViewUpdater;
import com.matvey.perelman.notepad2.database.connection.DatabaseConnection;
import com.matvey.perelman.notepad2.database.callback.CursorUpdatable;
import com.matvey.perelman.notepad2.database.DatabaseElement;
import com.matvey.perelman.notepad2.creator.CreatorDialog;
import com.matvey.perelman.notepad2.creator.CreatorElement;
import com.matvey.perelman.notepad2.database.callback.ViewListener;
import com.matvey.perelman.notepad2.utils.threads.Tasks;

public class Adapter extends RecyclerView.Adapter<MyViewHolder> {
    public ActionType actionType = ActionType.DISABLED;
    public boolean ask_before_delete = true;
    private final DatabaseConnection connection;
    public final CursorUpdatable cursor;
    private final DatabaseElement element_buff;
    private final MainActivity main_activity;

    private final Executor executor;

    private final Tasks tasks;

    public Adapter(MainActivity main_activity) {
        connection = new DatabaseConnection(main_activity, new ViewUpdater());
        cursor = new CursorUpdatable(connection, new ViewListener() {
            @Override
            public void onNewItem(int idx) {
                main_activity.runOnUiThread(() -> {
                    if (cursor.length() == 1)
                        notifyItemRemoved(0);
                    notifyItemInserted(idx);
                });
            }

            @Override
            public void onDeleteItem(int idx) {
                main_activity.runOnUiThread(() -> {
                    notifyItemRemoved(idx);
                    if (cursor.length() == 0)
                        notifyItemInserted(0);
                });
            }

            @Override
            public void onChangeItem(int idx_start, int idx_end) {
                main_activity.runOnUiThread(()-> {
                    if (idx_start != idx_end)
                        notifyItemMoved(idx_start, idx_end);
                    notifyItemChanged(idx_end);
                });
            }

            @Override
            public void onPathChanged() {
                if (cursor.layer() == 0)
                    main_activity.setTitle(R.string.app_name);
                else
                    main_activity.setTitle(cursor.path);
                notifyDataSetChanged();
            }
        });
        this.main_activity = main_activity;
        element_buff = new DatabaseElement();
        tasks = new Tasks();
        executor = new Executor();
        tasks.runTask(()->{
            if(!Python.isStarted())
                Python.start(new AndroidPlatform(main_activity));

            PythonAPI.activity = main_activity;
            PythonAPI.executor = executor;
            Python p = Python.getInstance();
            executor.setPython(p.getModule("python_api"));
        });
    }

    public void onClickAction(String name, int id, ElementType type, int position) {
        switch (actionType) {
            case DELETE:
                if (ask_before_delete) {
                    Snackbar.make(main_activity.root_layout,
                            String.format(main_activity.getString(R.string.ask_delete), name),
                            Snackbar.LENGTH_SHORT)
                            .setAction(R.string.action_delete, (view) -> cursor.deleteElement(id))
                            .show();
                } else
                    cursor.deleteElement(id);
                break;
            case SETTINGS:
                CreatorDialog d = main_activity.creator_dialog;
                d.element.setType(type);
                d.element.id = id;
                d.element.setIdx(position);
                d.element.setName(name);
                d.startEditing();
                break;
        }
    }

    public void runFile(int idx) {
        tasks.runTask(() -> {
            executor.begin(cursor, idx);
        });
    }

    public void onClickField(String name, int id, int position, ElementType type) {
        if (type == ElementType.FOLDER) {
            cursor.enter(position);
        } else if (type == ElementType.TEXT) {
            cursor.getElement(element_buff, position);
            main_activity.start_editor(id, position, name, element_buff.content);
        } else if (type == ElementType.EXECUTABLE) {
            runFile(position);
        }
    }

    public void setActionType(ActionType type) {
        this.actionType = type;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.view_element, parent, false), this);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        if (cursor.length() != 0) {
            cursor.getElement(element_buff, position);
            holder.setValues(element_buff, actionType);
        } else {
            String name;
            if (cursor.layer() == 0)
                name = main_activity.getString(R.string.app_name);
            else
                name = cursor.c.path.get(cursor.c.path.size() - 1);
            holder.setError(String.format(main_activity.getString(R.string.empty_folder_view), name), "");
        }
    }

    @Override
    public int getItemCount() {
        int count = cursor.length();
        if (count == 0)
            return 1;
        return count;
    }

    public void onClose() {
        connection.close();
        tasks.disposeOnFinish();
    }
}