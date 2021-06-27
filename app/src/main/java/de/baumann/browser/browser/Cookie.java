package de.baumann.browser.browser;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.unit.RecordUnit;

public class Cookie {

    private final Context context;
    private static final List<String> whitelistCookie = new ArrayList<>();

    private synchronized static void loadDomains(Context context) {
        RecordAction action = new RecordAction(context);
        action.open(false);
        whitelistCookie.clear();
        whitelistCookie.addAll(action.listDomains(RecordUnit.TABLE_COOKIE));
        action.close();
    }

    public Cookie(Context context) {
        this.context = context;
        loadDomains(context);
    }

    public boolean isWhite(String url) {
        for (String domain : whitelistCookie) {
            if (url != null && url.contains(domain)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void addDomain(String domain) {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.addDomain(domain, RecordUnit.TABLE_COOKIE);
        action.close();
        whitelistCookie.add(domain);
    }

    public synchronized void removeDomain(String domain) {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.deleteDomain(domain, RecordUnit.TABLE_COOKIE);
        action.close();
        whitelistCookie.remove(domain);
    }

    public synchronized void clearDomains() {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.clearTable(RecordUnit.TABLE_COOKIE);
        action.close();
        whitelistCookie.clear();
    }
}
