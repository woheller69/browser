package de.baumann.browser.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import de.baumann.browser.unit.RecordUnit;

class RecordHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "Browser.db";
    private static final int DATABASE_VERSION = 1;

    RecordHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(RecordUnit.CREATE_WHITELIST);
        database.execSQL(RecordUnit.CREATE_JAVASCRIPT);
        database.execSQL(RecordUnit.CREATE_DOM);
        database.execSQL(RecordUnit.CREATE_COOKIE);
        database.execSQL(RecordUnit.CREATE_BOOKMARK);
    }

    // UPGRADE ATTENTION!!!
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        switch(oldVersion) {
            case 1:
                // we want all updates, so no break statement here...
        }
    }
}
