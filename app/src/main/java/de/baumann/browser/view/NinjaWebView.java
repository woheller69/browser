package de.baumann.browser.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import android.util.AttributeSet;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import de.baumann.browser.browser.*;
import de.baumann.browser.R;
import de.baumann.browser.database.FaviconHelper;
import de.baumann.browser.database.Record;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
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
    private boolean stopped;
    private String oldDomain;
    private AlbumItem album;
    private NinjaWebViewClient webViewClient;
    private NinjaWebChromeClient webChromeClient;
    private NinjaDownloadListener downloadListener;

    private Javascript javaHosts;
    private Remote remoteHosts;
    private Cookie cookieHosts;
    private Bitmap favicon;
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
        this.stopped=false;
        this.oldDomain="";
        this.javaHosts = new Javascript(this.context);
        this.remoteHosts = new Remote(this.context);
        this.cookieHosts = new Cookie(this.context);
        this.album = new AlbumItem(this.context, this, this.browserController);
        this.webViewClient = new NinjaWebViewClient(this);
        this.webChromeClient = new NinjaWebChromeClient(this);
        this.downloadListener = new NinjaDownloadListener(this.context);
        initWebView();
        initAlbum();
    }

    private synchronized void initWebView() {
        setWebViewClient(webViewClient);
        setWebChromeClient(webChromeClient);
        setDownloadListener(downloadListener);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @TargetApi(Build.VERSION_CODES.O)
    public synchronized void initPreferences(String url) {

        HelperUnit.initRendering(this, this.context);
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        WebSettings webSettings = getSettings();

        this.desktopMode = false;
        String userAgent = getUserAgent(desktopMode);
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            webSettings.setSafeBrowsingEnabled(true);
        }

        webSettings.setUserAgentString(userAgent);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportMultipleWindows(true);
        webViewClient.enableAdBlock(sp.getBoolean("sp_ad_block", true));
        webSettings.setTextZoom(Integer.parseInt(Objects.requireNonNull(sp.getString("sp_fontSize", "100"))));
        webSettings.setBlockNetworkImage(!sp.getBoolean("sp_images", true));
        webSettings.setGeolocationEnabled(sp.getBoolean("sp_location", false));
        webSettings.setMediaPlaybackRequiresUserGesture(sp.getBoolean("sp_savedata",true));

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
            //do not change setting if staying within same domain
            setJavaScript(javaHosts.isWhite(url) || sp.getBoolean("sp_javascript", true));
            setDomStorage(remoteHosts.isWhite(url) || sp.getBoolean("sp_remote", true));
            e.printStackTrace();
        }

        if (oldDomain != null) {
            //do not change setting if staying within same domain
            if (!oldDomain.equals(domain)){
                setJavaScript(javaHosts.isWhite(url) || sp.getBoolean("sp_javascript", true));
                setDomStorage(remoteHosts.isWhite(url) || sp.getBoolean("sp_remote", true));
            }
        }

        if (sp.getBoolean("sp_autofill", true)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES);
            } else {
                webSettings.setSaveFormData(true);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
            } else {
                webSettings.setSaveFormData(false);
            }
        }
        oldDomain=domain;
    }

    public void setOldDomain(String url){
        String  domain="";
        try {
            domain = new URI(url).getHost();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        oldDomain=domain;
    }

    public void setJavaScript(boolean value){
        WebSettings webSettings = this.getSettings();
        webSettings.setJavaScriptCanOpenWindowsAutomatically(value);
        webSettings.setJavaScriptEnabled(value);
    }

    public void setDomStorage(boolean value){
        WebSettings webSettings = this.getSettings();
        webSettings.setDomStorageEnabled(value);
    }

    private synchronized void initAlbum() {
        album.setAlbumTitle(context.getString(R.string.app_name));
        album.setBrowserController(browserController);
    }

    public synchronized HashMap<String, String> getRequestHeaders() {
        HashMap<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("DNT", "1");
        //  Server-side detection for GlobalPrivacyControl
        requestHeaders.put("Sec-GPC","1");
        requestHeaders.put("X-Requested-With","com.duckduckgo.mobile.android");
        if (sp.getBoolean("sp_savedata", false)) {
            requestHeaders.put("Save-Data", "on");
        }
        return requestHeaders;
    }

    @Override
    public synchronized void stopLoading(){
        stopped=true;
        super.stopLoading();
    }

    @Override
    public synchronized void reload(){
        stopped=false;
        super.reload();
    }

    @Override
    public synchronized void loadUrl(String url) {
        initPreferences(BrowserUnit.queryWrapper(context, url.trim()));
        InputMethodManager imm = (InputMethodManager) this.context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
        favicon=null;
        stopped=false;
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
        if (foreground && !stopped) {
            browserController.updateProgress(progress);
        } else if (foreground && stopped) {
            browserController.updateProgress(BrowserUnit.LOADING_STOPPED);
        }
        if (isLoadFinish() && !stopped) {
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

    public boolean isDesktopMode() {
        return desktopMode;
    }

    public String getUserAgent(boolean desktopMode){
        String mobilePrefix = "Mozilla/5.0 (Linux; Android "+ Build.VERSION.RELEASE + ")";
        String desktopPrefix = "Mozilla/5.0 (X11; Linux "+ System.getProperty("os.arch") +")";

        String newUserAgent=WebSettings.getDefaultUserAgent(context);
        String prefix = newUserAgent.substring(0, newUserAgent.indexOf(")") + 1);

        if (desktopMode) {
            try {
                newUserAgent=newUserAgent.replace(prefix,desktopPrefix);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                newUserAgent=newUserAgent.replace(prefix,mobilePrefix);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //Override UserAgent if own UserAgent is defined
        if (!sp.contains("userAgentSwitch")){  //if new switch_text_preference has never been used initialize the switch
            if (sp.getString("userAgent", "").equals("")) {
                sp.edit().putBoolean("userAgentSwitch", false).apply();
            }else{
                sp.edit().putBoolean("userAgentSwitch", true).apply();
            }
        }

        String ownUserAgent = sp.getString("userAgent", "");
        if (!ownUserAgent.equals("") && (sp.getBoolean("userAgentSwitch",false))) newUserAgent=ownUserAgent;
        return newUserAgent;
    }

    public void toggleDesktopMode(boolean reload) {

        desktopMode=!desktopMode;
        String newUserAgent=getUserAgent(desktopMode);
        getSettings().setUserAgentString(newUserAgent);
        getSettings().setUseWideViewPort(desktopMode);
        getSettings().setSupportZoom(desktopMode);
        getSettings().setLoadWithOverviewMode(desktopMode);

        if (reload) {
            reload();
        }
    }

    public void resetFavicon(){this.favicon=null;}

    public void setFavicon(Bitmap favicon) {
        this.favicon = favicon;

        //Save favicon for existing bookmarks or start site entries
        FaviconHelper faviconHelper = new FaviconHelper(context);
        RecordAction action = new RecordAction(context);
        action.open(false);
        List<Record> list;
        list = action.listBookmark(context, false, 0);
        action.close();
        for (Record listitem: list){
            if(listitem.getURL().equals(getUrl())){
                if (faviconHelper.getFavicon(listitem.getURL())==null) faviconHelper.addFavicon(getUrl(),getFavicon());
            }
        }

        action.open(false);
        list = action.listStartSite((Activity) context);
        action.close();
        for (Record listitem: list){
            if(listitem.getURL().equals(getUrl())){
                if (faviconHelper.getFavicon(listitem.getURL())==null) faviconHelper.addFavicon(getUrl(),getFavicon());
            }
        }

        action.open(false);
        list = action.listHistory();
        action.close();
        for (Record listitem: list){
            if(listitem.getURL().equals(getUrl())){
                if (faviconHelper.getFavicon(listitem.getURL())==null) faviconHelper.addFavicon(getUrl(),getFavicon());
            }
        }

    }

    @Nullable
    @Override
    public Bitmap getFavicon() {
        return favicon;
    }

    public void setStopped(boolean stopped){this.stopped=stopped;}
}
