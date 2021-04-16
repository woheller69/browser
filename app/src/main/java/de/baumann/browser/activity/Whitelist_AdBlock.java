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

import de.baumann.browser.browser.AdBlock;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.R;
import de.baumann.browser.task.ExportWhiteListTask;
import de.baumann.browser.task.ImportWhitelistTask;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.unit.RecordUnit;
import de.baumann.browser.view.WhitelistAdapter;
import de.baumann.browser.view.NinjaToast;


public class Whitelist_AdBlock extends AppCompatActivity {
    private WhitelistAdapter adapter;
    private List<String> list;
    private AdBlock adBlock;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        HelperUnit.applyTheme(this);
        setContentView(R.layout.activity_whitelist);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        adBlock = new AdBlock(Whitelist_AdBlock.this);

        RecordAction action = new RecordAction(this);
        action.open(false);
        list = action.listDomains(RecordUnit.TABLE_WHITELIST);
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
                    adBlock.removeDomain(list.get(position));
                    list.remove(position);
                    notifyDataSetChanged();
                    NinjaToast.show(Whitelist_AdBlock.this, R.string.toast_delete_successful);
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
                NinjaToast.show(Whitelist_AdBlock.this, R.string.toast_input_empty);
            } else if (!BrowserUnit.isURL(domain)) {
                NinjaToast.show(Whitelist_AdBlock.this, R.string.toast_invalid_domain);
            } else {
                RecordAction action1 = new RecordAction(Whitelist_AdBlock.this);
                action1.open(true);
                if (action1.checkDomain(domain, RecordUnit.TABLE_WHITELIST)) {
                    NinjaToast.show(Whitelist_AdBlock.this, R.string.toast_domain_already_exists);
                } else {
                    AdBlock adBlock = new AdBlock(Whitelist_AdBlock.this);
                    adBlock.addDomain(domain.trim());
                    list.add(0, domain.trim());
                    adapter.notifyDataSetChanged();
                    NinjaToast.show(Whitelist_AdBlock.this, R.string.toast_add_whitelist_successful);
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

        if (menuItem.getItemId() == android.R.id.home) {
            finish();
        } else if (menuItem.getItemId() == R.id.menu_clear) {dialog = new BottomSheetDialog(Whitelist_AdBlock.this);
            dialogView = View.inflate(Whitelist_AdBlock.this, R.layout.dialog_action, null);
            textView = dialogView.findViewById(R.id.dialog_text);
            textView.setText(R.string.hint_database);
            action_ok = dialogView.findViewById(R.id.action_ok);
            action_ok.setOnClickListener(view -> {
                AdBlock adBlock = new AdBlock(Whitelist_AdBlock.this);
                adBlock.clearDomains();
                list.clear();
                adapter.notifyDataSetChanged();
                dialog.cancel();
            });
            dialog.setContentView(dialogView);
            dialog.show();
        } else if (menuItem.getItemId() == R.id.menu_backup) {dialog = new BottomSheetDialog(Whitelist_AdBlock.this);
            dialogView = View.inflate(Whitelist_AdBlock.this, R.layout.dialog_action, null);
            textView = dialogView.findViewById(R.id.dialog_text);
            textView.setText(R.string.toast_backup);
            action_ok = dialogView.findViewById(R.id.action_ok);
            action_ok.setOnClickListener(view -> {
                if (android.os.Build.VERSION.SDK_INT >= 23 && android.os.Build.VERSION.SDK_INT < 29) {
                    int hasWRITE_EXTERNAL_STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                        HelperUnit.grantPermissionsStorage(Whitelist_AdBlock.this);
                        dialog.cancel();
                    } else {
                        dialog.cancel();
                        HelperUnit.makeBackupDir(Whitelist_AdBlock.this);
                        new ExportWhiteListTask(Whitelist_AdBlock.this, 0).execute();
                    }
                } else {
                    dialog.cancel();
                    HelperUnit.makeBackupDir(Whitelist_AdBlock.this);
                    new ExportWhiteListTask(Whitelist_AdBlock.this, 0).execute();
                }
            });
            dialog.setContentView(dialogView);
            dialog.show();
            HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
        } else if (menuItem.getItemId() == R.id.menu_restore) {dialog = new BottomSheetDialog(Whitelist_AdBlock.this);
            dialogView = View.inflate(Whitelist_AdBlock.this, R.layout.dialog_action, null);
            textView = dialogView.findViewById(R.id.dialog_text);
            textView.setText(R.string.hint_database);
            action_ok = dialogView.findViewById(R.id.action_ok);
            action_ok.setOnClickListener(view -> {
                if (android.os.Build.VERSION.SDK_INT >= 23 && android.os.Build.VERSION.SDK_INT < 29) {
                    int hasWRITE_EXTERNAL_STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                        HelperUnit.grantPermissionsStorage(Whitelist_AdBlock.this);
                        dialog.cancel();
                    } else {
                        dialog.cancel();
                        new ImportWhitelistTask(Whitelist_AdBlock.this, 0).execute();
                    }
                } else {
                    dialog.cancel();
                    new ImportWhitelistTask(Whitelist_AdBlock.this, 0).execute();
                }
            });
            dialog.setContentView(dialogView);
            dialog.show();
            HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);

        }
        return true;
    }
}
