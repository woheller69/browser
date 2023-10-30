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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.baumann.browser.R;
import de.baumann.browser.database.Record;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.view.NinjaToast;

import static android.os.Environment.DIRECTORY_DOCUMENTS;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class BackupUnit {

    private static final String BOOKMARK_TYPE = "<DT><A HREF=\"{url}\" ADD_DATE=\"{time}\" COLOR=\"{color}\" FLAGS=\"{flags}\">{title}</A>";
    private static final String BOOKMARK_TITLE = "{title}";
    private static final String BOOKMARK_URL = "{url}";
    private static final String BOOKMARK_TIME = "{time}";
    private static final String BOOKMARK_COLOR = "{color}";
    private static final String BOOKMARK_FLAGS = "{flags}";


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

    public static void importPrefsFromFile(Context context, InputStream inputStream) throws ParserConfigurationException, IOException, SAXException {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear().commit();
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
    }

    public static void importBookmarksFromFile(Context context, BufferedReader reader, List<Record> list) throws IOException {
        BrowserUnit.clearBookmark(context);
        RecordAction action = new RecordAction(context);
        action.open(true);
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!((line.startsWith("<dt><a ") && line.endsWith("</a>")) || (line.startsWith("<DT><A ") && line.endsWith("</A>")))) {
                continue;
            }
            if (checkLegacyBookmark(line)){  //Legacy Bookmark without COLORS and FLAGS
                String title = BackupUnit.getBookmarkTitle(line);
                String url = BackupUnit.getBookmarkURL(line);
                long date = BackupUnit.getBookmarkDate(line);
                if (date >123) date=11;  //if no color defined yet set it red (123 is max: 11 for color + 16 for desktop mode + 32 for Javascript + 64 for DOM Content
                if (title.trim().isEmpty() || url.trim().isEmpty()) {
                    continue;
                }
                Record record = new Record();
                record.setTitle(title);
                record.setURL(url);
                record.setIconColor((int) (date&15));
                record.setDesktopMode((date&16)==16);
                record.setJavascript(!((date&32)==32));
                record.setDomStorage(!((date&64)==64));
                if (!action.checkUrl(url, RecordUnit.TABLE_BOOKMARK)) {
                    list.add(record);
                }
            } else {
                String title = BackupUnit.getBookmarkTitle(line);
                String url = BackupUnit.getBookmarkURL(line);
                if (title.trim().isEmpty() || url.trim().isEmpty()) {
                    continue;
                }
                Record record = new Record();
                record.setTitle(title);
                record.setURL(url);
                record.setTime(BackupUnit.getBookmarkDate(line));
                record.setIconColor(BackupUnit.getBookmarkColor(line));
                int flags = BackupUnit.getBookmarkFlags(line);
                record.setDesktopMode((flags&16)==16);
                record.setJavascript(!((flags&32)==32));
                record.setDomStorage(!((flags&64)==64));
                if (!action.checkUrl(url, RecordUnit.TABLE_BOOKMARK)) {
                    list.add(record);
                }
            }
        }
        reader.close();
        Collections.sort(list, (first, second) -> first.getTitle().compareTo(second.getTitle()));
        for (Record record : list) {
            action.addBookmark(record);
        }
        action.close();
        NinjaToast.show(context, context.getString(R.string.app_done));
    }

    public static void exportBookmarks(Context context) {
        RecordAction action = new RecordAction(context);
        action.open(false);
        List<Record> list = action.listBookmark(context, false, 0);
        action.close();
        File file = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS), "browser_backup//export_bookmark_free.html");
        if (!BackupUnit.checkPermissionStorage(context)) {
            BackupUnit.requestPermission((Activity) context);
        } else {
            if (file.exists()) {
                if (!file.delete()) {
                    Toast.makeText(context, context.getResources().getString(R.string.toast_delete), Toast.LENGTH_LONG).show();
                }
            }
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(file, false));
                for (Record record : list) {
                    String type = BOOKMARK_TYPE;
                    type = type.replace(BOOKMARK_TITLE, record.getTitle());
                    type = type.replace(BOOKMARK_URL, record.getURL());
                    type = type.replace(BOOKMARK_TIME, String.valueOf(record.getTime()));
                    type = type.replace(BOOKMARK_COLOR, String.valueOf(record.getIconColor()));
                    type = type.replace(BOOKMARK_FLAGS,String.valueOf((long) (record.getDesktopMode() ? 16 : 0) + (long) (record.getJavascript() ? 0 : 32) + (long) (record.getDomStorage() ? 0 : 64)));
                    writer.write(type);
                    writer.newLine();
                }
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context,e.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    public static boolean checkLegacyBookmark(String line){
        return !line.contains("COLOR=\"");
    }


    public static long getBookmarkDate(String line) {
        for (String string : line.split(" +")) {
            if (string.startsWith("ADD_DATE=\"")) {
                int index= string.lastIndexOf("\"");
                return Long.parseLong(string.substring(10,index));
            }
        }
        return 0;
    }

    public static int getBookmarkColor(String line) {
        for (String string : line.split(" +")) {
            if (string.startsWith("COLOR=\"")) {
                int index= string.lastIndexOf("\"");
                return Integer.parseInt(string.substring(7,index));
            }
        }
        return 11;  //if not defined use default color red
    }

    public static int getBookmarkFlags(String line) {
        for (String string : line.split(" +")) {
            if (string.startsWith("FLAGS=\"")) {
                int index= string.lastIndexOf("\"");
                return Integer.parseInt(string.substring(7,index));
            }
        }
        return 96;  //if not defined set Javascript and DOM true
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