package com.example.lightbrowser;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class FeatureManager {
    private Context context;
    private WebView webView;
    private FrameLayout rootLayout;
    private LinearLayout addressBar;
    private LinearLayout bottomBar;
    private EditText etAddress;

    public FeatureManager(Context context, WebView webView, FrameLayout rootLayout,
                          LinearLayout addressBar, LinearLayout bottomBar, EditText etAddress) {
        this.context = context;
        this.webView = webView;
        this.rootLayout = rootLayout;
        this.addressBar = addressBar;
        this.bottomBar = bottomBar;
        this.etAddress = etAddress;
    }

    public void applyNightMode(boolean enable) {
        if (enable) {
            rootLayout.setBackgroundColor(Color.parseColor("#1a1a2e"));
            addressBar.setBackgroundResource(R.drawable.glass_dark_bg);
            bottomBar.setBackgroundResource(R.drawable.glass_dark_bg);
            etAddress.setTextColor(Color.WHITE);
            etAddress.setHintTextColor(Color.GRAY);
            
            // 更完善的夜间模式 CSS 注入
            webView.loadUrl("javascript:(function(){" +
                "var s=document.getElementById('lb-night-mode');" +
                "if(!s){" +
                "s=document.createElement('style');s.id='lb-night-mode';" +
                "s.innerHTML='html,body{background-color:#1a1a2e!important;color:#e0e0e0!important;} " +
                "div,span,p,h1,h2,h3,h4,h5,h6,li,td,th{color:#e0e0e0!important;} " +
                "a{color:#64b5f6!important;} " +
                "input,textarea{background:#16213e!important;color:#e0e0e0!important;} " +
                "img,video{opacity:0.7!important;}' " +
                "document.head.appendChild(s);}})()");
        } else {
            rootLayout.setBackgroundColor(Color.WHITE);
            addressBar.setBackgroundResource(R.drawable.glass_gradient_bg);
            bottomBar.setBackgroundResource(R.drawable.glass_gradient_bg);
            etAddress.setTextColor(Color.BLACK);
            etAddress.setHintTextColor(Color.GRAY);
            webView.loadUrl("javascript:(function(){var s=document.getElementById('lb-night-mode');if(s)s.remove();})()");
        }
    }
}