package de.baumann.browser.activity;

import static de.baumann.browser.database.UserScript.META_BEGIN;

import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Objects;

import de.baumann.browser.R;
import de.baumann.browser.database.UserScript;
import de.baumann.browser.database.UserScriptsHelper;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.view.NinjaToast;
import de.baumann.browser.view.RecyclerOverviewListAdapter;

public class Manage_UserScripts extends AppCompatActivity {

    private RecyclerOverviewListAdapter adapter;
    private RecyclerView recyclerView;
    private List<UserScript> userScripts;
    private ItemTouchHelper.Callback callback;
    private ItemTouchHelper touchHelper;
    private EditText editText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        HelperUnit.initTheme(this);
        setContentView(R.layout.activity_manage_userscripts);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        UserScriptsHelper userScriptsHelper = new UserScriptsHelper(this);
        userScripts = userScriptsHelper.getAllScripts();

        editText = findViewById(R.id.whitelist_edit);
        recyclerView = findViewById(R.id.recycler_view);

        adapter = new RecyclerOverviewListAdapter(this, userScripts,editText);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        callback = new ItemTouchHelper.Callback() {
            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return false;
            }  //Swipe not used at the moment

            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                return makeMovementFlags(dragFlags, swipeFlags);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                adapter.onItemMove(viewHolder.getBindingAdapterPosition(), target.getBindingAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                //adapter.onItemDismiss(viewHolder.getBindingAdapterPosition()); //Swipe not used at the moment
            }
        };

        touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

        Button button = findViewById(R.id.whitelist_add);
        button.setOnClickListener(v -> {
            String userscript = editText.getText().toString().trim();
            if (userscript.isEmpty()) {
                NinjaToast.show(Manage_UserScripts.this, R.string.toast_input_empty);
            } else if (!userscript.contains(META_BEGIN)) {
                NinjaToast.show(Manage_UserScripts.this, R.string.app_error);
            } else {
                UserScript newScript = new UserScript(-1,userscript,UserScript.getTypefromScript(userscript),userScriptsHelper.getMaxRank()+1,true);
                int id = userScriptsHelper.addScript(newScript);
                newScript.setId(id);
                userScripts.add(newScript);
                adapter.notifyDataSetChanged();
            }
        }) ;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_whitelist, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {

        if (menuItem.getItemId() == android.R.id.home) {
            finish();
        } else if (menuItem.getItemId() == R.id.menu_clear) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setMessage(R.string.hint_database);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
                UserScriptsHelper userScriptsHelper = new UserScriptsHelper(this);
                userScriptsHelper.deleteAllScripts();
                userScripts.clear();
                adapter.notifyDataSetChanged();
            });
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        }
        return true;
    }
}