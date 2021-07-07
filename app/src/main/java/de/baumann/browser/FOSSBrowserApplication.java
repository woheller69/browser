package de.baumann.browser;
import android.app.Application;

public class FOSSBrowserApplication extends Application {

    @Override
    public String getPackageName() {
        try {
            throw new Exception();
        } catch (Exception e) {
            StackTraceElement[] elements = e.getStackTrace();
            for (StackTraceElement element : elements) {
                if (element.getClassName().startsWith("android.webkit.")) {
                    return "com.duckduckgo.mobile.android";
                }
            }
        }
        return super.getPackageName();
    }
}
