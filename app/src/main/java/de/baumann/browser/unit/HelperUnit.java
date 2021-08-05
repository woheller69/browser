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
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.File;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.baumann.browser.R;
import de.baumann.browser.browser.DataURIParser;
import de.baumann.browser.view.GridItem;
import de.baumann.browser.view.NinjaToast;

import static android.content.Context.DOWNLOAD_SERVICE;

public class HelperUnit {

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 123;
    private static final int REQUEST_CODE_ASK_PERMISSIONS_1 = 1234;
    private static SharedPreferences sp;

    public static boolean hasPermissionStorage (final Activity activity){
        if (android.os.Build.VERSION.SDK_INT >= 23 ) {
            int hasWRITE_EXTERNAL_STORAGE = activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
               /* if (!activity.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
                    builder.setMessage(R.string.toast_permission_sdCard);
                    builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_ASK_PERMISSIONS));
                    builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
                }*/
                activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_ASK_PERMISSIONS);
                return false;
            }else{
                return true;
            }
        } else {
            return true;
        }
    }

    public static void grantPermissionsLoc(final Activity activity) {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            int hasACCESS_FINE_LOCATION = activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if (hasACCESS_FINE_LOCATION != PackageManager.PERMISSION_GRANTED) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
                builder.setMessage(R.string.setting_summary_location);
                builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_ASK_PERMISSIONS_1));
                builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
                AlertDialog dialog = builder.create();
                dialog.show();
                Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
            }
        }
    }

    public static void saveAs(AlertDialog dialogToCancel, final Activity activity, final String url) {

        try {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
            View dialogView = View.inflate(activity, R.layout.dialog_edit_extension, null);

            final EditText editTitle = dialogView.findViewById(R.id.dialog_edit_1);
            final EditText editExtension = dialogView.findViewById(R.id.dialog_edit_2);

            String filename = URLUtil.guessFileName(url, null, null);
            editTitle.setText(HelperUnit.fileName(url));

            String extension = filename.substring(filename.lastIndexOf("."));
            if(extension.length() <= 8) {
                editExtension.setText(extension);
            }

            builder.setView(dialogView);
            builder.setTitle(R.string.menu_save_as);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {

                String title = editTitle.getText().toString().trim();
                String extension1 = editExtension.getText().toString().trim();
                String filename1 = title + extension1;

                if (title.isEmpty() || extension1.isEmpty() || !extension1.startsWith(".")) {
                    NinjaToast.show(activity, activity.getString(R.string.toast_input_empty));
                } else {
                    if (HelperUnit.hasPermissionStorage(activity)) {
                        Uri source = Uri.parse(url);
                        DownloadManager.Request request = new DownloadManager.Request(source);
                        request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename1);
                        DownloadManager dm = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
                        assert dm != null;
                        dm.enqueue(request);
                        dialogToCancel.cancel();
                    }
                }
            });
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> builder.setCancelable(true));

            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createShortcut (Context context, String title, String url) {
        try {
            Intent i = new Intent();
            i.setAction(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { // code for adding shortcut on pre oreo device
                Intent installer = new Intent();
                installer.putExtra("android.intent.extra.shortcut.INTENT", i);
                installer.putExtra("android.intent.extra.shortcut.NAME", title);
                installer.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context.getApplicationContext(), R.mipmap.ic_launcher));
                installer.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                context.sendBroadcast(installer);
            } else {
                ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
                assert shortcutManager != null;
                if (shortcutManager.isRequestPinShortcutSupported()) {
                    ShortcutInfo pinShortcutInfo =
                            new ShortcutInfo.Builder(context, url)
                                    .setShortLabel(title)
                                    .setLongLabel(title)
                                    .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
                                    .setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    .build();
                    shortcutManager.requestPinShortcut(pinShortcutInfo, null);
                } else {
                    System.out.println("failed_to_add");
                }
            }
        } catch (Exception e) {
            System.out.println("failed_to_add");
        }
    }

    public static String fileName (String url) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        String domain = Objects.requireNonNull(Uri.parse(url).getHost()).replace("www.", "").trim();
        return domain.replace(".", "_").trim() + "_" + currentTime.trim();
    }

    public static String domain (String url) {
        if(url == null){
            return "";
        }else {
            try {
                return Objects.requireNonNull(Uri.parse(url).getHost()).replace("www.", "").trim();
            } catch (Exception e) {
                return "";
            }
        }
    }

    private static final float[] NEGATIVE_COLOR = {
            -1.0f, 0, 0, 0, 255, // Red
            0, -1.0f, 0, 0, 255, // Green
            0, 0, -1.0f, 0, 255, // Blue
            0, 0, 0, 1.0f, 0     // Alpha
    };

    public static void initTheme(Context context) {
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        switch (Objects.requireNonNull(sp.getString("sp_theme", "1"))) {
            case "2":
                context.setTheme(R.style.AppTheme_day);
                break;
            case "3":
                context.setTheme(R.style.AppTheme_night);
                break;
            default:
                context.setTheme(R.style.AppTheme);
                break;
        }
    }

    public static void initRendering(WebView webView, Context context) {
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (sp.getBoolean("sp_invert", false)) {
            if(WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(webView.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
            } else {
                Paint paint = new Paint();
                ColorMatrix matrix = new ColorMatrix();
                matrix.set(NEGATIVE_COLOR);
                ColorMatrix gcm = new ColorMatrix();
                gcm.setSaturation(0);
                ColorMatrix concat = new ColorMatrix();
                concat.setConcat(matrix, gcm);
                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(concat);
                paint.setColorFilter(filter);
                // maybe sometime LAYER_TYPE_NONE would better?
                webView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
            }
        } else {
            if(WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(webView.getSettings(), WebSettingsCompat.FORCE_DARK_OFF);
            } else {
                webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
        }
    }

    public static void addFilterItems (Activity activity, List<GridItem> gridList) {
        GridItem item_01 = new GridItem(R.drawable.circle_red_big, sp.getString("icon_01", activity.getResources().getString(R.string.color_red)),  11);
        GridItem item_02 = new GridItem(R.drawable.circle_pink_big, sp.getString("icon_02", activity.getResources().getString(R.string.color_pink)),  10);
        GridItem item_03 = new GridItem(R.drawable.circle_purple_big, sp.getString("icon_03", activity.getResources().getString(R.string.color_purple)),  9);
        GridItem item_04 = new GridItem(R.drawable.circle_blue_big, sp.getString("icon_04", activity.getResources().getString(R.string.color_blue)),  8);
        GridItem item_05 = new GridItem(R.drawable.circle_teal_big, sp.getString("icon_05", activity.getResources().getString(R.string.color_teal)),  7);
        GridItem item_06 = new GridItem(R.drawable.circle_green_big, sp.getString("icon_06", activity.getResources().getString(R.string.color_green)),  6);
        GridItem item_07 = new GridItem(R.drawable.circle_lime_big, sp.getString("icon_07", activity.getResources().getString(R.string.color_lime)),  5);
        GridItem item_08 = new GridItem(R.drawable.circle_yellow_big, sp.getString("icon_08", activity.getResources().getString(R.string.color_yellow)),  4);
        GridItem item_09 = new GridItem(R.drawable.circle_orange_big, sp.getString("icon_09", activity.getResources().getString(R.string.color_orange)),  3);
        GridItem item_10 = new GridItem(R.drawable.circle_brown_big, sp.getString("icon_10", activity.getResources().getString(R.string.color_brown)),  2);
        GridItem item_11 = new GridItem(R.drawable.circle_grey_big, sp.getString("icon_11", activity.getResources().getString(R.string.color_grey)),  1);

        if (sp.getBoolean("filter_01", true)){ gridList.add(gridList.size(), item_01); }
        if (sp.getBoolean("filter_02", true)){ gridList.add(gridList.size(), item_02); }
        if (sp.getBoolean("filter_03", true)){ gridList.add(gridList.size(), item_03); }
        if (sp.getBoolean("filter_04", true)){ gridList.add(gridList.size(), item_04); }
        if (sp.getBoolean("filter_05", true)){ gridList.add(gridList.size(), item_05); }
        if (sp.getBoolean("filter_06", true)){ gridList.add(gridList.size(), item_06); }
        if (sp.getBoolean("filter_07", true)){ gridList.add(gridList.size(), item_07); }
        if (sp.getBoolean("filter_08", true)){ gridList.add(gridList.size(), item_08); }
        if (sp.getBoolean("filter_09", true)){ gridList.add(gridList.size(), item_09); }
        if (sp.getBoolean("filter_10", true)){ gridList.add(gridList.size(), item_10); }
        if (sp.getBoolean("filter_11", true)){ gridList.add(gridList.size(), item_11); }
    }


    public static void setFilterIcons (ImageView ib_icon, long newIcon) {
        newIcon=newIcon&15;
        if (newIcon == 11) {
            ib_icon.setImageResource(R.drawable.circle_red_big);
        } else if (newIcon == 10) {
            ib_icon.setImageResource(R.drawable.circle_pink_big);
        } else if (newIcon == 9) {
            ib_icon.setImageResource(R.drawable.circle_purple_big);
        } else if (newIcon == 8) {
            ib_icon.setImageResource(R.drawable.circle_blue_big);
        } else if (newIcon == 7) {
            ib_icon.setImageResource(R.drawable.circle_teal_big);
        } else if (newIcon == 6) {
            ib_icon.setImageResource(R.drawable.circle_green_big);
        } else if (newIcon == 5) {
            ib_icon.setImageResource(R.drawable.circle_lime_big);
        } else if (newIcon == 4) {
            ib_icon.setImageResource(R.drawable.circle_yellow_big);
        } else if (newIcon == 3) {
            ib_icon.setImageResource(R.drawable.circle_orange_big);
        } else if (newIcon == 2) {
            ib_icon.setImageResource(R.drawable.circle_brown_big);
        } else if (newIcon == 1) {
            ib_icon.setImageResource(R.drawable.circle_grey_big);
        }
    }

    public static void saveDataURI(AlertDialog dialogToCancel,Context context, Activity activity, DataURIParser dataUriParser) {

        byte[] imagedata = dataUriParser.getImagedata();
        String filename=dataUriParser.getFilename();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        View dialogView = View.inflate(activity, R.layout.dialog_edit_extension, null);

        final EditText editTitle = dialogView.findViewById(R.id.dialog_edit_1);
        final EditText editExtension = dialogView.findViewById(R.id.dialog_edit_2);

        editTitle.setText(filename.substring(0,filename.indexOf(".")));

        String extension = filename.substring(filename.lastIndexOf("."));
        if(extension.length() <= 8) {
            editExtension.setText(extension);
        }

        builder.setView(dialogView);
        builder.setTitle(R.string.menu_save_as);
        builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {

            String title = editTitle.getText().toString().trim();
            String extension1 = editExtension.getText().toString().trim();
            String filename1 = title + extension1;

            if (title.isEmpty() || extension1.isEmpty() || !extension1.startsWith(".")) {
                NinjaToast.show(activity, activity.getString(R.string.toast_input_empty));
            } else {
                if (BackupUnit.checkPermissionStorage(activity)) {
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename1);
                    try {FileOutputStream fos = new FileOutputStream(file);
                        fos.write(imagedata);
                    }catch (Exception e){
                        System.out.println("Error Downloading File: " + e.toString());
                        e.printStackTrace();
                    }
                    dialogToCancel.cancel();
                }else {
                    BackupUnit.requestPermission(context,activity,null);
                }
            }
        });
        builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> builder.setCancelable(true));

        AlertDialog dialog = builder.create();
        dialog.show();
        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);

    }
}