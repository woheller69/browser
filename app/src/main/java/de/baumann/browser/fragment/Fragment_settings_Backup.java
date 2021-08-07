package de.baumann.browser.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import android.view.Gravity;
import android.widget.Button;

import androidx.preference.PreferenceFragmentCompat;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import de.baumann.browser.R;
import de.baumann.browser.unit.BackupUnit;
import de.baumann.browser.view.NinjaToast;

import static android.os.Environment.DIRECTORY_DOCUMENTS;

public class Fragment_settings_Backup extends PreferenceFragmentCompat {

    public File sd;
    public File data;
    public Context context;
    public Activity activity;
    public ActivityResultLauncher<Intent> someActivityResultLauncher;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.preference_backup, rootKey);
        context = getContext();
        activity = getActivity();
        assert context != null;
        assert activity != null;

        // You can do the assignment inside onAttach or onCreate, i.e, before the activity is displayed
        someActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // There are no request codes
                });

        sd = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS);
        data = Environment.getDataDirectory();
        String database_app = "//data//" + requireActivity().getPackageName() + "//databases//Ninja4.db";
        String database_backup = "browser_backup//database.db";
        final File previewsFolder_app = new File(data, database_app);
        final File previewsFolder_backup = new File(sd, database_backup);

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
                            if (sp.getBoolean("database", false)) {
                                copyDirectory(previewsFolder_app, previewsFolder_backup);
                            }
                            if (sp.getBoolean("settings", false)) {
                                backupUserPrefs(context);
                            }
                            if (sp.getBoolean("bookmark", false)) {
                                BackupUnit.backupData(getActivity(), 4);
                            }
                            if (sp.getBoolean("java", false)) {
                                BackupUnit.backupData(getActivity(), 1);
                            }
                            if (sp.getBoolean("cookie", false)) {
                                BackupUnit.backupData(getActivity(), 2);
                            }
                            if (sp.getBoolean("dom", false)) {
                                BackupUnit.backupData(getActivity(), 3);
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
                    if (sp.getBoolean("database", false)) {
                        copyDirectory(previewsFolder_backup, previewsFolder_app);
                    }
                    if (sp.getBoolean("settings", false)) {
                        restoreUserPrefs(context);
                    }
                    if (sp.getBoolean("bookmark", false)) {
                        BackupUnit.restoreData(getActivity(), 4);
                    }
                    if (sp.getBoolean("java", false)) {
                        BackupUnit.restoreData(getActivity(), 1);
                    }
                    if (sp.getBoolean("cookie", false)) {
                        BackupUnit.restoreData(getActivity(), 2);
                    }
                    if (sp.getBoolean("dom", false)) {
                        BackupUnit.restoreData(getActivity(), 3);
                    }
                    dialogRestart();
                }
            });
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        });
    }

    private void dialogRestart () {
        SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
        sp.edit().putInt("restart_changed", 1).apply();
    }

    // If targetLocation does not exist, it will be created.
    private void copyDirectory(File sourceLocation, File targetLocation) {

        try {
            if (sourceLocation.isDirectory()) {
                if (!targetLocation.exists() && !targetLocation.mkdirs()) {
                    throw new IOException("Cannot create dir " + targetLocation.getAbsolutePath());
                }
                String[] children = sourceLocation.list();
                for (String aChildren : Objects.requireNonNull(children)) {
                    copyDirectory(new File(sourceLocation, aChildren), new File(targetLocation, aChildren));
                }
            } else {
                // make sure the directory we plan to store the recording in exists
                File directory = targetLocation.getParentFile();
                if (directory != null && !directory.exists() && !directory.mkdirs()) {
                    throw new IOException("Cannot create dir " + directory.getAbsolutePath());
                }

                InputStream in = new FileInputStream(sourceLocation);
                OutputStream out = new FileOutputStream(targetLocation);
                // Copy the bits from InputStream to OutputStream
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
                NinjaToast.show(getActivity(), getString(R.string.app_done));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void backupUserPrefs(Context context) {
        final File prefsFile = new File(context.getFilesDir(), "../shared_prefs/" + context.getPackageName() + "_preferences.xml");
        final File backupFile = new File(sd, "browser_backup/preferenceBackup.xml");
        try {
            FileChannel src = new FileInputStream(prefsFile).getChannel();
            FileChannel dst = new FileOutputStream(backupFile).getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();
            NinjaToast.show(getActivity(), getString(R.string.app_done));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void restoreUserPrefs(Context context) {
        final File backupFile = new File(sd, "browser_backup/preferenceBackup.xml");
        try {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            InputStream inputStream = new FileInputStream(backupFile);
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(inputStream);
            Element root = doc.getDocumentElement();
            Node child = root.getFirstChild();
            while (child != null) {
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) child;
                    String type = element.getNodeName();
                    String name = element.getAttribute("name");
                    // In my app, all prefs seem to get serialized as either "string" or
                    // "boolean" - this will need expanding if yours uses any other types!
                    if (type.equals("string")) {
                        String value = element.getTextContent();
                        editor.putString(name, value);
                    } else if (type.equals("boolean")) {
                        String value = element.getAttribute("value");
                        editor.putBoolean(name, value.equals("true"));
                    }
                }
                child = child.getNextSibling();
            }
            editor.apply();
            NinjaToast.show(context, context.getString(R.string.app_done));
        } catch (IOException | SAXException | ParserConfigurationException e) {
            e.printStackTrace();
            NinjaToast.show(context, context.getString(R.string.app_error));
        }
    }
}