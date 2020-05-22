package de.baumann.browser.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import de.baumann.browser.unit.RecordUnit;

public class RecordHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "Ninja4.db";
    private static final int DATABASE_VERSION = 4;

    RecordHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(RecordUnit.CREATE_HISTORY);
        database.execSQL(RecordUnit.CREATE_WHITELIST);
        database.execSQL(RecordUnit.CREATE_JAVASCRIPT);
        database.execSQL(RecordUnit.CREATE_COOKIE);
        database.execSQL(RecordUnit.CREATE_GRID);
        database.execSQL(RecordUnit.CREATE_BOOKMARK);
        database.execSQL(RecordUnit.CREATE_REMOTE);
        database.execSQL(RecordUnit.CREATE_TAB);
    }

    // UPGRADE ATTENTION!!!
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        switch(oldVersion) {
            case 1:
                database.execSQL(RecordUnit.CREATE_BOOKMARK);
            case 2:
                database.execSQL(RecordUnit.CREATE_REMOTE);
            case 3:
                database.execSQL(RecordUnit.CREATE_TAB);
                // we want all updates, so no break statement here...
        }
    }
}
