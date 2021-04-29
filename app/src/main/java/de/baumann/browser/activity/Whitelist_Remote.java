package de.baumann.browser.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Objects;

import de.baumann.browser.R;
import de.baumann.browser.browser.Remote;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.unit.RecordUnit;
import de.baumann.browser.view.WhitelistAdapter;
import de.baumann.browser.view.NinjaToast;

public class Whitelist_Remote extends AppCompatActivity {

    private WhitelistAdapter adapter;
    private List<String> list;
    private Remote remote;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        setContentView(R.layout.activity_whitelist);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        remote = new Remote(Whitelist_Remote.this);

        RecordAction action = new RecordAction(this);
        action.open(false);
        list = action.listDomains(RecordUnit.TABLE_REMOTE);
        action.close();

        ListView listView = findViewById(R.id.whitelist);
        listView.setEmptyView(findViewById(R.id.whitelist_empty));

        //noinspection NullableProblems
        adapter = new WhitelistAdapter(this, list){
            @Override
            public View getView (final int position, View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                ImageButton whitelist_item_cancel = v.findViewById(R.id.whitelist_item_cancel);
                whitelist_item_cancel.setOnClickListener(v1 -> {
                    remote.removeDomain(list.get(position));
                    list.remove(position);
                    notifyDataSetChanged();
                    NinjaToast.show(Whitelist_Remote.this, R.string.toast_delete_successful);
                });
                return v;
            }
        };
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        Button button = findViewById(R.id.whitelist_add);
        button.setOnClickListener(v -> {
            EditText editText = findViewById(R.id.whitelist_edit);
            String domain = editText.getText().toString().trim();
            if (domain.isEmpty()) {
                NinjaToast.show(Whitelist_Remote.this, R.string.toast_input_empty);
            } else if (!BrowserUnit.isURL(domain)) {
                NinjaToast.show(Whitelist_Remote.this, R.string.toast_invalid_domain);
            } else {
                RecordAction action1 = new RecordAction(Whitelist_Remote.this);
                action1.open(true);
                if (action1.checkDomain(domain, RecordUnit.TABLE_REMOTE)) {
                    NinjaToast.show(Whitelist_Remote.this, R.string.toast_domain_already_exists);
                } else {
                    remote.addDomain(domain.trim());
                    list.add(0, domain.trim());
                    adapter.notifyDataSetChanged();
                    NinjaToast.show(Whitelist_Remote.this, R.string.toast_add_whitelist_successful);
                }
                action1.close();
            }
        });
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
                Remote remote = new Remote(Whitelist_Remote.this);
                remote.clearDomains();
                list.clear();
                adapter.notifyDataSetChanged();
            });
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        } else if (menuItem.getItemId() == R.id.menu_backup) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setMessage(R.string.toast_backup);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
                if (android.os.Build.VERSION.SDK_INT >= 23 && android.os.Build.VERSION.SDK_INT < 29) {
                    int hasWRITE_EXTERNAL_STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                        HelperUnit.grantPermissionsStorage(Whitelist_Remote.this);
                        dialog.cancel();
                    } else {
                        dialog.cancel();
                        HelperUnit.makeBackupDir(Whitelist_Remote.this);
                        HelperUnit.backupData(Whitelist_Remote.this, 3);
                    }
                } else {
                    dialog.cancel();
                    HelperUnit.makeBackupDir(Whitelist_Remote.this);
                    HelperUnit.backupData(Whitelist_Remote.this, 3);
                }
            });
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        } else if (menuItem.getItemId() == R.id.menu_restore) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setMessage(R.string.hint_database);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
                if (android.os.Build.VERSION.SDK_INT >= 23 && android.os.Build.VERSION.SDK_INT < 29) {
                    int hasWRITE_EXTERNAL_STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                        HelperUnit.grantPermissionsStorage(Whitelist_Remote.this);
                        dialog.cancel();
                    } else {
                        dialog.cancel();
                        HelperUnit.restoreData(Whitelist_Remote.this, 3);
                    }
                } else {
                    dialog.cancel();
                    HelperUnit.restoreData(Whitelist_Remote.this, 3);
                }
            });
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        }
        return true;
    }
}