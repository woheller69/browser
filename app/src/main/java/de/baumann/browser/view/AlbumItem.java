package de.baumann.browser.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import de.baumann.browser.Utils;
import de.baumann.browser.browser.AlbumController;
import de.baumann.browser.browser.BrowserController;
import de.baumann.browser.R;

class AlbumItem {

    private final Context context;
    private final AlbumController albumController;
    private ImageView albumClose;
    private ImageView albumFavicon;

    private View albumView;
    View getAlbumView() {
        return albumView;
    }

    private TextView albumTitle;
    void setAlbumTitle(String title) {
        albumTitle.setText(title);
    }

    private BrowserController browserController;
    void setBrowserController(BrowserController browserController) {
        this.browserController = browserController;
    }

    AlbumItem(Context context, AlbumController albumController, BrowserController browserController) {
        this.context = context;
        this.albumController = albumController;
        this.browserController = browserController;
        initUI();
    }

    @SuppressLint("InflateParams")
    private void initUI() {
        albumView = LayoutInflater.from(context).inflate(R.layout.item_album_view, null, false);
        albumView.setOnLongClickListener(v -> {
            browserController.removeAlbum(albumController);
            return true;
        });
        albumClose = albumView.findViewById(R.id.item_cancel);
        albumClose.setOnClickListener(v -> browserController.removeAlbum(albumController));
        albumTitle = albumView.findViewById(R.id.item_title);
        albumFavicon = albumView.findViewById(R.id.faviconView);
    }

    public void activate(NinjaWebView ninjaWebView) {
        albumTitle.setTextColor(Utils.getThemeColor(context,R.attr.colorPrimary));
        albumClose.setImageResource(R.drawable.icon_close_enabled);
        albumView.setOnClickListener(v -> browserController.hideTabView());
        if (ninjaWebView.getFavicon()!=null) albumFavicon.setImageBitmap(ninjaWebView.getFavicon());
        else albumFavicon.setImageResource(R.drawable.icon_image_broken);
    }

    void deactivate(NinjaWebView ninjaWebView) {
        albumTitle.setTextColor(Utils.getThemeColor(context,android.R.attr.textColorPrimary));
        albumClose.setImageResource(R.drawable.icon_close);
        albumView.setOnClickListener(v -> {
            browserController.showAlbum(albumController);
            browserController.hideTabView();
        });
        if (ninjaWebView.getFavicon()!=null) albumFavicon.setImageBitmap(ninjaWebView.getFavicon());
        else albumFavicon.setImageResource(R.drawable.icon_image_broken);
    }
}