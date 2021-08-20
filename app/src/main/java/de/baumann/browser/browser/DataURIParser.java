package de.baumann.browser.browser;

import android.util.Base64;
import android.webkit.MimeTypeMap;

public class DataURIParser {

    private final String data;
    private final String mimeType;
    private final String filename;
    private final byte[] imagedata;

    public String getData() {
        return data;
    }

    public String getFilename() {
        return filename;
    }

    public String getMimeType() {
        return mimeType;
    }

    public byte[] getImagedata() { return imagedata; }

    public DataURIParser(String url) {
        //Log.d("DataURIParse", url);
        data=url.substring(url.indexOf(",")+1);
        //Log.d("DataURIParse", data);
        mimeType=url.substring(url.indexOf(":")+1,url.indexOf(";"));
        //Log.d("DataURIParse", mimeType);
        String fileType = url.substring(url.indexOf(":") + 1, url.indexOf("/"));
        //Log.d("DataURIParse", fileType);
        String suffix = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        //Log.d("DataURIParse", suffix);
        filename= fileType +"."+ suffix;
        //Log.d("DataURIParse", filename);
        imagedata = Base64.decode(data,Base64.DEFAULT);

    }
}
