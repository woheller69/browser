package de.baumann.browser.browser;

import android.view.GestureDetector;
import android.view.MotionEvent;
import de.baumann.browser.view.NinjaWebView;

public class NinjaGestureListener extends GestureDetector.SimpleOnGestureListener {
    private final NinjaWebView webView;
    private boolean longPress = true;
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    public NinjaGestureListener(NinjaWebView webView) {
        super();
        this.webView = webView;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if (longPress) {
            webView.onLongPress();
        }
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        longPress = false;
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        longPress = true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        boolean result = false;
        if (e1!=null && e2!=null) {
            try {
                float diffY = e2.getY() - e1.getY();
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeBottom();
                    }
                }
                result = true;

            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        return result;
    }

    public void onSwipeBottom() {
        if (webView.getScrollY()==0)  webView.reload();
    }
}
