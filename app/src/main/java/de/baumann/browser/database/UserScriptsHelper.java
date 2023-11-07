package de.baumann.browser.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class UserScriptsHelper extends SQLiteOpenHelper {
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "scripts.db";

    // Table Name
    private static final String TABLE_SCRIPTS = "Scripts";

    // Column names
    private static final String SCRIPT = "script";
    private static final String TYPE = "type";
    private static final String RANK = "rank";
    private static final String ID = "id";
    private static final String ACTIVE = "active";

    // create Table statement
    private static final String CREATE_TABLE_SCRIPTS = "CREATE TABLE " + TABLE_SCRIPTS + "("+
            ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            SCRIPT + " TEXT," +
            TYPE + " TEXT," +
            RANK + " INTEGER," +
            ACTIVE + " BIT);";

    public UserScriptsHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SCRIPTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch(oldVersion) {
            case 1:
                // we want all updates, so no break statement here...
        }
    }

    public synchronized int addScript( UserScript userScript) throws SQLiteException {

        SQLiteDatabase database = this.getWritableDatabase();

        ContentValues values = new  ContentValues();
        values.put(SCRIPT,  userScript.getScript());
        values.put(TYPE,    userScript.getType());
        values.put(RANK,    userScript.getRank());
        values.put(ACTIVE,  userScript.isActive());
        int id = (int) database.insert(TABLE_SCRIPTS, null, values );
        database.close();
        return id;
    }


    public synchronized void updateScript(UserScript userScript) {
        SQLiteDatabase database = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(ID, userScript.getId());
        values.put(SCRIPT, userScript.getScript());
        values.put(TYPE,userScript.getType());
        values.put(RANK,userScript.getRank());
        values.put(ACTIVE,userScript.isActive());

        database.update(TABLE_SCRIPTS, values, ID + " = ?",
                new String[]{String.valueOf(userScript.getId())});
        database.close();
    }

    public synchronized void deleteScript( int id) throws SQLiteException {
        SQLiteDatabase database = this.getWritableDatabase();
        database.delete(TABLE_SCRIPTS, ID + " = ?", new String[]{Integer.toString(id)});
        database.close();
    }

    public synchronized void deleteAllScripts() throws SQLiteException {
        SQLiteDatabase database = this.getWritableDatabase();
        database.delete(TABLE_SCRIPTS, null, null);
        database.close();
    }

    public synchronized List<UserScript> getAllScripts(){
        SQLiteDatabase database = this.getReadableDatabase();
        List<UserScript> result = new ArrayList<>();
        Cursor cursor;

        cursor = database.query(TABLE_SCRIPTS,
                new String[]{ID, SCRIPT, TYPE, RANK, ACTIVE},
                null, null, null, null, RANK);

        UserScript userScript;

        if (cursor.moveToFirst()) {
            do {
                userScript = new UserScript();
                userScript.setId(Integer.parseInt(cursor.getString(0)));
                userScript.setScript((cursor.getString(1)));
                userScript.setType(cursor.getString(2));
                userScript.setRank(Integer.parseInt(cursor.getString(3)));
                userScript.setActive(Integer.parseInt(cursor.getString(4))==1);

                result.add(userScript);
            } while (cursor.moveToNext());
        }

        cursor.close();
        database.close();
        return result;
    }

    public synchronized List<UserScript> getActiveScriptsByType(String type){
        SQLiteDatabase database = this.getReadableDatabase();
        List<UserScript> result = new ArrayList<>();
        Cursor cursor;

        cursor = database.query(TABLE_SCRIPTS,
                new String[]{ID, SCRIPT, TYPE, RANK, ACTIVE},
                TYPE + "=?" + " AND " + ACTIVE + "=?", new String[] {type,"1"}, null, null, RANK);

        UserScript userScript;

        if (cursor.moveToFirst()) {
            do {
                userScript = new UserScript();
                userScript.setId(Integer.parseInt(cursor.getString(0)));
                userScript.setScript((cursor.getString(1)));
                userScript.setType(cursor.getString(2));
                userScript.setRank(Integer.parseInt(cursor.getString(3)));
                userScript.setActive(Integer.parseInt(cursor.getString(4))==1);

                result.add(userScript);
            } while (cursor.moveToNext());
        }

        cursor.close();
        database.close();
        return result;
    }

    public synchronized int getNumScripts(){
        SQLiteDatabase database = this.getReadableDatabase();

        Cursor cursor;
        cursor = database.query(TABLE_SCRIPTS,
                new String[]{SCRIPT},null, null, null, null, null);

        int result=cursor.getCount();
        cursor.close();
        database.close();
        return result;
    }

    public int getMaxRank() {
        List<UserScript> userScripts = getAllScripts();
        int maxRank = 0;
        for (UserScript script : userScripts) {
            if (script.getRank() > maxRank) maxRank = script.getRank();
        }
        return maxRank;
    }

}