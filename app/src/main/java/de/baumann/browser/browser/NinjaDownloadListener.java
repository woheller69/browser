package de.baumann.browser.browser;

import android.content.Context;
import android.webkit.DownloadListener;
import de.baumann.browser.unit.BrowserUnit;

public class NinjaDownloadListener implements DownloadListener {
    private final Context context;

    public NinjaDownloadListener(Context context) {
        super();
        this.context = context;
    }

    @Override
    public void onDownloadStart(final String url, String userAgent, final String contentDisposition, final String mimeType, long contentLength) {
        BrowserUnit.download(context, url, contentDisposition, mimeType);
    }
}
