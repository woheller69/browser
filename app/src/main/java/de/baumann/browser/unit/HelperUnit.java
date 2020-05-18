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

import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.text.Html;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.baumann.browser.Ninja.R;
import de.baumann.browser.view.GridItem;
import de.baumann.browser.view.NinjaToast;

public class HelperUnit {

    public static void bound (Context context, View view) {
        int windowWidth = context.getResources().getDisplayMetrics().widthPixels;
        int windowHeight = context.getResources().getDisplayMetrics().heightPixels;

        int widthSpec = View.MeasureSpec.makeMeasureSpec(windowWidth, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(windowHeight, View.MeasureSpec.EXACTLY);

        view.measure(widthSpec, heightSpec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
    }

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 123;
    private static final int REQUEST_CODE_ASK_PERMISSIONS_1 = 1234;
    private static SharedPreferences sp;

    public static void grantPermissionsStorage(final Activity activity) {
        if (android.os.Build.VERSION.SDK_INT >= 23 && android.os.Build.VERSION.SDK_INT < 29) {
            int hasWRITE_EXTERNAL_STORAGE = activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
                if (!activity.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
                    View dialogView = View.inflate(activity, R.layout.dialog_action, null);
                    TextView textView = dialogView.findViewById(R.id.dialog_text);
                    textView.setText(R.string.toast_permission_sdCard);
                    Button action_ok = dialogView.findViewById(R.id.action_ok);
                    action_ok.setOnClickListener(new View.OnClickListener() {
                        @RequiresApi(api = Build.VERSION_CODES.M)
                        @Override
                        public void onClick(View view) {
                            activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    REQUEST_CODE_ASK_PERMISSIONS);
                            bottomSheetDialog.cancel();
                        }
                    });
                    bottomSheetDialog.setContentView(dialogView);
                    bottomSheetDialog.show();
                    HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        }
    }

    public static void grantPermissionsLoc(final Activity activity) {

        if (android.os.Build.VERSION.SDK_INT >= 23) {

            int hasACCESS_FINE_LOCATION = activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if (hasACCESS_FINE_LOCATION != PackageManager.PERMISSION_GRANTED) {

                final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
                View dialogView = View.inflate(activity, R.layout.dialog_action, null);
                TextView textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.toast_permission_loc);
                Button action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                REQUEST_CODE_ASK_PERMISSIONS_1);
                        bottomSheetDialog.cancel();
                    }
                });
                bottomSheetDialog.setContentView(dialogView);
                bottomSheetDialog.show();
                HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    }

    public static void applyTheme(Context context) {
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        String showNavButton = Objects.requireNonNull(sp.getString("sp_theme", "1"));
        switch (showNavButton) {
            case "0":
                context.setTheme(R.style.AppTheme_system);
                break;
            case "2":
                context.setTheme(R.style.AppTheme_dark);
                break;
            case "3":
                context.setTheme(R.style.AppTheme_amoled);
                break;
            default:
                context.setTheme(R.style.AppTheme);
                break;
        }
    }

    public static void setFavorite (Context context, String url) {
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putString("favoriteURL", url).apply();
        NinjaToast.show(context, R.string.toast_fav);
    }

    public static void setBottomSheetBehavior (final BottomSheetDialog dialog, final View view, int beh) {
        BottomSheetBehavior mBehavior = BottomSheetBehavior.from((View) view.getParent());
        mBehavior.setState(beh);
        mBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dialog.cancel();
                }
            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });
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
                installer.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context.getApplicationContext(), R.mipmap.qc_bookmarks));
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
                                    .setIcon(Icon.createWithResource(context, R.mipmap.qc_bookmarks))
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

    public static SpannableString textSpannable (String text) {
        SpannableString s;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            s = new SpannableString(Html.fromHtml(text,Html.FROM_HTML_MODE_LEGACY));
        } else {
            s = new SpannableString(Html.fromHtml(text));
        }
        Linkify.addLinks(s, Linkify.WEB_URLS);
        return s;
    }

    private static final float[] NEGATIVE_COLOR = {
            -1.0f, 0, 0, 0, 255, // Red
            0, -1.0f, 0, 0, 255, // Green
            0, 0, -1.0f, 0, 255, // Blue
            0, 0, 0, 1.0f, 0     // Alpha
    };

    public static void initRendering(View view) {
        if (sp.getBoolean("sp_invert", false)) {
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
            view.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
        } else {
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
    }

    @SuppressWarnings("unchecked")
    public static void addFilterItems (Activity activity, List gridList) {
        GridItem item_01 = new GridItem(R.drawable.circle_red_big, sp.getString("icon_01", activity.getResources().getString(R.string.color_red)), null, 11);
        GridItem item_02 = new GridItem(R.drawable.circle_pink_big, sp.getString("icon_02", activity.getResources().getString(R.string.color_pink)), null, 10);
        GridItem item_03 = new GridItem(R.drawable.circle_purple_big, sp.getString("icon_03", activity.getResources().getString(R.string.color_purple)), null, 9);
        GridItem item_04 = new GridItem(R.drawable.circle_blue_big, sp.getString("icon_04", activity.getResources().getString(R.string.color_blue)), null, 8);
        GridItem item_05 = new GridItem(R.drawable.circle_teal_big, sp.getString("icon_05", activity.getResources().getString(R.string.color_teal)), null, 7);
        GridItem item_06 = new GridItem(R.drawable.circle_green_big, sp.getString("icon_06", activity.getResources().getString(R.string.color_green)), null, 6);
        GridItem item_07 = new GridItem(R.drawable.circle_lime_big, sp.getString("icon_07", activity.getResources().getString(R.string.color_lime)), null, 5);
        GridItem item_08 = new GridItem(R.drawable.circle_yellow_big, sp.getString("icon_08", activity.getResources().getString(R.string.color_yellow)), null, 4);
        GridItem item_09 = new GridItem(R.drawable.circle_orange_big, sp.getString("icon_09", activity.getResources().getString(R.string.color_orange)), null, 3);
        GridItem item_10 = new GridItem(R.drawable.circle_brown_big, sp.getString("icon_10", activity.getResources().getString(R.string.color_brown)), null, 2);
        GridItem item_11 = new GridItem(R.drawable.circle_grey_big, sp.getString("icon_11", activity.getResources().getString(R.string.color_grey)), null, 1);

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
}