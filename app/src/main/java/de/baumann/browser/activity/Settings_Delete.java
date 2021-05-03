package de.baumann.browser.activity;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import android.view.Gravity;
import android.view.MenuItem;
import android.widget.Button;

import java.util.Objects;

import de.baumann.browser.fragment.Fragment_settings_Delete;
import de.baumann.browser.R;
import de.baumann.browser.service.ClearService;

public class Settings_Delete extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        setContentView(R.layout.activity_settings_delete);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, new Fragment_settings_Delete())
                .commit();

        Button button = findViewById(R.id.whitelist_add);
        button.setOnClickListener(v -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setMessage(R.string.hint_database);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
                Intent toClearService = new Intent(Settings_Delete.this, ClearService.class);
                startService(toClearService);
            });
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }
}