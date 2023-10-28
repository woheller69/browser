package de.baumann.browser.browser;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

import de.baumann.browser.database.RecordAction;
import de.baumann.browser.unit.RecordUnit;

public class DOM {

    private static final List<String> whitelistDOM = new ArrayList<>();

    private synchronized static void loadDomains(Context context) {
        RecordAction action = new RecordAction(context);
        action.open(false);
        whitelistDOM.clear();
        whitelistDOM.addAll(action.listDomains(RecordUnit.TABLE_DOM));
        action.close();
    }

    private final Context context;

    public DOM(Context context) {
        this.context = context;

        loadDomains(context);
    }

    public boolean isWhite(String url) {
        for (String domain : whitelistDOM) {
            if (url != null && url.contains(domain)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void addDomain(String domain) {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.addDomain(domain, RecordUnit.TABLE_DOM);
        action.close();
        whitelistDOM.add(domain);
    }

    public synchronized void removeDomain(String domain) {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.deleteDomain(domain, RecordUnit.TABLE_DOM);
        action.close();
        whitelistDOM.remove(domain);
    }

    public synchronized void clearDomains() {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.clearTable(RecordUnit.TABLE_DOM);
        action.close();
        whitelistDOM.clear();
    }
}
