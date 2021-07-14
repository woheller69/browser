package de.baumann.browser.browser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;

import androidx.preference.PreferenceManager;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class AdBlock {
    private static final String FILE = "hosts.txt";
    private static final Set<String> hosts = new HashSet<>();
    @SuppressLint("ConstantLocale")
    private static final Locale locale = Locale.getDefault();


    public static String getHostsDate(Context context){
        File file = new File(context.getDir("filesdir", Context.MODE_PRIVATE) + "/"+FILE);
        String date="";
        if (!file.exists()) {
            return "";
        }

        try {
            FileReader in = new FileReader(file);
            BufferedReader reader = new BufferedReader(in) ;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Date:"))  {
                    date="hosts.txt " + line.substring(2);
                    in.close();
                    break;
                }
            }
            in.close();

        } catch (IOException i) {
            Log.w("browser", "Error getting hosts date", i);
        }
        return date;
    }

    private static void loadHosts(final Context context) {
        Thread thread = new Thread(() -> {
            try {
                File file = new File(context.getDir("filesdir", Context.MODE_PRIVATE) + "/"+FILE);
                FileReader in = new FileReader(file);
                BufferedReader reader = new BufferedReader(in) ;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("#"))  continue;
                    hosts.add(line.toLowerCase(locale));
                }
                in.close();
            } catch (IOException i) {
                Log.w("browser", "Error loading adBlockHosts", i);
            }
        });
        thread.start();
    }

    public static void downloadHosts(final Context context) {
        Thread thread = new Thread(() -> {

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            String hostURL = "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/fakenews-gambling-porn/hosts";

            if (Objects.equals(sp.getString("ab_hosts", "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"),
                    "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts")) {
                hostURL = "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/fakenews-gambling-porn/hosts";
            } else if (Objects.equals(sp.getString("ab_hosts", "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/fakenews/hosts"),
                    "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/fakenews/hosts")) {
                hostURL = "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/fakenews/hosts";
            }

            try {
                URL url = new URL(hostURL);
                Log.d("browser","Download AdBlock hosts");
                URLConnection ucon = url.openConnection();
                ucon.setReadTimeout(5000);
                ucon.setConnectTimeout(10000);

                InputStream is = ucon.getInputStream();
                BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);

                File tempfile = new File(context.getDir("filesdir", Context.MODE_PRIVATE) + "/temp.txt");

                if (tempfile.exists())
                {
                    tempfile.delete();
                }
                tempfile.createNewFile();

                FileOutputStream outStream = new FileOutputStream(tempfile);
                byte[] buff = new byte[5 * 1024];

                int len;
                while ((len = inStream.read(buff)) != -1)
                {
                    outStream.write(buff, 0, len);
                }

                outStream.flush();
                outStream.close();
                inStream.close();

                //now remove leading 0.0.0.0 from file
                FileReader in = new FileReader(tempfile);
                BufferedReader reader = new BufferedReader(in) ;
                File outfile = new File(context.getDir("filesdir", Context.MODE_PRIVATE) + "/"+FILE);
                FileWriter out = new FileWriter(outfile);
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("0.0.0.0 ")) {
                        line=line.substring(8);
                    }
                    out.write(line+"\n");
                }
                in.close();
                out.close();

                tempfile.delete();

                hosts.clear();
                loadHosts(context);  //reload hosts after update
                Log.w("browser", "AdBlock hosts updated");

            } catch (IOException i) {
                Log.w("browser", "Error updating AdBlock hosts", i);
            }
        });
        thread.start();
    }

    private static String getDomain(String url) throws URISyntaxException {
        url = url.toLowerCase(locale);

        int index = url.indexOf('/', 8); // -> http://(7) and https://(8)
        if (index != -1) {
            url = url.substring(0, index);
        }

        URI uri = new URI(url);
        String domain = uri.getHost();
        if (domain == null) {
            return url;
        }
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    public AdBlock(Context context) {
        File file = new File(context.getDir("filesdir", Context.MODE_PRIVATE) + "/"+FILE);
        if (!file.exists()) {
            //copy hosts.txt from assets if not available
            Log.d("Hosts file","does not exist");
            try {
                AssetManager manager = context.getAssets();
                copyFile(manager.open(FILE), new FileOutputStream(file));
                downloadHosts(context);  //try to update hosts.txt from internet
            } catch(IOException e) {
                Log.e("browser", "Failed to copy asset file", e);
            }
        }

        Calendar time = Calendar.getInstance();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (sp.getBoolean("sp_savedata", false)){
            time.add(Calendar.DAY_OF_YEAR,-7);
        }else{
            time.add(Calendar.DAY_OF_YEAR,-1);
        }

        Date lastModified = new Date(file.lastModified());
        if (lastModified.before(time.getTime())) {
            //update if file is older than a day
            downloadHosts(context);
        }

        if (hosts.isEmpty()) {
            loadHosts(context);
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    boolean isAd(String url) {
        String domain;
        try {
            domain = getDomain(url);
        } catch (URISyntaxException u) {
            return false;
        }
        return hosts.contains(domain.toLowerCase(locale));
    }
}
