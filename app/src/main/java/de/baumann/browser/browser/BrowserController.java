package de.baumann.browser.browser;

import android.net.Uri;
import android.os.Message;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

public interface BrowserController {
    void updateAutoComplete();
    void updateProgress(int progress);
    void showAlbum(AlbumController albumController);
    void removeAlbum(AlbumController albumController);
    void showFileChooser(ValueCallback<Uri[]> filePathCallback);
    void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback);
    void onLongPress(String url);
    void hideOverview ();
    void onCreateView(WebView view, Message resultMsg);
    boolean onHideCustomView();
}
