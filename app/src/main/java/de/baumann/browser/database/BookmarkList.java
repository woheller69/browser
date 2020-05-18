/*
    This file is part of FOSS browser.

    FOSS browser is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FOSS browser is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the Diaspora Native WebApp.

    If not, see <http://www.gnu.org/licenses/>.
 */

package de.baumann.browser.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.preference.PreferenceManager;

import java.util.Objects;


public class BookmarkList {

    //define static variable
    private static final int dbVersion = 7;
    private static final String dbName = "pass_DB_v01.db";
    private static final String dbTable = "pass";

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context,dbName,null, dbVersion);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS "+dbTable+" (_id INTEGER PRIMARY KEY autoincrement, pass_title, pass_content, pass_icon, pass_attachment, pass_creation, UNIQUE(pass_content))");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS "+dbTable);
            onCreate(db);
        }
    }

    //establish connection with SQLiteDataBase
    private final Context c;
    private SQLiteDatabase sqlDb;

    public BookmarkList(Context context) {
        this.c = context;
    }
    public void open() throws SQLException {
        DatabaseHelper dbHelper = new DatabaseHelper(c);
        sqlDb = dbHelper.getWritableDatabase();
    }

    //fetch data
    public Cursor fetchAllData(Context activity) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        String[] columns = new String[]{"_id", "pass_title", "pass_content", "pass_icon","pass_attachment","pass_creation"};
        switch (Objects.requireNonNull(sp.getString("sortDBB", "title"))) {
            case "title":
                return sqlDb.query(dbTable, columns, null, null, null, null, "pass_title" + " COLLATE NOCASE DESC;");

            case "icon": {
                String orderBy = "pass_creation" + " COLLATE NOCASE DESC;" + "," + "pass_title" + " COLLATE NOCASE ASC;";
                return sqlDb.query(dbTable, columns, null, null, null, null, orderBy);
            }
        }
        return null;
    }
}