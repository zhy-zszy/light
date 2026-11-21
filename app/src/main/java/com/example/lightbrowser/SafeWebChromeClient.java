package com.example.lightbrowser;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

public class SafeWebChromeClient extends WebChromeClient {
    private Activity activity;
    private ProgressBar progressBar;
    private ValueCallback<Uri[]> filePathCallback;
    private FullscreenHelper fullscreenHelper;

    public SafeWebChromeClient(Activity activity, ProgressBar progressBar, FrameLayout fullscreenContainer) {
        this.activity = activity;
        this.progressBar = progressBar;
        this.fullscreenHelper = new FullscreenHelper(activity, fullscreenContainer);
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        if (progressBar != null) {
            progressBar.setProgress(newProgress);
        }
    }

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        fullscreenHelper.showCustomView(view, callback);
    }

    @Override
    public void onHideCustomView() {
        fullscreenHelper.hideCustomView();
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                     FileChooserParams fileChooserParams) {
        if (this.filePathCallback != null) {
            this.filePathCallback.onReceiveValue(null);
        }
        this.filePathCallback = filePathCallback;

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        // 尝试获取 acceptTypes
        String[] acceptTypes = fileChooserParams.getAcceptTypes();
        if (acceptTypes != null && acceptTypes.length > 0) {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, acceptTypes);
        }

        try {
            activity.startActivityForResult(Intent.createChooser(intent, "选择文件"), Utils.REQ_FILE_CHOOSER);
        } catch (Exception e) {
            Toast.makeText(activity, "无法打开文件选择器", Toast.LENGTH_SHORT).show();
            this.filePathCallback = null;
            return false;
        }
        return true;
    }

    public void onFileChooserResult(int resultCode, Intent data) {
        if (filePathCallback != null) {
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && data != null) {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    public boolean isFullscreen() {
        return fullscreenHelper.isFullscreen();
    }

    public void exitFullscreen() {
        fullscreenHelper.hideCustomView();
    }
}
