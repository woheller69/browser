package de.baumann.browser.browser;

import android.view.View;

public interface AlbumController {
    View getAlbumView();
    void setAlbumTitle(String title);
    void activate();
    void deactivate();
}
