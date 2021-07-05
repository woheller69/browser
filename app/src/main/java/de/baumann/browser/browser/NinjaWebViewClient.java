package de.baumann.browser.browser;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import android.view.Gravity;
import android.view.View;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.ByteArrayInputStream;
import java.net.URISyntaxException;
import java.util.Objects;

import de.baumann.browser.database.Record;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.R;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.RecordUnit;
import de.baumann.browser.view.NinjaWebView;

public class NinjaWebViewClient extends WebViewClient {

    private final NinjaWebView ninjaWebView;
    private final Context context;
    private final SharedPreferences sp;
    private final AdBlock adBlock;

    private final boolean white;
    private boolean enable;
    public void enableAdBlock(boolean enable) {
        this.enable = enable;
    }

    public NinjaWebViewClient(NinjaWebView ninjaWebView) {
        super();
        this.ninjaWebView = ninjaWebView;
        this.context = ninjaWebView.getContext();
        this.sp = PreferenceManager.getDefaultSharedPreferences(context);
        this.adBlock = new AdBlock(this.context);
        this.white = false;
        this.enable = true;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        if (sp.getBoolean("saveHistory", true)) {
            RecordAction action = new RecordAction(context);
            action.open(true);
            if (action.checkUrl(url, RecordUnit.TABLE_HISTORY)) {
                action.deleteURL(url, RecordUnit.TABLE_HISTORY);
            }
            action.addHistory(new Record(ninjaWebView.getTitle(), url, System.currentTimeMillis(), 0));
            action.close();
        }

        if (ninjaWebView.isForeground()) {
            ninjaWebView.invalidate();
        } else {
            ninjaWebView.postInvalidate();
        }
    }

    @Override
    public void onLoadResource(WebView view, String url) {
       if (view.getSettings().getUseWideViewPort() && (view.getWidth()<1300)) view.evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content', 'width=1200px');", null);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        final Uri uri = Uri.parse(url);
        return handleUri(uri);
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        final Uri uri = request.getUrl();
        return handleUri(uri);
    }

    private boolean handleUri(final Uri uri) {

        String url = uri.toString();

        if (url.startsWith("http")) {
            ninjaWebView.getSettings();
            ninjaWebView.initPreferences(url);
            ninjaWebView.loadUrl(url, ninjaWebView.getRequestHeaders());
            return true;
        }

        if (url.startsWith("intent:")) {
            try {
                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                //try to find fallback url
                String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                if (fallbackUrl != null) {
                    this.ninjaWebView.loadUrl(fallbackUrl);
                    return true;
                }
            } catch (URISyntaxException e) {
                //not an intent uri
                return false;
            }
        }
        return true;//do nothing in other cases
    }

    @Override
    @SuppressWarnings("deprecation")
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if (enable && !white && adBlock.isAd(url)) {
            return new WebResourceResponse(
                    BrowserUnit.MIME_TYPE_TEXT_PLAIN,
                    BrowserUnit.URL_ENCODING,
                    new ByteArrayInputStream("".getBytes())
            );
        }
        return super.shouldInterceptRequest(view, url);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        if (enable && !white && adBlock.isAd(request.getUrl().toString())) {
            return new WebResourceResponse(
                    BrowserUnit.MIME_TYPE_TEXT_PLAIN,
                    BrowserUnit.URL_ENCODING,
                    new ByteArrayInputStream("".getBytes())
            );
        }
        return super.shouldInterceptRequest(view, request);
    }

    @Override
    public void onFormResubmission(WebView view, @NonNull final Message doNotResend, final Message resend) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setMessage(R.string.dialog_content_resubmission);
        builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> resend.sendToTarget());
        builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.setOnCancelListener(dialog1 -> doNotResend.sendToTarget());
        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
    }

    @Override
    public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {

        String message = "\"SSL Certificate error.\"";
        switch (error.getPrimaryError()) {
            case SslError.SSL_UNTRUSTED:
                message = "\"Certificate authority is not trusted.\"";
                break;
            case SslError.SSL_EXPIRED:
                message = "\"Certificate has expired.\"";
                break;
            case SslError.SSL_IDMISMATCH:
                message = "\"Certificate Hostname mismatch.\"";
                break;
            case SslError.SSL_NOTYETVALID:
                message = "\"Certificate is not yet valid.\"";
                break;
            case SslError.SSL_DATE_INVALID:
                message = "\"Certificate date is invalid.\"";
                break;
            case SslError.SSL_INVALID:
                message = "\"Certificate is invalid.\"";
                break;
        }
        String text = message + " - " + context.getString(R.string.dialog_content_ssl_error);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setMessage(text);
        builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> handler.proceed());
        builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.setOnCancelListener(dialog1 -> handler.cancel());
        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
    }

    @Override
    public void onReceivedHttpAuthRequest(WebView view, @NonNull final HttpAuthHandler handler, String host, String realm) {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.dialog_edit_title, null);

        TextInputLayout edit_title_layout = dialogView.findViewById(R.id.edit_title_layout);
        TextInputLayout edit_userName_layout = dialogView.findViewById(R.id.edit_userName_layout);
        TextInputLayout edit_PW_layout = dialogView.findViewById(R.id.edit_PW_layout);
        ImageView ib_icon = dialogView.findViewById(R.id.edit_icon);
        ib_icon.setVisibility(View.GONE);
        edit_title_layout.setVisibility(View.GONE);
        edit_userName_layout.setVisibility(View.VISIBLE);
        edit_PW_layout.setVisibility(View.VISIBLE);

        EditText pass_userNameET = dialogView.findViewById(R.id.edit_userName);
        EditText pass_userPWET = dialogView.findViewById(R.id.edit_PW);

        builder.setView(dialogView);
        builder.setTitle("HttpAuthRequest");
        builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
            String user = pass_userNameET.getText().toString().trim();
            String pass = pass_userPWET.getText().toString().trim();
            handler.proceed(user, pass);
            dialog.cancel();
        });
        builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        dialog.setOnCancelListener(dialog1 -> {
            handler.cancel();
            dialog1.cancel();
        });
    }
}
