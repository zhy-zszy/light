package com.example.lightbrowser;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.WebView;
import android.widget.ImageButton;

import org.json.JSONArray;

public class UiManager {
    private Context context;
    private ImageButton btnBack;
    private ImageButton btnForward;
    private ImageButton btnBookmark;
    private BrowserManager browserManager;

    public UiManager(Context context, ImageButton btnBack, ImageButton btnForward,
                     ImageButton btnBookmark, BrowserManager browserManager) {
        this.context = context;
        this.btnBack = btnBack;
        this.btnForward = btnForward;
        this.btnBookmark = btnBookmark;
        this.browserManager = browserManager;
    }

    public void updateNavButtons(WebView webView) {
        if (webView == null) return;
        boolean canBack = webView.canGoBack();
        boolean canForward = webView.canGoForward();
        btnBack.setEnabled(canBack);
        btnBack.setAlpha(canBack ? 1.0f : 0.5f);
        btnForward.setEnabled(canForward);
        btnForward.setAlpha(canForward ? 1.0f : 0.5f);
    }

    public void updateBookmarkButton(String url) {
        if (url == null) return;
        SharedPreferences prefs = Utils.prefs(context);
        String json = prefs.getString("bookmarks_json", "");
        boolean isBookmarked = false;
        if (json.length() > 0) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    if (url.equals(arr.getJSONObject(i).optString("url"))) {
                        isBookmarked = true;
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }
        btnBookmark.setImageResource(isBookmarked ? R.drawable.ic_star : R.drawable.ic_star_border);
        btnBookmark.setColorFilter(context.getResources().getColor(R.color.primary));
    }
}
