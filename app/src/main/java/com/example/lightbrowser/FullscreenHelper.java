package com.example.lightbrowser;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;

public class FullscreenHelper {
    private Activity activity;
    private FrameLayout fullscreenContainer;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private View customView;
    private int originalOrientation;

    public FullscreenHelper(Activity activity, FrameLayout fullscreenContainer) {
        this.activity = activity;
        this.fullscreenContainer = fullscreenContainer;
    }

    public void showCustomView(View view, WebChromeClient.CustomViewCallback callback) {
        if (customView != null) {
            callback.onCustomViewHidden();
            return;
        }
        customView = view;
        customViewCallback = callback;

        // 隐藏原本的 UI，把视频全屏覆盖上去
        fullscreenContainer.setVisibility(View.VISIBLE);
        fullscreenContainer.addView(view, new FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 横屏（可选，视视频本身而定）
        originalOrientation = activity.getRequestedOrientation();
        // activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    public void hideCustomView() {
        if (customView == null) return;
        fullscreenContainer.removeView(customView);
        fullscreenContainer.setVisibility(View.GONE);
        customView = null;

        if (customViewCallback != null) customViewCallback.onCustomViewHidden();
        customViewCallback = null;

        activity.setRequestedOrientation(originalOrientation);
    }

    public boolean isFullscreen() {
        return customView != null;
    }
}
