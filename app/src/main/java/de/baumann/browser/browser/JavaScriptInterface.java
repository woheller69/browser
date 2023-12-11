/*      Copyright (C) 2023 woheller69

        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.baumann.browser.browser;

import android.content.Context;
import android.os.Environment;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.widget.Toast;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.view.NinjaWebView;

public class JavaScriptInterface {

    private final Context context;
    private final NinjaWebView ninjaWebView;

    public JavaScriptInterface(Context context, NinjaWebView ninjaWebView) {
        this.context = context;
        this.ninjaWebView = ninjaWebView;
    }

    @JavascriptInterface
    public void getBase64FromBlobData(String filename, String mimeType, String base64Data) throws IOException {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
        byte[] base64AsBytes = Base64.decode(base64Data.replaceFirst("^data:"+mimeType+";base64,", ""), 0);
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(base64AsBytes);
        fos.flush();
        fos.close();
        HelperUnit.openDialogDownloads(context);
    }

    @JavascriptInterface
    public void errorHandler(String error){
        Toast.makeText(context,error,Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void print(){
        HelperUnit.print(context, ninjaWebView);
    }

    public static String injectPrintSupport(){
        return  "window.print = function(){" +
                "   NinjaWebViewJS.print();" +
                "};";
    }

    public static String getBase64StringFromBlobUrl(String blobUrl, String filename, String mimeType) {

        return "var xhr = new XMLHttpRequest();" +
                "xhr.open('GET', '" + blobUrl + "', true);" +
                "xhr.setRequestHeader('Content-type','" + mimeType + "');" +
                "xhr.responseType = 'blob';" +
                "xhr.onload = function(e) {" +
                "   if (this.status == 200) {" +
                "       var blob = this.response;" +
                "       var reader = new FileReader();" +
                "       reader.readAsDataURL(blob);" +
                "       reader.onloadend = function() {" +
                "           base64data = reader.result;" +
                "           NinjaWebViewJS.getBase64FromBlobData('" + filename + "', '" + mimeType + "', base64data);" +
                "       };" +
                "       reader.onerror = function(e) {" +
                "           NinjaWebViewJS.errorHandler('Error: ' + e.message);" +
                "       };" +
                "   } else {" +
                "       NinjaWebViewJS.errorHandler('XHR Error: ' + this.status);" +
                "   }" +
                "};" +
                "xhr.onerror = function() {" +
                "   NinjaWebViewJS.errorHandler('XHR request failed');" +
                "};" +
                "xhr.send();";
    }
}

