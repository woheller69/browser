package de.baumann.browser.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Message;

import androidx.preference.PreferenceManager;

import android.util.AttributeSet;
import android.view.*;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import de.baumann.browser.browser.*;
import de.baumann.browser.R;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Objects;

public class NinjaWebView extends WebView implements AlbumController {

    private OnScrollChangeListener onScrollChangeListener;
    public NinjaWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NinjaWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onScrollChanged(int l, int t, int old_l, int old_t) {
        super.onScrollChanged(l, t, old_l, old_t);
        if (onScrollChangeListener != null) {
            onScrollChangeListener.onScrollChange(t, old_t);
        }
    }

    public void setOnScrollChangeListener(OnScrollChangeListener onScrollChangeListener) {
        this.onScrollChangeListener = onScrollChangeListener;
    }

    public interface OnScrollChangeListener {
        /**
         * Called when the scroll position of a view changes.
         *
         * @param scrollY    Current vertical scroll origin.
         * @param oldScrollY Previous vertical scroll origin.
         */
        void onScrollChange(int scrollY, int oldScrollY);
    }

    private Context context;
    private boolean desktopMode;
    private String oldDomain;
    private AlbumItem album;
    private NinjaWebViewClient webViewClient;
    private NinjaWebChromeClient webChromeClient;
    private NinjaDownloadListener downloadListener;
    private NinjaClickHandler clickHandler;
    private GestureDetector gestureDetector;

    private Javascript javaHosts;
    private Remote remoteHosts;
    private Cookie cookieHosts;
    private SharedPreferences sp;

    private boolean foreground;

    public boolean isForeground() {
        return foreground;
    }

    private BrowserController browserController = null;

    public BrowserController getBrowserController() {
        return browserController;
    }

    public void setBrowserController(BrowserController browserController) {
        this.browserController = browserController;
        this.album.setBrowserController(browserController);
    }

    public NinjaWebView(Context context) {
        super(context); // Cannot create a dialog, the WebView context is not an activity

        this.context = context;
        this.foreground = false;
        this.desktopMode=false;
        this.oldDomain="";
        this.javaHosts = new Javascript(this.context);
        this.remoteHosts = new Remote(this.context);
        this.cookieHosts = new Cookie(this.context);
        this.album = new AlbumItem(this.context, this, this.browserController);
        this.webViewClient = new NinjaWebViewClient(this);
        this.webChromeClient = new NinjaWebChromeClient(this);
        this.downloadListener = new NinjaDownloadListener(this.context);
        this.clickHandler = new NinjaClickHandler(this);
        this.gestureDetector = new GestureDetector(context, new NinjaGestureListener(this));

        initWebView();
        initAlbum();
    }

    @SuppressWarnings("SameReturnValue")
    @SuppressLint("ClickableViewAccessibility")
    private synchronized void initWebView() {
        setWebViewClient(webViewClient);
        setWebChromeClient(webChromeClient);
        setDownloadListener(downloadListener);
        setOnTouchListener((view, motionEvent) -> {
            gestureDetector.onTouchEvent(motionEvent);
            return false;
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    @TargetApi(Build.VERSION_CODES.O)
    public synchronized void initPreferences(String url) {

        HelperUnit.initRendering(this, this.context);
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        WebSettings webSettings = getSettings();

        String userAgent = sp.getString("userAgent", "");
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            webSettings.setSafeBrowsingEnabled(true);
        }
        assert userAgent != null;
        if (!userAgent.isEmpty()) {
            webSettings.setUserAgentString(userAgent);
        }

        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportMultipleWindows(true);
        webViewClient.enableAdBlock(sp.getBoolean("sp_ad_block", true));
        webSettings.setTextZoom(Integer.parseInt(Objects.requireNonNull(sp.getString("sp_fontSize", "100"))));
        webSettings.setBlockNetworkImage(!sp.getBoolean("sp_images", true));
        webSettings.setGeolocationEnabled(sp.getBoolean("sp_location", false));

        if (javaHosts.isWhite(url) || sp.getBoolean("sp_javascript", true)) {
            webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
            webSettings.setJavaScriptEnabled(true);
        } else {
            webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
            webSettings.setJavaScriptEnabled(false);
        }
        webSettings.setDomStorageEnabled(remoteHosts.isWhite(url) || sp.getBoolean("sp_remote", true));

        CookieManager manager = CookieManager.getInstance();
        if (cookieHosts.isWhite(url) || sp.getBoolean("sp_cookies", true)) {
            manager.setAcceptCookie(true);
            manager.getCookie(url);
        } else {
            manager.setAcceptCookie(false);
        }

        String  domain="";
        try {
            domain = new URI(url).getHost();
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (oldDomain != null) {
            if (!oldDomain.equals(domain)){
                //do not change setting if staying within same domain
                if (javaHosts.isWhite(url) || sp.getBoolean("sp_javascript", true)) {
                    webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
                    webSettings.setJavaScriptEnabled(true);
                } else {
                    webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
                    webSettings.setJavaScriptEnabled(false);
                }
                webSettings.setDomStorageEnabled(remoteHosts.isWhite(url) || sp.getBoolean("sp_remote", true));
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES);         }

        oldDomain=domain;
    }

    private synchronized void initAlbum() {
        album.setAlbumTitle(context.getString(R.string.app_name));
        album.setBrowserController(browserController);
    }

    public synchronized HashMap<String, String> getRequestHeaders() {
        HashMap<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("DNT", "1");
        if (sp.getBoolean("sp_savedata", false)) {
            requestHeaders.put("Save-Data", "on");
        }
        return requestHeaders;
    }

    @Override
    public synchronized void loadUrl(String url) {
        initPreferences(BrowserUnit.queryWrapper(context, url.trim()));
        super.loadUrl(BrowserUnit.queryWrapper(context, url.trim()), getRequestHeaders());
    }

    @Override
    public View getAlbumView() {
        return album.getAlbumView();
    }

    public void setAlbumTitle(String title) {
        album.setAlbumTitle(title);
    }

    @Override
    public synchronized void activate() {
        requestFocus();
        foreground = true;
        album.activate();
    }

    @Override
    public synchronized void deactivate() {
        clearFocus();
        foreground = false;
        album.deactivate();
    }

    public synchronized void update(int progress) {
        if (foreground) {
            browserController.updateProgress(progress);
        }
        if (isLoadFinish()) {
            browserController.updateAutoComplete();
        }
    }

    public synchronized void update(String title) {
        album.setAlbumTitle(title);
    }

    @Override
    public synchronized void destroy() {
        stopLoading();
        onPause();
        clearHistory();
        setVisibility(GONE);
        removeAllViews();
        super.destroy();
    }

    public boolean isLoadFinish() {
        return getProgress() >= BrowserUnit.PROGRESS_MAX;
    }

    public void onLongPress() {
        Message click = clickHandler.obtainMessage();
        click.setTarget(clickHandler);
        requestFocusNodeHref(click);
    }

    public boolean isDesktopMode() {
        return desktopMode;
    }

    public void toggleDesktopMode(boolean reload) {

        desktopMode=!desktopMode;
        String newUserAgent = getSettings().getUserAgentString();
        if (desktopMode) {
            try {
                String ua = getSettings().getUserAgentString();
                String androidOSString = getSettings().getUserAgentString().substring(ua.indexOf("("), ua.indexOf(")") + 1);
                newUserAgent = getSettings().getUserAgentString().replace(androidOSString, "(X11; Linux x86_64)");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            sp = PreferenceManager.getDefaultSharedPreferences(context);
            newUserAgent = sp.getString("userAgent", "");
        }
        getSettings().setUserAgentString(newUserAgent);
        getSettings().setUseWideViewPort(desktopMode);
        getSettings().setSupportZoom(desktopMode);
        getSettings().setLoadWithOverviewMode(desktopMode);

        if (reload) {
            reload();
        }
    }
}
