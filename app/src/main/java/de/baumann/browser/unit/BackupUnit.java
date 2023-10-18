/*
    This file is part of the browser WebApp.

    browser WebApp is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    browser WebApp is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the browser webview app.

    If not, see <http://www.gnu.org/licenses/>.
 */

package de.baumann.browser.unit;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.baumann.browser.R;
import de.baumann.browser.database.Record;
import de.baumann.browser.database.RecordAction;

import static android.os.Environment.DIRECTORY_DOCUMENTS;

public class BackupUnit {

    private static final String BOOKMARK_TYPE = "<DT><A HREF=\"{url}\" ADD_DATE=\"{time}\">{title}</A>";
    private static final String BOOKMARK_TITLE = "{title}";
    private static final String BOOKMARK_URL = "{url}";
    private static final String BOOKMARK_TIME = "{time}";


    public static final int PERMISSION_REQUEST_CODE = 123;

    public static boolean checkPermissionStorage (Context context) {
        int result = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return true;
        } else {
            return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
        }
    }

    public static void requestPermission(Activity activity) {

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getResources().getString(R.string.app_name));
        builder.setMessage(R.string.toast_permission_sdCard);
        builder.setPositiveButton(R.string.app_ok, (dialog, which) -> {
            dialog.cancel();
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        });
        builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void zipExtract(Context context, File targetDir, Uri zipFile) {
        ZipEntry zipEntry;
        int readLen;
        byte[] readBuffer = new byte[4096];
        try {
            InputStream src = context.getContentResolver().openInputStream(zipFile);
            try {
                try (ZipInputStream zipInputStream = new ZipInputStream(src)) {
                    while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                        File extractedFile = new File(targetDir ,zipEntry.getName());
                        try (OutputStream outputStream = Files.newOutputStream(extractedFile.toPath())) {
                            while ((readLen = zipInputStream.read(readBuffer)) != -1) {
                                outputStream.write(readBuffer, 0, readLen);
                            }
                        }
                    }
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void makeBackupDir () {
        File backupDir = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS), "browser_backup//");
        boolean wasSuccessful = backupDir.mkdirs();
        if (!wasSuccessful) {
            System.out.println("was not successful.");
        }
    }

    public static void exportBookmarks(Context context) {
        RecordAction action = new RecordAction(context);
        action.open(false);
        List<Record> list = action.listBookmark(context, false, 0);
        action.close();
        File file = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS), "browser_backup//export_bookmark_list.html");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, false));
            for (Record record : list) {
                String type = BOOKMARK_TYPE;
                type = type.replace(BOOKMARK_TITLE, record.getTitle());
                type = type.replace(BOOKMARK_URL, record.getURL());
                type = type.replace(BOOKMARK_TIME, String.valueOf(record.getIconColor() + (long) (record.getDesktopMode() ? 16 : 0) + (long) (record.getJavascript() ? 0 : 32) + (long) (record.getDomStorage() ? 0 : 64)));
                writer.write(type);
                writer.newLine();
            }
            writer.close();
            file.getAbsolutePath();
        } catch (Exception ignored) {
        }
    }


    public static long getBookmarkDate(String line) {
        for (String string : line.split(" +")) {
            if (string.startsWith("ADD_DATE=\"")) {
                int index= string.indexOf("\">");
                return Long.parseLong(string.substring(10,index));
            }
        }
        return 0;
    }

    public static String getBookmarkTitle(String line) {
        line = line.substring(0, line.length() - 4); // Remove last </a>
        int index = line.lastIndexOf(">");
        return line.substring(index + 1);
    }

    public static String getBookmarkURL(String line) {
        for (String string : line.split(" +")) {
            if (string.startsWith("href=\"") || string.startsWith("HREF=\"")) {
                return string.substring(6, string.length() - 1); // Remove href=\" and \"
            }
        }
        return "";
    }
}