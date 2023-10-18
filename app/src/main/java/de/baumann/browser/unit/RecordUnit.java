package de.baumann.browser.unit;

public class RecordUnit {
    public static final String TABLE_WHITELIST = "WHITELIST";
    public static final String TABLE_JAVASCRIPT = "JAVASCRIPT";
    public static final String TABLE_COOKIE = "COOKIE";
    public static final String TABLE_REMOTE = "REMOTE";

    public static final String TABLE_BOOKMARK = "BOOKAMRK";

    public static final String COLUMN_TITLE = "TITLE";
    public static final String COLUMN_URL = "URL";
    public static final String COLUMN_TIME = "TIME";
    public static final String COLUMN_DOMAIN = "DOMAIN";

    public static final String CREATE_BOOKMARK = "CREATE TABLE "
            + TABLE_BOOKMARK
            + " ("
            + " " + COLUMN_TITLE + " text,"
            + " " + COLUMN_URL + " text,"
            + " " + COLUMN_TIME + " integer"
            + ")";

    public static final String CREATE_WHITELIST = "CREATE TABLE "
            + TABLE_WHITELIST
            + " ("
            + " " + COLUMN_DOMAIN + " text"
            + ")";

    public static final String CREATE_JAVASCRIPT = "CREATE TABLE "
            + TABLE_JAVASCRIPT
            + " ("
            + " " + COLUMN_DOMAIN + " text"
            + ")";

    public static final String CREATE_COOKIE = "CREATE TABLE "
            + TABLE_COOKIE
            + " ("
            + " " + COLUMN_DOMAIN + " text"
            + ")";

    public static final String CREATE_REMOTE = "CREATE TABLE "
            + TABLE_REMOTE
            + " ("
            + " " + COLUMN_DOMAIN + " text"
            + ")";

}
