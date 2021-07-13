package de.baumann.browser.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;

public class FaviconHelper extends SQLiteOpenHelper {
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "favicon.db";

    // Table Name
    private static final String TABLE_FAVICON = "Favicon";

    // Column names
    private static final String DOMAIN = "domain";
    private static final String IMAGE = "image";

    // create Table statement
    private static final String CREATE_TABLE_FAVICON = "CREATE TABLE " + TABLE_FAVICON + "("+
            DOMAIN + " TEXT," +
            IMAGE + " BLOB);";

    public FaviconHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(CREATE_TABLE_FAVICON);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVICON);
        onCreate(db);
    }

    public void addFavicon( String url, Bitmap bitmap) throws SQLiteException {
        String domain=getDomain(url);
        if (domain==null || bitmap==null) return;

        byte[] byteimage= convertBytes(bitmap);
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new  ContentValues();
        values.put(DOMAIN,     domain);
        values.put(IMAGE,     byteimage);
        database.insert(TABLE_FAVICON, null, values );
        database.close();
    }

    public void deleteFavicon( String url) throws SQLiteException {
        String domain=getDomain(url);
        if (domain==null) return;
        SQLiteDatabase database = this.getWritableDatabase();
        database.delete(TABLE_FAVICON, DOMAIN + " = ?", new String[]{domain});
        database.close();
    }

    public void deleteAllFavicons() throws SQLiteException {
        SQLiteDatabase database = this.getWritableDatabase();
        database.delete(TABLE_FAVICON, null, null);
        database.close();
    }

    public Bitmap getFavicon(String url){
        String domain=getDomain(url);
        if (domain==null) return null;

        SQLiteDatabase database = this.getReadableDatabase();

        Cursor cursor;
        cursor = database.query(TABLE_FAVICON,
                new String[]{DOMAIN,
                        IMAGE},
                DOMAIN + " = ?",
                new String[]{domain}, null, null, null, null);

        byte[] image;

        if (cursor != null && cursor.moveToFirst()){
            image = cursor.getBlob(1);
            cursor.close();
            database.close();
            return getBitmap(image);
        }else{
            cursor.close();
            database.close();
            return null;
        }
    }

    public static byte[] convertBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        return stream.toByteArray();
    }

    public static Bitmap getBitmap(byte[] byteimage) {
        return BitmapFactory.decodeByteArray(byteimage, 0, byteimage.length);
    }

    public static String getDomain(String url){
        try {
            return new URI(url).getHost();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

}
