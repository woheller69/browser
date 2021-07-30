package de.baumann.browser.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import android.view.Gravity;

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
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.view.NinjaToast;

public class Fragment_settings_Backup extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.preference_backup, rootKey);
        Context context = getContext();
        assert context != null;

        File sd = context.getExternalFilesDir(null);
        File data = Environment.getDataDirectory();
        String database_app = "//data//" + requireActivity().getPackageName() + "//databases//Ninja4.db";
        String database_backup = "browser_backup//Ninja4.db";
        final File previewsFolder_app = new File(data, database_app);
        final File previewsFolder_backup = new File(sd, database_backup);

        Preference data_exDB = findPreference("data_exDB");
        assert data_exDB != null;
        data_exDB.setOnPreferenceClickListener(preference -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setMessage(R.string.toast_backup);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
                dialog.cancel();
                if (HelperUnit.hasPermissionStorage(this.getActivity())) {
                    makeBackupDir();
                    copyDirectory(previewsFolder_app, previewsFolder_backup);
                    backupUserPrefs(context);
                    NinjaToast.show(getActivity(), getString(R.string.app_done));
                }
            });
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
            return false;
        });

        Preference data_imDB = findPreference("data_imDB");
        assert data_imDB != null;
        data_imDB.setOnPreferenceClickListener(preference -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setMessage(R.string.hint_database);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
                dialog.cancel();
                if (HelperUnit.hasPermissionStorage(this.getActivity())) {
                    copyDirectory(previewsFolder_backup, previewsFolder_app);
                    restoreUserPrefs(context);
                    dialogRestart();
                }
            });
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
            return false;
        });

        Preference data_imBookmark = findPreference("data_imBookmark");
        assert data_imBookmark != null;
        data_imBookmark.setOnPreferenceClickListener(preference -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setMessage(R.string.hint_database);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
                dialog.cancel();
                if (HelperUnit.hasPermissionStorage(this.getActivity())) {
                    HelperUnit.restoreData(getActivity(), 4);
                }
            });
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
            return false;
        });

        Preference data_exBookmark = findPreference("data_exBookmark");
        assert data_exBookmark != null;
        data_exBookmark.setOnPreferenceClickListener(preference -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setMessage(R.string.toast_backup);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
                dialog.cancel();
                if (HelperUnit.hasPermissionStorage(this.getActivity())) {
                    makeBackupDir();
                    HelperUnit.backupData(getActivity(), 4);
                }
            });
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
            return false;
        });
    }

    private void makeBackupDir () {
        Context context = getContext();
        assert context != null;
        if (HelperUnit.hasPermissionStorage(this.getActivity())) {
            File backupDir = new File(context.getExternalFilesDir(null), "browser_backup//");
            boolean wasSuccessful = backupDir.mkdirs();
            if (!wasSuccessful) {
                System.out.println("was not successful.");
            }
        }
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
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void backupUserPrefs(Context context) {
        final File prefsFile = new File(context.getFilesDir(), "../shared_prefs/" + context.getPackageName() + "_preferences.xml");
        final File backupFile = new File(context.getExternalFilesDir(null), "browser_backup/preferenceBackup.xml");
        try {
            FileChannel src = new FileInputStream(prefsFile).getChannel();
            FileChannel dst = new FileOutputStream(backupFile).getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();
            NinjaToast.show(context, "Backed up user prefs to " + backupFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void restoreUserPrefs(Context context) {
        final File backupFile = new File(context.getExternalFilesDir(null), "browser_backup/preferenceBackup.xml");
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