package de.baumann.browser.browser;

import android.content.Context;
import android.webkit.DownloadListener;
import android.webkit.WebView;

import de.baumann.browser.unit.BrowserUnit;

public class NinjaDownloadListener implements DownloadListener {
    private final Context context;
    private WebView webview;

    public NinjaDownloadListener(Context context, WebView webview) {
        super();
        this.context = context;
        this.webview = webview;
    }

    @Override
    public void onDownloadStart(final String url, String userAgent, final String contentDisposition, final String mimeType, long contentLength) {
        BrowserUnit.download(context, webview, url, contentDisposition, mimeType);
    }
}
