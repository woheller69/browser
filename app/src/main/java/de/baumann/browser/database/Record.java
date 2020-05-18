package de.baumann.browser.database;

public class Record {

    private String title;
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    private String url;
    public String getURL() {
        return url;
    }
    public void setURL(String url) {
        this.url = url;
    }

    private long time;
    public long getTime() {
        return time;
    }
    public void setTime(long time) {
        this.time = time;
    }

    private int ordinal;
    public int getOrdinal() {
        return ordinal;
    }

    Record() {
        this.title = null;
        this.url = null;
        this.time = 0L;
        this.ordinal = -1;
    }

    public Record(String title, String url, long time, int ordinal) {
        this.title = title;
        this.url = url;
        this.time = time;
        this.ordinal = ordinal;
    }
}
