package de.baumann.browser.database;

public class Record {

    private  Boolean isJavascript;
    private Boolean isDesktopMode;
    private Boolean isDomStorage;
    private int iconColor;
    private String title;
    private long time;
    private String url;

    public int getIconColor() { return iconColor; }
    public void setIconColor(int iconColor) {this.iconColor = iconColor; }

    public Boolean getDesktopMode() {return isDesktopMode; }
    public void setDesktopMode(Boolean desktopMode) {isDesktopMode = desktopMode; }

    public Boolean getDomStorage() {return isDomStorage; }
    public void setDomStorage(Boolean domStorage) {isDomStorage = domStorage; }

    public Boolean getJavascript() {return isJavascript; }
    public void setJavascript(Boolean javascript) {isJavascript = javascript; }

    public String getUpperCaseTitle() { return  title.toUpperCase();}
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getURL() {
        return url;
    }
    public void setURL(String url) {
        this.url = url;
    }

    public long getTime() {
        return time;
    }
    public void setTime(long time) {
        this.time = time;
    }

    public Record() {
        this.title = null;
        this.url = null;
        this.time = 0L;
        this.isDesktopMode = null;
        this.isJavascript = null;
        this.isDomStorage = null;
        this.iconColor = 0;
    }

    public Record(String title, String url, long time, Boolean DesktopMode, Boolean Javascript, Boolean DomStorage, int iconColor) {
        this.title = title;
        this.url = url;
        this.time = time;
        this.isDesktopMode = DesktopMode;
        this.isJavascript = Javascript;
        this.isDomStorage = DomStorage;
        this.iconColor = iconColor;
    }
}
