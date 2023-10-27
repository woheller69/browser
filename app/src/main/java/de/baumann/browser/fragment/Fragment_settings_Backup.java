package de.baumann.browser.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import android.view.Gravity;
import android.widget.Button;
import android.widget.Toast;

import androidx.preference.PreferenceFragmentCompat;

import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.ParserConfigurationException;

import de.baumann.browser.R;
import de.baumann.browser.database.Record;
import de.baumann.browser.unit.BackupUnit;
import de.baumann.browser.view.NinjaToast;

import static android.os.Environment.DIRECTORY_DOCUMENTS;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public class Fragment_settings_Backup extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    ActivityResultLauncher<Intent> mRestoreDatabase;
    ActivityResultLauncher<Intent> mRestorePrefs;
    ActivityResultLauncher<Intent> mImportBookmarks;

    public File sd;
    public Context context;
    public Activity activity;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.preference_backup, rootKey);
        PreferenceManager.setDefaultValues(getContext(), R.xml.preference_backup, false);
        initSummary(getPreferenceScreen());
        context = getContext();
        activity = getActivity();
        assert context != null;
        assert activity != null;

        Preference backupRestore = findPreference("backrestore");
        backupRestore.setTitle(getString(R.string.setting_title_data)+" / "+getString(R.string.settings_data_restore));

        sd = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        Button ib_backup = activity.findViewById(R.id.ib_backup);
        ib_backup.setOnClickListener(v -> {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                    builder.setMessage(R.string.toast_backup);
                    builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
                        if (!BackupUnit.checkPermissionStorage(context)) {
                            BackupUnit.requestPermission(activity);
                        } else {
                            BackupUnit.makeBackupDir();
                            if (sp.getString("backrestore", "1").equals("1")) {
                                backupDatabase();
                            } else if (sp.getString("backrestore", "1").equals("2")){
                                backupUserPrefs(context);
                            } else if (sp.getString("backrestore", "1").equals("3")) {
                                BackupUnit.exportBookmarks(context);
                            }
                        }
                    });
                    builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
                });

        Button ib_restore = activity.findViewById(R.id.ib_restore);
        ib_restore.setOnClickListener(v -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setMessage(R.string.hint_database);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
                if (!BackupUnit.checkPermissionStorage(context)) {
                    BackupUnit.requestPermission(activity);
                } else {
                    if (sp.getString("backrestore", "1").equals("1")) {
                        restoreDatabase();
                    } else if (sp.getString("backrestore", "1").equals("2")) {
                        restoreUserPrefs(context);
                    } else if (sp.getString("backrestore", "1").equals("3")) {
                        importBookmarks(context);
                    }
                    dialogRestart();
                }
            });
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        });

        mRestoreDatabase = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    File intData = new File(Environment.getDataDirectory() + "//data//" + context.getPackageName());
                    if (result.getData()!=null && result.getData().getData()!=null){
                        BackupUnit.zipExtract(context, intData, result.getData().getData());
                        NinjaToast.show(context, context.getString(R.string.app_done));
                    }
                });

        mRestorePrefs = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getData()!=null && result.getData().getData()!=null) {
                        try {
                            InputStream inputStream = context.getContentResolver().openInputStream(result.getData().getData());
                            BackupUnit.importPrefsFromFile(context, inputStream);
                        } catch (IOException | SAXException | ParserConfigurationException e) {
                            e.printStackTrace();
                            Toast.makeText(context,e.toString(), Toast.LENGTH_LONG).show();
                        }
                    }
                });

        mImportBookmarks = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getData()!=null && result.getData().getData()!=null) {
                        List<Record> list = new ArrayList<>();
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getContentResolver().openInputStream(result.getData().getData())));
                            BackupUnit.importBookmarksFromFile(context, reader, list);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(context,e.toString(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void dialogRestart () {
        SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
        sp.edit().putInt("restart_changed", 1).apply();
    }


    public void restoreDatabase() {
        File intData;
        intData = new File(Environment.getDataDirectory() + "//data//" + context.getPackageName());
        String filesBackup = "//browser_backup//"+"app_data.zip";
        final File zipFileBackup = new File(sd, filesBackup);
        if (!BackupUnit.checkPermissionStorage(context)) {
            BackupUnit.requestPermission((Activity) context);
        } else {
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) && !Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("application/zip");
                mRestoreDatabase.launch(intent);
            } else {
                BackupUnit.zipExtract(context, intData, Uri.fromFile(zipFileBackup));
                NinjaToast.show(context, context.getString(R.string.app_done));
            }
        }
    }

    public void backupDatabase() {
        File intDatabase;
        intDatabase = new File(Environment.getDataDirectory()+"//data//" + context.getPackageName() + "//databases//");
        String filesBackup = "app_data.zip";
        final File dbBackup = new File(sd + "//browser_backup//", filesBackup);
        if (!BackupUnit.checkPermissionStorage(context)) {
            BackupUnit.requestPermission((Activity) context);
        } else {
            if (dbBackup.exists()){
                if (!dbBackup.delete()){
                    Toast.makeText(context,getResources().getString(R.string.toast_delete), Toast.LENGTH_LONG).show();
                }
            }
            try {
                ZipFile zipFile = new ZipFile(dbBackup);
                zipFile.addFolder(intDatabase);
                NinjaToast.show(context," -> " + dbBackup.toString());
            } catch (ZipException e) {
                e.printStackTrace();
                Toast.makeText(context,e.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void backupUserPrefs(Context context) {
        final File prefsFile = new File(context.getFilesDir(), "../shared_prefs/" + context.getPackageName() + "_preferences.xml");
        final File backupFile = new File(sd, "browser_backup/preferenceBackup.xml");
        if (!BackupUnit.checkPermissionStorage(context)) {
            BackupUnit.requestPermission((Activity) context);
        } else {
            if (backupFile.exists()){
                if (!backupFile.delete()){
                    Toast.makeText(context,getResources().getString(R.string.toast_delete), Toast.LENGTH_LONG).show();
                }
            }
            try {
                FileChannel src = new FileInputStream(prefsFile).getChannel();
                FileChannel dst = new FileOutputStream(backupFile).getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();
                NinjaToast.show(getActivity(), getString(R.string.app_done));
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(context,e.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void restoreUserPrefs(Context context) {

        final File backupFile = new File(sd, "browser_backup/preferenceBackup.xml");
        if (!BackupUnit.checkPermissionStorage(context)) {
            BackupUnit.requestPermission((Activity) context);
        } else {
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) && !Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("text/xml");
                mRestorePrefs.launch(intent);
            } else {
                try {
                    InputStream inputStream = new FileInputStream(backupFile);
                    BackupUnit.importPrefsFromFile(context, inputStream);
                } catch (IOException | SAXException | ParserConfigurationException e) {
                    e.printStackTrace();
                    Toast.makeText(context,e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public void importBookmarks(Context context) {
        File file = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS), "browser_backup//export_bookmark_list.html");
        if (!BackupUnit.checkPermissionStorage(context)) {
            BackupUnit.requestPermission((Activity) context);
        } else {
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) && !Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("text/html");
                mImportBookmarks.launch(intent);
            } else {
                List<Record> list = new ArrayList<>();
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    BackupUnit.importBookmarksFromFile(context, reader, list);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(context,e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void initSummary(Preference p) {
        if (p instanceof PreferenceGroup) {
            PreferenceGroup pGrp = (PreferenceGroup) p;
            for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
                initSummary(pGrp.getPreference(i));
            }
        } else {
            updatePrefSummary(p);
        }
    }
    private void updatePrefSummary(Preference p) {
        if (p instanceof ListPreference) {
            ListPreference listPref = (ListPreference) p;
            p.setSummary(listPref.getEntry());
        }
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sp, String key) {
        updatePrefSummary(findPreference(key));
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
}