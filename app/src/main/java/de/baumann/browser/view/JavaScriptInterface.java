package de.baumann.browser.view;

import android.content.Context;
import android.os.Environment;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.widget.Toast;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import de.baumann.browser.R;

public class JavaScriptInterface {
    private final Context context;
    public JavaScriptInterface(Context context) {
        this.context = context;
    }

    @JavascriptInterface
    public void getBase64FromBlobData(String filename, String mimeType, String base64Data) throws IOException {
        if (mimeType.equals("application/pdf")) convertBase64StringToPdfAndStoreIt(filename, base64Data);
        else NinjaToast.show(context,mimeType + " not supported");
    }

    public static String getBase64StringFromBlobUrl(String blobUrl, String filename, String mimeType) {

            return "var xhr = new XMLHttpRequest();" +
                    "xhr.open('GET', '"+ blobUrl +"', true);" +
                    "xhr.setRequestHeader('Content-type','" + mimeType + "');" +
                    "xhr.responseType = 'blob';" +
                    "xhr.onload = function(e) {" +
                    "    if (this.status == 200) {" +
                    "        var blobPdf = this.response;" +
                    "        var reader = new FileReader();" +
                    "        reader.readAsDataURL(blobPdf);" +
                    "        reader.onloadend = function() {" +
                    "            base64data = reader.result;" +
                    "            NinjaWebViewJS.getBase64FromBlobData('" + filename + "', '" + mimeType + "', base64data);" +
                    "        }" +
                    "    }" +
                    "};" +
                    "xhr.send();";
    }

    private void convertBase64StringToPdfAndStoreIt(String filename, String base64PDf) throws IOException {

            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
            byte[] pdfAsBytes = Base64.decode(base64PDf.replaceFirst("^data:application/pdf;base64,", ""), 0);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(pdfAsBytes);
            fos.flush();
            fos.close();
            Toast.makeText(context, context.getString(R.string.app_done), Toast.LENGTH_SHORT).show();
    }
}

