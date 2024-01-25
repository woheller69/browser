package de.baumann.browser.browser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

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
import java.nio.file.Files;
import java.util.*;

import de.baumann.browser.R;
import de.baumann.browser.unit.BrowserUnit;

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
                    if (line.startsWith("#") || line.isBlank())  continue;
                    hosts.add(line.toLowerCase(locale));
                }
                in.close();
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                if (sp.getBoolean("customHostListSwitch", false)){
                    String customHostsList = sp.getString("sp_custom_host_list","");
                    Scanner scanner = new Scanner(customHostsList);
                    while (scanner.hasNextLine()){
                        line = scanner.nextLine();
                        if (line.startsWith("#") || line.isBlank())  continue;
                        hosts.add(line.toLowerCase(locale));
                     }
                }
            } catch (IOException i) {
                Log.w("browser", "Error loading adBlockHosts", i);
            }
        });
        thread.start();
    }

    public static void downloadHosts(final Context context) {
        Thread thread = new Thread(() -> {

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            String hostURL = sp.getString("ab_hosts", "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts");

            try {
                URL url = new URL(hostURL);
                Log.d("browser","Download AdBlock hosts");

                SpannableStringBuilder biggerText = new SpannableStringBuilder("\u27f3 " + context.getResources().getString(R.string.setting_title_adblock));
                biggerText.setSpan(new RelativeSizeSpan(1.35f), 0, 1, 0);
                ((Activity) context).runOnUiThread(() -> {
                    Toast.makeText(context, biggerText, Toast.LENGTH_LONG).show();
                });
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
                boolean foundStart = false;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("# Start StevenBlack")) foundStart = true;
                    if (line.startsWith("0.0.0.0 ")) {
                        line=line.substring(8);
                    }
                    if (foundStart || line.startsWith("#")) out.write(line+"\n");
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

    public static String getDomain(String url) throws URISyntaxException {
        String domain = null;
        url = url.toLowerCase(locale);

        // remove view-source: if available
        if (url.startsWith(BrowserUnit.URL_SCHEME_VIEW_SOURCE)) url = url.substring(BrowserUnit.URL_SCHEME_VIEW_SOURCE.length());

        if (url.startsWith(BrowserUnit.URL_SCHEME_HTTP) || url.startsWith(BrowserUnit.URL_SCHEME_HTTPS)){

            int index = url.indexOf('/', 8); // -> http://(7) and https://(8)
            if (index != -1) {
                url = url.substring(0, index);
            }

            URI uri = new URI(url);
            domain = uri.getHost();
        }

        return domain==null ? "" : domain;
    }

    public AdBlock(Context context) {
        File file = new File(context.getDir("filesdir", Context.MODE_PRIVATE) + "/"+FILE);
        if (!file.exists()) {
            //copy hosts.txt from assets if not available
            Log.d("Hosts file","does not exist");
            try {
                AssetManager manager = context.getAssets();
                copyFile(manager.open(FILE), Files.newOutputStream(file.toPath()));
                downloadHosts(context);  //try to update hosts.txt from internet
            } catch(IOException e) {
                Log.e("browser", "Failed to copy asset file", e);
            }
        } else {
            Calendar time = Calendar.getInstance();

            if (BrowserUnit.isUnmeteredConnection(context))
                time.add(Calendar.DAY_OF_YEAR,-3);
            else
                time.add(Calendar.DAY_OF_YEAR,-7);

            Date lastModified = new Date(file.lastModified());
            if (lastModified.before(time.getTime())||getHostsDate(context).equals("")) {  //also download again if something is wrong with the file
                //update if file is older than 7 days
                downloadHosts(context);
            }
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
        if (domain.equals("")) return false;

        boolean domainInHosts = false;

        String[] testDomains = splitDomain(domain);
        for (String i : testDomains){
            if (hosts.contains(i.toLowerCase(locale))) {
                domainInHosts = true;
                break;
            }
        }

        return domainInHosts;
    }

    private static String[] splitDomain(String domain) {

        // Split the domain using the dot as a delimiter
        String[] segments = domain.split("\\.");
        // Domain should not be a Top Level domain, so it must have at least 2 segments
        if (segments.length < 2) return new String[0];
        // Create an array to store subdomains
        String[] subdomains = new String[segments.length-1];
        // Build subdomains from right to left
        StringBuilder currentSubdomain = new StringBuilder();
        for (int i = segments.length - 1; i >= 0; i--) {
            currentSubdomain.insert(0, segments[i]);
            // Store domain if it contains at least 2 segments (not in first iteration)
            if (i < segments.length -1) subdomains[i] = currentSubdomain.toString();
            // Add a dot if it's not the last segment
            if (i > 0) currentSubdomain.insert(0, ".");
        }

        return subdomains;
    }
}
