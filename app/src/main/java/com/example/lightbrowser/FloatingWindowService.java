package com.example.lightbrowser;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.net.URLDecoder;
import java.util.List;

public class FloatingWindowService extends android.app.Service {
    private static final String TAG = "LightBrowserFloat";
    private static final int MIN_SIZE = 100;

    private WindowManager windowManager;
    private View floatingView;
    private WebView floatingWebView;
    private EditText floatingAddress;
    private ImageButton floatingBack, floatingForward, floatingRefresh;
    private ImageButton floatingMenuBtn, floatingResizeBtn, floatingCloseBtn;
    private LinearLayout floatingTitleBar;
    private WindowManager.LayoutParams params;

    private boolean isResizing = false;
    private boolean isDesktopMode = false;
    private float initialTouchX, initialTouchY;
    private int initialWidth, initialHeight;
    private String mobileUserAgent;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        initFloatingWindow();
    }

    private void initFloatingWindow() {
        LayoutInflater inflater = LayoutInflater.from(this);
        floatingView = inflater.inflate(R.layout.layout_floating_window, null);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
            600, 800,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            android.graphics.PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 50;
        params.y = 100;
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;

        windowManager.addView(floatingView, params);
        initViews();
        setupWebView();
        setupListeners();
        loadScripts();
    }

    private void initViews() {
        floatingWebView = (WebView) floatingView.findViewById(R.id.floating_webview);
        floatingAddress = (EditText) floatingView.findViewById(R.id.floating_address);
        floatingBack = (ImageButton) floatingView.findViewById(R.id.floating_back);
        floatingForward = (ImageButton) floatingView.findViewById(R.id.floating_forward);
        floatingRefresh = (ImageButton) floatingView.findViewById(R.id.floating_refresh);
        floatingMenuBtn = (ImageButton) floatingView.findViewById(R.id.floating_menu_btn);
        floatingResizeBtn = (ImageButton) floatingView.findViewById(R.id.floating_resize_btn);
        floatingCloseBtn = (ImageButton) floatingView.findViewById(R.id.floating_close_btn);
        floatingTitleBar = (LinearLayout) floatingView.findViewById(R.id.floating_title_bar);
    }

    private void setupWebView() {
        WebSettings ws = floatingWebView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setDatabaseEnabled(true);
        ws.setCacheMode(WebSettings.LOAD_DEFAULT);
        ws.setSupportZoom(true);
        ws.setBuiltInZoomControls(true);
        ws.setDisplayZoomControls(false);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setJavaScriptCanOpenWindowsAutomatically(true);
        ws.setSupportMultipleWindows(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setAllowFileAccess(false);
        ws.setAllowFileAccessFromFileURLs(false);
        ws.setAllowUniversalAccessFromFileURLs(false);
        ws.setAllowContentAccess(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ws.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }

        floatingWebView.removeJavascriptInterface("searchBoxJavaBridge_");
        floatingWebView.removeJavascriptInterface("accessibility");
        floatingWebView.removeJavascriptInterface("accessibilityTraversal");

        mobileUserAgent = ws.getUserAgentString();

        SharedPreferences prefs = Utils.prefs(this);
        isDesktopMode = prefs.getBoolean("desktop_mode", false);
        if (isDesktopMode) {
            applyFloatingDesktopMode(ws);
        }

        floatingWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                floatingAddress.setText(url);
                updateNavButtons();
                List<String> scripts = ScriptRepository.get().getCached();
                for (String s : scripts) {
                    try { view.loadUrl("javascript:" + s); } catch (Exception ignored) {}
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("http://") || url.startsWith("https://")) return false;
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(FloatingWindowService.this, R.string.cannot_open_link, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        floatingWebView.setWebChromeClient(new WebChromeClient());

        floatingWebView.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                    String mimetype, long contentLength) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(FloatingWindowService.this, "需要存储权限才能下载", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                try {
                    String fileName = smartGuessFileName(url, contentDisposition, mimetype);
                    String fixedMime = Utils.guessMime(url, mimetype, fileName);

                    android.app.DownloadManager.Request request =
                        new android.app.DownloadManager.Request(Uri.parse(url));
                    request.setMimeType(fixedMime);
                    if (userAgent != null && userAgent.length() > 0) {
                        request.addRequestHeader("User-Agent", userAgent);
                    }
                    request.addRequestHeader("Accept", "*/*");
                    request.addRequestHeader("Referer", floatingWebView != null ? floatingWebView.getUrl() : url);
                    request.setTitle(getString(R.string.downloading));
                    request.setDescription(fileName);
                    request.setNotificationVisibility(
                        android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                    request.setAllowedNetworkTypes(
                        android.app.DownloadManager.Request.NETWORK_WIFI
                        | android.app.DownloadManager.Request.NETWORK_MOBILE);

                    android.app.DownloadManager dm =
                        (android.app.DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (dm != null) {
                        long downloadId = dm.enqueue(request);
                        DownloadManagerActivity.addDownloadRecord(
                            FloatingWindowService.this, downloadId, fileName, url);
                        Toast.makeText(FloatingWindowService.this, "开始下载: " + fileName, Toast.LENGTH_SHORT).show();
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "Download SecurityException", e);
                    Toast.makeText(FloatingWindowService.this, "下载权限被拒绝", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Download failed", e);
                    Toast.makeText(FloatingWindowService.this, "下载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        String homepage = prefs.getString("homepage", Utils.DEFAULT_HOMEPAGE);
        if (Utils.DEFAULT_HOMEPAGE.equals(homepage)) {
            floatingWebView.loadDataWithBaseURL(homepage, HomePageHtml.getHtml(this), "text/html", "UTF-8", null);
        } else {
            floatingWebView.loadUrl(homepage);
        }
    }

    private String smartGuessFileName(String url, String contentDisposition, String mimetype) {
        String fileName = null;
        if (contentDisposition != null && contentDisposition.length() > 0) {
            String cd = contentDisposition.toLowerCase();
            int idx = cd.indexOf("filename=");
            if (idx >= 0) {
                int start = idx + 9;
                int end = contentDisposition.indexOf(";", start);
                if (end < 0) end = contentDisposition.length();
                fileName = contentDisposition.substring(start, end).trim();
                if (fileName.startsWith("\"") && fileName.endsWith("\"")) {
                    fileName = fileName.substring(1, fileName.length() - 1);
                }
                try { fileName = URLDecoder.decode(fileName, "UTF-8"); } catch (Exception ignored) {}
            }
        }
        if (fileName == null || fileName.length() == 0) {
            try {
                String path = url;
                int qIdx = path.indexOf("?");
                if (qIdx > 0) path = path.substring(0, qIdx);
                int hashIdx = path.indexOf("#");
                if (hashIdx > 0) path = path.substring(0, hashIdx);
                int lastSlash = path.lastIndexOf("/");
                if (lastSlash >= 0 && lastSlash < path.length() - 1) {
                    fileName = path.substring(lastSlash + 1);
                    try { fileName = URLDecoder.decode(fileName, "UTF-8"); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }
        if (fileName == null || fileName.length() == 0) {
            fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype);
        }
        return Utils.sanitizeFileName(fileName);
    }

    private void setupListeners() {
        floatingTitleBar.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float touchX, touchY;
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int)(event.getRawX() - touchX);
                        params.y = initialY + (int)(event.getRawY() - touchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });

        floatingBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { if (floatingWebView.canGoBack()) floatingWebView.goBack(); }
        });
        floatingForward.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { if (floatingWebView.canGoForward()) floatingWebView.goForward(); }
        });
        floatingRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { floatingWebView.reload(); }
        });
        floatingAddress.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            public boolean onEditorAction(android.widget.TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    loadFloatingUrl();
                    return true;
                }
                return false;
            }
        });

        floatingAddress.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    params.flags = params.flags & ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                    windowManager.updateViewLayout(floatingView, params);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.showSoftInput(floatingAddress, InputMethodManager.SHOW_IMPLICIT);
                } else {
                    params.flags = params.flags | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                    windowManager.updateViewLayout(floatingView, params);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(floatingAddress.getWindowToken(), 0);
                }
            }
        });

        floatingMenuBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showFloatingMenu(); }
        });
        floatingCloseBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { stopSelf(); }
        });

        floatingResizeBtn.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isResizing = true;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        initialWidth = params.width;
                        initialHeight = params.height;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (isResizing) {
                            int newWidth = initialWidth + (int)(event.getRawX() - initialTouchX);
                            int newHeight = initialHeight + (int)(event.getRawY() - initialTouchY);
                            params.width = Math.max(MIN_SIZE, newWidth);
                            params.height = Math.max(MIN_SIZE, newHeight);
                            windowManager.updateViewLayout(floatingView, params);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        isResizing = false;
                        return true;
                }
                return false;
            }
        });
    }

    private void loadFloatingUrl() {
        String input = floatingAddress.getText().toString().trim();
        if (input.length() == 0) return;
        String url = PageUtils.resolveInput(this, input);
        floatingWebView.loadUrl(url);
    }

    private void updateNavButtons() {
        floatingBack.setEnabled(floatingWebView.canGoBack());
        floatingBack.setAlpha(floatingWebView.canGoBack() ? 1.0f : 0.5f);
        floatingForward.setEnabled(floatingWebView.canGoForward());
        floatingForward.setAlpha(floatingWebView.canGoForward() ? 1.0f : 0.5f);
    }

    private void showFloatingMenu() {
        final String[] items = new String[] {
            getString(R.string.homepage),
            getString(R.string.fullscreen),
            isDesktopMode ? getString(R.string.mobile_mode) : getString(R.string.desktop_mode),
            getString(R.string.refresh),
            getString(R.string.share),
            getString(R.string.close_floating)
        };

        AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_Holo_Light_Dialog)
            .setTitle(R.string.menu)
            .setItems(items, new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:
                            String homepage = Utils.getString(FloatingWindowService.this, "homepage", Utils.DEFAULT_HOMEPAGE);
                            if (Utils.DEFAULT_HOMEPAGE.equals(homepage)) {
                                floatingWebView.loadDataWithBaseURL(homepage, HomePageHtml.getHtml(), "text/html", "UTF-8", null);
                            } else {
                                floatingWebView.loadUrl(homepage);
                            }
                            break;
                        case 1:
                            params.width = WindowManager.LayoutParams.MATCH_PARENT;
                            params.height = WindowManager.LayoutParams.MATCH_PARENT;
                            params.x = 0;
                            params.y = 0;
                            windowManager.updateViewLayout(floatingView, params);
                            break;
                        case 2:
                            toggleFloatingDesktopMode(!isDesktopMode);
                            break;
                        case 3:
                            floatingWebView.reload();
                            break;
                        case 4:
                            shareFloatingPage();
                            break;
                        case 5:
                            stopSelf();
                            break;
                    }
                }
            })
            .create();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        } else {
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }
        dialog.show();
    }

    private void toggleFloatingDesktopMode(boolean desktop) {
        isDesktopMode = desktop;
        WebSettings ws = floatingWebView.getSettings();
        if (desktop) {
            applyFloatingDesktopMode(ws);
        } else {
            applyFloatingMobileMode(ws);
        }
        floatingWebView.reload();
        Toast.makeText(this, desktop ? R.string.desktop_mode : R.string.mobile_mode, Toast.LENGTH_SHORT).show();
    }

    private void applyFloatingDesktopMode(WebSettings ws) {
        ws.setUserAgentString(Utils.DESKTOP_UA);
        ws.setUseWideViewPort(true);
        ws.setLoadWithOverviewMode(false);
        ws.setSupportMultipleWindows(true);
        ws.setJavaScriptCanOpenWindowsAutomatically(true);
        ws.setDefaultZoom(WebSettings.ZoomDensity.MEDIUM);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ws.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
    }

    private void applyFloatingMobileMode(WebSettings ws) {
        ws.setUserAgentString(mobileUserAgent);
        ws.setUseWideViewPort(true);
        ws.setLoadWithOverviewMode(true);
        ws.setSupportMultipleWindows(false);
        ws.setJavaScriptCanOpenWindowsAutomatically(false);
        ws.setDefaultZoom(WebSettings.ZoomDensity.FAR);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ws.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }
    }

    private void shareFloatingPage() {
        String url = floatingWebView.getUrl();
        if (url == null) return;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, url);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(Intent.createChooser(intent, getString(R.string.share)));
    }

    private void loadScripts() {
        ScriptRepository.get().loadAsync(new ScriptRepository.Callback() {
            public void onLoaded(List<String> scripts) {
                if (floatingWebView != null) {
                    for (String s : scripts) {
                        try { floatingWebView.loadUrl("javascript:" + s); } catch (Exception ignored) {}
                    }
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("url")) {
            String url = intent.getStringExtra("url");
            if (url != null && floatingWebView != null) {
                floatingWebView.loadUrl(url);
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (floatingView != null) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }
        super.onDestroy();
    }
}
