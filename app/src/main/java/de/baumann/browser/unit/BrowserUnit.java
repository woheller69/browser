package de.baumann.browser.unit;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import de.baumann.browser.browser.AdBlock;
import de.baumann.browser.browser.Cookie;
import de.baumann.browser.browser.Javascript;
import de.baumann.browser.browser.Remote;
import de.baumann.browser.database.Record;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.R;
import de.baumann.browser.view.NinjaToast;

public class BrowserUnit {

    public static final int PROGRESS_MAX = 100;
    public static final String MIME_TYPE_TEXT_PLAIN = "text/plain";

    private static final String SEARCH_ENGINE_GOOGLE = "https://www.google.com/search?q=";
    private static final String SEARCH_ENGINE_DUCKDUCKGO = "https://duckduckgo.com/?q=";
    private static final String SEARCH_ENGINE_STARTPAGE = "https://startpage.com/do/search?query=";
    private static final String SEARCH_ENGINE_BING = "http://www.bing.com/search?q=";
    private static final String SEARCH_ENGINE_BAIDU = "https://www.baidu.com/s?wd=";
    private static final String SEARCH_ENGINE_QWANT = "https://www.qwant.com/?q=";
    private static final String SEARCH_ENGINE_ECOSIA = "https://www.ecosia.org/search?q=";

    private static final String SEARCH_ENGINE_STARTPAGE_DE = "https://startpage.com/do/search?lui=deu&language=deutsch&query=";
    private static final String SEARCH_ENGINE_SEARX = "https://searx.be/?q=";

    public static final String URL_ENCODING = "UTF-8";
    private static final String URL_ABOUT_BLANK = "about:blank";
    public static final String URL_SCHEME_ABOUT = "about:";
    public static final String URL_SCHEME_MAIL_TO = "mailto:";
    private static final String URL_SCHEME_FILE = "file://";
    private static final String URL_SCHEME_HTTP = "https://";
    public static final String URL_SCHEME_INTENT = "intent://";

    private static final String URL_PREFIX_GOOGLE_PLAY = "www.google.com/url?q=";
    private static final String URL_SUFFIX_GOOGLE_PLAY = "&sa";
    private static final String URL_PREFIX_GOOGLE_PLUS = "plus.url.google.com/url?q=";
    private static final String URL_SUFFIX_GOOGLE_PLUS = "&rct";

    private static final String BOOKMARK_TYPE = "<DT><A HREF=\"{url}\" ADD_DATE=\"{time}\">{title}</A>";
    private static final String BOOKMARK_TITLE = "{title}";
    private static final String BOOKMARK_URL = "{url}";
    private static final String BOOKMARK_TIME = "{time}";

    public static boolean isURL(String url) {
        if (url == null) {
            return false;
        }

        url = url.toLowerCase(Locale.getDefault());
        if (url.startsWith(URL_ABOUT_BLANK)
                || url.startsWith(URL_SCHEME_MAIL_TO)
                || url.startsWith(URL_SCHEME_FILE)
                || url.startsWith(URL_SCHEME_HTTP)) {
            return true;
        }

        String regex = "^((ftp|http|https|intent)?://)"                      // support scheme
                + "?(([0-9a-z_!~*'().&=+$%-]+: )?[0-9a-z_!~*'().&=+$%-]+@)?" // ftp的user@
                + "(([0-9]{1,3}\\.){3}[0-9]{1,3}"                            // IP形式的URL -> 199.194.52.184
                + "|"                                                        // 允许IP和DOMAIN（域名）
                + "([0-9a-z_!~*'()-]+\\.)*"                                  // 域名 -> www.
                + "([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]\\."                    // 二级域名
                + "[a-z]{2,6})"                                              // first level domain -> .com or .museum
                + "(:[0-9]{1,4})?"                                           // 端口 -> :80
                + "((/?)|"                                                   // a slash isn't required if there is no file name
                + "(/[0-9a-z_!~*'().;?:@&=+$,%#-]+)+/?)$";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(url).matches();
    }

    public static String queryWrapper(Context context, String query) {
        // Use prefix and suffix to process some special links
        String temp = query.toLowerCase(Locale.getDefault());
        if (temp.contains(URL_PREFIX_GOOGLE_PLAY) && temp.contains(URL_SUFFIX_GOOGLE_PLAY)) {
            int start = temp.indexOf(URL_PREFIX_GOOGLE_PLAY) + URL_PREFIX_GOOGLE_PLAY.length();
            int end = temp.indexOf(URL_SUFFIX_GOOGLE_PLAY);
            query = query.substring(start, end);
        } else if (temp.contains(URL_PREFIX_GOOGLE_PLUS) && temp.contains(URL_SUFFIX_GOOGLE_PLUS)) {
            int start = temp.indexOf(URL_PREFIX_GOOGLE_PLUS) + URL_PREFIX_GOOGLE_PLUS.length();
            int end = temp.indexOf(URL_SUFFIX_GOOGLE_PLUS);
            query = query.substring(start, end);
        }

        if (isURL(query)) {
            if (query.startsWith(URL_SCHEME_ABOUT) || query.startsWith(URL_SCHEME_MAIL_TO)) {
                return query;
            }

            if (!query.contains("://")) {
                query = URL_SCHEME_HTTP + query;
            }

            return query;
        }

        try {
            query = URLEncoder.encode(query, URL_ENCODING);
        } catch (UnsupportedEncodingException u) {
            Log.w("browser", "Unsupported Encoding Exception");
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String custom = sp.getString("sp_search_engine_custom", SEARCH_ENGINE_STARTPAGE);
        final int i = Integer.parseInt(Objects.requireNonNull(sp.getString(context.getString(R.string.sp_search_engine), "6")));
        switch (i) {
            case 0:
                return SEARCH_ENGINE_STARTPAGE + query;
            case 1:
                return SEARCH_ENGINE_STARTPAGE_DE + query;
            case 2:
                return SEARCH_ENGINE_BAIDU + query;
            case 3:
                return SEARCH_ENGINE_BING + query;
            case 4:
                return SEARCH_ENGINE_DUCKDUCKGO + query;
            case 5:
                return SEARCH_ENGINE_GOOGLE + query;
            case 6:
                return SEARCH_ENGINE_SEARX + query;
            case 7:
                return SEARCH_ENGINE_QWANT + query;
            case 8:
                return custom + query;
            case 9:
            default:
                return SEARCH_ENGINE_ECOSIA + query;
        }
    }

    public static void download(final Context context, final String url, final String contentDisposition, final String mimeType) {

        String text = context.getString(R.string.dialog_title_download) + " - " + URLUtil.guessFileName(url, contentDisposition, mimeType);
        final BottomSheetDialog dialog = new BottomSheetDialog(context);
        View dialogView = View.inflate(context, R.layout.dialog_action, null);
        TextView textView = dialogView.findViewById(R.id.dialog_text);
        textView.setText(text);
        Button action_ok = dialogView.findViewById(R.id.action_ok);
        action_ok.setOnClickListener(view -> {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            String filename = URLUtil.guessFileName(url, contentDisposition, mimeType); // Maybe unexpected filename.

            CookieManager cookieManager = CookieManager.getInstance();
            String cookie = cookieManager.getCookie(url);
            request.addRequestHeader("Cookie", cookie);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setTitle(filename);
            request.setMimeType(mimeType);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            assert manager != null;

            if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 29) {
                int hasWRITE_EXTERNAL_STORAGE = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                    Activity activity = (Activity) context;
                    HelperUnit.grantPermissionsStorage(activity);
                } else {
                    manager.enqueue(request);
                    try {
                        NinjaToast.show(context, R.string.toast_start_download);
                    } catch (Exception e) {
                        Toast.makeText(context, R.string.toast_start_download, Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                manager.enqueue(request);
                try {
                    NinjaToast.show(context, R.string.toast_start_download);
                } catch (Exception e) {
                    Toast.makeText(context, R.string.toast_start_download, Toast.LENGTH_SHORT).show();
                }
            }
            dialog.cancel();
        });
        dialog.setContentView(dialogView);
        dialog.show();
        HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
    }

    public static String exportWhitelist(Context context, int i) {
        RecordAction action = new RecordAction(context);
        List<String> list;
        String filename;
        action.open(false);
        switch (i) {
            case 0:
                list = action.listDomains(RecordUnit.TABLE_WHITELIST);
                filename = "export_whitelist_AdBlock.txt";
                break;
            case 1:
                list = action.listDomains(RecordUnit.TABLE_JAVASCRIPT);
                filename = "export_whitelist_java.txt";
                break;
            case 3:
                list = action.listDomains(RecordUnit.TABLE_REMOTE);
                filename = "export_whitelist_remote.txt";
                break;
            default:
                list = action.listDomains(RecordUnit.TABLE_COOKIE);
                filename = "export_whitelist_cookie.txt";
                break;
        }
        action.close();
        File file = new File(context.getExternalFilesDir(null), "browser_backup//" + filename);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, false));
            for (String domain : list) {
                writer.write(domain);
                writer.newLine();
            }
            writer.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    public static int importWhitelist (Context context, int i) {
        int count = 0;
        try {
            String filename;
            AdBlock adBlock = null;
            Javascript js = null;
            Cookie cookie = null;
            Remote remote = null;
            switch (i) {
                case 0:
                    adBlock= new AdBlock(context);
                    filename = "export_whitelist_AdBlock.txt";
                    break;
                case 1:
                    js = new Javascript(context);
                    filename = "export_whitelist_java.txt";
                    break;
                case 3:
                    remote = new Remote(context);
                    filename = "export_whitelist_remote.txt";
                    break;
                default:
                    cookie = new Cookie(context);
                    filename = "export_whitelist_cookie.txt";
                    break;
            }
            File file = new File(context.getExternalFilesDir(null), "browser_backup//" + filename);
            RecordAction action = new RecordAction(context);
            action.open(true);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                switch (i) {
                    case 0:
                        if (!action.checkDomain(line, RecordUnit.TABLE_WHITELIST)) {
                            adBlock.addDomain(line);
                            count++;
                        }
                        break;
                    case 1:
                        if (!action.checkDomain(line, RecordUnit.TABLE_JAVASCRIPT)) {
                            js.addDomain(line);
                            count++;
                        }
                        break;
                    case 3:
                        if (!action.checkDomain(line, RecordUnit.TABLE_REMOTE)) {
                            remote.addDomain(line);
                            count++;
                        }
                        break;
                    default:
                        if (!action.checkDomain(line, RecordUnit.TABLE_COOKIE)) {
                            cookie.addDomain(line);
                            count++;
                        }
                        break;
                }
            }
            reader.close();
            action.close();
        } catch (Exception e) {
            Log.w("browser", "Error reading file", e);
        }
        return count;
    }

    public static String exportBookmarks(Context context) {
        RecordAction action = new RecordAction(context);
        action.open(false);
        List<Record> list = action.listBookmark(context, false, 0);
        action.close();
        File file = new File(context.getExternalFilesDir(null), "browser_backup//export_Bookmark.html");

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, false));
            for (Record record : list) {
                String type = BOOKMARK_TYPE;
                type = type.replace(BOOKMARK_TITLE, record.getTitle());
                type = type.replace(BOOKMARK_URL, record.getURL());
                type = type.replace(BOOKMARK_TIME, String.valueOf(record.getTime()));
                writer.write(type);
                writer.newLine();
            }
            writer.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    public static int importBookmarks(Context context) {
        File file = new File(context.getExternalFilesDir(null), "browser_backup//export_Bookmark.html");
        List<Record> list = new ArrayList<>();
        try {
            RecordAction action = new RecordAction(context);
            action.open(true);

            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!((line.startsWith("<dt><a ") && line.endsWith("</a>")) || (line.startsWith("<DT><A ") && line.endsWith("</A>")))) {
                    continue;
                }
                String title = getBookmarkTitle(line);
                String url = getBookmarkURL(line);
                if (title.trim().isEmpty() || url.trim().isEmpty()) {
                    continue;
                }
                Record record = new Record();
                record.setTitle(title);
                record.setURL(url);
                record.setTime(System.currentTimeMillis());
                if (!action.checkUrl(url, RecordUnit.TABLE_BOOKMARK)) {
                    list.add(record);
                }
            }
            reader.close();
            Collections.sort(list, (first, second) -> first.getTitle().compareTo(second.getTitle()));
            for (Record record : list) {
                action.addBookmark(record);
            }
            action.close();
        } catch (Exception ignored) {}
        return list.size();
    }

    private static String getBookmarkTitle(String line) {
        line = line.substring(0, line.length() - 4); // Remove last </a>
        int index = line.lastIndexOf(">");
        return line.substring(index + 1);
    }

    private static String getBookmarkURL(String line) {
        for (String string : line.split(" +")) {
            if (string.startsWith("href=\"") || string.startsWith("HREF=\"")) {
                return string.substring(6, string.length() - 1); // Remove href=\" and \"
            }
        }
        return "";
    }

    public static void clearHome(Context context) {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.clearTable(RecordUnit.TABLE_GRID);
        action.close();
    }

    public static void clearCache(Context context) {
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception exception) {
            Log.w("browser", "Error clearing cache");
        }
    }

    public static void clearCookie() {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.flush();
        cookieManager.removeAllCookies(value -> {});
    }

    public static void clearBookmark (Context context) {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.clearTable(RecordUnit.TABLE_BOOKMARK);
        action.close();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            Objects.requireNonNull(shortcutManager).removeAllDynamicShortcuts();
        }
    }

    public static void clearHistory(Context context) {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.clearTable(RecordUnit.TABLE_HISTORY);
        action.close();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            Objects.requireNonNull(shortcutManager).removeAllDynamicShortcuts();
        }
    }

    public static void clearIndexedDB (Context context) {
        File data = Environment.getDataDirectory();
        String indexedDB = "//data//" + context.getPackageName() + "//app_webview//" + "//IndexedDB";
        String localStorage = "//data//" + context.getPackageName()  + "//app_webview//" + "//Local Storage";
        final File indexedDB_dir = new File(data, indexedDB);
        final File localStorage_dir = new File(data, localStorage);
        BrowserUnit.deleteDir(indexedDB_dir);
        BrowserUnit.deleteDir(localStorage_dir);
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : Objects.requireNonNull(children)) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
        }
        return dir != null && dir.delete();
    }
}
