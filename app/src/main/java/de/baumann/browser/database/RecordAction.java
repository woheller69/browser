package de.baumann.browser.database;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import de.baumann.browser.unit.RecordUnit;

public class RecordAction {

    private SQLiteDatabase database;
    private final RecordHelper helper;

    public RecordAction(Context context) {
        this.helper = new RecordHelper(context);
    }
    public void open(boolean rw) { database = rw ? helper.getWritableDatabase() : helper.getReadableDatabase(); }
    public void close() {
        helper.close();
    }

    //BOOKMARK

    public void addBookmark (Record record) {
        if (record == null
                || record.getTitle() == null
                || record.getTitle().trim().isEmpty()
                || record.getURL() == null
                || record.getURL().trim().isEmpty()
                || record.getDesktopMode() == null
                || record.getJavascript() == null
                || record.getDomStorage() == null
                || record.getTime() < 0L) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put(RecordUnit.COLUMN_TITLE, record.getTitle().trim());
        values.put(RecordUnit.COLUMN_URL, record.getURL().trim());
        values.put(RecordUnit.COLUMN_TIME,record.getTime() > 0 ? record.getTime() : System.currentTimeMillis());
        values.put(RecordUnit.COLUMN_ICON_COLOR,record.getIconColor());
        values.put(RecordUnit.COLUMN_DESKTOP_MODE,record.getDesktopMode());
        values.put(RecordUnit.COLUMN_JAVASCRIPT,record.getJavascript());
        values.put(RecordUnit.COLUMN_DOM,record.getDomStorage());

        database.insert(RecordUnit.TABLE_BOOKMARK, null, values);
    }

    public List<Record> listBookmark (Context context, boolean filter, long filterBy) {

        List<Record> list = new LinkedList<>();
        Cursor cursor;
        cursor = database.query(
                RecordUnit.TABLE_BOOKMARK,
                new String[] {
                        RecordUnit.COLUMN_TITLE,
                        RecordUnit.COLUMN_URL,
                        RecordUnit.COLUMN_TIME,
                        RecordUnit.COLUMN_ICON_COLOR,
                        RecordUnit.COLUMN_DESKTOP_MODE,
                        RecordUnit.COLUMN_JAVASCRIPT,
                        RecordUnit.COLUMN_DOM
                },
                null,
                null,
                null,
                null,
                "time"
        );
        if (cursor == null) {
            return list;
        }
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            if (filter) {
                if ((getRecord(cursor).getIconColor()) == filterBy) {
                    list.add(getRecord(cursor));
                }
            } else {
                list.add(getRecord(cursor));
            }
            cursor.moveToNext();
        }
        cursor.close();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String sortBy = Objects.requireNonNull(sp.getString("sort_bookmark", "title"));

        switch (sortBy) {
            case "icon":
                Collections.sort(list, Comparator.comparing(Record::getUpperCaseTitle));
                Collections.sort(list, Comparator.comparingLong(Record::getIconColor));
                break;
            case "date":
                Collections.sort(list, Comparator.comparingLong(Record::getTime));
                Collections.reverse(list);
                break;
            case "title":
                Collections.sort(list, Comparator.comparing(Record::getUpperCaseTitle));
                break;
        }
        Collections.reverse(list);
        return list;
    }

    public Record getBookmarkRecordFromUrl(String url) {
        String selection = RecordUnit.COLUMN_URL + " =?";
        String[] selectionArgs = {url};
        Cursor cursor;
        cursor = database.query(
                RecordUnit.TABLE_BOOKMARK,
                new String[] {
                        RecordUnit.COLUMN_TITLE,
                        RecordUnit.COLUMN_URL,
                        RecordUnit.COLUMN_TIME,
                        RecordUnit.COLUMN_ICON_COLOR,
                        RecordUnit.COLUMN_DESKTOP_MODE,
                        RecordUnit.COLUMN_JAVASCRIPT,
                        RecordUnit.COLUMN_DOM
                },
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        if (cursor == null) { return null;}
        cursor.moveToFirst();
        Record bookmark = getRecord(cursor);
        cursor.close();
        return bookmark;
    }


    // General

    public void addDomain(String domain, String table) {
        if (domain == null || domain.trim().isEmpty()) { return; }
        ContentValues values = new ContentValues();
        values.put(RecordUnit.COLUMN_DOMAIN, domain.trim());
        database.insert(table, null, values);
    }

    public boolean checkDomain(String domain, String table) {
        if (domain == null || domain.trim().isEmpty()) {
            return false;
        }
        Cursor cursor = database.query(
                table,
                new String[] {RecordUnit.COLUMN_DOMAIN},
                RecordUnit.COLUMN_DOMAIN + "=?",
                new String[] {domain.trim()},
                null,
                null,
                null
        );
        if (cursor != null) {
            boolean result = cursor.moveToFirst();
            cursor.close();
            return result;
        }
        return false;
    }

    public void deleteDomain(String domain, String table) {
        if (domain == null || domain.trim().isEmpty()) { return; }
        database.execSQL("DELETE FROM "+ table + " WHERE " + RecordUnit.COLUMN_DOMAIN + " = " + "\"" + domain.trim() + "\"");
    }

    public List<String> listDomains(String table) {
        List<String> list = new ArrayList<>();
        Cursor cursor = database.query(
                table,
                new String[] {RecordUnit.COLUMN_DOMAIN},
                null,
                null,
                null,
                null,
                RecordUnit.COLUMN_DOMAIN
        );
        if (cursor == null) {
            return list;
        }
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            list.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
        return list;
    }

    public boolean checkUrl (String url, String table) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        Cursor cursor = database.query(
                table,
                new String[] {RecordUnit.COLUMN_URL},
                RecordUnit.COLUMN_URL + "=?",
                new String[] {url.trim()},
                null,
                null,
                null
        );
        if (cursor != null) {
            boolean result = cursor.moveToFirst();
            cursor.close();

            return result;
        }
        return false;
    }

    public void deleteURL (String domain, String table) {
        if (domain == null || domain.trim().isEmpty()) { return; }
        database.execSQL("DELETE FROM "+ table + " WHERE " + RecordUnit.COLUMN_URL + " = " + "\"" + domain.trim() + "\"");
    }

    public void clearTable (String table) {
        database.execSQL("DELETE FROM " + table);
    }

    private Record getRecord(Cursor cursor) {
        Record record = new Record();
        record.setTitle(cursor.getString(0));
        record.setURL(cursor.getString(1));
        record.setTime(cursor.getLong(2));
        record.setIconColor(cursor.getInt(3));
        record.setDesktopMode(cursor.getInt(4)>0);
        record.setJavascript(cursor.getInt(5)>0);
        record.setDomStorage(cursor.getInt(6)>0);

        return record;
    }

    public List<Record> listEntries (Activity activity) {
        List<Record> list = new ArrayList<>();
        RecordAction action = new RecordAction(activity);
        action.open(false);
        list.addAll(action.listBookmark(activity, false, 0)); //move bookmarks to top of list
        action.close();
        return list;
    }
}