package de.baumann.browser.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;
import java.util.Objects;

import de.baumann.browser.browser.Javascript;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.R;
import de.baumann.browser.task.ExportWhiteListTask;
import de.baumann.browser.task.ImportWhitelistTask;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.unit.RecordUnit;
import de.baumann.browser.view.WhitelistAdapter;
import de.baumann.browser.view.NinjaToast;

public class Whitelist_Javascript extends AppCompatActivity {

    private WhitelistAdapter adapter;
    private List<String> list;
    private Javascript javascript;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        HelperUnit.applyTheme(this);
        setContentView(R.layout.activity_whitelist);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        javascript = new Javascript(Whitelist_Javascript.this);

        RecordAction action = new RecordAction(this);
        action.open(false);
        list = action.listDomains(RecordUnit.TABLE_JAVASCRIPT);
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
                    javascript.removeDomain(list.get(position));
                    list.remove(position);
                    notifyDataSetChanged();
                    NinjaToast.show(Whitelist_Javascript.this, R.string.toast_delete_successful);
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
                NinjaToast.show(Whitelist_Javascript.this, R.string.toast_input_empty);
            } else if (!BrowserUnit.isURL(domain)) {
                NinjaToast.show(Whitelist_Javascript.this, R.string.toast_invalid_domain);
            } else {
                RecordAction action1 = new RecordAction(Whitelist_Javascript.this);
                action1.open(true);
                if (action1.checkDomain(domain, RecordUnit.TABLE_JAVASCRIPT)) {
                    NinjaToast.show(Whitelist_Javascript.this, R.string.toast_domain_already_exists);
                } else {
                    Javascript adBlock = new Javascript(Whitelist_Javascript.this);
                    adBlock.addDomain(domain.trim());
                    list.add(0, domain.trim());
                    adapter.notifyDataSetChanged();
                    NinjaToast.show(Whitelist_Javascript.this, R.string.toast_add_whitelist_successful);
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

        final BottomSheetDialog dialog;
        final View dialogView;
        final TextView textView;
        final Button action_ok;

        switch (menuItem.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_clear:
                dialog = new BottomSheetDialog(Whitelist_Javascript.this);
                dialogView = View.inflate(Whitelist_Javascript.this, R.layout.dialog_action, null);
                textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.hint_database);
                action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(view -> {
                    Javascript javaScript = new Javascript(Whitelist_Javascript.this);
                    javaScript.clearDomains();
                    list.clear();
                    adapter.notifyDataSetChanged();
                    dialog.cancel();
                });
                dialog.setContentView(dialogView);
                dialog.show();
                break;
            case R.id.menu_backup:
                dialog = new BottomSheetDialog(Objects.requireNonNull(Whitelist_Javascript.this));
                dialogView = View.inflate(Whitelist_Javascript.this, R.layout.dialog_action, null);
                textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.toast_backup);
                action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(view -> {
                    if (android.os.Build.VERSION.SDK_INT >= 23 && android.os.Build.VERSION.SDK_INT < 29) {
                        int hasWRITE_EXTERNAL_STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                            HelperUnit.grantPermissionsStorage(Whitelist_Javascript.this);
                            dialog.cancel();
                        } else {
                            dialog.cancel();
                            HelperUnit.makeBackupDir(Whitelist_Javascript.this);
                            new ExportWhiteListTask(Whitelist_Javascript.this, 1).execute();
                        }
                    } else {
                        dialog.cancel();
                        HelperUnit.makeBackupDir(Whitelist_Javascript.this);
                        new ExportWhiteListTask(Whitelist_Javascript.this, 1).execute();
                    }
                });
                dialog.setContentView(dialogView);
                dialog.show();
                HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
                break;
            case R.id.menu_restore:
                dialog = new BottomSheetDialog(Objects.requireNonNull(Whitelist_Javascript.this));
                dialogView = View.inflate(Whitelist_Javascript.this, R.layout.dialog_action, null);
                textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.hint_database);
                action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(view -> {
                    if (android.os.Build.VERSION.SDK_INT >= 23 && android.os.Build.VERSION.SDK_INT < 29) {
                        int hasWRITE_EXTERNAL_STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                            HelperUnit.grantPermissionsStorage(Whitelist_Javascript.this);
                            dialog.cancel();
                        } else {
                            dialog.cancel();
                            new ImportWhitelistTask(Whitelist_Javascript.this, 1).execute();
                        }
                    } else {
                        dialog.cancel();
                        new ImportWhitelistTask(Whitelist_Javascript.this, 1).execute();
                    }
                });
                dialog.setContentView(dialogView);
                dialog.show();
                HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
                break;
            default:
                break;
        }
        return true;
    }
}
