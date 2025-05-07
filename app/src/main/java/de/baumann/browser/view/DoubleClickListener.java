package de.baumann.browser.view;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;

public abstract class DoubleClickListener implements View.OnClickListener {
    private static final long DOUBLE_CLICK_SPAN = 250;
    private final long delta;
    private long deltaClick;
    private final Handler han = new Handler(Looper.getMainLooper());

    public DoubleClickListener() {
        delta = DOUBLE_CLICK_SPAN;
        deltaClick = 0;
    }

    @Override
    public void onClick(View v) {
        han.removeCallbacksAndMessages(null);
        han.postDelayed(this::onSingleClick, delta);
        if ((SystemClock.elapsedRealtime() - deltaClick) < delta) {
            han.removeCallbacksAndMessages(null);
            onDoubleClick();
        }
        deltaClick = SystemClock.elapsedRealtime();
    }

    public abstract void onDoubleClick();

    public abstract void onSingleClick();
}
