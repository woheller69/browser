package de.baumann.browser.unit;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ShortcutManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import de.baumann.browser.browser.DataURIParser;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.R;

public class BrowserUnit {

    public static final int PROGRESS_MAX = 100;
    public static final int LOADING_STOPPED = 101;  //Must be > PROGRESS_MAX !
    public static final String MIME_TYPE_TEXT_PLAIN = "text/plain";

    private static final String SEARCH_ENGINE_GOOGLE = "https://www.google.com/search?q=";
    private static final String SEARCH_ENGINE_DUCKDUCKGO = "https://duckduckgo.com/?q=";
    private static final String SEARCH_ENGINE_STARTPAGE = "https://startpage.com/do/search?query=";
    private static final String SEARCH_ENGINE_BING = "https://www.bing.com/search?q=";
    private static final String SEARCH_ENGINE_BAIDU = "https://www.baidu.com/s?wd=";
    private static final String SEARCH_ENGINE_QWANT = "https://www.qwant.com/?q=";
    private static final String SEARCH_ENGINE_ECOSIA = "https://www.ecosia.org/search?q=";
    private static final String SEARCH_ENGINE_Metager = "https://metager.org/meta/meta.ger3?eingabe=";

    private static final String SEARCH_ENGINE_STARTPAGE_DE = "https://startpage.com/do/search?lui=deu&language=deutsch&query=";
    private static final String SEARCH_ENGINE_SEARX = "https://searx.be/?q=";

    public static final String URL_ENCODING = "UTF-8";
    private static final String URL_ABOUT_BLANK = "about:blank";
    public static final String URL_SCHEME_ABOUT = "about:";
    public static final String URL_SCHEME_MAIL_TO = "mailto:";
    private static final String URL_SCHEME_FILE = "file://";
    private static final String URL_SCHEME_HTTPS = "https://";
    private static final String URL_SCHEME_HTTP = "http://";
    private static final String URL_SCHEME_FTP = "ftp://";
    private static final String URL_SCHEME_INTENT = "intent://";

    public static boolean isURL(String url) {


        url = url.toLowerCase(Locale.getDefault());

        if (url.startsWith(URL_ABOUT_BLANK)
                || url.startsWith(URL_SCHEME_MAIL_TO)
                || url.startsWith(URL_SCHEME_FILE)
                || url.startsWith(URL_SCHEME_HTTP)
                || url.startsWith(URL_SCHEME_HTTPS)
                || url.startsWith(URL_SCHEME_FTP)
                || url.startsWith(URL_SCHEME_INTENT)) {
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

        if (isURL(query)) {
            if (query.startsWith(URL_SCHEME_ABOUT) || query.startsWith(URL_SCHEME_MAIL_TO)) {
                return query;
            }

            if (!query.contains("://")) {
                query = URL_SCHEME_HTTPS + query;
            }

            return query;
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String customSearchEngine = sp.getString("sp_search_engine_custom", "");
        assert customSearchEngine != null;

        //Override UserAgent if own UserAgent is defined
        if (!sp.contains("searchEngineSwitch")){  //if new switch_text_preference has never been used initialize the switch
            if (customSearchEngine.equals("")) {
                sp.edit().putBoolean("searchEngineSwitch", false).apply();
            }else{
                sp.edit().putBoolean("searchEngineSwitch", true).apply();
            }
        }

        if (sp.getBoolean("searchEngineSwitch",false)){  //if new switch_text_preference has never been used initialize the switch
            return customSearchEngine + query;
        } else {
            final int i = Integer.parseInt(Objects.requireNonNull(sp.getString("sp_search_engine", "0")));
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
                    return SEARCH_ENGINE_ECOSIA + query;
                case 9:
                    return SEARCH_ENGINE_Metager + query;
                default:
                    return SEARCH_ENGINE_STARTPAGE + query;
            }
        }
    }

    public static void download(final Context context, final String url, final String contentDisposition, final String mimeType) {

        String text = context.getString(R.string.dialog_title_download) + " - " + URLUtil.guessFileName(url, contentDisposition, mimeType);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setMessage(text);
        builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
            try {
                Activity activity = (Activity) context;
                String filename = URLUtil.guessFileName(url, contentDisposition, mimeType); // Maybe unexpected filename.
                if (url.startsWith("data:")) {
                    DataURIParser dataURIParser = new DataURIParser(url);
                    if (BackupUnit.checkPermissionStorage(context)) {
                        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(dataURIParser.getImagedata());
                    } else BackupUnit.requestPermission(context,activity);
                }else {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    CookieManager cookieManager = CookieManager.getInstance();
                    String cookie = cookieManager.getCookie(url);
                    request.addRequestHeader("Cookie", cookie);
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setTitle(filename);
                    request.setMimeType(mimeType);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                    DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                    assert manager != null;
                    if (BackupUnit.checkPermissionStorage(context)) {
                        manager.enqueue(request);
                    }else {
                        BackupUnit.requestPermission(context,activity);
                    }
                }
            } catch (Exception e) {
            System.out.println("Error Downloading File: " + e.toString());
            Toast.makeText(context, context.getString(R.string.app_error)+e.toString().substring(e.toString().indexOf(":")),Toast.LENGTH_LONG).show();
            e.printStackTrace();
            }
        });
        builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
        AlertDialog dialog = builder.create();
        dialog.show();
        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
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

        String blob_storage = "//data//" + context.getPackageName() + "//app_webview//" + "//Default//" + "//blob_storage";
        String databases = "//data//" + context.getPackageName()  + "//app_webview//" + "//Default//" + "//databases";
        String indexedDB = "//data//" + context.getPackageName() + "//app_webview//" + "//Default//" + "//IndexedDB";
        String localStorage = "//data//" + context.getPackageName()  + "//app_webview//" + "//Default//" + "//Local Storage";
        String serviceWorker = "//data//" + context.getPackageName() + "//app_webview//" + "//Default//" + "//Service Worker";
        String sessionStorage = "//data//" + context.getPackageName()  + "//app_webview//" + "//Default//" + "//Session Storage";
        String shared_proto_db = "//data//" + context.getPackageName() + "//app_webview//" + "//Default//" + "//shared_proto_db";
        String VideoDecodeStats = "//data//" + context.getPackageName()  + "//app_webview//" + "//Default//" + "//VideoDecodeStats";
        String QuotaManager = "//data//" + context.getPackageName() + "//app_webview//" + "//Default//" + "//QuotaManager";
        String QuotaManager_journal = "//data//" + context.getPackageName()  + "//app_webview//" + "//Default//" + "//QuotaManager-journal";
        String webData = "//data//" + context.getPackageName()  + "//app_webview//" + "//Default//" + "//Web Data";
        String WebDataJournal = "//data//" + context.getPackageName() + "//app_webview//" + "//Default//" + "//Web Data-journal";

        final File blob_storage_file = new File(data, blob_storage);
        final File databases_file = new File(data, databases);
        final File indexedDB_file = new File(data, indexedDB);
        final File localStorage_file = new File(data, localStorage);
        final File serviceWorker_file = new File(data, serviceWorker);
        final File sessionStorage_file = new File(data, sessionStorage);
        final File shared_proto_db_file = new File(data, shared_proto_db);
        final File VideoDecodeStats_file = new File(data, VideoDecodeStats);
        final File QuotaManager_file = new File(data, QuotaManager);
        final File QuotaManager_journal_file = new File(data, QuotaManager_journal);
        final File webData_file = new File(data, webData);
        final File WebDataJournal_file = new File(data, WebDataJournal);

        BrowserUnit.deleteDir(blob_storage_file);
        BrowserUnit.deleteDir(databases_file);
        BrowserUnit.deleteDir(indexedDB_file);
        BrowserUnit.deleteDir(localStorage_file);
        BrowserUnit.deleteDir(serviceWorker_file);
        BrowserUnit.deleteDir(sessionStorage_file);
        BrowserUnit.deleteDir(shared_proto_db_file);
        BrowserUnit.deleteDir(VideoDecodeStats_file);
        BrowserUnit.deleteDir(QuotaManager_file);
        BrowserUnit.deleteDir(QuotaManager_journal_file);
        BrowserUnit.deleteDir(webData_file);
        BrowserUnit.deleteDir(WebDataJournal_file);
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