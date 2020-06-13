package de.baumann.browser.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import android.os.Message;
import android.os.StrictMode;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
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
import de.baumann.browser.browser.Javascript;
import de.baumann.browser.browser.Remote;
import de.baumann.browser.database.BookmarkList;
import de.baumann.browser.database.Record;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.R;
import de.baumann.browser.service.ClearService;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.unit.RecordUnit;
import de.baumann.browser.unit.ViewUnit;
import de.baumann.browser.view.CompleteAdapter;
import de.baumann.browser.view.GridAdapter;

import de.baumann.browser.view.GridItem;
import de.baumann.browser.view.NinjaToast;
import de.baumann.browser.view.NinjaWebView;
import de.baumann.browser.view.RecordAdapter;
import de.baumann.browser.view.SwipeTouchListener;

import static android.content.ContentValues.TAG;

@SuppressWarnings({"ApplySharedPref"})
public class BrowserActivity extends AppCompatActivity implements BrowserController {

    // Menus

    private RecordAdapter adapter;

    // Views

    private ImageButton omniboxRefresh;
    private ImageButton open_startPage;
    private ImageButton open_bookmark;
    private ImageButton open_history;
    private ImageButton open_menu;
    private ImageButton omniboxOverview;
    private FloatingActionButton fab_imageButtonNav;
    private AutoCompleteTextView inputBox;
    private ProgressBar progressBar;
    private EditText searchBox;
    private BottomSheetDialog bottomSheetDialog_OverView;
    private NinjaWebView ninjaWebView;
    private ListView listView;
    private TextView omniboxTitle;
    private View customView;
    private VideoView videoView;
    private ScrollView tab_ScrollView;

    // Layouts

    private RelativeLayout appBar;
    private RelativeLayout omnibox;
    private RelativeLayout searchPanel;
    private FrameLayout contentFrame;
    private LinearLayout tab_container;
    private FrameLayout fullscreenHolder;

    private View open_startPageView;
    private View open_bookmarkView;
    private View open_historyView;
    private View open_tabView;

    // Others

    private String overViewTab;
    private BroadcastReceiver downloadReceiver;
    private BottomSheetBehavior mBehavior;

    private Activity activity;
    private Context context;
    private SharedPreferences sp;
    private Javascript javaHosts;
    private Cookie cookieHosts;
    private AdBlock adBlock;
    private Remote remote;

    private long newIcon;
    private boolean filter;
    private long filterBy;
    private boolean showOverflow = false;
    private TextView overflowTitle;

    private boolean prepareRecord() {
        NinjaWebView webView = (NinjaWebView) currentAlbumController;
        String title = webView.getTitle();
        String url = webView.getUrl();
        return (title == null
                || title.isEmpty()
                || url == null
                || url.isEmpty()
                || url.startsWith(BrowserUnit.URL_SCHEME_ABOUT)
                || url.startsWith(BrowserUnit.URL_SCHEME_MAIL_TO)
                || url.startsWith(BrowserUnit.URL_SCHEME_INTENT));
    }

    private int originalOrientation;
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

    // Overrides

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        WebView.enableSlowWholeDocumentDraw();
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        context = BrowserActivity.this;
        activity = BrowserActivity.this;

        sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt("restart_changed", 0).apply();
        sp.edit().putBoolean("pdf_create", false).commit();

        HelperUnit.applyTheme(context);
        setContentView(R.layout.activity_main);

        if (Objects.requireNonNull(sp.getString("saved_key_ok", "no")).equals("no")) {
            if (Locale.getDefault().getCountry().equals("CN")) {
                sp.edit().putString(getString(R.string.sp_search_engine), "2").apply();
            }
            sp.edit().putString("saved_key_ok", "yes").apply();

            sp.edit().putString("setting_gesture_tb_up", "08").apply();
            sp.edit().putString("setting_gesture_tb_down", "01").apply();
            sp.edit().putString("setting_gesture_tb_left", "07").apply();
            sp.edit().putString("setting_gesture_tb_right", "06").apply();

            sp.edit().putString("setting_gesture_nav_up", "04").apply();
            sp.edit().putString("setting_gesture_nav_down", "05").apply();
            sp.edit().putString("setting_gesture_nav_left", "03").apply();
            sp.edit().putString("setting_gesture_nav_right", "02").apply();

            sp.edit().putBoolean(getString(R.string.sp_location), false).apply();
        }

        contentFrame = findViewById(R.id.main_content);
        appBar = findViewById(R.id.appBar);

        initOmnibox();
        initSearchPanel();
        initOverview();

        new AdBlock(context); // For AdBlock cold boot
        new Javascript(context);
        new Cookie(context);
        new Remote(context);

        downloadReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
                View dialogView = View.inflate(context, R.layout.dialog_action, null);
                TextView textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.toast_downloadComplete);
                Button action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
                        bottomSheetDialog.cancel();
                    }
                });
                bottomSheetDialog.setContentView(dialogView);
                bottomSheetDialog.show();
                HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
            }
        };

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadReceiver, filter);
        dispatchIntent(getIntent());

        if (sp.getBoolean("start_tabStart", false)){
            showOverview();
        }
        HelperUnit.initRendering(ninjaWebView);
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

        if (sp.getInt("restart_changed", 1) == 1) {
            sp.edit().putInt("restart_changed", 0).apply();
            final BottomSheetDialog dialog = new BottomSheetDialog(context);
            View dialogView = View.inflate(context, R.layout.dialog_action, null);
            TextView textView = dialogView.findViewById(R.id.dialog_text);
            textView.setText(R.string.toast_restart);
            Button action_ok = dialogView.findViewById(R.id.action_ok);
            action_ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });
            dialog.setContentView(dialogView);
            dialog.show();
            HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
        }

        dispatchIntent(getIntent());

        if (sp.getBoolean("pdf_create", false)) {
            sp.edit().putBoolean("pdf_create", false).commit();

            final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
            View dialogView = View.inflate(context, R.layout.dialog_action, null);
            TextView textView = dialogView.findViewById(R.id.dialog_text);
            textView.setText(R.string.toast_downloadComplete);

            Button action_ok = dialogView.findViewById(R.id.action_ok);
            action_ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
                    bottomSheetDialog.cancel();
                }
            });
            bottomSheetDialog.setContentView(dialogView);
            bottomSheetDialog.show();
            HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    @Override
    public void onDestroy() {
        if (sp.getBoolean(getString(R.string.sp_clear_quit), false)) {
            Intent toClearService = new Intent(this, ClearService.class);
            startService(toClearService);
        }
        BrowserContainer.clear();
        unregisterReceiver(downloadReceiver);
        finish();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                showOverflow();
            case KeyEvent.KEYCODE_BACK:
                hideKeyboard(activity);
                hideOverview();
                if (fullscreenHolder != null || customView != null || videoView != null) {
                    Log.v(TAG, "FOSS Browser in fullscreen mode");
                } else if (omnibox.getVisibility() == View.GONE && sp.getBoolean("sp_toolbarShow", true)) {
                    showOmnibox();
                } else {
                    if (ninjaWebView.canGoBack()) {
                        ninjaWebView.goBack();
                    } else {
                        removeAlbum(currentAlbumController);
                    }
                }
                return true;
        }
        return false;
    }

    @Override
    public synchronized void showAlbum(AlbumController controller) {
        if (currentAlbumController != null) {
            currentAlbumController.deactivate();
            View av = (View) controller;
            contentFrame.removeAllViews();
            contentFrame.addView(av);
        } else {
            contentFrame.removeAllViews();
            contentFrame.addView((View) controller);
        }
        currentAlbumController = controller;
        currentAlbumController.activate();
        updateOmnibox();
    }

    @Override
    public void updateAutoComplete() {
        RecordAction action = new RecordAction(this);
        action.open(false);
        List<Record> list = action.listEntries(activity);
        action.close();
        CompleteAdapter adapter = new CompleteAdapter(this, R.layout.list_item, list);
        inputBox.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        inputBox.setThreshold(1);
        inputBox.setDropDownVerticalOffset(-16);
        inputBox.setDropDownWidth(ViewUnit.getWindowWidth(this));
        inputBox.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String url = ((TextView) view.findViewById(R.id.record_item_time)).getText().toString();
                updateAlbum(url);
                hideKeyboard(activity);
            }
        });
    }

    private void showOverview() {
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetDialog_OverView.show();
    }

    public void hideOverview () {
        if (bottomSheetDialog_OverView != null) {
            bottomSheetDialog_OverView.cancel();
        }
    }

    private void printPDF () {
        String title = HelperUnit.fileName(ninjaWebView.getUrl());
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        PrintDocumentAdapter printAdapter = ninjaWebView.createPrintDocumentAdapter(title);
        Objects.requireNonNull(printManager).print(title, printAdapter, new PrintAttributes.Builder().build());
        sp.edit().putBoolean("pdf_create", true).commit();
    }

    private void dispatchIntent(Intent intent) {

        String action = intent.getAction();
        String url = intent.getStringExtra(Intent.EXTRA_TEXT);

        if ("".equals(action)) {
            Log.i(TAG, "resumed FOSS browser");
        } else if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_WEB_SEARCH)) {
            addAlbum(null, intent.getStringExtra(SearchManager.QUERY), true);
        } else if (filePathCallback != null) {
            filePathCallback = null;
        } else if ("sc_history".equals(action)) {
            addAlbum(getString(R.string.app_name), sp.getString("favoriteURL", "https://github.com/scoute-dich/browser"), true);
            showOverview();
            open_history.performClick();
        } else if ("sc_bookmark".equals(action)) {
            addAlbum(getString(R.string.app_name), sp.getString("favoriteURL", "https://github.com/scoute-dich/browser"), true);
            showOverview();
            open_bookmark.performClick();
        } else if ("sc_startPage".equals(action)) {
            addAlbum(getString(R.string.app_name), sp.getString("favoriteURL", "https://github.com/scoute-dich/browser"), true);
            showOverview();
            open_startPage.performClick();
        } else if (Intent.ACTION_SEND.equals(action)) {
            addAlbum(getString(R.string.app_name), url, true);
        } else if (Intent.ACTION_VIEW.equals(action)) {
            String data = Objects.requireNonNull(getIntent().getData()).toString();
            addAlbum(getString(R.string.app_name), data, true);
        } else {
            addAlbum(getString(R.string.app_name), sp.getString("favoriteURL", "https://github.com/scoute-dich/browser"), true);
        }
        getIntent().setAction("");
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initOmnibox() {

        omnibox = findViewById(R.id.main_omnibox);
        inputBox = findViewById(R.id.main_omnibox_input);
        omniboxOverview = findViewById(R.id.omnibox_overview);
        ImageButton omniboxOverflow = findViewById(R.id.omnibox_overflow);
        omniboxTitle = findViewById(R.id.omnibox_title);
        progressBar = findViewById(R.id.main_progress_bar);

        String nav_position = Objects.requireNonNull(sp.getString("nav_position", "0"));

        switch (nav_position) {
            case "1":
                fab_imageButtonNav = findViewById(R.id.fab_imageButtonNav_left);
                break;
            case "2":
                fab_imageButtonNav = findViewById(R.id.fab_imageButtonNav_center);
                break;
            case "3":
                fab_imageButtonNav = findViewById(R.id.fab_imageButtonNav_null);
                break;
            default:
                fab_imageButtonNav = findViewById(R.id.fab_imageButtonNav_right);
                break;
        }

        fab_imageButtonNav.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                show_dialogFastToggle();
                return false;
            }
        });

        omniboxOverflow.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                show_dialogFastToggle();
                return false;
            }
        });

        fab_imageButtonNav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOverflow();
            }
        });

        omniboxOverflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOverflow();
            }
        });

        if (sp.getBoolean("sp_gestures_use", true)) {
            fab_imageButtonNav.setOnTouchListener(new SwipeTouchListener(context) {
                public void onSwipeTop() { performGesture("setting_gesture_nav_up"); }
                public void onSwipeBottom() { performGesture("setting_gesture_nav_down"); }
                public void onSwipeRight() { performGesture("setting_gesture_nav_right"); }
                public void onSwipeLeft() { performGesture("setting_gesture_nav_left"); }
            });

            omniboxOverflow.setOnTouchListener(new SwipeTouchListener(context) {
                public void onSwipeTop() { performGesture("setting_gesture_nav_up"); }
                public void onSwipeBottom() { performGesture("setting_gesture_nav_down"); }
                public void onSwipeRight() { performGesture("setting_gesture_nav_right"); }
                public void onSwipeLeft() { performGesture("setting_gesture_nav_left"); }
            });

            inputBox.setOnTouchListener(new SwipeTouchListener(context) {
                public void onSwipeTop() { performGesture("setting_gesture_tb_up"); }
                public void onSwipeBottom() { performGesture("setting_gesture_tb_down"); }
                public void onSwipeRight() { performGesture("setting_gesture_tb_right"); }
                public void onSwipeLeft() { performGesture("setting_gesture_tb_left"); }
            });
        }

        inputBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String query = inputBox.getText().toString().trim();
                if (query.isEmpty()) {
                    NinjaToast.show(context, getString(R.string.toast_input_empty));
                    return true;
                }
                updateAlbum(query);
                hideKeyboard(activity);
                return false;
            }
        });

        inputBox.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (inputBox.hasFocus()) {
                    ninjaWebView.stopLoading();
                    inputBox.setText(ninjaWebView.getUrl());
                    omniboxTitle.setVisibility(View.GONE);
                    inputBox.requestFocus();
                    inputBox.setSelection(0,inputBox.getText().toString().length());
                } else {
                    omniboxTitle.setVisibility(View.VISIBLE);
                    omniboxTitle.setText(ninjaWebView.getTitle());
                    hideKeyboard(activity);
                }
            }
        });
        updateAutoComplete();
        omniboxOverview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOverview();
            }
        });
    }

    private void performGesture (String gesture) {
        String gestureAction = Objects.requireNonNull(sp.getString(gesture, "0"));
        AlbumController controller;
        ninjaWebView = (NinjaWebView) currentAlbumController;

        switch (gestureAction) {
            case "01":
                break;
            case "02":
                if (ninjaWebView.canGoForward()) {
                    ninjaWebView.goForward();
                } else {
                    NinjaToast.show(context,R.string.toast_webview_forward);
                }
                break;
            case "03":
                if (ninjaWebView.canGoBack()) {
                    ninjaWebView.goBack();
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
                controller = nextAlbumController(false);
                showAlbum(controller);
                break;
            case "07":
                controller = nextAlbumController(true);
                showAlbum(controller);
                break;
            case "08":
                showOverview();
                break;
            case "09":
                addAlbum(getString(R.string.app_name), sp.getString("favoriteURL", "https://github.com/scoute-dich/browser"), true);
                break;
            case "10":
                removeAlbum(currentAlbumController);
                break;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initOverview() {

        bottomSheetDialog_OverView = new BottomSheetDialog(context);
        View dialogView = View.inflate(context, R.layout.dialog_overview, null);
        final TextView overview_title = dialogView.findViewById(R.id.overview_title);

        open_startPage = dialogView.findViewById(R.id.open_startSite);
        open_bookmark = dialogView.findViewById(R.id.open_bookmark_2);
        open_history = dialogView.findViewById(R.id.open_history_2);
        open_menu = dialogView.findViewById(R.id.open_menu);
        tab_container = dialogView.findViewById(R.id.tab_container);
        ImageButton open_tab = dialogView.findViewById(R.id.open_tab);
        tab_ScrollView = dialogView.findViewById(R.id.listTabs);
        listView = dialogView.findViewById(R.id.listRecord);

        open_startPageView = dialogView.findViewById(R.id.open_startSiteView);
        open_bookmarkView = dialogView.findViewById(R.id.open_bookmarkView);
        open_historyView = dialogView.findViewById(R.id.open_historyView);
        open_tabView = dialogView.findViewById(R.id.open_tabView);

        // allow scrolling in listView without closing the bottomSheetDialog
        listView.setOnTouchListener(new ListView.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {// Disallow NestedScrollView to intercept touch events.
                    if (listView.canScrollVertically(-1)) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
                // Handle ListView touch events.
                v.onTouchEvent(event);
                return true;
            }
        });

        open_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final BottomSheetDialog menuList = new BottomSheetDialog(context);
                View view = View.inflate(context, R.layout.dialog_menu, null);
                GridView gridView = view.findViewById(R.id.menu_grid);
                TextView gridTitle = view.findViewById(R.id.overview_title);
                gridTitle.setText(overViewTab);

                List<GridItem> list = new LinkedList<>();
                GridItem item_01 = new GridItem(R.drawable.icon_delete, getResources().getString(R.string.menu_delete), null, 0);
                GridItem item_02 = new GridItem(R.drawable.icon_sort_title, getResources().getString(R.string.menu_sort), null, 0);
                GridItem item_03 = new GridItem(R.drawable.filter_variant, getResources().getString(R.string.menu_filter), null, 0);

                list.add(list.size(), item_01);

                if (overViewTab.equals(getString(R.string.album_title_home)) || overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                    list.add(list.size(), item_02);
                }

                if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                    list.add(list.size(), item_03);
                }

                GridAdapter gridAdapter = new GridAdapter(context, list);
                gridView.setNumColumns(1);
                gridView.setAdapter(gridAdapter);
                gridAdapter.notifyDataSetChanged();
                gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                        if (position == 0) {
                            menuList.cancel ();
                            final BottomSheetDialog dialogDelete = new BottomSheetDialog(context);
                            View dialogDeleteView = View.inflate(context, R.layout.dialog_action, null);
                            TextView textView = dialogDeleteView.findViewById(R.id.dialog_text);
                            textView.setText(R.string.hint_database);
                            Button action_ok = dialogDeleteView.findViewById(R.id.action_ok);
                            action_ok.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (overViewTab.equals(getString(R.string.album_title_home))) {
                                        BrowserUnit.clearHome(context);
                                        open_startPage.performClick();
                                    } else if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                                        BrowserUnit.clearBookmark(context);
                                        open_bookmark.performClick();
                                    } else if (overViewTab.equals(getString(R.string.album_title_history))) {
                                        BrowserUnit.clearHistory(context);
                                        open_history.performClick();
                                    }
                                    dialogDelete.cancel();
                                }
                            });
                            dialogDelete.setContentView(dialogDeleteView);
                            dialogDelete.show();
                            HelperUnit.setBottomSheetBehavior(dialogDelete, dialogDeleteView, BottomSheetBehavior.STATE_EXPANDED);

                        } else if (position == 1) {
                            menuList.cancel();

                            final BottomSheetDialog menuSort = new BottomSheetDialog(context);
                            View menuSortView = View.inflate(context, R.layout.dialog_menu, null);
                            GridView gridView = menuSortView.findViewById(R.id.menu_grid);
                            TextView gridTitle = menuSortView.findViewById(R.id.overview_title);
                            gridTitle.setText(overViewTab);

                            List<GridItem> list = new LinkedList<>();
                            GridItem item_01 = new GridItem(R.drawable.icon_sort_title, getResources().getString(R.string.dialog_sortName), null, 0);
                            GridItem item_02 = new GridItem(R.drawable.icon_sort_icon, getResources().getString(R.string.dialog_sortIcon), null, 0);
                            GridItem item_03 = new GridItem(R.drawable.icon_sort_tme, getResources().getString(R.string.dialog_sortDate), null, 0);


                            if (overViewTab.equals(getString(R.string.album_title_home))) {
                                list.add(list.size(), item_01);
                                list.add(list.size(), item_03);
                            }
                            if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                                list.add(list.size(), item_01);
                                list.add(list.size(), item_02);
                            }
                            GridAdapter gridAdapter = new GridAdapter(context, list);
                            gridView.setNumColumns(1);
                            gridView.setAdapter(gridAdapter);
                            gridAdapter.notifyDataSetChanged();
                            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                                    if (position == 0) {
                                        if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                                            sp.edit().putString("sort_bookmark", "title").apply();
                                            menuSort.cancel();
                                            open_bookmark.performClick();
                                        } else if (overViewTab.equals(getString(R.string.album_title_home))){
                                            sp.edit().putString("sort_startSite", "title").apply();
                                            menuSort.cancel();
                                            open_startPage.performClick();
                                        }

                                    } else if (position == 1) {
                                        if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                                            sp.edit().putString("sort_bookmark", "time").apply();
                                            menuSort.cancel();
                                            open_bookmark.performClick();
                                        } else if (overViewTab.equals(getString(R.string.album_title_home))){
                                            sp.edit().putString("sort_startSite", "ordinal").apply();
                                            menuSort.cancel();
                                            open_startPage.performClick();
                                        }
                                    }
                                }
                            });
                            menuSort.setContentView(menuSortView);
                            menuSort.show();
                            HelperUnit.setBottomSheetBehavior(menuSort, menuSortView, BottomSheetBehavior.STATE_EXPANDED);
                        } else if (position == 2) {
                            menuList.cancel();
                            show_dialogFilter();
                        }
                    }
                });
                menuList.setContentView(view);
                menuList.show();
                HelperUnit.setBottomSheetBehavior(menuList, view, BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        bottomSheetDialog_OverView.setContentView(dialogView);

        mBehavior = BottomSheetBehavior.from((View) dialogView.getParent());
        mBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN || newState == BottomSheetBehavior.STATE_COLLAPSED){
                    hideOverview();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });

        open_tab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                omniboxOverview.setImageResource(R.drawable.icon_preview);
                mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                open_startPageView.setVisibility(View.INVISIBLE);
                open_bookmarkView.setVisibility(View.INVISIBLE);
                open_historyView.setVisibility(View.INVISIBLE);
                open_tabView.setVisibility(View.VISIBLE);
                overViewTab = getString(R.string.album_title_tab);
                overview_title.setText(overViewTab);
                open_menu.setVisibility(View.GONE);
                listView.setVisibility(View.GONE);
                tab_ScrollView.setVisibility(View.VISIBLE);
            }
        });

        open_startPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                omniboxOverview.setImageResource(R.drawable.icon_earth);
                mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                open_startPageView.setVisibility(View.VISIBLE);
                open_bookmarkView.setVisibility(View.INVISIBLE);
                open_historyView.setVisibility(View.INVISIBLE);
                open_tabView.setVisibility(View.INVISIBLE);
                overViewTab = getString(R.string.album_title_home);
                overview_title.setText(overViewTab);
                open_menu.setVisibility(View.VISIBLE);
                listView.setVisibility(View.VISIBLE);
                tab_ScrollView.setVisibility(View.GONE);

                RecordAction action = new RecordAction(context);
                action.open(false);
                final List<Record> list = action.listStartSite(activity);
                action.close();

                adapter = new RecordAdapter(context, list);
                listView.setAdapter(adapter);
                adapter.notifyDataSetChanged();

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        updateAlbum(list.get(position).getURL());
                        hideOverview();
                    }
                });

                listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        show_contextMenu_list(list.get(position).getTitle(), list.get(position).getURL(), adapter, list, position,0);
                        return true;
                    }
                });
            }
        });

        open_bookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                omniboxOverview.setImageResource(R.drawable.icon_bookmark);
                mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                open_startPageView.setVisibility(View.INVISIBLE);
                open_bookmarkView.setVisibility(View.VISIBLE);
                open_historyView.setVisibility(View.INVISIBLE);
                open_tabView.setVisibility(View.INVISIBLE);
                overViewTab = getString(R.string.album_title_bookmarks);
                overview_title.setText(overViewTab);
                open_menu.setVisibility(View.VISIBLE);
                listView.setVisibility(View.VISIBLE);
                tab_ScrollView.setVisibility(View.GONE);

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
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        updateAlbum(list.get(position).getURL());
                        hideOverview();
                    }
                });

                listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        show_contextMenu_list(list.get(position).getTitle(), list.get(position).getURL(), adapter, list, position, list.get(position).getTime());
                        return true;
                    }
                });
                initBookmarkList();
            }
        });

        open_bookmark.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                show_dialogFilter();
                return false;
            }
        });

        open_history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                omniboxOverview.setImageResource(R.drawable.icon_history);
                mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                open_startPageView.setVisibility(View.INVISIBLE);
                open_bookmarkView.setVisibility(View.INVISIBLE);
                open_historyView.setVisibility(View.VISIBLE);
                open_tabView.setVisibility(View.INVISIBLE);
                overViewTab = getString(R.string.album_title_history);
                overview_title.setText(overViewTab);
                open_menu.setVisibility(View.VISIBLE);
                listView.setVisibility(View.VISIBLE);
                tab_ScrollView.setVisibility(View.GONE);

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
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        ninjaWebView.loadUrl(list.get(position).getURL());
                        hideOverview();
                    }
                });

                listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        show_contextMenu_list(list.get(position).getTitle(), list.get(position).getURL(), adapter, list, position,0);
                        return true;
                    }
                });
            }
        });

        switch (Objects.requireNonNull(sp.getString("start_tab", "0"))) {
            case "3":
                open_bookmark.performClick();
                break;
            case "4":
                open_history.performClick();
                break;
            case "5":
                open_tab.performClick();
                break;
            default:
                open_startPage.performClick();
                break;
        }
    }

    private void initSearchPanel() {
        searchPanel = findViewById(R.id.main_search_panel);
        searchBox = findViewById(R.id.main_search_box);
        ImageView searchUp = findViewById(R.id.main_search_up);
        ImageView searchDown = findViewById(R.id.main_search_down);
        ImageView searchCancel = findViewById(R.id.main_search_cancel);
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
        searchUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard(activity);
                ((NinjaWebView) currentAlbumController).findNext(false);
            }
        });
        searchDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard(activity);
                ((NinjaWebView) currentAlbumController).findNext(true);
            }
        });
        searchCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard(activity);
                searchOnSite = false;
                searchBox.setText("");
                showOmnibox();
            }
        });
    }

    private void initBookmarkList() {
        BookmarkList db = new BookmarkList(context);
        db.open();
        Cursor cursor = db.fetchAllData(activity);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            RecordAction action = new RecordAction(context);
            action.open(true);
            action.addBookmark(new Record(
                    cursor.getString(cursor.getColumnIndexOrThrow("pass_title")),
                    cursor.getString(cursor.getColumnIndexOrThrow("pass_content")),
                    1, 0));
            cursor.moveToNext();
            action.close();
            deleteDatabase("pass_DB_v01.db");
        }
    }

    private void show_dialogFastToggle() {

        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View dialogView = View.inflate(context, R.layout.dialog_toggle, null);

        CheckBox sw_java = dialogView.findViewById(R.id.switch_js);
        final ImageButton whiteList_js = dialogView.findViewById(R.id.imageButton_js);
        CheckBox sw_adBlock = dialogView.findViewById(R.id.switch_adBlock);
        final ImageButton whiteList_ab = dialogView.findViewById(R.id.imageButton_ab);
        CheckBox sw_cookie = dialogView.findViewById(R.id.switch_cookie);
        final ImageButton whitelist_cookie = dialogView.findViewById(R.id.imageButton_cookie);
        CheckBox switch_ext = dialogView.findViewById(R.id.switch_ext);
        final ImageButton imageButton_ext = dialogView.findViewById(R.id.imageButton_ext);

        TextView dialog_title = dialogView.findViewById(R.id.dialog_title);
        dialog_title.setText(HelperUnit.domain(ninjaWebView.getUrl()));

        javaHosts = new Javascript(context);
        cookieHosts = new Cookie(context);
        adBlock = new AdBlock(context);
        remote = new Remote(context);
        ninjaWebView = (NinjaWebView) currentAlbumController;

        final String url = ninjaWebView.getUrl();

        if (sp.getBoolean(getString(R.string.sp_javascript), true)){
            sw_java.setChecked(true);
        } else {
            sw_java.setChecked(false);
        }

        if (sp.getBoolean(getString(R.string.sp_ad_block), true)){
            sw_adBlock.setChecked(true);
        } else {
            sw_adBlock.setChecked(false);
        }

        if (sp.getBoolean(getString(R.string.sp_cookies), true)){
            sw_cookie.setChecked(true);
        } else {
            sw_cookie.setChecked(false);
        }

        if (sp.getBoolean("sp_remote", true)){
            switch_ext.setChecked(true);
        } else {
            switch_ext.setChecked(false);
        }

        if (javaHosts.isWhite(url)) {
            whiteList_js.setImageResource(R.drawable.check_green);
        } else {
            whiteList_js.setImageResource(R.drawable.ic_action_close_red);
        }

        if (cookieHosts.isWhite(url)) {
            whitelist_cookie.setImageResource(R.drawable.check_green);
        } else {
            whitelist_cookie.setImageResource(R.drawable.ic_action_close_red);
        }

        if (adBlock.isWhite(url)) {
            whiteList_ab.setImageResource(R.drawable.check_green);
        } else {
            whiteList_ab.setImageResource(R.drawable.ic_action_close_red);
        }

        if (remote.isWhite(url)) {
            imageButton_ext.setImageResource(R.drawable.check_green);
        } else {
            whiteList_ab.setImageResource(R.drawable.ic_action_close_red);
        }

        imageButton_ext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (remote.isWhite(ninjaWebView.getUrl())) {
                    imageButton_ext.setImageResource(R.drawable.ic_action_close_red);
                    remote.removeDomain(HelperUnit.domain(url));
                } else {
                    imageButton_ext.setImageResource(R.drawable.check_green);
                    remote.addDomain(HelperUnit.domain(url));
                }
            }
        });

        whiteList_js.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (javaHosts.isWhite(ninjaWebView.getUrl())) {
                    whiteList_js.setImageResource(R.drawable.ic_action_close_red);
                    javaHosts.removeDomain(HelperUnit.domain(url));
                } else {
                    whiteList_js.setImageResource(R.drawable.check_green);
                    javaHosts.addDomain(HelperUnit.domain(url));
                }
            }
        });

        whitelist_cookie.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cookieHosts.isWhite(ninjaWebView.getUrl())) {
                    whitelist_cookie.setImageResource(R.drawable.ic_action_close_red);
                    cookieHosts.removeDomain(HelperUnit.domain(url));
                } else {
                    whitelist_cookie.setImageResource(R.drawable.check_green);
                    cookieHosts.addDomain(HelperUnit.domain(url));
                }
            }
        });


        whiteList_ab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (adBlock.isWhite(ninjaWebView.getUrl())) {
                    whiteList_ab.setImageResource(R.drawable.ic_action_close_red);
                    adBlock.removeDomain(HelperUnit.domain(url));
                } else {
                    whiteList_ab.setImageResource(R.drawable.check_green);
                    adBlock.addDomain(HelperUnit.domain(url));
                }
            }
        });

        sw_java.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    sp.edit().putBoolean(getString(R.string.sp_javascript), true).commit();
                }else{
                    sp.edit().putBoolean(getString(R.string.sp_javascript), false).commit();
                }

            }
        });

        sw_adBlock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    sp.edit().putBoolean(getString(R.string.sp_ad_block), true).commit();
                }else{
                    sp.edit().putBoolean(getString(R.string.sp_ad_block), false).commit();
                }
            }
        });

        sw_cookie.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if(isChecked){
                    sp.edit().putBoolean(getString(R.string.sp_cookies), true).commit();
                }else{
                    sp.edit().putBoolean(getString(R.string.sp_cookies), false).commit();
                }
            }
        });

        switch_ext.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if(isChecked){
                    sp.edit().putBoolean("sp_remote", true).commit();
                }else{
                    sp.edit().putBoolean("sp_remote", false).commit();
                }
            }
        });

        final ImageButton toggle_history = dialogView.findViewById(R.id.toggle_history);
        final View toggle_historyView = dialogView.findViewById(R.id.toggle_historyView);

        final ImageButton toggle_location = dialogView.findViewById(R.id.toggle_location);
        final View toggle_locationView = dialogView.findViewById(R.id.toggle_locationView);

        final ImageButton toggle_images = dialogView.findViewById(R.id.toggle_images);
        final View toggle_imagesView = dialogView.findViewById(R.id.toggle_imagesView);

        final ImageButton toggle_invert = dialogView.findViewById(R.id.toggle_invert);
        final View toggle_invertView = dialogView.findViewById(R.id.toggle_invertView);

        final ImageButton toggle_font = dialogView.findViewById(R.id.toggle_font);

        toggle_font.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.cancel();
                Intent intent = new Intent(context, Settings_Activity.class);
                startActivity(intent);
            }
        });

        if (sp.getBoolean("saveHistory", true)) {
            toggle_historyView.setVisibility(View.VISIBLE);
        } else {
            toggle_historyView.setVisibility(View.INVISIBLE);
        }

        toggle_history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sp.getBoolean("saveHistory", true)) {
                    toggle_historyView.setVisibility(View.INVISIBLE);
                    sp.edit().putBoolean("saveHistory", false).commit();
                } else {
                    toggle_historyView.setVisibility(View.VISIBLE);
                    sp.edit().putBoolean("saveHistory", true).commit();
                }
            }
        });

        if (sp.getBoolean(getString(R.string.sp_location), false)) {
            toggle_locationView.setVisibility(View.VISIBLE);
        } else {
            toggle_locationView.setVisibility(View.INVISIBLE);
        }

        toggle_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sp.getBoolean(getString(R.string.sp_location), false)) {
                    toggle_locationView.setVisibility(View.INVISIBLE);
                    sp.edit().putBoolean(getString(R.string.sp_location), false).commit();
                } else {
                    toggle_locationView.setVisibility(View.VISIBLE);
                    sp.edit().putBoolean(getString(R.string.sp_location), true).commit();
                }
            }
        });

        if (sp.getBoolean(getString(R.string.sp_images), true)) {
            toggle_imagesView.setVisibility(View.VISIBLE);
        } else {
            toggle_imagesView.setVisibility(View.INVISIBLE);
        }

        toggle_images.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sp.getBoolean(getString(R.string.sp_images), true)) {
                    toggle_imagesView.setVisibility(View.INVISIBLE);
                    sp.edit().putBoolean(getString(R.string.sp_images), false).commit();
                } else {
                    toggle_imagesView.setVisibility(View.VISIBLE);
                    sp.edit().putBoolean(getString(R.string.sp_images), true).commit();
                }
            }
        });

        if (sp.getBoolean("sp_invert", false)) {
            toggle_invertView.setVisibility(View.VISIBLE);
        } else {
            toggle_invertView.setVisibility(View.INVISIBLE);
        }

        toggle_invert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sp.getBoolean("sp_invert", false)) {
                    toggle_invertView.setVisibility(View.INVISIBLE);
                    sp.edit().putBoolean("sp_invert", false).commit();
                } else {
                    toggle_invertView.setVisibility(View.VISIBLE);
                    sp.edit().putBoolean("sp_invert", true).commit();
                }
                HelperUnit.initRendering(ninjaWebView);
            }
        });

        ImageButton ib_reload = dialogView.findViewById(R.id.ib_reload);
        ib_reload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ninjaWebView != null) {
                    bottomSheetDialog.cancel();
                    ninjaWebView.initPreferences();
                    addAlbum(ninjaWebView.getTitle(), ninjaWebView.getUrl(), false);
                    removeAlbum(currentAlbumController);
                }
            }
        });

        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.show();
        HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
    }

    private synchronized void addAlbum(String title, final String url, final boolean foreground) {

        ninjaWebView = new NinjaWebView(context);
        ninjaWebView.setBrowserController(this);
        ninjaWebView.setAlbumTitle(title);
        HelperUnit.bound(context, ninjaWebView);

        final View albumView = ninjaWebView.getAlbumView();
        if (currentAlbumController != null) {
            int index = BrowserContainer.indexOf(currentAlbumController) + 1;
            BrowserContainer.add(ninjaWebView, index);
            tab_container.addView(albumView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        } else {
            BrowserContainer.add(ninjaWebView);
            tab_container.addView(albumView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        }

        if (!foreground) {
            HelperUnit.bound(context, ninjaWebView);
            ninjaWebView.loadUrl(url);
            ninjaWebView.deactivate();
            return;
        } else {
            showAlbum(ninjaWebView);
        }

        if (url != null && !url.isEmpty()) {
            ninjaWebView.loadUrl(url);
        }
    }

    private synchronized void updateAlbum(String url) {
        ((NinjaWebView) currentAlbumController).loadUrl(url);
    }

    private void closeTabConfirmation(final Runnable okAction) {
        if(!sp.getBoolean("sp_close_tab_confirm", false)) {
            okAction.run();
        } else {
            final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
            View dialogView = View.inflate(context, R.layout.dialog_action, null);
            TextView textView = dialogView.findViewById(R.id.dialog_text);
            textView.setText(R.string.toast_close_tab);
            Button action_ok = dialogView.findViewById(R.id.action_ok);
            action_ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    okAction.run();
                    bottomSheetDialog.cancel();
                }
            });
            bottomSheetDialog.setContentView(dialogView);
            bottomSheetDialog.show();
            HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    @Override
    public synchronized void removeAlbum (final AlbumController controller) {
        if (BrowserContainer.size() <= 1) {
            if(!sp.getBoolean("sp_reopenLastTab", false)) {
                doubleTapsQuit();
            }else{
                updateAlbum(sp.getString("favoriteURL", "https://github.com/scoute-dich/browser"));
                hideOverview();
            }
        } else {
            closeTabConfirmation( new Runnable() {
                @Override
                public void run() {
                    tab_container.removeView(controller.getAlbumView());
                    int index = BrowserContainer.indexOf(controller);
                    BrowserContainer.remove(controller);
                    if (index >= BrowserContainer.size()) {
                        index = BrowserContainer.size() - 1;
                    }
                    showAlbum(BrowserContainer.get(index));
                }
            });
        }
    }

    private void updateOmnibox() {
        if (ninjaWebView == currentAlbumController) {
            if (ninjaWebView.getTitle().isEmpty()) {
                omniboxTitle.setText(ninjaWebView.getUrl());
            } else {
                omniboxTitle.setText(ninjaWebView.getTitle());
            }
        } else {
            ninjaWebView = (NinjaWebView) currentAlbumController;
            updateProgress(ninjaWebView.getProgress());
        }

        if (showOverflow) {
            if (ninjaWebView.getTitle().isEmpty()) {
                overflowTitle.setText(ninjaWebView.getUrl());
            } else {
                overflowTitle.setText(ninjaWebView.getTitle());
            }
        }
    }

    @Override
    public synchronized void updateProgress(int progress) {

        updateOmnibox();

        if (progress < BrowserUnit.PROGRESS_MAX) {
            progressBar.setProgress(progress);
            progressBar.setVisibility(View.VISIBLE);
            updateRefresh(true);
        } else {
            progressBar.setVisibility(View.GONE);
            updateRefresh(false);
        }

        if (Objects.requireNonNull(sp.getBoolean("hideToolbar", true))) {
            ninjaWebView.setOnScrollChangeListener(new NinjaWebView.OnScrollChangeListener() {
                @Override
                public void onScrollChange(int scrollY, int oldScrollY) {
                    int height = (int) Math.floor(ninjaWebView.getContentHeight() * ninjaWebView.getResources().getDisplayMetrics().density);
                    int webViewHeight = ninjaWebView.getHeight();
                    int cutoff = height - webViewHeight - 112 * Math.round(getResources().getDisplayMetrics().density);
                    if (scrollY > oldScrollY && cutoff >= scrollY) {
                        if (!searchOnSite)  {
                            fab_imageButtonNav.setVisibility(View.VISIBLE);
                            searchPanel.setVisibility(View.GONE);
                            omnibox.setVisibility(View.GONE);
                            omniboxTitle.setVisibility(View.GONE);
                            appBar.setVisibility(View.GONE);
                        }
                    } else if (scrollY < oldScrollY){
                        showOmnibox();
                    }
                }
            });
        }
    }

    private void updateRefresh(boolean running) {
        if (showOverflow) {
            if (running) {
                omniboxRefresh.setImageResource(R.drawable.icon_close);
            } else {
                try {
                    if (ninjaWebView.getUrl().startsWith("https://")) {
                        omniboxRefresh.setImageResource(R.drawable.icon_refresh);
                    } else {
                        omniboxRefresh.setImageResource(R.drawable.icon_alert);
                    }
                } catch (Exception e) {
                    omniboxRefresh.setImageResource(R.drawable.icon_refresh);
                }
            }
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
        originalOrientation = getRequestedOrientation();

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
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
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
        setRequestedOrientation(originalOrientation);
    }

    private void show_contextMenu_link(final String url) {

        final BottomSheetDialog dialogContextList = new BottomSheetDialog(context);
        View dialogContext = View.inflate(context, R.layout.dialog_menu, null);

        GridView menu_grid = dialogContext.findViewById(R.id.menu_grid);
        menu_grid.setNumColumns(1);
        TextView grid_title = dialogContext.findViewById(R.id.overview_title);
        grid_title.setText(url);

        GridItem item_01 = new GridItem(R.drawable.icon_tab_plus, getString(R.string.main_menu_new_tabOpen), null, 0);
        GridItem item_02 = new GridItem(R.drawable.icon_tab_unselected, getString(R.string.main_menu_new_tab), null, 0);
        GridItem item_03 = new GridItem(R.drawable.icon_menu_share, getString(R.string.menu_share_link), null, 0);
        GridItem item_04 = new GridItem(R.drawable.icon_exit, getString(R.string.menu_open_with), null, 0);
        GridItem item_05 = new GridItem(R.drawable.icon_menu_save, getString(R.string.menu_save_as), null, 0);
        GridItem item_06 = new GridItem(R.drawable.icon_earth, getString(R.string.menu_save_home), null, 0);

        final List<GridItem> gridList = new LinkedList<>();
        gridList.add(gridList.size(), item_01);
        gridList.add(gridList.size(), item_02);
        gridList.add(gridList.size(), item_03);
        gridList.add(gridList.size(), item_04);
        gridList.add(gridList.size(), item_05);
        gridList.add(gridList.size(), item_06);

        GridAdapter gridAdapter = new GridAdapter(context, gridList);
        menu_grid.setAdapter(gridAdapter);
        gridAdapter.notifyDataSetChanged();

        menu_grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    dialogContextList.cancel();
                    addAlbum(getString(R.string.app_name), url, true);
                } else if (position == 1) {
                    dialogContextList.cancel();
                    addAlbum(getString(R.string.app_name), url, false);
                    NinjaToast.show(context, getString(R.string.toast_new_tab_successful));
                } else if (position == 2) {
                    dialogContextList.cancel();
                    shareLink("", url);
                } else if (position == 3) {
                    dialogContextList.cancel();
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    Intent chooser = Intent.createChooser(intent, getString(R.string.menu_open_with));
                    startActivity(chooser);
                } else if (position == 4) {
                    dialogContextList.cancel();
                    HelperUnit.save_as(activity, url);
                } else if (position == 5) {
                    dialogContextList.cancel();
                    save_atHome(url.replace("http://www.", "").replace("https://www.", ""), url);
                }
            }
        });
        dialogContextList.setContentView(dialogContext);
        dialogContextList.show();
        HelperUnit.setBottomSheetBehavior(dialogContextList, dialogContext, BottomSheetBehavior.STATE_EXPANDED);
    }

    private void shareLink (String title, String url) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        sharingIntent.putExtra(Intent.EXTRA_TEXT, url);
        context.startActivity(Intent.createChooser(sharingIntent, (context.getString(R.string.menu_share_link))));
    }

    @Override
    public void onCreateView(final Message resultMsg) {
        if (resultMsg != null) {
            final WebView webView = new WebView(context);
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(webView);
            resultMsg.sendToTarget();
        }
    }

    @Override
    public void onLongPress(final String url) {
        WebView.HitTestResult result = ninjaWebView.getHitTestResult();
        if (url != null) {
            show_contextMenu_link(url);
        } else if (result.getExtra() != null) {
            show_contextMenu_link(result.getExtra());
        }
    }

    private void doubleTapsQuit() {
        if (!sp.getBoolean("sp_close_browser_confirm", true)) {
            finish();
        } else {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
            View dialogView = View.inflate(context, R.layout.dialog_action, null);
            TextView textView = dialogView.findViewById(R.id.dialog_text);
            textView.setText(R.string.toast_quit);
            Button action_ok = dialogView.findViewById(R.id.action_ok);
            action_ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });
            bottomSheetDialog.setContentView(dialogView);
            bottomSheetDialog.show();
            HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        Objects.requireNonNull(imm).hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void showOmnibox() {
        if (!searchOnSite)  {
            fab_imageButtonNav.setVisibility(View.GONE);
            searchPanel.setVisibility(View.GONE);
            omnibox.setVisibility(View.VISIBLE);
            omniboxTitle.setVisibility(View.VISIBLE);
            appBar.setVisibility(View.VISIBLE);
            hideKeyboard(activity);
        }
    }

    private void showOverflow() {

        showOverflow = true;
        final String url = ninjaWebView.getUrl();
        final String title = ninjaWebView.getTitle();
        final BottomSheetDialog dialog_overview = new BottomSheetDialog(context);
        View view = View.inflate(context, R.layout.dialog_menu_overflow, null);

        overflowTitle = view.findViewById(R.id.overflow_title);
        updateOmnibox();
        omniboxRefresh = view.findViewById(R.id.menu_refresh);

        if (ninjaWebView.getUrl().startsWith("https://")) {
            omniboxRefresh.setImageResource(R.drawable.icon_refresh);
        } else {
            omniboxRefresh.setImageResource(R.drawable.icon_alert);
        }


        omniboxRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog_overview.cancel();
                final String url = ninjaWebView.getUrl();
                if (url != null && ninjaWebView.isLoadFinish()) {
                    if (!url.startsWith("https://")) {
                        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
                        View dialogView = View.inflate(context, R.layout.dialog_action, null);
                        TextView textView = dialogView.findViewById(R.id.dialog_text);
                        textView.setText(R.string.toast_unsecured);
                        Button action_ok = dialogView.findViewById(R.id.action_ok);
                        action_ok.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                bottomSheetDialog.cancel();
                                ninjaWebView.loadUrl(url.replace("http://", "https://"));
                            }
                        });
                        bottomSheetDialog.setContentView(dialogView);
                        bottomSheetDialog.show();
                        HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
                    } else {
                        ninjaWebView.initPreferences();
                        addAlbum(ninjaWebView.getTitle(), ninjaWebView.getUrl(), false);
                        removeAlbum(currentAlbumController);
                    }
                } else if (url == null ){
                    String text = getString(R.string.toast_load_error);
                    NinjaToast.show(context, text);
                } else {
                    ninjaWebView.stopLoading();
                }
            }
        });

        final GridView menu_grid_tab = view.findViewById(R.id.menu_grid_tab);
        final GridView menu_grid_share = view.findViewById(R.id.menu_grid_share);
        final GridView menu_grid_save = view.findViewById(R.id.menu_grid_save);
        final GridView menu_grid_other = view.findViewById(R.id.menu_grid_other);
        final View floatButton_tabView = view.findViewById(R.id.floatButton_tabView);
        final View floatButton_shareView = view.findViewById(R.id.floatButton_shareView);
        final View floatButton_saveView = view.findViewById(R.id.floatButton_saveView);
        final View floatButton_moreView = view.findViewById(R.id.floatButton_moreView);

        int orientation = this.getResources().getConfiguration().orientation;
        int numberColumns;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            numberColumns = 1;
        } else {
            numberColumns = 2;
        }

        menu_grid_tab.setVisibility(View.VISIBLE);
        menu_grid_share.setVisibility(View.GONE);
        menu_grid_save.setVisibility(View.GONE);
        menu_grid_other.setVisibility(View.GONE);

        menu_grid_tab.setNumColumns(numberColumns);
        menu_grid_share.setNumColumns(numberColumns);
        menu_grid_save.setNumColumns(numberColumns);
        menu_grid_other.setNumColumns(numberColumns);

        floatButton_tabView.setVisibility(View.VISIBLE);
        floatButton_shareView.setVisibility(View.GONE);
        floatButton_saveView.setVisibility(View.GONE);
        floatButton_moreView.setVisibility(View.GONE);

        // Tab
        GridItem item_01 = new GridItem(R.drawable.icon_preview, getString(R.string.main_menu_tabPreview), null, 0);
        GridItem item_02 = new GridItem(R.drawable.icon_tab_plus, getString(R.string.main_menu_new_tabOpen), null, 0);
        GridItem item_03 = new GridItem(R.drawable.star_grey, getString(R.string.menu_openFav), null, 0);
        GridItem item_04 = new GridItem(R.drawable.icon_close, getString(R.string.menu_closeTab), null, 0);
        GridItem item_05 = new GridItem(R.drawable.icon_exit, getString(R.string.menu_quit), null, 0);

        final List<GridItem> gridList_tab = new LinkedList<>();
        gridList_tab.add(gridList_tab.size(), item_01);
        gridList_tab.add(gridList_tab.size(), item_02);
        gridList_tab.add(gridList_tab.size(), item_03);
        gridList_tab.add(gridList_tab.size(), item_04);
        gridList_tab.add(gridList_tab.size(), item_05);

        GridAdapter gridAdapter_tab = new GridAdapter(context, gridList_tab);
        menu_grid_tab.setAdapter(gridAdapter_tab);
        gridAdapter_tab.notifyDataSetChanged();

        menu_grid_tab.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    dialog_overview.cancel();
                    showOverview();
                } else if (position == 1) {
                    dialog_overview.cancel();
                    addAlbum(getString(R.string.app_name), sp.getString("favoriteURL", "https://github.com/scoute-dich/browser"), true);
                } else if (position == 2) {
                    dialog_overview.cancel();
                    updateAlbum(sp.getString("favoriteURL", "https://github.com/scoute-dich/browser"));
                } else if (position == 3) {
                    removeAlbum(currentAlbumController);
                    dialog_overview.cancel();
                } else if (position == 4) {
                    dialog_overview.cancel();
                    doubleTapsQuit();
                }
            }
        });

        // Save
        GridItem item_21 = new GridItem(R.drawable.star_grey, getString(R.string.menu_fav), null, 0);
        GridItem item_22 = new GridItem(R.drawable.icon_earth, getString(R.string.menu_save_home), null, 0);
        GridItem item_23 = new GridItem(R.drawable.icon_bookmark, getString(R.string.menu_save_bookmark), null, 0);
        GridItem item_24 = new GridItem(R.drawable.icon_document, getString(R.string.menu_save_pdf), null, 0);
        GridItem item_25 = new GridItem(R.drawable.link_plus, getString(R.string.menu_sc), null, 0);
        GridItem item_26 = new GridItem(R.drawable.icon_menu_save, getString(R.string.menu_save_as), null, 0);

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

        menu_grid_save.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RecordAction action = new RecordAction(context);
                if (position == 0) {
                    dialog_overview.cancel();
                    HelperUnit.setFavorite(context, url);
                } else if (position == 1) {
                    dialog_overview.cancel();
                    save_atHome(title, url);
                } else if (position == 2) {
                    dialog_overview.cancel();
                    action.open(true);
                    if (action.checkUrl(url, RecordUnit.TABLE_BOOKMARK)) {
                        NinjaToast.show(context, getString(R.string.toast_already_exist_in_home));
                    } else {
                        action.addBookmark(new Record(ninjaWebView.getTitle(), url, System.currentTimeMillis(), 0));
                        NinjaToast.show(context, getString(R.string.toast_add_to_home_successful));
                        open_bookmark.performClick();
                    }
                    action.close();
                } else if (position == 3) {
                    dialog_overview.cancel();
                    printPDF();
                } else if (position == 4) {
                    dialog_overview.cancel();
                    HelperUnit.createShortcut(context, ninjaWebView.getTitle(), ninjaWebView.getUrl());
                } else if (position == 5) {
                    dialog_overview.cancel();
                    HelperUnit.save_as(activity, url);
                }
            }
        });

        // Share
        GridItem item_11 = new GridItem(R.drawable.icon_menu_share, getString(R.string.menu_share_link), null, 0);
        GridItem item_12 = new GridItem(R.drawable.clipboard_outline, getString(R.string.menu_shareClipboard), null, 0);
        GridItem item_13 = new GridItem(R.drawable.icon_exit, getString(R.string.menu_open_with), null, 0);

        final List<GridItem> gridList_share = new LinkedList<>();
        gridList_share.add(gridList_share.size(), item_11);
        gridList_share.add(gridList_share.size(), item_12);
        gridList_share.add(gridList_share.size(), item_13);

        GridAdapter gridAdapter_share = new GridAdapter(context, gridList_share);
        menu_grid_share.setAdapter(gridAdapter_share);
        gridAdapter_share.notifyDataSetChanged();

        menu_grid_share.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    dialog_overview.cancel();
                    if (prepareRecord()) {
                        NinjaToast.show(context, getString(R.string.toast_share_failed));
                    } else {
                        shareLink(title, url);
                    }
                } else if (position == 1) {
                    dialog_overview.cancel();
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("text", url);
                    Objects.requireNonNull(clipboard).setPrimaryClip(clip);
                    NinjaToast.show(context, R.string.toast_copy_successful);
                } else if (position == 2) {
                    dialog_overview.cancel();
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    Intent chooser = Intent.createChooser(intent, getString(R.string.menu_open_with));
                    startActivity(chooser);
                }
            }
        });

        // Other
        GridItem item_31 = new GridItem(R.drawable.icon_search, getString(R.string.menu_other_searchSite), null, 0);
        GridItem item_32 = new GridItem(R.drawable.icon_download, getString(R.string.menu_download), null, 0);
        GridItem item_33 = new GridItem(R.drawable.icon_settings, getString(R.string.setting_label), null, 0);

        final List<GridItem> gridList_other = new LinkedList<>();
        gridList_other.add(gridList_other.size(), item_31);
        gridList_other.add(gridList_other.size(), item_32);
        gridList_other.add(gridList_other.size(), item_33);

        GridAdapter gridAdapter_other = new GridAdapter(context, gridList_other);
        menu_grid_other.setAdapter(gridAdapter_other);
        gridAdapter_other.notifyDataSetChanged();

        menu_grid_other.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    dialog_overview.cancel();
                    searchOnSite = true;
                    fab_imageButtonNav.setVisibility(View.GONE);
                    omnibox.setVisibility(View.GONE);
                    searchPanel.setVisibility(View.VISIBLE);
                    omniboxTitle.setVisibility(View.GONE);
                    appBar.setVisibility(View.VISIBLE);
                } else if (position == 1) {
                    dialog_overview.cancel();
                    startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
                } else if (position == 2) {
                    dialog_overview.cancel();
                    Intent settings = new Intent(BrowserActivity.this, Settings_Activity.class);
                    startActivity(settings);
                }
            }
        });

        ImageButton fab_tab = view.findViewById(R.id.floatButton_tab);
        fab_tab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menu_grid_tab.setVisibility(View.VISIBLE);
                menu_grid_share.setVisibility(View.GONE);
                menu_grid_save.setVisibility(View.GONE);
                menu_grid_other.setVisibility(View.GONE);

                floatButton_tabView.setVisibility(View.VISIBLE);
                floatButton_shareView.setVisibility(View.GONE);
                floatButton_saveView.setVisibility(View.GONE);
                floatButton_moreView.setVisibility(View.GONE);
            }
        });

        ImageButton fab_share = view.findViewById(R.id.floatButton_share);
        fab_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menu_grid_tab.setVisibility(View.GONE);
                menu_grid_share.setVisibility(View.VISIBLE);
                menu_grid_save.setVisibility(View.GONE);
                menu_grid_other.setVisibility(View.GONE);

                floatButton_tabView.setVisibility(View.GONE);
                floatButton_shareView.setVisibility(View.VISIBLE);
                floatButton_saveView.setVisibility(View.GONE);
                floatButton_moreView.setVisibility(View.GONE);
            }
        });

        ImageButton fab_save = view.findViewById(R.id.floatButton_save);
        fab_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menu_grid_tab.setVisibility(View.GONE);
                menu_grid_share.setVisibility(View.GONE);
                menu_grid_save.setVisibility(View.VISIBLE);
                menu_grid_other.setVisibility(View.GONE);

                floatButton_tabView.setVisibility(View.GONE);
                floatButton_shareView.setVisibility(View.GONE);
                floatButton_saveView.setVisibility(View.VISIBLE);
                floatButton_moreView.setVisibility(View.GONE);
            }
        });

        ImageButton fab_more = view.findViewById(R.id.floatButton_more);
        fab_more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menu_grid_tab.setVisibility(View.GONE);
                menu_grid_share.setVisibility(View.GONE);
                menu_grid_save.setVisibility(View.GONE);
                menu_grid_other.setVisibility(View.VISIBLE);

                floatButton_tabView.setVisibility(View.GONE);
                floatButton_shareView.setVisibility(View.GONE);
                floatButton_saveView.setVisibility(View.GONE);
                floatButton_moreView.setVisibility(View.VISIBLE);
            }
        });

        dialog_overview.setContentView(view);
        dialog_overview.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                showOverflow = false;
            }
        });
        dialog_overview.show();
        HelperUnit.setBottomSheetBehavior(dialog_overview, view, BottomSheetBehavior.STATE_EXPANDED);
    }

    private void show_contextMenu_list (final String title, final String url,
                                        final RecordAdapter adapterRecord, final List<Record> recordList, final int location,
                                        final long icon) {

        final BottomSheetDialog dialogContextList = new BottomSheetDialog(context);
        View dialogContext = View.inflate(context, R.layout.dialog_menu, null);

        GridView menu_grid = dialogContext.findViewById(R.id.menu_grid);
        menu_grid.setNumColumns(1);
        TextView grid_title = dialogContext.findViewById(R.id.overview_title);
        grid_title.setText(title);

        GridItem item_01 = new GridItem(R.drawable.icon_tab_plus, getString(R.string.main_menu_new_tabOpen), null, 0);
        GridItem item_02 = new GridItem(R.drawable.icon_tab_unselected, getString(R.string.main_menu_new_tab), null, 0);
        GridItem item_03 = new GridItem(R.drawable.icon_delete, getString(R.string.menu_delete), null, 0);
        GridItem item_04 = new GridItem(R.drawable.icon_edit, getString(R.string.menu_edit), null, 0);

        final List<GridItem> gridList = new LinkedList<>();
        gridList.add(gridList.size(), item_01);
        gridList.add(gridList.size(), item_02);
        gridList.add(gridList.size(), item_03);

        if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
            gridList.add(gridList.size(), item_04);
        }

        GridAdapter gridAdapter = new GridAdapter(context, gridList);
        menu_grid.setAdapter(gridAdapter);
        gridAdapter.notifyDataSetChanged();

        menu_grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Button action_ok;

                if (position == 0) {
                    dialogContextList.cancel();
                    addAlbum(getString(R.string.app_name), url, true);
                    hideOverview();
                } else if (position == 1) {
                    dialogContextList.cancel();
                    addAlbum(getString(R.string.app_name), url, false);
                    NinjaToast.show(context, getString(R.string.toast_new_tab_successful));
                } else if (position == 2) {
                    dialogContextList.cancel();
                    final BottomSheetDialog dialogDelete = new BottomSheetDialog(context);
                    View dialogDeleteView = View.inflate(context, R.layout.dialog_action, null);
                    TextView dialog_text = dialogDeleteView.findViewById(R.id.dialog_text);
                    dialog_text.setText(R.string.hint_database);
                    action_ok = dialogDeleteView.findViewById(R.id.action_ok);
                    action_ok.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Record record = recordList.get(location);
                            RecordAction action = new RecordAction(context);
                            action.open(true);
                            if (overViewTab.equals(getString(R.string.album_title_home))) {
                                action.deleteURL(record.getURL(), RecordUnit.TABLE_GRID);
                            } else if (overViewTab.equals(getString(R.string.album_title_bookmarks))){
                                action.deleteURL(record.getURL(), RecordUnit.TABLE_BOOKMARK);
                            } else if (overViewTab.equals(getString(R.string.album_title_history))){
                                action.deleteURL(record.getURL(), RecordUnit.TABLE_HISTORY);
                            }
                            action.close();
                            recordList.remove(location);
                            updateAutoComplete();
                            adapterRecord.notifyDataSetChanged();
                            dialogDelete.cancel ();
                        }
                    });
                    dialogDelete.setContentView(dialogDeleteView);
                    dialogDelete.show();
                    HelperUnit.setBottomSheetBehavior(dialogDelete, dialogDeleteView, BottomSheetBehavior.STATE_EXPANDED);
                } if (position == 3) {

                    dialogContextList.cancel();

                    final BottomSheetDialog dialogEdit = new BottomSheetDialog(context);
                    View dialogEditView = View.inflate(context, R.layout.dialog_edit_title, null);

                    final EditText editTitle = dialogEditView.findViewById(R.id.pass_title);
                    final ImageView ib_icon = dialogEditView.findViewById(R.id.edit_icon);
                    editTitle.setText(title);

                    newIcon = icon;
                    HelperUnit.setFilterIcons(ib_icon, newIcon);

                    action_ok = dialogEditView.findViewById(R.id.action_ok);
                    action_ok.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            hideKeyboard(activity);
                            RecordAction action = new RecordAction(context);
                            action.open(true);
                            action.deleteURL(url, RecordUnit.TABLE_BOOKMARK);
                            action.addBookmark(new Record(editTitle.getText().toString(), url, newIcon, 0));
                            action.close();
                            updateAutoComplete();
                            open_bookmark.performClick();
                            dialogEdit.cancel();
                        }
                    });

                    dialogEdit.setContentView(dialogEditView);
                    dialogEdit.show();

                    HelperUnit.setBottomSheetBehavior(dialogEdit, dialogEditView, BottomSheetBehavior.STATE_EXPANDED);

                    ib_icon.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            final BottomSheetDialog dialogFilter = new BottomSheetDialog(context);
                            View dialogView = View.inflate(context, R.layout.dialog_menu, null);

                            TextView grid_title = dialogView.findViewById(R.id.overview_title);
                            grid_title.setText(R.string.setting_filter);

                            final GridView grid = dialogView.findViewById(R.id.menu_grid);
                            grid.setNumColumns(2);

                            final List<GridItem> gridList = new LinkedList<>();
                            HelperUnit.addFilterItems(activity, gridList);

                            final GridAdapter gridAdapter = new GridAdapter(context, gridList);
                            grid.setAdapter(gridAdapter);
                            gridAdapter.notifyDataSetChanged();
                            grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    newIcon = gridList.get(position).getData();
                                    HelperUnit.setFilterIcons(ib_icon, newIcon);
                                    dialogFilter.cancel();
                                }
                            });

                            dialogFilter.setContentView(dialogView);
                            dialogFilter.show();
                            HelperUnit.setBottomSheetBehavior(dialogFilter, dialogView, BottomSheetBehavior.STATE_EXPANDED);
                        }
                    });
                }
            }
        });

        dialogContextList.setContentView(dialogContext);
        dialogContextList.show();
        HelperUnit.setBottomSheetBehavior(dialogContextList, dialogContext, BottomSheetBehavior.STATE_EXPANDED);
    }

    private void save_atHome (final String title, final String url) {
        RecordAction action = new RecordAction(context);
        action.open(true);
        if (action.checkUrl(url, RecordUnit.TABLE_GRID)) {
            NinjaToast.show(context, getString(R.string.toast_already_exist_in_home));
        } else {
            int counter = sp.getInt("counter", 0);
            counter = counter + 1;
            sp.edit().putInt("counter", counter).commit();
            if (action.addGridItem(new Record(title, url, 0, counter))) {
                NinjaToast.show(context, getString(R.string.toast_add_to_home_successful));
                open_startPage.performClick();
            } else {
                NinjaToast.show(context, getString(R.string.toast_add_to_home_failed));
            }
        }
        action.close();
    }

    private void show_dialogFilter() {
        open_bookmark.performClick();
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View dialogView = View.inflate(context, R.layout.dialog_menu, null);

        GridView menu_grid = dialogView.findViewById(R.id.menu_grid);

        TextView grid_title = dialogView.findViewById(R.id.overview_title);
        grid_title.setText(R.string.setting_filter);

        final List<GridItem> gridList = new LinkedList<>();
        HelperUnit.addFilterItems(activity, gridList);

        GridAdapter gridAdapter = new GridAdapter(context, gridList);
        menu_grid.setAdapter(gridAdapter);
        gridAdapter.notifyDataSetChanged();

        menu_grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                filter = true;
                filterBy = gridList.get(position).getData();
                open_bookmark.performClick();
                bottomSheetDialog.cancel();
            }
        });

        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.show();
        HelperUnit.setBottomSheetBehavior(bottomSheetDialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
    }

    private void setCustomFullscreen(boolean fullscreen) {
        View decorView = getWindow().getDecorView();
        if (fullscreen) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
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
}