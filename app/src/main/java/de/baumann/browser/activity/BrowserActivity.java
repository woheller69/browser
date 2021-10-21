package de.baumann.browser.activity;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import androidx.annotation.NonNull;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;

import androidx.appcompat.app.AppCompatActivity;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.baumann.browser.browser.AdBlock;
import de.baumann.browser.browser.AlbumController;
import de.baumann.browser.browser.BrowserContainer;
import de.baumann.browser.browser.BrowserController;
import de.baumann.browser.browser.Cookie;
import de.baumann.browser.browser.DOM;
import de.baumann.browser.browser.DataURIParser;
import de.baumann.browser.browser.Javascript;
import de.baumann.browser.database.FaviconHelper;
import de.baumann.browser.database.Record;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.R;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.unit.RecordUnit;
import de.baumann.browser.view.CompleteAdapter;
import de.baumann.browser.view.GridAdapter;

import de.baumann.browser.view.GridItem;
import de.baumann.browser.view.NinjaToast;
import de.baumann.browser.view.NinjaWebView;
import de.baumann.browser.view.RecordAdapter;
import de.baumann.browser.view.SwipeTouchListener;

import static android.content.ContentValues.TAG;
import static android.webkit.WebView.HitTestResult.IMAGE_TYPE;
import static android.webkit.WebView.HitTestResult.SRC_ANCHOR_TYPE;
import static android.webkit.WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE;
import static de.baumann.browser.database.RecordAction.BOOKMARK_ITEM;
import static de.baumann.browser.database.RecordAction.HISTORY_ITEM;
import static de.baumann.browser.database.RecordAction.STARTSITE_ITEM;

public class BrowserActivity extends AppCompatActivity implements BrowserController {

    // Menus

    private RecordAdapter adapter;
    private RelativeLayout omniBox;
    private ImageButton omniBox_overview;
    private AutoCompleteTextView omniBox_text;
    private ImageButton tab_openOverView;

    // Views

    private EditText searchBox;
    private BottomSheetDialog bottomSheetDialog_OverView;
    private AlertDialog dialog_tabPreview;
    private NinjaWebView ninjaWebView;
    private View customView;
    private VideoView videoView;
    private ImageButton omniBox_tab;
    private KeyListener listener;
    private BadgeDrawable badgeDrawable;

    // Layouts

    private RelativeLayout searchPanel;
    private FrameLayout contentFrame;
    private LinearLayout tab_container;
    private FrameLayout fullscreenHolder;

    // Others

    private int mLastContentHeight = 0;
    private BottomNavigationView bottom_navigation;
    private BottomAppBar bottomAppBar;

    private String overViewTab;
    private BroadcastReceiver downloadReceiver;

    private Activity activity;
    private Context context;
    private SharedPreferences sp;
    private Javascript javaHosts;
    private Cookie cookieHosts;
    private DOM DOM;
    private ObjectAnimator animation;
    private long newIcon;
    private boolean filter;
    private long filterBy;

    private boolean searchOnSite;

    private ValueCallback<Uri[]> filePathCallback = null;
    private AlbumController currentAlbumController = null;

    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private ValueCallback<Uri[]> mFilePathCallback;

    // Classes

    private class VideoCompletionListener implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            return false;
        }
        @Override
        public void onCompletion(MediaPlayer mp) {
            onHideCustomView();
        }
    }

    private final ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override public void onGlobalLayout() {
            int currentContentHeight = findViewById(Window.ID_ANDROID_CONTENT).getHeight();
            if (mLastContentHeight > currentContentHeight + 100) {
                mLastContentHeight = currentContentHeight;
            } else if (currentContentHeight > mLastContentHeight + 100) {
                mLastContentHeight = currentContentHeight;
                omniBox_text.clearFocus();
            }
        }
    };

    // Overrides

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        context = BrowserActivity.this;
        activity = BrowserActivity.this;
        HelperUnit.initTheme(context);

        sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt("restart_changed", 0).apply();
        sp.edit().putBoolean("pdf_create", false).apply();

        switch (Objects.requireNonNull(sp.getString("start_tab", "0"))) {
            case "3":
                overViewTab = getString(R.string.album_title_bookmarks);
                break;
            case "4":
                overViewTab = getString(R.string.album_title_history);
                break;
            default:
                overViewTab = getString(R.string.album_title_home);
                break;
        }
        setContentView(R.layout.activity_main);

        if (Objects.requireNonNull(sp.getString("saved_key_ok", "no")).equals("no")) {
            if (Locale.getDefault().getCountry().equals("CN")) {
                sp.edit().putString("sp_search_engine", "2").apply();
            }
            sp.edit().putString("saved_key_ok", "yes")
                    .putString("setting_gesture_tb_up", "08")
                    .putString("setting_gesture_tb_down", "01")
                    .putString("setting_gesture_tb_left", "07")
                    .putString("setting_gesture_tb_right", "06")
                    .putString("setting_gesture_nav_up", "04")
                    .putString("setting_gesture_nav_down", "05")
                    .putString("setting_gesture_nav_left", "03")
                    .putString("setting_gesture_nav_right", "02")
                    .putBoolean("sp_autofill", true)
                    .putBoolean("sp_location", false).apply();
        }
        contentFrame = findViewById(R.id.main_content);
        contentFrame.getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);

        // Calculate ActionBar height
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true) && !sp.getBoolean("hideToolbar", true)) {
            int actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
            contentFrame.setPadding(0,0,0,actionBarHeight);
        }

        new AdBlock(context);
        new Javascript(context);
        new Cookie(context);
        new DOM(context);

        downloadReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                builder.setMessage(R.string.toast_downloadComplete);
                builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)));
                builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
                Dialog dialog = builder.create();
                dialog.show();
                Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
            }
        };

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadReceiver, filter);

        mLastContentHeight = findViewById(Window.ID_ANDROID_CONTENT).getHeight();

        initOmniBox();
        initTabDialog();
        initSearchPanel();
        initOverview();
        if (sp.getBoolean("start_tabStart", false)){ //put showOverview first. May be closed again later depending on intent
            showOverview();
        }
        dispatchIntent(getIntent());
    }

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        if(requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        Uri[] results = null;
        // Check that the response is a good one
        if(resultCode == Activity.RESULT_OK) {
            if(data != null) {
                // If there is not data, then we may have taken a photo
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
        }
        mFilePathCallback.onReceiveValue(results);
        mFilePathCallback = null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (sp.getBoolean("sp_camera",false)){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, 1);
            }
        }

        if (sp.getInt("restart_changed", 1) == 1) {
            sp.edit().putInt("restart_changed", 0).apply();
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setMessage(R.string.toast_restart);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> finish());
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        }
        if (sp.getBoolean("pdf_create", false)) {
            sp.edit().putBoolean("pdf_create", false).apply();
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setMessage(R.string.toast_downloadComplete);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)));
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        }
        dispatchIntent(getIntent());
    }

    @Override
    public void onDestroy() {

        if (sp.getBoolean("sp_clear_quit", false)) {

            boolean clearCache = sp.getBoolean("sp_clear_cache", false);
            boolean clearCookie = sp.getBoolean("sp_clear_cookie", false);
            boolean clearHistory = sp.getBoolean("sp_clear_history", false);
            boolean clearIndexedDB = sp.getBoolean("sp_clearIndexedDB", false);

            if (clearCache) {
                BrowserUnit.clearCache(this);
            }
            if (clearCookie) {
                BrowserUnit.clearCookie();
            }
            if (clearHistory) {
                BrowserUnit.clearHistory(this);
            }
            if (clearIndexedDB) {
                BrowserUnit.clearIndexedDB(this);
                WebStorage.getInstance().deleteAllData();
            }
        }
        BrowserContainer.clear();
        unregisterReceiver(downloadReceiver);
        ninjaWebView.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardLayoutListener);
        System.exit(0);
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                showOverflow();
            case KeyEvent.KEYCODE_BACK:
                hideOverview();
                if (fullscreenHolder != null || customView != null || videoView != null) {
                    Log.v(TAG, "FOSS Browser in fullscreen mode");
                } else if (searchPanel.getVisibility() == View.VISIBLE) {
                    searchOnSite = false;
                    searchBox.setText("");
                    searchPanel.setVisibility(View.GONE);
                    omniBox.setVisibility(View.VISIBLE);
                } else if (ninjaWebView.canGoBack()) {
                    WebBackForwardList mWebBackForwardList = ninjaWebView.copyBackForwardList();
                    String historyUrl = mWebBackForwardList.getItemAtIndex(mWebBackForwardList.getCurrentIndex()-1).getUrl();
                    ninjaWebView.initPreferences(historyUrl);
                    goBack_skipRedirects();
                } else {
                    removeAlbum(currentAlbumController);
                }
                return true;
        }
        return false;
    }

    @Override
    public synchronized void showAlbum(AlbumController controller) {

        if (sp.getBoolean("hideToolbar", true)) {
            ObjectAnimator animation = ObjectAnimator.ofFloat(bottomAppBar, "translationY", 0);
            animation.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
            animation.start();
        }

        View av = (View) controller;

        if (currentAlbumController != null) {
            currentAlbumController.deactivate();
        }

        currentAlbumController = controller;
        currentAlbumController.activate();

        contentFrame.removeAllViews();
        contentFrame.addView(av);

        updateOmniBox();

        HelperUnit.initRendering(ninjaWebView, context);

        if (searchPanel.getVisibility() == View.VISIBLE) {
            searchOnSite = false;
            searchBox.setText("");
            searchPanel.setVisibility(View.GONE);
            omniBox.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void updateAutoComplete() {
        RecordAction action = new RecordAction(this);
        List<Record> list = action.listEntries(activity);
        CompleteAdapter adapter = new CompleteAdapter(this, R.layout.item_icon_left, list);
        omniBox_text.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        omniBox_text.setDropDownWidth(context.getResources().getDisplayMetrics().widthPixels);
        omniBox_text.setOnItemClickListener((parent, view, position, id) -> {
            String url = ((TextView) view.findViewById(R.id.record_item_time)).getText().toString();
            for (Record record:list){
                if (record.getURL().equals(url)){
                    if ((record.getType()==BOOKMARK_ITEM)||(record.getType()==STARTSITE_ITEM)||(record.getType()== HISTORY_ITEM) ) {
                        if (record.getDesktopMode() != ninjaWebView.isDesktopMode()) ninjaWebView.toggleDesktopMode(false);
                        ninjaWebView.setJavaScript(record.getJavascript());
                        ninjaWebView.setDomStorage(record.getDomStorage());
                        ninjaWebView.setOldDomain(url);
                        break;
                    }
                }
            }
            ninjaWebView.loadUrl(url);
        });
    }

    private void showOverview() {
        initOverview();
        bottomSheetDialog_OverView.show();
    }

    public void hideOverview () {
        if (bottomSheetDialog_OverView != null) {
            bottomSheetDialog_OverView.cancel();
        }
    }

    public void hideTabView () {
        if (dialog_tabPreview != null) {
            dialog_tabPreview.hide();
        }
    }

    public void showTabView () {
        HelperUnit.hideSoftKeyboard(omniBox_text, context);
        if (overViewTab.equals(getString(R.string.album_title_home))) {
            tab_openOverView.setImageResource(R.drawable.icon_web_light);
        } else if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
            tab_openOverView.setImageResource(R.drawable.icon_bookmark_light);
        } else if (overViewTab.equals(getString(R.string.album_title_history))) {
            tab_openOverView.setImageResource(R.drawable.icon_history_light);
        }
        dialog_tabPreview.show();
    }

    private void printPDF () {
        String title = HelperUnit.fileName(ninjaWebView.getUrl());
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        PrintDocumentAdapter printAdapter = ninjaWebView.createPrintDocumentAdapter(title);
        Objects.requireNonNull(printManager).print(title, printAdapter, new PrintAttributes.Builder().build());
        sp.edit().putBoolean("pdf_create", true).apply();
    }

    private void dispatchIntent(Intent intent) {
        String action = intent.getAction();
        String url = intent.getStringExtra(Intent.EXTRA_TEXT);

        if ("".equals(action)) {
            Log.i(TAG, "resumed FOSS browser");
        } else if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_WEB_SEARCH)) {
            addAlbum(null, Objects.requireNonNull(intent.getStringExtra(SearchManager.QUERY)), true);
            getIntent().setAction("");
            hideOverview();
        } else if (filePathCallback != null) {
            filePathCallback = null;
            getIntent().setAction("");
        } else if (url != null && Intent.ACTION_SEND.equals(action)) {
            addAlbum(getString(R.string.app_name), url, true);
            getIntent().setAction("");
            hideOverview();
        } else if (Intent.ACTION_VIEW.equals(action)) {
            String data = Objects.requireNonNull(getIntent().getData()).toString();
            addAlbum(getString(R.string.app_name), data, true);
            getIntent().setAction("");
            hideOverview();
        } else if (BrowserContainer.size() < 1) {
            addAlbum(getString(R.string.app_name), Objects.requireNonNull(sp.getString("favoriteURL", "https://github.com/scoute-dich/browser")), true);
            getIntent().setAction("");
        }
    }

    private void initTabDialog () {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.dialog_tabs, null);

        tab_container = dialogView.findViewById(R.id.tab_container);
        tab_openOverView = dialogView.findViewById(R.id.tab_openOverView);
        tab_openOverView.setOnClickListener(view -> {
            dialog_tabPreview.cancel();
            showOverview();
        });
        tab_openOverView.setOnLongClickListener(v -> {
            bottom_navigation.setSelectedItemId(R.id.page_2);
            hideTabView();
            showOverview();
            show_dialogFilter();
            return false;
        });

        builder.setView(dialogView);
        dialog_tabPreview = builder.create();
        Objects.requireNonNull(dialog_tabPreview.getWindow()).setGravity(Gravity.BOTTOM);
        dialog_tabPreview.setOnCancelListener(dialog ->
                dialog_tabPreview.hide());
    }

    @SuppressLint({"ClickableViewAccessibility", "UnsafeExperimentalUsageError"})
    private void initOmniBox() {

        omniBox = findViewById(R.id.omniBox);
        omniBox_text = findViewById(R.id.omniBox_input);
        listener = omniBox_text.getKeyListener(); // Save the default KeyListener!!!

        omniBox_text.setKeyListener(null); // Disable input
        omniBox_text.setEllipsize(TextUtils.TruncateAt.END);
        omniBox_overview = findViewById(R.id.omnibox_overview);
        omniBox_tab = findViewById(R.id.omniBox_tab);
        omniBox_tab.setOnClickListener(v -> showTabView());


        bottomAppBar = findViewById(R.id.bottomAppBar);
        bottomAppBar.setTitle("Foss Browser");

        badgeDrawable = BadgeDrawable.create(context);
        badgeDrawable.setBadgeGravity(BadgeDrawable.TOP_END);
        badgeDrawable.setNumber(BrowserContainer.size());
        badgeDrawable.setBackgroundColor(getResources().getColor(R.color.primaryColor));
        BadgeUtils.attachBadgeDrawable(badgeDrawable, omniBox_tab, findViewById(R.id.layout));

        ImageButton omnibox_overflow = findViewById(R.id.omnibox_overflow);
        omnibox_overflow.setOnClickListener(v -> showOverflow());
        omnibox_overflow.setOnLongClickListener(v -> {
            show_dialogFastToggle();
            return false;
        });
        omnibox_overflow.setOnTouchListener(new SwipeTouchListener(context) {
            public void onSwipeTop() { performGesture("setting_gesture_nav_up"); }
            public void onSwipeBottom() { performGesture("setting_gesture_nav_down"); }
            public void onSwipeRight() { performGesture("setting_gesture_nav_right"); }
            public void onSwipeLeft() { performGesture("setting_gesture_nav_left"); }
        });
        omniBox_overview.setOnTouchListener(new SwipeTouchListener(context) {
            public void onSwipeTop() { performGesture("setting_gesture_nav_up"); }
            public void onSwipeBottom() { performGesture("setting_gesture_nav_down"); }
            public void onSwipeRight() { performGesture("setting_gesture_nav_right"); }
            public void onSwipeLeft() { performGesture("setting_gesture_nav_left"); }
        });
        omniBox_tab.setOnTouchListener(new SwipeTouchListener(context) {
            public void onSwipeTop() { performGesture("setting_gesture_nav_up"); }
            public void onSwipeBottom() { performGesture("setting_gesture_nav_down"); }
            public void onSwipeRight() { performGesture("setting_gesture_nav_right"); }
            public void onSwipeLeft() { performGesture("setting_gesture_nav_left"); }
        });
        omniBox_text.setOnTouchListener(new SwipeTouchListener(context) {
            public void onSwipeTop() {
                if (!omniBox_text.hasFocus()) {
                    performGesture("setting_gesture_tb_up");
                }
            }
            public void onSwipeBottom() {
                if (!omniBox_text.hasFocus()) {
                    performGesture("setting_gesture_tb_down");
                }
            }
            public void onSwipeRight() {
                if (!omniBox_text.hasFocus()) {
                    performGesture("setting_gesture_tb_right");
                }
            }
            public void onSwipeLeft() {
                if (!omniBox_text.hasFocus()) {
                    performGesture("setting_gesture_tb_left");
                }
            }
        });
        omniBox_text.setOnEditorActionListener((v, actionId, event) -> {
            String query = omniBox_text.getText().toString().trim();
            ninjaWebView.loadUrl(query);
            return false;
        });
        omniBox_text.setOnFocusChangeListener((v, hasFocus) -> {
            if (omniBox_text.hasFocus()) {
                String url = ninjaWebView.getUrl();
                ninjaWebView.stopLoading();
                omniBox_text.setKeyListener(listener);
                if (url==null || url.contains("about:blank")) {
                    omniBox_text.setText("");
                } else {
                    omniBox_text.setText(url);
                }
                updateAutoComplete();
                omniBox_text.selectAll();
            } else {
                omniBox_text.setKeyListener(null);
                omniBox_text.setEllipsize(TextUtils.TruncateAt.END);
                omniBox_text.setText(ninjaWebView.getTitle());
                updateOmniBox();
            }
        });
        omniBox_overview.setOnClickListener(v -> showOverview());
        omniBox_overview.setOnLongClickListener(v -> {
            bottom_navigation.setSelectedItemId(R.id.page_2);
            showOverview();
            show_dialogFilter();
            return false;
        });
    }

    private void performGesture (String gesture) {
        String gestureAction = Objects.requireNonNull(sp.getString(gesture, "0"));
        switch (gestureAction) {
            case "01":
                break;
            case "02":
                if (ninjaWebView.canGoForward()) {
                    WebBackForwardList mWebBackForwardList = ninjaWebView.copyBackForwardList();
                    String historyUrl = mWebBackForwardList.getItemAtIndex(mWebBackForwardList.getCurrentIndex()+1).getUrl();
                    ninjaWebView.initPreferences(historyUrl);
                    ninjaWebView.goForward();
                } else {
                    NinjaToast.show(this, R.string.toast_webview_forward);
                }
                break;
            case "03":
                if (ninjaWebView.canGoBack()) {
                    WebBackForwardList mWebBackForwardList = ninjaWebView.copyBackForwardList();
                    String historyUrl = mWebBackForwardList.getItemAtIndex(mWebBackForwardList.getCurrentIndex()-1).getUrl();
                    ninjaWebView.initPreferences(historyUrl);
                    goBack_skipRedirects();
                } else {
                    removeAlbum(currentAlbumController);
                }
                break;
            case "04":
                ninjaWebView.pageUp(true);
                break;
            case "05":
                ninjaWebView.pageDown(true);
                break;
            case "06":
                // currentAlbumController = nextAlbumController(false);
                showAlbum(nextAlbumController(false));
                break;
            case "07":
                //currentAlbumController = nextAlbumController(true);
                showAlbum(nextAlbumController(true));
                break;
            case "08":
                showOverview();
                break;
            case "09":
                addAlbum(getString(R.string.app_name), Objects.requireNonNull(sp.getString("favoriteURL", "https://github.com/scoute-dich/browser")), true);
                break;
            case "10":
                removeAlbum(currentAlbumController);
                break;
            case "11":
                showTabView();
                break;
            case "12":
                shareLink(ninjaWebView.getTitle(), ninjaWebView.getUrl());
                break;
            case "13":
                searchOnSite();
                break;
            case "14":
                saveBookmark();
                break;
            case "15":
                save_atHome(ninjaWebView.getTitle(), ninjaWebView.getUrl());
                break;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initOverview() {
        bottomSheetDialog_OverView = new BottomSheetDialog(context);
        View dialogView = View.inflate(context, R.layout.dialog_overview, null);
        ListView listView = dialogView.findViewById(R.id.list_overView);
        // allow scrolling in listView without closing the bottomSheetDialog
        listView.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                // Disallow NestedScrollView to intercept touch events.
                if (listView.canScrollVertically(-1)) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                }
            }
            // Handle ListView touch events.
            v.onTouchEvent(event);
            return true;
        });

        bottomSheetDialog_OverView.setContentView(dialogView);

        BottomNavigationView.OnNavigationItemSelectedListener navListener = menuItem -> {
            if (menuItem.getItemId() == R.id.page_0) {
                hideOverview();
                showTabView();
            } else if (menuItem.getItemId() == R.id.page_1) {
                omniBox_overview.setImageResource(R.drawable.icon_web_light);
                overViewTab = getString(R.string.album_title_home);

                RecordAction action = new RecordAction(context);
                action.open(false);
                final List<Record> list = action.listStartSite(activity);
                action.close();

                adapter = new RecordAdapter(context, list);
                listView.setAdapter(adapter);
                adapter.notifyDataSetChanged();

                listView.setOnItemClickListener((parent, view, position, id) -> {
                    if ((list.get(position).getDesktopMode()) != ninjaWebView.isDesktopMode()) ninjaWebView.toggleDesktopMode(false);
                    ninjaWebView.setJavaScript(list.get(position).getJavascript());
                    ninjaWebView.setDomStorage(list.get(position).getDomStorage());
                    ninjaWebView.setOldDomain(list.get(position).getURL());
                    ninjaWebView.loadUrl(list.get(position).getURL());
                    hideOverview();
                });

                listView.setOnItemLongClickListener((parent, view, position, id) -> {
                    showContextMenuList(list.get(position).getTitle(), list.get(position).getURL(), adapter, list, position);
                    return true;
                });
            } else if (menuItem.getItemId() == R.id.page_2) {
                omniBox_overview.setImageResource(R.drawable.icon_bookmark_light);
                overViewTab = getString(R.string.album_title_bookmarks);

                RecordAction action = new RecordAction(context);
                action.open(false);
                final List<Record> list;
                list = action.listBookmark(activity, filter, filterBy);
                action.close();

                adapter = new RecordAdapter(context, list){
                    @SuppressWarnings("NullableProblems")
                    @Override
                    public View getView (int position, View convertView, @NonNull ViewGroup parent) {
                        View v = super.getView(position, convertView, parent);
                        ImageView record_item_icon = v.findViewById(R.id.record_item_icon);
                        record_item_icon.setVisibility(View.VISIBLE);
                        return v;
                    }
                };

                listView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                filter = false;
                listView.setOnItemClickListener((parent, view, position, id) -> {
                    if ((list.get(position).getDesktopMode()) != ninjaWebView.isDesktopMode()) ninjaWebView.toggleDesktopMode(false);
                    ninjaWebView.setJavaScript(list.get(position).getJavascript());
                    ninjaWebView.setDomStorage(list.get(position).getDomStorage());
                    ninjaWebView.setOldDomain(list.get(position).getURL());
                    ninjaWebView.loadUrl(list.get(position).getURL());
                    hideOverview();
                });
                listView.setOnItemLongClickListener((parent, view, position, id) -> {
                    showContextMenuList(list.get(position).getTitle(), list.get(position).getURL(), adapter, list, position);
                    return true;
                });
            } else if (menuItem.getItemId() == R.id.page_3) {
                omniBox_overview.setImageResource(R.drawable.icon_history_light);
                overViewTab = getString(R.string.album_title_history);

                RecordAction action = new RecordAction(context);
                action.open(false);
                final List<Record> list;
                list = action.listHistory();
                action.close();

                //noinspection NullableProblems
                adapter = new RecordAdapter(context, list){
                    @Override
                    public View getView (int position, View convertView, @NonNull ViewGroup parent) {
                        View v = super.getView(position, convertView, parent);
                        TextView record_item_time = v.findViewById(R.id.record_item_time);
                        record_item_time.setVisibility(View.VISIBLE);
                        return v;
                    }
                };

                listView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                listView.setOnItemClickListener((parent, view, position, id) -> {
                    if ((list.get(position).getDesktopMode()) != ninjaWebView.isDesktopMode()) ninjaWebView.toggleDesktopMode(false);
                    ninjaWebView.setJavaScript(list.get(position).getJavascript());
                    ninjaWebView.setDomStorage(list.get(position).getDomStorage());
                    ninjaWebView.setOldDomain(list.get(position).getURL());
                    ninjaWebView.loadUrl(list.get(position).getURL());
                    hideOverview();
                });

                listView.setOnItemLongClickListener((parent, view, position, id) -> {
                    showContextMenuList(list.get(position).getTitle(), list.get(position).getURL(), adapter, list, position);
                    return true;
                });
            } else if (menuItem.getItemId() == R.id.page_4) {

                PopupMenu popup = new PopupMenu(this, bottom_navigation.findViewById(R.id.page_2));
                if (overViewTab.equals(getString(R.string.album_title_home))) {
                    popup.inflate(R.menu.menu_list_start);
                } else if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                    popup.inflate(R.menu.menu_list_bookmark);
                } else {
                    popup.inflate(R.menu.menu_list_history);
                }
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.menu_delete) {
                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                        builder.setMessage(R.string.hint_database);
                        builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
                            if (overViewTab.equals(getString(R.string.album_title_home))) {
                                BrowserUnit.clearHome(context);
                                bottom_navigation.setSelectedItemId(R.id.page_1);
                            } else if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                                BrowserUnit.clearBookmark(context);
                                bottom_navigation.setSelectedItemId(R.id.page_2);
                            } else if (overViewTab.equals(getString(R.string.album_title_history))) {
                                BrowserUnit.clearHistory(context);
                                bottom_navigation.setSelectedItemId(R.id.page_3);
                            }
                        });
                        builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
                    } else if (item.getItemId() == R.id.menu_sortName) {
                        if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                            sp.edit().putString("sort_bookmark", "title").apply();
                            bottom_navigation.setSelectedItemId(R.id.page_2);
                        } else if (overViewTab.equals(getString(R.string.album_title_home))) {
                            sp.edit().putString("sort_startSite", "title").apply();
                            bottom_navigation.setSelectedItemId(R.id.page_1);
                        }
                    } else if (item.getItemId() == R.id.menu_sortIcon) {
                        sp.edit().putString("sort_bookmark", "time").apply();
                        bottom_navigation.setSelectedItemId(R.id.page_2);
                    } else if (item.getItemId() == R.id.menu_sortDate) {
                        sp.edit().putString("sort_startSite", "ordinal").apply();
                        bottom_navigation.setSelectedItemId(R.id.page_1);
                    } else if (item.getItemId() == R.id.menu_filter) {
                        show_dialogFilter();
                    }
                    return true;
                });
                popup.show();
                popup.setOnDismissListener(v -> {
                    if (overViewTab.equals(getString(R.string.album_title_home))) {
                        bottom_navigation.setSelectedItemId(R.id.page_1);
                    } else if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                        bottom_navigation.setSelectedItemId(R.id.page_2);
                    } else if (overViewTab.equals(getString(R.string.album_title_history))) {
                        bottom_navigation.setSelectedItemId(R.id.page_3);
                    }
                });
            }
            return true;
        };

        bottom_navigation = dialogView.findViewById(R.id.bottom_navigation);
        bottom_navigation.setOnNavigationItemSelectedListener(navListener);
        bottom_navigation.findViewById(R.id.page_2).setOnLongClickListener(v -> {
            show_dialogFilter();
            return true;
        });

        bottom_navigation.getOrCreateBadge(R.id.page_0).setNumber(BrowserContainer.size());
        bottom_navigation.getOrCreateBadge(R.id.page_0).setBackgroundColor(getResources().getColor(R.color.primaryColor));

        if (overViewTab.equals(getString(R.string.album_title_home))) {
            bottom_navigation.setSelectedItemId(R.id.page_1);
        } else if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
            bottom_navigation.setSelectedItemId(R.id.page_2);
        } else if (overViewTab.equals(getString(R.string.album_title_history))) {
            bottom_navigation.setSelectedItemId(R.id.page_3);
        }

        BottomSheetBehavior<View> mBehavior = BottomSheetBehavior.from((View) dialogView.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        mBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN || newState == BottomSheetBehavior.STATE_COLLAPSED){
                    hideOverview();
                }
            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });
    }

    private void initSearchPanel() {
        searchPanel = findViewById(R.id.searchBox);
        searchBox = findViewById(R.id.searchBox_input);
        ImageView searchUp = findViewById(R.id.searchBox_up);
        ImageView searchDown = findViewById(R.id.searchBox_down);
        ImageView searchCancel = findViewById(R.id.searchBox_cancel);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                if (currentAlbumController != null) {
                    ((NinjaWebView) currentAlbumController).findAllAsync(s.toString());
                }
            }
        });
        searchUp.setOnClickListener(v -> ((NinjaWebView) currentAlbumController).findNext(false));
        searchDown.setOnClickListener(v -> ((NinjaWebView) currentAlbumController).findNext(true));
        searchCancel.setOnClickListener(v -> {
            if (searchBox.getText().length() > 0) {
                searchBox.setText("");
            } else {
                searchOnSite = false;
                HelperUnit.hideSoftKeyboard(searchBox, context);
                searchPanel.setVisibility(View.GONE);
                omniBox.setVisibility(View.VISIBLE);
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void show_dialogFastToggle() {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.dialog_toggle, null);
        builder.setView(dialogView);
        FaviconHelper.setFavicon(context, dialogView, ninjaWebView.getUrl(), R.id.menu_icon, R.drawable.icon_image_broken);

        AlertDialog dialog = builder.create();
        dialog.show();
        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);

        //TabControl

        Chip chip_javaScript_Tab = dialogView.findViewById(R.id.chip_javaScript_Tab);
        if (ninjaWebView.getSettings().getJavaScriptEnabled()) {
            chip_javaScript_Tab.setChecked(true);
            chip_javaScript_Tab.setOnClickListener(view -> {
                ninjaWebView.setJavaScript(false);
                ninjaWebView.reload();
                dialog.cancel();
            });
        } else {
            chip_javaScript_Tab.setChecked(false);
            chip_javaScript_Tab.setOnClickListener(view -> {
                ninjaWebView.setJavaScript(true);
                ninjaWebView.reload();
                dialog.cancel();
            });
        }

        Chip chip_dom_Tab = dialogView.findViewById(R.id.chip_dom_Tab);
        if (ninjaWebView.getSettings().getDomStorageEnabled()) {
            chip_dom_Tab.setChecked(true);
            chip_dom_Tab.setOnClickListener(view -> {
                ninjaWebView.setDomStorage(false);
                ninjaWebView.reload();
                dialog.cancel();
            });
        } else {
            chip_dom_Tab.setChecked(false);
            chip_dom_Tab.setOnClickListener(view -> {
                ninjaWebView.setDomStorage(true);
                ninjaWebView.reload();
                dialog.cancel();
            });
        }

        Chip chip_allow_popups_WL = dialogView.findViewById(R.id.chip_allow_popups_WL);
        chip_allow_popups_WL.setChecked(ninjaWebView.getSettings().getJavaScriptCanOpenWindowsAutomatically());
        chip_allow_popups_WL.setOnClickListener(v -> {
            if (ninjaWebView.getSettings().getJavaScriptEnabled()) ninjaWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(chip_allow_popups_WL.isChecked());
            ninjaWebView.reload();
            dialog.cancel();
        });

        Chip chip_fingerpint_tab = dialogView.findViewById(R.id.chip_fingerpint_tab);
        chip_fingerpint_tab.setChecked(ninjaWebView.isFingerPrintProtection());
        chip_fingerpint_tab.setOnClickListener(v -> {
            ninjaWebView.toggleAllowFingerprint(true);
            dialog.cancel();
        });

        // CheckBox

        TextView dialog_title = dialogView.findViewById(R.id.dialog_title);
        dialog_title.setText(HelperUnit.domain(ninjaWebView.getUrl()));

        javaHosts = new Javascript(context);
        cookieHosts = new Cookie(context);
        DOM = new DOM(context);
        ninjaWebView = (NinjaWebView) currentAlbumController;

        final String url = ninjaWebView.getUrl();

        Chip chip_javaScript_WL = dialogView.findViewById(R.id.chip_javaScript_WL);
        chip_javaScript_WL.setChecked(javaHosts.isWhite(url));
        chip_javaScript_WL.setOnClickListener(v -> {
            if (javaHosts.isWhite(ninjaWebView.getUrl())) {
                chip_javaScript_WL.setChecked(false);
                javaHosts.removeDomain(HelperUnit.domain(url));
            } else {
                chip_javaScript_WL.setChecked(true);
                javaHosts.addDomain(HelperUnit.domain(url));
            }
        });

        Chip chip_dom_WL = dialogView.findViewById(R.id.chip_dom_WL);
        chip_dom_WL.setChecked(DOM.isWhite(url));
        chip_dom_WL.setOnClickListener(v -> {
            if (DOM.isWhite(ninjaWebView.getUrl())) {
                chip_dom_WL.setChecked(false);
                DOM.removeDomain(HelperUnit.domain(url));
            } else {
                chip_dom_WL.setChecked(true);
                DOM.addDomain(HelperUnit.domain(url));
            }
        });

        Chip chip_cookie_WL = dialogView.findViewById(R.id.chip_cookie_WL);
        chip_cookie_WL.setChecked(cookieHosts.isWhite(url));
        chip_cookie_WL.setOnClickListener(v -> {
            if (cookieHosts.isWhite(ninjaWebView.getUrl())) {
                chip_cookie_WL.setChecked(false);
                cookieHosts.removeDomain(HelperUnit.domain(url));
            } else {
                chip_cookie_WL.setChecked(true);
                cookieHosts.addDomain(HelperUnit.domain(url));
            }
        });

        Chip chip_javaScript = dialogView.findViewById(R.id.chip_javaScript);
        chip_javaScript.setChecked(sp.getBoolean("sp_javascript", true));
        chip_javaScript.setOnClickListener(v -> {
            if (sp.getBoolean("sp_javascript", true)) {
                chip_javaScript.setChecked(false);
                sp.edit().putBoolean("sp_javascript", false).apply();
            } else {
                chip_javaScript.setChecked(true);
                sp.edit().putBoolean("sp_javascript", true).apply();
            }
        });

        Chip chip_dom = dialogView.findViewById(R.id.chip_dom);
        chip_dom.setChecked(sp.getBoolean("sp_remote", true));
        chip_dom.setOnClickListener(v -> {
            if (sp.getBoolean("sp_remote", true)) {
                chip_dom.setChecked(false);
                sp.edit().putBoolean("sp_remote", false).apply();
            } else {
                chip_dom.setChecked(true);
                sp.edit().putBoolean("sp_remote", true).apply();
            }
        });

        Chip chip_cookie = dialogView.findViewById(R.id.chip_cookie);
        chip_cookie.setChecked(sp.getBoolean("sp_cookies", true));
        chip_cookie.setOnClickListener(v -> {
            if (sp.getBoolean("sp_cookies", true)) {
                chip_cookie.setChecked(false);
                sp.edit().putBoolean("sp_cookies", false).apply();
            } else {
                chip_cookie.setChecked(true);
                sp.edit().putBoolean("sp_cookies", true).apply();
            }
        });

        Chip chip_adBlock = dialogView.findViewById(R.id.chip_adBlock);
        chip_adBlock.setChecked(sp.getBoolean("sp_ad_block", true));
        chip_adBlock.setOnClickListener(v -> {
            if (sp.getBoolean("sp_ad_block", true)) {
                chip_adBlock.setChecked(false);
                sp.edit().putBoolean("sp_ad_block", false).apply();
            } else {
                chip_adBlock.setChecked(true);
                sp.edit().putBoolean("sp_ad_block", true).apply();
            }
        });

        Chip chip_fingerprint = dialogView.findViewById(R.id.chip_Fingerprint);
        chip_fingerprint.setChecked(sp.getBoolean("sp_fingerPrintProtection",false));
        chip_fingerprint.setOnClickListener(v -> sp.edit().putBoolean("sp_fingerPrintProtection",chip_fingerprint.isChecked()).apply());

        Chip chip_history = dialogView.findViewById(R.id.chip_history);
        chip_history.setChecked(sp.getBoolean("saveHistory", true));
        chip_history.setOnClickListener(v -> {
            if (sp.getBoolean("saveHistory", true)) {
                chip_history.setChecked(false);
                sp.edit().putBoolean("saveHistory", false).apply();
            } else {
                chip_history.setChecked(true);
                sp.edit().putBoolean("saveHistory", true).apply();
            }
        });

        Chip chip_image = dialogView.findViewById(R.id.chip_image);
        chip_image.setChecked(sp.getBoolean("sp_images", true));
        chip_image.setOnClickListener(v -> {
            if (sp.getBoolean("sp_images", true)) {
                chip_image.setChecked(false);
                sp.edit().putBoolean("sp_images", false).apply();
            } else {
                chip_image.setChecked(true);
                sp.edit().putBoolean("sp_images", true).apply();
            }
        });

        Chip chip_desktopMode = dialogView.findViewById(R.id.chip_desktopMode);
        chip_desktopMode.setChecked(ninjaWebView.isDesktopMode());
        chip_desktopMode.setOnClickListener(v -> {
            ninjaWebView.toggleDesktopMode(true);
            dialog.cancel();
        });

        Chip chip_night = dialogView.findViewById(R.id.chip_night);
        chip_night.setChecked(sp.getBoolean("sp_invert", false));
        chip_night.setOnClickListener(v -> {
            if (sp.getBoolean("sp_invert", false)) {
                chip_night.setChecked(false);
                sp.edit().putBoolean("sp_invert", false).apply();
            } else {
                chip_night.setChecked(true);
                sp.edit().putBoolean("sp_invert", true).apply();
            }
            HelperUnit.initRendering(ninjaWebView, context);
            dialog.cancel();
        });

        ImageButton ib_reload = dialogView.findViewById(R.id.ib_reload);
        ib_reload.setOnClickListener(view -> {
            if (ninjaWebView != null) {
                dialog.cancel();
                ninjaWebView.initPreferences(ninjaWebView.getUrl());
                ninjaWebView.reload();
            }
        });

        ImageButton ib_settings = dialogView.findViewById(R.id.ib_settings);
        ib_settings.setOnClickListener(view -> {
            if (ninjaWebView != null) {
                dialog.cancel();
                Intent settings = new Intent(BrowserActivity.this, Settings_Activity.class);
                startActivity(settings);
            }
        });
    }


    @SuppressLint("ClickableViewAccessibility")
    private synchronized void addAlbum(String title, final String url, final boolean foreground) {
        ninjaWebView = new NinjaWebView(context);
        ninjaWebView.setBrowserController(this);
        ninjaWebView.setAlbumTitle(title, url);
        activity.registerForContextMenu(ninjaWebView);

        SwipeTouchListener swipeTouchListener;
        swipeTouchListener = new SwipeTouchListener(context) {
            public void onSwipeBottom() {

                if (sp.getBoolean("sp_swipeToReload", true)) {
                    ninjaWebView.reload();
                }

                if (sp.getBoolean("hideToolbar", true)) {
                    if (animation==null || !animation.isRunning()) {
                        animation = ObjectAnimator.ofFloat(bottomAppBar, "translationY", 0);
                        animation.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
                        animation.start();
                    }
                }

            }
            public void onSwipeTop(){
                if (!ninjaWebView.canScrollVertically(0) && sp.getBoolean("hideToolbar", true)) {
                    if (animation==null || !animation.isRunning()) {
                        animation = ObjectAnimator.ofFloat(bottomAppBar, "translationY", bottomAppBar.getHeight());
                        animation.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
                        animation.start();
                    }
                }
            }
        };

        ninjaWebView.setOnTouchListener(swipeTouchListener);

        ninjaWebView.setOnScrollChangeListener((scrollY, oldScrollY) -> {
            if (!searchOnSite) {
                if (sp.getBoolean("hideToolbar", true)) {
                    if (scrollY > oldScrollY) {
                        if (animation==null || !animation.isRunning()) {
                            animation = ObjectAnimator.ofFloat(bottomAppBar, "translationY", bottomAppBar.getHeight());
                            animation.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
                            animation.start();
                        }
                    } else if (scrollY < oldScrollY) {
                        if (animation==null || !animation.isRunning()) {
                            animation = ObjectAnimator.ofFloat(bottomAppBar, "translationY", 0);
                            animation.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
                            animation.start();
                        }
                    }
                }
            }
            if (scrollY==0) {
                ninjaWebView.setOnTouchListener(swipeTouchListener);
            } else {
                ninjaWebView.setOnTouchListener(null);
            }
        });

        if (url.isEmpty() || url.equals("about:blank")) {
            ninjaWebView.loadData("about:blank","","");
        } else {
            ninjaWebView.loadUrl(url);
        }

        if (currentAlbumController != null) {
            int index = BrowserContainer.indexOf(currentAlbumController) + 1;
            BrowserContainer.add(ninjaWebView, index);
        } else {
            BrowserContainer.add(ninjaWebView);
        }

        if (!foreground) {
            ninjaWebView.deactivate();
        } else {
            ninjaWebView.activate();
            showAlbum(ninjaWebView);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                ninjaWebView.initPreferences(ninjaWebView.getUrl());
                ninjaWebView.reload();
            }
        }

        View albumView = ninjaWebView.getAlbumView();
        tab_container.addView(albumView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        updateOmniBox();
    }

    private void closeTabConfirmation(final Runnable okAction) {
        if(!sp.getBoolean("sp_close_tab_confirm", false)) {
            okAction.run();
        } else {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setMessage(R.string.toast_quit_TAB);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> okAction.run());
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        }
    }

    @Override
    public synchronized void removeAlbum (final AlbumController controller) {
        if (BrowserContainer.size() <= 1) {
            if(!sp.getBoolean("sp_reopenLastTab", false)) {
                doubleTapsQuit();
            }else{
                ninjaWebView.loadUrl(Objects.requireNonNull(sp.getString("favoriteURL", "https://github.com/scoute-dich/browser")));
                hideOverview();
            }
        } else {
            closeTabConfirmation(() -> {
                tab_container.removeView(controller.getAlbumView());
                int index = BrowserContainer.indexOf(controller);
                BrowserContainer.remove(controller);
                if (index >= BrowserContainer.size()) {
                    index = BrowserContainer.size() - 1;
                }
                showAlbum(BrowserContainer.get(index));
            });
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private void updateOmniBox() {

        bottom_navigation.getOrCreateBadge(R.id.page_0).setNumber(BrowserContainer.size());
        badgeDrawable.setNumber(BrowserContainer.size());
        BadgeUtils.attachBadgeDrawable(badgeDrawable, omniBox_tab, findViewById(R.id.layout));
        omniBox_text.clearFocus();

        ninjaWebView = (NinjaWebView) currentAlbumController;
        this.cookieHosts = new Cookie(this.context);
        CookieManager manager = CookieManager.getInstance();

        String url = ninjaWebView.getUrl();
        if (url != null) {
            if (cookieHosts.isWhite(url) || sp.getBoolean("sp_cookies", true)) {
                manager.setAcceptCookie(true);
                manager.getCookie(url);
            } else {
                manager.setAcceptCookie(false);
            }
            if (Objects.requireNonNull(ninjaWebView.getTitle()).isEmpty()) {
                omniBox_text.setText(url);
            } else {
                omniBox_text.setText(ninjaWebView.getTitle());
            }
            if (url.startsWith("https://")) {
                omniBox_tab.setImageResource(R.drawable.icon_menu_light);
                omniBox_tab.setOnClickListener(v -> showTabView());
            } else if (url.contains("about:blank")){
                omniBox_tab.setImageResource(R.drawable.icon_menu_light);
                omniBox_tab.setOnClickListener(v -> showTabView());
                omniBox_text.setText("");
            } else {
                omniBox_tab.setImageResource(R.drawable.icon_alert);
                omniBox_tab.setOnClickListener(v -> {
                    MaterialAlertDialogBuilder builderR = new MaterialAlertDialogBuilder(context);
                    builderR.setMessage(R.string.toast_unsecured);
                    builderR.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> ninjaWebView.loadUrl(url.replace("http://", "https://")));
                    builderR.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> {
                        dialog.cancel();
                        omniBox_tab.setImageResource(R.drawable.icon_menu_light);
                        omniBox_tab.setOnClickListener(v2 -> showTabView());
                    });
                    AlertDialog dialog = builderR.create();
                    dialog.show();
                    Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
                });
            }
        }
    }

    @Override
    public synchronized void updateProgress(int progress) {
        CircularProgressIndicator progressBar = findViewById(R.id.main_progress_bar);
        progressBar.setOnClickListener(v -> ninjaWebView.stopLoading());
        progressBar.setProgressCompat(progress, true);
        if (progress != BrowserUnit.LOADING_STOPPED) updateOmniBox();
        if (progress < BrowserUnit.PROGRESS_MAX) {
            progressBar.setVisibility(View.VISIBLE);
            omniBox_tab.setVisibility(View.INVISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
            omniBox_tab.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void showFileChooser(ValueCallback<Uri[]> filePathCallback) {
        if(mFilePathCallback != null) {
            mFilePathCallback.onReceiveValue(null);
        }
        mFilePathCallback = filePathCallback;
        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("*/*");
        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        //noinspection deprecation
        startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);
    }

    @Override
    public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
        if (view == null) {
            return;
        }
        if (customView != null && callback != null) {
            callback.onCustomViewHidden();
            return;
        }

        customView = view;
        fullscreenHolder = new FrameLayout(context);
        fullscreenHolder.addView(
                customView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));

        FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
        decorView.addView(
                fullscreenHolder,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));

        customView.setKeepScreenOn(true);
        ((View) currentAlbumController).setVisibility(View.GONE);
        setCustomFullscreen(true);

        if (view instanceof FrameLayout) {
            if (((FrameLayout) view).getFocusedChild() instanceof VideoView) {
                videoView = (VideoView) ((FrameLayout) view).getFocusedChild();
                videoView.setOnErrorListener(new VideoCompletionListener());
                videoView.setOnCompletionListener(new VideoCompletionListener());
            }
        }
    }

    @Override
    public void onHideCustomView() {
        FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
        decorView.removeView(fullscreenHolder);

        customView.setKeepScreenOn(false);
        ((View) currentAlbumController).setVisibility(View.VISIBLE);
        setCustomFullscreen(false);

        fullscreenHolder = null;
        customView = null;
        if (videoView != null) {
            videoView.setOnErrorListener(null);
            videoView.setOnCompletionListener(null);
            videoView = null;
        }
        contentFrame.requestFocus();
    }

    private void showContextMenuLink (final String title, final String url, int type) {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.dialog_menu, null);

        TextView menuTitle = dialogView.findViewById(R.id.menuTitle);
        menuTitle.setText(url);
        ImageView menu_icon = dialogView.findViewById(R.id.menu_icon);

        if (type == SRC_ANCHOR_TYPE) {
            FaviconHelper faviconHelper = new FaviconHelper(context);
            Bitmap bitmap=faviconHelper.getFavicon(url);
            if (bitmap != null){
                menu_icon.setImageBitmap(bitmap);
            }else {
                menu_icon.setImageResource(R.drawable.icon_link);
            }
        } else if (type == IMAGE_TYPE) {
            menu_icon.setImageResource(R.drawable.icon_image);
        } else {
            menu_icon.setImageResource(R.drawable.icon_link);
        }

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();
        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);

        GridItem item_01 = new GridItem(0, getString(R.string.main_menu_new_tabOpen),  0);
        GridItem item_02 = new GridItem(0, getString(R.string.main_menu_new_tab),  0);
        GridItem item_03 = new GridItem(0, getString(R.string.menu_share_link),  0);
        GridItem item_04 = new GridItem(0, getString(R.string.menu_open_with),  0);
        GridItem item_05 = new GridItem(0, getString(R.string.menu_save_as),  0);
        GridItem item_06 = new GridItem(0, getString(R.string.menu_save_home),  0);

        final List<GridItem> gridList = new LinkedList<>();

        gridList.add(gridList.size(), item_01);
        gridList.add(gridList.size(), item_02);
        gridList.add(gridList.size(), item_03);
        gridList.add(gridList.size(), item_04);
        gridList.add(gridList.size(), item_05);
        gridList.add(gridList.size(), item_06);

        GridView menu_grid = dialogView.findViewById(R.id.menu_grid);
        GridAdapter gridAdapter = new GridAdapter(context, gridList);
        menu_grid.setAdapter(gridAdapter);
        gridAdapter.notifyDataSetChanged();
        menu_grid.setOnItemClickListener((parent, view, position, id) -> {
            dialog.cancel();
            switch (position) {
                case 0:
                    addAlbum(getString(R.string.app_name), url, true);
                    break;
                case 1:
                    addAlbum(getString(R.string.app_name), url, false);
                    break;
                case 2:
                    shareLink("", url);
                    break;
                case 3:
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    Intent chooser = Intent.createChooser(intent, getString(R.string.menu_open_with));
                    startActivity(chooser);
                    break;
                case 4:
                    if (url.startsWith("data:")) {
                        DataURIParser dataURIParser= new DataURIParser(url);
                        HelperUnit.saveDataURI(dialog, activity, dataURIParser);
                    } else HelperUnit.saveAs(dialog, activity, url);
                    break;
                case 5:
                    save_atHome(title, url);
                    break;
            }
        });
    }

    private void shareLink (String title, String url) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        sharingIntent.putExtra(Intent.EXTRA_TEXT, url);
        context.startActivity(Intent.createChooser(sharingIntent, (context.getString(R.string.menu_share_link))));
    }

    private void searchOnSite() {
        searchOnSite = true;
        omniBox.setVisibility(View.GONE);
        searchPanel.setVisibility(View.VISIBLE);
        HelperUnit.showSoftKeyboard(searchBox, activity);
    }

    private void saveBookmark() {

        FaviconHelper faviconHelper = new FaviconHelper(context);
        faviconHelper.addFavicon(ninjaWebView.getUrl(),ninjaWebView.getFavicon());
        RecordAction action = new RecordAction(context);
        action.open(true);
        if (action.checkUrl(ninjaWebView.getUrl(), RecordUnit.TABLE_BOOKMARK)) {
            NinjaToast.show(this, R.string.app_error);
        } else {

            long value= 11;  //default red icon
            action.addBookmark(new Record(ninjaWebView.getTitle(), ninjaWebView.getUrl(), 0,  0,2,ninjaWebView.isDesktopMode(), ninjaWebView.getSettings().getJavaScriptEnabled(),ninjaWebView.getSettings().getDomStorageEnabled(),value));

            NinjaToast.show(this, R.string.app_done);
        }
        action.close();
    }



    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
        super.onCreateContextMenu(menu, v, menuInfo);
        WebView.HitTestResult result = ninjaWebView.getHitTestResult();
        if (result.getExtra() != null) {
            if (result.getType() == SRC_ANCHOR_TYPE) {
                showContextMenuLink(HelperUnit.domain(result.getExtra()), result.getExtra(), SRC_ANCHOR_TYPE);
            } else if (result.getType() == SRC_IMAGE_ANCHOR_TYPE) {
                // Create a background thread that has a Looper
                HandlerThread handlerThread = new HandlerThread("HandlerThread");
                handlerThread.start();
                // Create a handler to execute tasks in the background thread.
                Handler backgroundHandler = new Handler(handlerThread.getLooper());
                Message msg = backgroundHandler.obtainMessage();
                ninjaWebView.requestFocusNodeHref(msg);
                String url = (String) msg.getData().get("url");
                showContextMenuLink(HelperUnit.domain(url), url, SRC_ANCHOR_TYPE);
            }  else if (result.getType() == IMAGE_TYPE) {
                showContextMenuLink(HelperUnit.domain(result.getExtra()), result.getExtra(), IMAGE_TYPE);
            } else {
                showContextMenuLink(HelperUnit.domain(result.getExtra()), result.getExtra(), 0);
            }
        }
    }

    private void doubleTapsQuit() {
        if (!sp.getBoolean("sp_close_browser_confirm", true)) {
            finish();
        } else {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setMessage(R.string.toast_quit);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
                FaviconHelper db=new FaviconHelper(context);
                db.cleanUpFaviconDB(context);
                finish();});
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showOverflow() {

        HelperUnit.hideSoftKeyboard(omniBox_text, context);

        final String url = ninjaWebView.getUrl();
        final String title = ninjaWebView.getTitle();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.dialog_menu_overflow, null);

        builder.setView(dialogView);
        AlertDialog dialog_overflow = builder.create();
        dialog_overflow.show();
        Objects.requireNonNull(dialog_overflow.getWindow()).setGravity(Gravity.BOTTOM);
        FaviconHelper.setFavicon(context, dialogView, url, R.id.menu_icon, R.drawable.icon_image_broken);

        TextView overflow_title = dialogView.findViewById(R.id.overflow_title);
        assert title != null;
        if (title.isEmpty()) {
            overflow_title.setText(url);
        } else {
            overflow_title.setText(title);
        }

        ImageButton overflow_settings = dialogView.findViewById(R.id.overflow_settings);
        overflow_settings.setOnClickListener(v -> {
            dialog_overflow.cancel();
            Intent settings = new Intent(BrowserActivity.this, Settings_Activity.class);
            startActivity(settings);
        });

        ImageButton overflow_reload = dialogView.findViewById(R.id.overflow_reload);
        overflow_reload.setOnClickListener(v -> {
            dialog_overflow.cancel();
            ninjaWebView.initPreferences(ninjaWebView.getUrl());
            ninjaWebView.reload();
        });

        final GridView menu_grid_tab = dialogView.findViewById(R.id.overflow_tab);
        final GridView menu_grid_share = dialogView.findViewById(R.id.overflow_share);
        final GridView menu_grid_save = dialogView.findViewById(R.id.overflow_save);
        final GridView menu_grid_other = dialogView.findViewById(R.id.overflow_other);

        menu_grid_tab.setVisibility(View.VISIBLE);
        menu_grid_share.setVisibility(View.GONE);
        menu_grid_save.setVisibility(View.GONE);
        menu_grid_other.setVisibility(View.GONE);

        // Tab

        GridItem item_01 = new GridItem(0, getString(R.string.main_menu_tabPreview), 0);
        GridItem item_02 = new GridItem(0, getString(R.string.main_menu_new_tabOpen),  0);
        GridItem item_03 = new GridItem(0, getString(R.string.menu_openFav),  0);
        GridItem item_04 = new GridItem(0, getString(R.string.menu_closeTab),  0);
        GridItem item_05 = new GridItem(0, getString(R.string.menu_quit),  0);

        final List<GridItem> gridList_tab = new LinkedList<>();

        gridList_tab.add(gridList_tab.size(), item_03);
        gridList_tab.add(gridList_tab.size(), item_01);
        gridList_tab.add(gridList_tab.size(), item_02);
        gridList_tab.add(gridList_tab.size(), item_04);
        gridList_tab.add(gridList_tab.size(), item_05);

        GridAdapter gridAdapter_tab = new GridAdapter(context, gridList_tab);
        menu_grid_tab.setAdapter(gridAdapter_tab);
        gridAdapter_tab.notifyDataSetChanged();

        menu_grid_tab.setOnItemClickListener((parent, view14, position, id) -> {
            dialog_overflow.cancel();
            if (position == 1) {
                showOverview();
            } else if (position == 2) {
                addAlbum(getString(R.string.app_name), Objects.requireNonNull(sp.getString("favoriteURL", "https://github.com/scoute-dich/browser")), true);
            } else if (position == 0) {
                ninjaWebView.loadUrl(Objects.requireNonNull(sp.getString("favoriteURL", "https://github.com/scoute-dich/browser")));
            } else if (position == 3) {
                removeAlbum(currentAlbumController);
            } else if (position == 4) {
                doubleTapsQuit();
            }
        });

        // Save
        GridItem item_21 = new GridItem(0, getString(R.string.menu_fav),  0);
        GridItem item_22 = new GridItem(0, getString(R.string.menu_save_home),  0);
        GridItem item_23 = new GridItem(0, getString(R.string.menu_save_bookmark),  0);
        GridItem item_24 = new GridItem(0, getString(R.string.menu_save_pdf),  0);
        GridItem item_25 = new GridItem(0, getString(R.string.menu_sc),  0);
        GridItem item_26 = new GridItem(0, getString(R.string.menu_save_as),  0);

        final List<GridItem> gridList_save = new LinkedList<>();
        gridList_save.add(gridList_save.size(), item_21);
        gridList_save.add(gridList_save.size(), item_22);
        gridList_save.add(gridList_save.size(), item_23);
        gridList_save.add(gridList_save.size(), item_24);
        gridList_save.add(gridList_save.size(), item_25);
        gridList_save.add(gridList_save.size(), item_26);

        GridAdapter gridAdapter_save = new GridAdapter(context, gridList_save);
        menu_grid_save.setAdapter(gridAdapter_save);
        gridAdapter_save.notifyDataSetChanged();

        menu_grid_save.setOnItemClickListener((parent, view13, position, id) -> {
            dialog_overflow.cancel();
            RecordAction action = new RecordAction(context);
            if (position == 0) {
                sp.edit().putString("favoriteURL", url).apply();
                NinjaToast.show(this, R.string.app_done);
            } else if (position == 1) {
                save_atHome(title, url);
            } else if (position == 2) {
                saveBookmark();
                action.close();
            } else if (position == 3) {
                printPDF();
            } else if (position == 4) {
                HelperUnit.createShortcut(context, ninjaWebView.getTitle(), ninjaWebView.getUrl());
            } else if (position == 5) {
                HelperUnit.saveAs(dialog_overflow, activity, url);
            }
        });

        // Share
        GridItem item_11 = new GridItem(0, getString(R.string.menu_share_link),  0);
        GridItem item_12 = new GridItem(0, getString(R.string.menu_shareClipboard),  0);
        GridItem item_13 = new GridItem(0, getString(R.string.menu_open_with),  0);

        final List<GridItem> gridList_share = new LinkedList<>();
        gridList_share.add(gridList_share.size(), item_11);
        gridList_share.add(gridList_share.size(), item_12);
        gridList_share.add(gridList_share.size(), item_13);

        GridAdapter gridAdapter_share = new GridAdapter(context, gridList_share);
        menu_grid_share.setAdapter(gridAdapter_share);
        gridAdapter_share.notifyDataSetChanged();

        menu_grid_share.setOnItemClickListener((parent, view12, position, id) -> {
            dialog_overflow.cancel();
            if (position == 0) {
                shareLink(title, url);
            } else if (position == 1) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("text", url);
                Objects.requireNonNull(clipboard).setPrimaryClip(clip);
                NinjaToast.show(this, R.string.toast_copy_successful);
            } else if (position == 2) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                Intent chooser = Intent.createChooser(intent, getString(R.string.menu_open_with));
                startActivity(chooser);
            }
        });

        // Other
        GridItem item_31 = new GridItem(0, getString(R.string.menu_other_searchSite),  0);
        GridItem item_32 = new GridItem(0, getString(R.string.menu_download),  0);
        GridItem item_33 = new GridItem(0, getString(R.string.setting_label),  0);
        GridItem item_34;
        if (ninjaWebView.isDesktopMode()) item_34 = new GridItem(0,getString((R.string.menu_mobileView)),0);
        else item_34 = new GridItem(0,getString((R.string.menu_desktopView)),0);

        GridItem item_35;
        if (sp.getBoolean("sp_invert", false)) item_35 = new GridItem(0,getString((R.string.menu_dayView)),0);
        else item_35 = new GridItem(0,getString((R.string.menu_nightView)),0);

        final List<GridItem> gridList_other = new LinkedList<>();
        gridList_other.add(gridList_other.size(), item_31);
        gridList_other.add(gridList_other.size(), item_34);
        gridList_other.add(gridList_other.size(), item_35);
        gridList_other.add(gridList_other.size(), item_32);
        gridList_other.add(gridList_other.size(), item_33);

        GridAdapter gridAdapter_other = new GridAdapter(context, gridList_other);
        menu_grid_other.setAdapter(gridAdapter_other);
        gridAdapter_other.notifyDataSetChanged();

        menu_grid_other.setOnItemClickListener((parent, view1, position, id) -> {
            dialog_overflow.cancel();
            if (position == 0) {
                searchOnSite();
            } else if (position == 1) {
                ninjaWebView.toggleDesktopMode(true);
            } else if (position == 2) {
                if (sp.getBoolean("sp_invert", false)) {
                    sp.edit().putBoolean("sp_invert", false).apply();
                } else {
                    sp.edit().putBoolean("sp_invert", true).apply();
                }
                HelperUnit.initRendering(ninjaWebView, context);
            } else if (position == 3) {
                startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
            } else if (position == 4) {
                Intent settings = new Intent(BrowserActivity.this, Settings_Activity.class);
                startActivity(settings);
            }
        });

        TabLayout tabLayout= dialogView.findViewById(R.id.tabLayout);

        TabLayout.Tab tab_tab = tabLayout.newTab().setIcon(R.drawable.icon_tab);
        TabLayout.Tab tab_share = tabLayout.newTab().setIcon(R.drawable.icon_menu_share);
        TabLayout.Tab tab_save = tabLayout.newTab().setIcon(R.drawable.icon_menu_save);
        TabLayout.Tab tab_other = tabLayout.newTab().setIcon(R.drawable.icon_dots);

        tabLayout.addTab(tab_tab);
        tabLayout.addTab(tab_share);
        tabLayout.addTab(tab_save);
        tabLayout.addTab(tab_other);

        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    menu_grid_tab.setVisibility(View.VISIBLE);
                    menu_grid_share.setVisibility(View.GONE);
                    menu_grid_save.setVisibility(View.GONE);
                    menu_grid_other.setVisibility(View.GONE);
                } else if (tab.getPosition() == 1) {
                    menu_grid_tab.setVisibility(View.GONE);
                    menu_grid_share.setVisibility(View.VISIBLE);
                    menu_grid_save.setVisibility(View.GONE);
                    menu_grid_other.setVisibility(View.GONE);
                } else if (tab.getPosition() == 2) {
                    menu_grid_tab.setVisibility(View.GONE);
                    menu_grid_share.setVisibility(View.GONE);
                    menu_grid_save.setVisibility(View.VISIBLE);
                    menu_grid_other.setVisibility(View.GONE);
                } else if (tab.getPosition() == 3) {
                    menu_grid_tab.setVisibility(View.GONE);
                    menu_grid_share.setVisibility(View.GONE);
                    menu_grid_save.setVisibility(View.GONE);
                    menu_grid_other.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void showContextMenuList (final String title, final String url,
                                      final RecordAdapter adapterRecord, final List<Record> recordList, final int location) {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.dialog_menu, null);

        TextView menuTitle = dialogView.findViewById(R.id.menuTitle);
        menuTitle.setText(title);
        FaviconHelper.setFavicon(context, dialogView, url, R.id.menu_icon, R.drawable.icon_image_broken);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();
        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);

        GridItem item_01 = new GridItem(0, getString(R.string.main_menu_new_tabOpen),  0);
        GridItem item_02 = new GridItem(0, getString(R.string.main_menu_new_tab),  0);
        GridItem item_03 = new GridItem(0, getString(R.string.menu_delete),  0);
        GridItem item_04 = new GridItem(0, getString(R.string.menu_edit),  0);
        GridItem item_05 = new GridItem(0,getString(R.string.menu_share_link),0);

        final List<GridItem> gridList = new LinkedList<>();

        if (overViewTab.equals(getString(R.string.album_title_bookmarks)) || overViewTab.equals(getString(R.string.album_title_home))) {
            gridList.add(gridList.size(), item_05);
            gridList.add(gridList.size(), item_01);
            gridList.add(gridList.size(), item_02);
            gridList.add(gridList.size(), item_03);
            gridList.add(gridList.size(), item_04);
        } else {
            gridList.add(gridList.size(), item_05);
            gridList.add(gridList.size(), item_01);
            gridList.add(gridList.size(), item_02);
            gridList.add(gridList.size(), item_03);
        }

        GridView menu_grid = dialogView.findViewById(R.id.menu_grid);
        GridAdapter gridAdapter = new GridAdapter(context, gridList);
        menu_grid.setAdapter(gridAdapter);
        gridAdapter.notifyDataSetChanged();
        menu_grid.setOnItemClickListener((parent, view, position, id) -> {
            dialog.cancel();
            MaterialAlertDialogBuilder builderSubMenu;
            AlertDialog dialogSubMenu;
            switch (position) {
                case 0:
                    shareLink("",url);
                    break;
                case 1:
                    addAlbum(getString(R.string.app_name), url, true);
                    hideOverview();
                    break;
                case 2:
                    addAlbum(getString(R.string.app_name), url, false);
                    break;
                case 3:
                    builderSubMenu = new MaterialAlertDialogBuilder(context);
                    builderSubMenu.setMessage(R.string.hint_database);
                    builderSubMenu.setPositiveButton(R.string.app_ok, (dialog2, whichButton) -> {
                        Record record = recordList.get(location);
                        RecordAction action = new RecordAction(context);
                        action.open(true);
                        if (overViewTab.equals(getString(R.string.album_title_home))) {
                            action.deleteURL(record.getURL(), RecordUnit.TABLE_GRID);
                        } else if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                            action.deleteURL(record.getURL(), RecordUnit.TABLE_BOOKMARK);
                        } else if (overViewTab.equals(getString(R.string.album_title_history))) {
                            action.deleteURL(record.getURL(), RecordUnit.TABLE_HISTORY);
                        }
                        action.close();
                        recordList.remove(location);
                        adapterRecord.notifyDataSetChanged();
                    });
                    builderSubMenu.setNegativeButton(R.string.app_cancel, (dialog2, whichButton) -> builderSubMenu.setCancelable(true));
                    dialogSubMenu = builderSubMenu.create();
                    dialogSubMenu.show();
                    Objects.requireNonNull(dialogSubMenu.getWindow()).setGravity(Gravity.BOTTOM);
                    break;
                case 4:
                    builderSubMenu = new MaterialAlertDialogBuilder(context);
                    View dialogViewSubMenu = View.inflate(context, R.layout.dialog_edit_title, null);

                    TextInputLayout edit_title_layout = dialogViewSubMenu.findViewById(R.id.edit_title_layout);
                    TextInputLayout edit_userName_layout = dialogViewSubMenu.findViewById(R.id.edit_userName_layout);
                    TextInputLayout edit_PW_layout = dialogViewSubMenu.findViewById(R.id.edit_PW_layout);
                    ImageView ib_icon = dialogViewSubMenu.findViewById(R.id.edit_icon);
                    if (!overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                        ib_icon.setVisibility(View.GONE);
                    }
                    Chip chip_desktopMode = dialogViewSubMenu.findViewById(R.id.edit_bookmark_desktopMode);
                    Chip chip_javascript = dialogViewSubMenu.findViewById(R.id.edit_bookmark_Javascript);
                    Chip chip_remoteContent = dialogViewSubMenu.findViewById(R.id.edit_bookmark_RemoteContent);
                    chip_desktopMode.setVisibility(View.VISIBLE);
                    chip_javascript.setVisibility(View.VISIBLE);
                    chip_remoteContent.setVisibility(View.VISIBLE);

                    edit_title_layout.setVisibility(View.VISIBLE);
                    edit_userName_layout.setVisibility(View.GONE);
                    edit_PW_layout.setVisibility(View.GONE);

                    EditText edit_title = dialogViewSubMenu.findViewById(R.id.edit_title);
                    edit_title.setText(title);

                    TextInputLayout edit_URL_layout=dialogViewSubMenu.findViewById(R.id.edit_URL_layout);
                    edit_URL_layout.setVisibility(View.VISIBLE);
                    EditText edit_URL = dialogViewSubMenu.findViewById(R.id.edit_URL);
                    edit_URL.setVisibility(View.VISIBLE);
                    edit_URL.setText(url);

                    ib_icon.setOnClickListener(v -> {
                        MaterialAlertDialogBuilder builderFilter = new MaterialAlertDialogBuilder(context);
                        View dialogViewFilter = View.inflate(context, R.layout.dialog_menu, null);
                        builderFilter.setView(dialogViewFilter);
                        AlertDialog dialogFilter = builderFilter.create();
                        dialogFilter.show();
                        Objects.requireNonNull(dialogFilter.getWindow()).setGravity(Gravity.BOTTOM);
                        GridView menu_grid2 = dialogViewFilter.findViewById(R.id.menu_grid);
                        final List<GridItem> gridList2 = new LinkedList<>();
                        HelperUnit.addFilterItems(activity, gridList2);
                        GridAdapter gridAdapter2 = new GridAdapter(context, gridList2);
                        menu_grid2.setAdapter(gridAdapter2);
                        gridAdapter2.notifyDataSetChanged();
                        menu_grid2.setOnItemClickListener((parent2, view2, position2, id2) -> {
                            newIcon = gridList2.get(position2).getData();
                            HelperUnit.setFilterIcons(ib_icon, newIcon);
                            dialogFilter.cancel();
                        });
                    });

                    chip_desktopMode.setChecked(recordList.get(location).getDesktopMode());
                    chip_javascript.setChecked(recordList.get(location).getJavascript());
                    chip_remoteContent.setChecked(recordList.get(location).getDomStorage());

                    newIcon=recordList.get(location).getIconColor();

                    HelperUnit.setFilterIcons(ib_icon, newIcon);

                    builderSubMenu.setView(dialogViewSubMenu);
                    builderSubMenu.setTitle(getString(R.string.menu_edit));
                    builderSubMenu.setPositiveButton(R.string.app_ok, (dialog3, whichButton) -> {
                        if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                            RecordAction action = new RecordAction(context);
                            action.open(true);
                            action.deleteURL(url, RecordUnit.TABLE_BOOKMARK);
                            action.addBookmark(new Record(edit_title.getText().toString(), edit_URL.getText().toString(), 0, 0, BOOKMARK_ITEM, chip_desktopMode.isChecked(),chip_javascript.isChecked(),chip_remoteContent.isChecked(),newIcon));
                            action.close();
                            bottom_navigation.setSelectedItemId(R.id.page_2);
                        } else {
                            RecordAction action = new RecordAction(context);
                            action.open(true);
                            action.deleteURL(url, RecordUnit.TABLE_GRID);
                            int counter = sp.getInt("counter", 0);
                            counter = counter + 1;
                            sp.edit().putInt("counter", counter).apply();
                            action.addStartSite(new Record(edit_title.getText().toString(), edit_URL.getText().toString(), 0, counter, STARTSITE_ITEM,chip_desktopMode.isChecked(),chip_javascript.isChecked(),chip_remoteContent.isChecked(),0));
                            action.close();
                            bottom_navigation.setSelectedItemId(R.id.page_1);
                        }
                    });
                    builderSubMenu.setNegativeButton(R.string.app_cancel, (dialog3, whichButton) -> builderSubMenu.setCancelable(true));
                    dialogSubMenu = builderSubMenu.create();
                    dialogSubMenu.show();
                    Objects.requireNonNull(dialogSubMenu.getWindow()).setGravity(Gravity.BOTTOM);
                    break;
            }
        });
    }

    private void save_atHome (final String title, final String url) {

        FaviconHelper faviconHelper = new FaviconHelper(context);
        faviconHelper.addFavicon(ninjaWebView.getUrl(),ninjaWebView.getFavicon());

        RecordAction action = new RecordAction(context);
        action.open(true);
        if (action.checkUrl(url, RecordUnit.TABLE_GRID)) {
            NinjaToast.show(this, R.string.app_error);
        } else {
            int counter = sp.getInt("counter", 0);
            counter = counter + 1;
            sp.edit().putInt("counter", counter).apply();
            if (action.addStartSite(new Record(title, url, 0, counter,1,ninjaWebView.isDesktopMode(),ninjaWebView.getSettings().getJavaScriptEnabled(),ninjaWebView.getSettings().getDomStorageEnabled(),0))) {
                NinjaToast.show(this, R.string.app_done);
            } else {
                NinjaToast.show(this, R.string.app_error);
            }
        }
        action.close();
    }

    private void show_dialogFilter() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.dialog_menu, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        CardView cardView = dialogView.findViewById(R.id.cardView);
        cardView.setVisibility(View.GONE);

        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        GridView menu_grid = dialogView.findViewById(R.id.menu_grid);
        final List<GridItem> gridList = new LinkedList<>();
        HelperUnit.addFilterItems(activity, gridList);
        GridAdapter gridAdapter = new GridAdapter(context, gridList);
        menu_grid.setAdapter(gridAdapter);
        gridAdapter.notifyDataSetChanged();
        menu_grid.setOnItemClickListener((parent, view, position, id) -> {
            filter = true;
            filterBy = gridList.get(position).getData();
            dialog.cancel();
            bottom_navigation.setSelectedItemId(R.id.page_2);
        });
    }

    private void setCustomFullscreen(boolean fullscreen) {
        if (fullscreen) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                final WindowInsetsController insetsController = getWindow().getInsetsController();
                if (insetsController != null) {
                    insetsController.hide(WindowInsets.Type.statusBars());
                    insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                getWindow().setFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
                );
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                final WindowInsetsController insetsController = getWindow().getInsetsController();
                if (insetsController != null) {
                    insetsController.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                getWindow().setFlags(
                        WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN
                );
            }
        }
    }

    private AlbumController nextAlbumController(boolean next) {
        if (BrowserContainer.size() <= 1) {
            return currentAlbumController;
        }
        List<AlbumController> list = BrowserContainer.list();
        int index = list.indexOf(currentAlbumController);
        if (next) {
            index++;
            if (index >= list.size()) {
                index = 0;
            }
        } else {
            index--;
            if (index < 0) {
                index = list.size() - 1;
            }
        }
        return list.get(index);
    }

    public void goBack_skipRedirects() {
        if (ninjaWebView.canGoBack()) {
            ninjaWebView.setIsBackPressed(true);
            ninjaWebView.goBack();
        }
    }
}