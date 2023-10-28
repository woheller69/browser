package de.baumann.browser.unit;

public class RecordUnit {
    public static final String TABLE_BOOKMARK = "BOOKMARK";
    public static final String TABLE_WHITELIST = "WHITELIST";
    public static final String TABLE_JAVASCRIPT = "JAVASCRIPT";
    public static final String TABLE_COOKIE = "COOKIE";
    public static final String TABLE_DOM = "DOM";

    public static final String COLUMN_TITLE = "TITLE";
    public static final String COLUMN_URL = "URL";
    public static final String COLUMN_TIME = "TIME";
    public static final String COLUMN_DOMAIN = "DOMAIN";
    public static final String COLUMN_ICON_COLOR = "ICON_COLOR";
    public static final String COLUMN_DESKTOP_MODE = "DESKTOP_MODE";
    public static final String COLUMN_JAVASCRIPT = "JAVASCRIPT";
    public static final String COLUMN_DOM = "DOM";

    public static final String CREATE_BOOKMARK = "CREATE TABLE "
            + TABLE_BOOKMARK
            + " ("
            + " " + COLUMN_TITLE + " text,"
            + " " + COLUMN_URL + " text,"
            + " " + COLUMN_TIME + " long,"
            + " " + COLUMN_ICON_COLOR + " integer,"
            + " " + COLUMN_DESKTOP_MODE + " bit,"
            + " " + COLUMN_JAVASCRIPT + " bit,"
            + " " + COLUMN_DOM + " bit"
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

    public static final String CREATE_DOM = "CREATE TABLE "
            + TABLE_DOM
            + " ("
            + " " + COLUMN_DOMAIN + " text"
            + ")";

}
