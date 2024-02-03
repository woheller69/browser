package de.baumann.browser.browser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.webkit.*;

import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.Objects;

import de.baumann.browser.R;
import de.baumann.browser.Utils;
import de.baumann.browser.activity.BrowserActivity;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.view.NinjaToast;
import de.baumann.browser.view.NinjaWebView;

public class NinjaWebChromeClient extends WebChromeClient {

    private final NinjaWebView ninjaWebView;

    public NinjaWebChromeClient(NinjaWebView ninjaWebView) {
        super();
        this.ninjaWebView = ninjaWebView;
    }

    @Override
    public void onProgressChanged(WebView view, int progress) {
        super.onProgressChanged(view, progress);
        ninjaWebView.updateTitle(progress);
        if (Objects.requireNonNull(view.getTitle()).isEmpty()) {
            ninjaWebView.updateTitle(view.getUrl());
        } else {
            ninjaWebView.updateTitle(view.getTitle());
        }
        //ninjaWebView.updateFavicon();
    }

    @Override
    public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, android.os.Message resultMsg) {

        Context context = view.getContext();
        WebView newWebView = new WebView(context);
        view.addView(newWebView);
        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
        transport.setWebView(newWebView);
        resultMsg.sendToTarget();

        newWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Intent browserIntent = new Intent(context, BrowserActivity.class);
                browserIntent.setAction(Intent.ACTION_VIEW);
                browserIntent.setData(request.getUrl());
                context.startActivity(browserIntent);
                return true;
            }
        });
        return true;
    }

    @Override
    public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
        ninjaWebView.getBrowserController().onShowCustomView(view, callback);
        super.onShowCustomView(view, callback);
    }

    @Override
    public void onHideCustomView() {
        ninjaWebView.getBrowserController().onHideCustomView();
        super.onHideCustomView();
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        ninjaWebView.getBrowserController().showFileChooser(filePathCallback);
        return true;
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
        Activity activity =  (Activity) ninjaWebView.getContext();
        HelperUnit.grantPermissionsLoc(activity);
        callback.invoke(origin, true, false);
        super.onGeolocationPermissionsShowPrompt(origin, callback);
    }

    @Override
    public void onPermissionRequest(final PermissionRequest request){
        Activity activity =  (Activity) ninjaWebView.getContext();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ninjaWebView.getContext());
        String[] resources = request.getResources();
        boolean audioGranted=false;
        for (String resource : resources) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                if (sp.getBoolean("sp_camera", false)) {
                    //Reminder: switch off setMediaPlaybackRequiresUserGesture; done in NinjaWebView if sp_camera TRUE
                    if (sp.getBoolean("sp_microphone", false)) {
                        audioGranted = true;
                        request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE,PermissionRequest.RESOURCE_AUDIO_CAPTURE});
                    } else {
                        request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                    }
                } else {
                    Snackbar.make(ninjaWebView, activity.getString(R.string.error_allow_camera), 3000).setAction(activity.getString(R.string.app_ok), view -> {
                        sp.edit().putBoolean("sp_camera", true).apply();
                        HelperUnit.grantPermissionsCam(activity);
                        if (!HelperUnit.checkPermissionsCam(activity))
                            NinjaToast.show(activity, activity.getResources().getString(R.string.error_missing_permission) + "\n" + activity.getResources().getString(R.string.error_allow_camera));
                        ninjaWebView.reload();
                    }).setBackgroundTint(Utils.getThemeColor(activity,R.attr.colorHint)).setTextColor(activity.getColor(android.R.color.holo_red_dark)).setActionTextColor(activity.getColor(android.R.color.holo_red_dark)).show();
                }
            } else if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                if (sp.getBoolean("sp_microphone", false)) {
                    if (!audioGranted) request.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});  //otherwise crash:  Either grant() or deny() has been already called.
                } else {
                    Snackbar.make(ninjaWebView, activity.getString(R.string.error_allow_microphone), 3000).setAction(activity.getString(R.string.app_ok), view -> {
                        sp.edit().putBoolean("sp_microphone", true).apply();
                        HelperUnit.grantPermissionsMic(activity);
                        if (!HelperUnit.checkPermissionsMic(activity))
                            NinjaToast.show(activity, activity.getResources().getString(R.string.error_missing_permission) + "\n" + activity.getString(R.string.error_allow_microphone));
                        ninjaWebView.reload();
                    }).setBackgroundTint(Utils.getThemeColor(activity,R.attr.colorHint)).setTextColor(activity.getColor(android.R.color.holo_red_dark)).setActionTextColor(activity.getColor(android.R.color.holo_red_dark)).show();
                }
            } else if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(resource)) {
                if (ninjaWebView.getBlockNetworkVideo()) { //if videos are blocked there is no need to allow DRM requests
                    request.deny();
                } else if (sp.getBoolean("sp_drm",false)){  //granted already in settings, otherwise ask every time
                    request.grant(new String[]{PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID});
                } else {
                    final boolean[] handled = {false}; //flag if request has been handled to avoid crash
                    Snackbar.make(ninjaWebView, activity.getString(R.string.hint_DRM_Media),3000).setAction(activity.getString(R.string.app_ok), view -> {
                        handled[0] = true;
                        request.grant(new String[]{PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID});
                    }).addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                        @Override
                        public void onDismissed(Snackbar transientBottomBar, int event) {
                            super.onDismissed(transientBottomBar, event);
                            if (!handled[0]) { request.deny(); }
                        }
                    }).setBackgroundTint(Utils.getThemeColor(activity,R.attr.colorHint)).setTextColor(activity.getColor(android.R.color.holo_red_dark)).setActionTextColor(activity.getColor(android.R.color.holo_red_dark)).show();
                }
            }
        }

    }

    @Override
    public void onReceivedIcon(WebView view, Bitmap icon) {
        super.onReceivedIcon(view, icon);
        ninjaWebView.setFavicon(icon);
    }

    @Override
    public void onReceivedTitle(WebView view, String sTitle) {
        super.onReceivedTitle(view, sTitle);
    }
}
