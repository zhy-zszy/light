package com.example.lightbrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PictureInPictureParams;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Rational;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int MAX_TABS = 10;

    private FrameLayout webViewContainer;
    private FrameLayout fullscreenContainer;
    private EditText etAddress;
    private ImageButton btnBack, btnForward, btnRefresh, btnGo, btnSiteSettings;
    private ImageButton btnHome, btnBookmark, btnTabManager, btnMenu, fabMenu;
    private LinearLayout addressBar, bottomBar, findBar;
    private FrameLayout rootLayout;
    private ProgressBar progressBar;

    private EditText etFind;
    private ImageButton btnFindPrev, btnFindNext, btnFindClose;

    private boolean isFullscreen = false;
    private boolean isDesktopMode = false;
    private boolean isNightMode = false;

    private List<TabEntry> tabs = new ArrayList<TabEntry>();
    private int currentTabIndex = -1;
    private WebView webView;
    private TabEntry currentTab;

    private UiManager uiManager;
    private Dialog menuDialog;
    private GestureDetector gestureDetector;

    private static class TabEntry {
        WebView webView;
        BrowserManager browserManager;
        DownloadManager downloadManager;
        SafeWebChromeClient chromeClient;
        String lastUrl;
        boolean initialized;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }

        initViews();
        setupListeners();
        setupFabDrag();
        loadSettings();
        requestStoragePermission();

        addNewTab(Utils.DEFAULT_HOMEPAGE);

        webView.postDelayed(new Runnable() {
                public void run() {
                    if (webView != null) {
                        webView.setPadding(0, Utils.dp2px(MainActivity.this, 80), 0, Utils.dp2px(MainActivity.this, 72));
                    }
                }
            }, 300);
    }

    private void initViews() {
        webViewContainer = (FrameLayout) findViewById(R.id.webview_container);
        fullscreenContainer = (FrameLayout) findViewById(R.id.fullscreen_container);
        etAddress = (EditText) findViewById(R.id.et_address);
        btnBack = (ImageButton) findViewById(R.id.btn_back);
        btnForward = (ImageButton) findViewById(R.id.btn_forward);
        btnRefresh = (ImageButton) findViewById(R.id.btn_refresh);
        btnGo = (ImageButton) findViewById(R.id.btn_go);
        btnSiteSettings = (ImageButton) findViewById(R.id.btn_site_settings);
        btnHome = (ImageButton) findViewById(R.id.btn_home);
        btnBookmark = (ImageButton) findViewById(R.id.btn_bookmark);
        btnTabManager = (ImageButton) findViewById(R.id.btn_tab_manager);
        btnMenu = (ImageButton) findViewById(R.id.btn_menu);
        fabMenu = (ImageButton) findViewById(R.id.fab_menu);
        addressBar = (LinearLayout) findViewById(R.id.address_bar);
        bottomBar = (LinearLayout) findViewById(R.id.bottom_bar);
        rootLayout = (FrameLayout) findViewById(R.id.root_layout);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        findBar = (LinearLayout) findViewById(R.id.find_bar);
        etFind = (EditText) findViewById(R.id.et_find);
        btnFindPrev = (ImageButton) findViewById(R.id.btn_find_prev);
        btnFindNext = (ImageButton) findViewById(R.id.btn_find_next);
        btnFindClose = (ImageButton) findViewById(R.id.btn_find_close);
        if (findBar != null) findBar.setVisibility(View.GONE);

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    if (webView != null) webView.scrollTo(0, 0);
                    return true;
                }
            });

        tintIcons();
    }

    private void tintIcons() {
        int color = getResources().getColor(R.color.primary);
        if (btnBack != null) btnBack.setColorFilter(color);
        if (btnForward != null) btnForward.setColorFilter(color);
        if (btnRefresh != null) btnRefresh.setColorFilter(color);
        if (btnGo != null) btnGo.setColorFilter(color);
        if (btnSiteSettings != null) btnSiteSettings.setColorFilter(color);
        if (btnHome != null) btnHome.setColorFilter(color);
        if (btnBookmark != null) btnBookmark.setColorFilter(color);
        if (btnTabManager != null) btnTabManager.setColorFilter(color);
        if (btnMenu != null) btnMenu.setColorFilter(color);
        if (fabMenu != null) fabMenu.setColorFilter(color);
    }

    public void addNewTab(String url) {
        if (tabs.size() >= MAX_TABS) {
            TabEntry old = tabs.remove(0);
            webViewContainer.removeView(old.webView);
            destroyTab(old);
            if (currentTabIndex > 0) currentTabIndex--;
        }
        WebView newWebView = new WebView(this);
        newWebView.setLayoutParams(new FrameLayout.LayoutParams(
                                       FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        webViewContainer.addView(newWebView);

        TabEntry entry = new TabEntry();
        entry.webView = newWebView;
        entry.browserManager = new BrowserManager(this, newWebView, progressBar, etAddress);
        entry.downloadManager = new DownloadManager(this, newWebView);
        // 传入全屏容器
        entry.chromeClient = new SafeWebChromeClient(this, progressBar, fullscreenContainer);

        newWebView.setWebChromeClient(entry.chromeClient);
        newWebView.setDownloadListener(entry.downloadManager);

        newWebView.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    final WebView.HitTestResult result = webView.getHitTestResult();
                    if (result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                        final String u = result.getExtra();
                        new AlertDialog.Builder(MainActivity.this)
                            .setItems(new String[]{"复制链接", "新标签页打开"}, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == 0) {
                                        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                        if (cm != null) {
                                            cm.setPrimaryClip(ClipData.newPlainText("url", u));
                                            Toast.makeText(MainActivity.this, "已复制", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        addNewTab(u);
                                    }
                                }
                            }).show();
                        return true;
                    }
                    return false;
                }
            });

        newWebView.setOnTouchListener(new View.OnTouchListener() {
                float startX = 0;
                public boolean onTouch(View v, MotionEvent event) {
                    if (gestureDetector.onTouchEvent(event)) return true;
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startX = event.getX();
                            break;
                        case MotionEvent.ACTION_UP:
                            float endX = event.getX();
                            if (startX < 50 && endX - startX > 150) {
                                if (webView != null && webView.canGoBack()) webView.goBack();
                            }
                            if (startX > v.getWidth() - 50 && startX - endX > 150) {
                                if (webView != null && webView.canGoForward()) webView.goForward();
                            }
                            break;
                    }
                    return false;
                }
            });

        tabs.add(entry);
        switchToTab(tabs.size() - 1, url);
    }

    public void switchToTab(int index, String url) {
        if (index < 0 || index >= tabs.size()) return;
        for (TabEntry entry : tabs) entry.webView.setVisibility(View.GONE);
        currentTabIndex = index;
        currentTab = tabs.get(index);
        webView = currentTab.webView;
        webView.setVisibility(View.VISIBLE);

        if (!currentTab.initialized) {
            currentTab.browserManager.init();
            currentTab.initialized = true;
        }

        uiManager = new UiManager(this, btnBack, btnForward, btnBookmark, currentTab.browserManager);

        if (url != null) {
            currentTab.lastUrl = url;
            if (url.equals(Utils.DEFAULT_HOMEPAGE)) {
                webView.loadDataWithBaseURL(url, HomePageHtml.getHtml(this), "text/html", "UTF-8", null);
            } else {
                webView.loadUrl(url);
            }
        }

        currentTab.browserManager.applySiteSettings(webView.getUrl());
        if (isNightMode) {
            webView.loadUrl(BrowserManager.NIGHT_MODE_JS_ON);
        }
    }

    public void loadHomePage() {
        if (webView == null) return;
        webView.loadDataWithBaseURL(Utils.DEFAULT_HOMEPAGE, HomePageHtml.getHtml(this), "text/html", "UTF-8", null);
    }

    private void destroyTab(TabEntry entry) {
        if (entry == null || entry.webView == null) return;
        try {
            entry.webView.stopLoading();
            entry.webView.loadUrl("about:blank");
            entry.webView.removeAllViews();
            entry.webView.destroy();
        } catch (Exception ignored) {}
    }

    private void setupListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { if (webView != null && webView.canGoBack()) webView.goBack(); }
            });
        btnForward.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { if (webView != null && webView.canGoForward()) webView.goForward(); }
            });
        btnRefresh.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { if (webView != null) webView.reload(); }
            });
        btnGo.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { loadUrlFromInput(); }
            });
        etAddress.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_GO) { loadUrlFromInput(); return true; }
                    return false;
                }
            });
        btnSiteSettings.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { openSiteSettings(); }
            });
        btnHome.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { loadHomePage(); }
            });
        btnBookmark.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { toggleBookmark(); }
            });
        btnTabManager.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { openTabManager(); }
            });
        btnMenu.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showMenu(); } });
        fabMenu.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showMenu(); } });

        if (btnFindPrev != null) btnFindPrev.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { if (webView != null) webView.findNext(false); }
                });
        if (btnFindNext != null) btnFindNext.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { if (webView != null) webView.findNext(true); }
                });
        if (btnFindClose != null) btnFindClose.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { hideFindBar(); if (webView != null) webView.clearMatches(); }
                });
        if (etFind != null) etFind.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                            String query = etFind.getText().toString();
                            if (query.length() > 0 && webView != null) {
                                webView.findAllAsync(query);
                                showFindBar();
                            }
                            return true;
                        }
                        return false;
                    }
                });
    }

    private void setupFabDrag() {
        fabMenu.setOnTouchListener(new View.OnTouchListener() {
                private float lastX, lastY;
                private boolean dragging = false;
                private boolean gravityConverted = false;
                public boolean onTouch(View v, MotionEvent event) {
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) fabMenu.getLayoutParams();
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            if (!gravityConverted) {
                                View parent = (View) fabMenu.getParent();
                                params.gravity = Gravity.LEFT | Gravity.TOP;
                                params.leftMargin = parent.getWidth() - fabMenu.getWidth() - params.rightMargin;
                                params.topMargin = parent.getHeight() - fabMenu.getHeight() - params.bottomMargin;
                                params.rightMargin = 0;
                                params.bottomMargin = 0;
                                fabMenu.setLayoutParams(params);
                                gravityConverted = true;
                            }
                            lastX = event.getRawX();
                            lastY = event.getRawY();
                            dragging = false;
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            float dx = event.getRawX() - lastX;
                            float dy = event.getRawY() - lastY;
                            if (Math.abs(dx) > 5 || Math.abs(dy) > 5) dragging = true;
                            if (dragging) {
                                params.leftMargin += (int) dx;
                                params.topMargin += (int) dy;
                                fabMenu.setLayoutParams(params);
                            }
                            lastX = event.getRawX();
                            lastY = event.getRawY();
                            return true;
                        case MotionEvent.ACTION_UP:
                            if (!dragging) v.performClick();
                            return true;
                    }
                    return false;
                }
            });
    }

    private void loadUrlFromInput() {
        if (webView == null) return;
        String input = etAddress.getText().toString().trim();
        if (input.length() == 0) return;

        if (input.equals(Utils.DEFAULT_HOMEPAGE)) {
            loadHomePage();
        } else {
            String url = PageUtils.resolveInput(this, input);
            webView.loadUrl(url);
        }

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etAddress.getWindowToken(), 0);
    }

    private void loadSettings() {
        SharedPreferences prefs = Utils.prefs(this);
        isNightMode = prefs.getBoolean("night_mode", false);
        isDesktopMode = prefs.getBoolean("desktop_mode", false);
        if (isNightMode) {
            if (addressBar != null) addressBar.setBackgroundResource(R.drawable.glass_dark_bg);
            if (bottomBar != null) bottomBar.setBackgroundResource(R.drawable.glass_dark_bg);
            if (etAddress != null) {
                etAddress.setTextColor(Color.WHITE);
                etAddress.setHintTextColor(Color.GRAY);
            }
        }
    }

    private void showMenu() {
        String[] titles = new String[]{
            getString(R.string.homepage), getString(R.string.fullscreen),
            isDesktopMode ? getString(R.string.mobile_mode) : getString(R.string.desktop_mode),
            isNightMode ? getString(R.string.exit_night_mode) : getString(R.string.night_mode),
            getString(R.string.refresh), getString(R.string.share), getString(R.string.copy_url),
            getString(R.string.find_in_page), getString(R.string.save_page), getString(R.string.open_html),
            getString(R.string.floating_window), getString(R.string.scripts), getString(R.string.extensions),
            getString(R.string.history), getString(R.string.bookmarks), getString(R.string.adblock),
            getString(R.string.download_manager), getString(R.string.resource_sniffer),
            getString(R.string.developer_mode), getString(R.string.settings_title),
            "阅读模式", "页面长截图", "画中画 (PiP)", "隐藏页面元素"
        };
        int[] icons = new int[]{
            R.drawable.ic_home, android.R.drawable.ic_menu_crop, android.R.drawable.ic_menu_compass,
            android.R.drawable.ic_menu_view, R.drawable.ic_refresh, android.R.drawable.ic_menu_share,
            android.R.drawable.ic_menu_edit, android.R.drawable.ic_menu_search, android.R.drawable.ic_menu_save,
            android.R.drawable.ic_menu_upload, android.R.drawable.ic_menu_add, android.R.drawable.ic_menu_manage,
            android.R.drawable.ic_menu_manage, android.R.drawable.ic_menu_recent_history, R.drawable.ic_star,
            android.R.drawable.ic_menu_view, android.R.drawable.ic_menu_save, android.R.drawable.ic_menu_search,
            android.R.drawable.ic_menu_preferences, R.drawable.ic_settings,
            android.R.drawable.ic_menu_agenda, android.R.drawable.ic_menu_gallery, android.R.drawable.ic_menu_camera, android.R.drawable.ic_menu_close_clear_cancel
        };

        menuDialog = new Dialog(this, R.style.BottomSheetDialog);
        menuDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        menuDialog.setContentView(R.layout.bottom_sheet_container);
        LinearLayout container = (LinearLayout) menuDialog.findViewById(R.id.menu_container);

        for (int i = 0; i < titles.length; i++) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(Utils.dp2px(this, 20), Utils.dp2px(this, 12),
                            Utils.dp2px(this, 20), Utils.dp2px(this, 12));
            TypedValue outValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            item.setBackgroundResource(outValue.resourceId);

            ImageView icon = new ImageView(this);
            icon.setImageResource(icons[i]);
            icon.setLayoutParams(new LinearLayout.LayoutParams(
                                     Utils.dp2px(this, 24), Utils.dp2px(this, 24)));
            icon.setColorFilter(getResources().getColor(R.color.gray));

            TextView text = new TextView(this);
            text.setText(titles[i]);
            text.setTextSize(16);
            text.setTextColor(isNightMode ? Color.WHITE : Color.BLACK);
            text.setPadding(Utils.dp2px(this, 16), 0, 0, 0);

            item.addView(icon);
            item.addView(text);

            final int index = i;
            item.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        menuDialog.dismiss();
                        handleMenuItem(index);
                    }
                });
            container.addView(item);
        }

        Window window = menuDialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                             (int) (getResources().getDisplayMetrics().heightPixels * 0.7));
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        menuDialog.show();
    }

    private void handleMenuItem(int which) {
        switch (which) {
            case 0: loadHomePage(); break;
            case 1: toggleFullscreen(); break;
            case 2: toggleDesktopMode(); break;
            case 3: toggleNightMode(); break;
            case 4: if (webView != null) webView.reload(); break;
            case 5: sharePage(); break;
            case 6: copyCurrentUrl(); break;
            case 7: showFindBar(); break;
            case 8: savePage(); break;
            case 9: openHtmlFile(); break;
            case 10: openFloatingWindow(); break;
            case 11: startActivity(new Intent(this, ScriptManagerActivity.class)); break;
            case 12: startActivity(new Intent(this, ExtensionActivity.class)); break;
            case 13: startActivity(new Intent(this, HistoryActivity.class)); break;
            case 14: startActivity(new Intent(this, BookmarkActivity.class)); break;
            case 15: startActivity(new Intent(this, AdBlockActivity.class)); break;
            case 16: startActivity(new Intent(this, DownloadManagerActivity.class)); break;
            case 17:
                Intent si = new Intent(this, ResourceSnifferActivity.class);
                if (webView != null && webView.getUrl() != null) si.putExtra("url", webView.getUrl());
                startActivity(si);
                break;
            case 18: startActivityForResult(new Intent(this, DeveloperActivity.class), Utils.REQ_DEVELOPER); break;
            case 19: startActivity(new Intent(this, SettingsActivity.class)); break;
            case 20: 
                if (currentTab != null) currentTab.browserManager.toggleReaderMode(); 
                break;
            case 21: 
                takeLongScreenshot();
                break;
            case 22: 
                enterPipMode();
                break;
            case 23: 
                if (currentTab != null) currentTab.browserManager.toggleImmersive(); 
                break;
        }
    }

    private void showFindBar() {
        if (findBar != null) findBar.setVisibility(View.VISIBLE);
        if (etFind != null) etFind.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && etFind != null) imm.showSoftInput(etFind, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideFindBar() {
        if (findBar != null) findBar.setVisibility(View.GONE);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && etFind != null) imm.hideSoftInputFromWindow(etFind.getWindowToken(), 0);
    }

    private void toggleFullscreen() {
        isFullscreen = !isFullscreen;
        addressBar.setVisibility(isFullscreen ? View.GONE : View.VISIBLE);
        bottomBar.setVisibility(isFullscreen ? View.GONE : View.VISIBLE);
        fabMenu.setVisibility(isFullscreen ? View.VISIBLE : View.GONE);
        if (isFullscreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private void toggleDesktopMode() {
        isDesktopMode = !isDesktopMode;
        Utils.putBool(this, "desktop_mode", isDesktopMode);
        if (currentTab != null) currentTab.browserManager.setDesktopMode(isDesktopMode);
        if (webView != null) webView.reload();
        Toast.makeText(this, isDesktopMode ? R.string.desktop_mode : R.string.mobile_mode, Toast.LENGTH_SHORT).show();
    }

    private void toggleNightMode() {
        isNightMode = !isNightMode;
        Utils.putBool(this, "night_mode", isNightMode);
        if (webView != null) {
            webView.loadUrl(isNightMode ? BrowserManager.NIGHT_MODE_JS_ON : BrowserManager.NIGHT_MODE_JS_OFF);
        }
        if (addressBar != null) {
            addressBar.setBackgroundResource(isNightMode ? R.drawable.glass_dark_bg : R.drawable.glass_gradient_bg);
        }
        if (bottomBar != null) {
            bottomBar.setBackgroundResource(isNightMode ? R.drawable.glass_dark_bg : R.drawable.glass_gradient_bg);
        }
        if (etAddress != null) {
            etAddress.setTextColor(isNightMode ? Color.WHITE : Color.BLACK);
            etAddress.setHintTextColor(isNightMode ? Color.GRAY : Color.GRAY);
        }
        Toast.makeText(this, isNightMode ? R.string.night_mode_on : R.string.night_mode_off, Toast.LENGTH_SHORT).show();
    }

    private void sharePage() {
        if (webView == null || webView.getUrl() == null) return;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, webView.getUrl());
        startActivity(Intent.createChooser(intent, getString(R.string.share)));
    }

    private void copyCurrentUrl() {
        if (webView == null || webView.getUrl() == null) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("URL", webView.getUrl()));
            Toast.makeText(this, R.string.url_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void savePage() {
        if (webView == null || webView.getUrl() == null) return;
        try {
            String title = webView.getTitle();
            String fileName = Utils.sanitizeFileName(title != null ? title : "page") + ".mhtml";
            File dir = new File(Environment.getExternalStorageDirectory(), "LightBrowser/saved_pages");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, fileName);
            webView.saveWebArchive(file.getAbsolutePath());
            Toast.makeText(this, R.string.page_saved + "\n" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void takeLongScreenshot() {
        if (webView == null) return;
        Toast.makeText(this, "正在生成长截图，请稍候...", Toast.LENGTH_SHORT).show();
        PageUtils.captureLongScreenshot(webView, new PageUtils.OnScreenshotListener() {
                public void onSuccess(final String path) {
                    runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(MainActivity.this, "截图已保存至相册\n" + path, Toast.LENGTH_LONG).show();
                                Utils.openFile(MainActivity.this, new File(path), "image/jpeg");
                            }
                        });
                }
                public void onFailed() {
                    runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(MainActivity.this, "截图失败，页面可能过长", Toast.LENGTH_SHORT).show();
                            }
                        });
                }
            });
    }

    private void enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                PictureInPictureParams.Builder pipBuilder = new PictureInPictureParams.Builder();
                // 设置 16:9 比例
                pipBuilder.setAspectRatio(new Rational(16, 9));
                enterPictureInPictureMode(pipBuilder.build());
            } catch (Exception e) {
                Toast.makeText(this, "当前页面不支持画中画", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "系统版本过低，不支持画中画", Toast.LENGTH_SHORT).show();
        }
    }

    private void openHtmlFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "选择HTML文件"), Utils.REQ_OPEN_HTML);
    }

    private void openFloatingWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                       Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, Utils.REQ_OVERLAY_PERM);
            return;
        }
        try {
            Intent intent = new Intent(this, FloatingWindowService.class);
            if (webView != null && webView.getUrl() != null) intent.putExtra("url", webView.getUrl());
            startService(intent);
            Toast.makeText(this, R.string.floating_opened, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "悬浮窗启动失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void openSiteSettings() {
        if (webView == null || webView.getUrl() == null) return;
        Intent intent = new Intent(this, SiteSettingsActivity.class);
        intent.putExtra("url", webView.getUrl());
        startActivityForResult(intent, Utils.REQ_SITE_SETTINGS);
    }

    private void openTabManager() {
        Intent intent = new Intent(this, TabManagerActivity.class);
        if (webView != null) intent.putExtra("current_url", webView.getUrl());
        startActivityForResult(intent, Utils.REQ_TAB_MANAGER);
    }

    private void toggleBookmark() {
        if (webView == null) return;
        String url = webView.getUrl();
        String title = webView.getTitle();
        if (url == null || url.equals(Utils.DEFAULT_HOMEPAGE)) return;

        try {
            SharedPreferences prefs = Utils.prefs(this);
            String json = prefs.getString("bookmarks_json", "");
            org.json.JSONArray arr = json.length() > 0 ? new org.json.JSONArray(json) : new org.json.JSONArray();
            int found = -1;
            for (int i = 0; i < arr.length(); i++) {
                if (url.equals(arr.getJSONObject(i).optString("url"))) {
                    found = i;
                    break;
                }
            }
            if (found >= 0) {
                arr.remove(found);
                Toast.makeText(this, "书签已移除", Toast.LENGTH_SHORT).show();
            } else {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("title", title != null ? title : url);
                obj.put("url", url);
                arr.put(obj);
                Toast.makeText(this, "书签已添加", Toast.LENGTH_SHORT).show();
            }
            prefs.edit().putString("bookmarks_json", arr.toString()).apply();
            if (uiManager != null) uiManager.updateBookmarkButton(url);
        } catch (Exception e) {
            Toast.makeText(this, "书签操作失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                                       android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                       android.Manifest.permission.READ_EXTERNAL_STORAGE
                                   }, Utils.REQ_STORAGE_PERM);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Utils.REQ_OPEN_HTML && resultCode == RESULT_OK && data != null) {
            final Uri uri = data.getData();
            if (uri != null) {
                if (uri.toString().startsWith("content://")) {
                    new Thread(new Runnable() {
                            public void run() {
                                try {
                                    InputStream is = getContentResolver().openInputStream(uri);
                                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                                    StringBuilder sb = new StringBuilder();
                                    String line;
                                    while ((line = reader.readLine()) != null) sb.append(line).append("\n");
                                    reader.close();
                                    final String htmlData = sb.toString();
                                    runOnUiThread(new Runnable() {
                                            public void run() {
                                                if (webView != null) {
                                                    webView.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null);
                                                }
                                            }
                                        });
                                } catch (Exception ignored) {}
                            }
                        }).start();
                } else {
                    if (webView != null) webView.loadUrl(uri.toString());
                }
            }
        } else if (requestCode == Utils.REQ_OVERLAY_PERM) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
                openFloatingWindow();
            }
        } else if (requestCode == Utils.REQ_FILE_CHOOSER) {
            if (currentTab != null && currentTab.chromeClient != null) {
                currentTab.chromeClient.onFileChooserResult(resultCode, data);
            }
        } else if (requestCode == Utils.REQ_SITE_SETTINGS && resultCode == RESULT_OK) {
            if (webView != null && currentTab != null) {
                currentTab.browserManager.applySiteSettings(webView.getUrl());
                webView.reload();
            }
        } else if (requestCode == Utils.REQ_TAB_MANAGER && resultCode == RESULT_OK && data != null) {
            String openUrl = data.getStringExtra("open_url");
            boolean clearHistory = data.getBooleanExtra(TabManagerActivity.EXTRA_CLEAR_HISTORY, false);
            if (clearHistory && currentTabIndex >= 0 && currentTabIndex < tabs.size()) {
                TabEntry removed = tabs.remove(currentTabIndex);
                webViewContainer.removeView(removed.webView);
                destroyTab(removed);
                if (openUrl != null) {
                    addNewTab(openUrl);
                } else if (!tabs.isEmpty()) {
                    switchToTab(Math.max(0, currentTabIndex - 1), tabs.get(0).lastUrl);
                }
            } else if (openUrl != null) {
                if (webView != null) webView.loadUrl(openUrl);
            }
        } else if (requestCode == Utils.REQ_DEVELOPER && resultCode == RESULT_OK && data != null) {
            if (webView != null && currentTab != null) {
                if (data.hasExtra("inject_js")) {
                    webView.loadUrl("javascript:" + data.getStringExtra("inject_js"));
                }
                if (data.hasExtra("view_source")) {
                    webView.loadUrl("javascript:(function(){var w=window.open('about:blank');var d=w.document;d.open();d.write('<pre>'+document.documentElement.outerHTML.replace(/</g,'&lt;').replace(/>/g,'&gt;')+'</pre>');d.close();})()");
                }
                currentTab.browserManager.applyDevSettings();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // 优先退出网页全屏视频
        if (currentTab != null && currentTab.chromeClient != null && currentTab.chromeClient.isFullscreen()) {
            currentTab.chromeClient.exitFullscreen();
            return;
        }

        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else if (tabs.size() > 1) {
            TabEntry removed = tabs.remove(currentTabIndex);
            webViewContainer.removeView(removed.webView);
            destroyTab(removed);
            int newIndex = Math.max(0, currentTabIndex - 1);
            switchToTab(newIndex, tabs.get(newIndex).lastUrl);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 物理键盘快捷键
        if (event.isCtrlPressed()) {
            if (keyCode == KeyEvent.KEYCODE_T) {
                addNewTab(Utils.DEFAULT_HOMEPAGE);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_W) {
                if (tabs.size() > 1) {
                    TabEntry removed = tabs.remove(currentTabIndex);
                    webViewContainer.removeView(removed.webView);
                    destroyTab(removed);
                    switchToTab(Math.max(0, currentTabIndex - 1), tabs.get(0).lastUrl);
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_R) {
                if (webView != null) webView.reload();
                return true;
            }
        }

        // 音量键翻页
        if (Utils.getBool(this, "volume_scroll", true)) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                if (webView != null) webView.pageDown(false);
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                if (webView != null) webView.pageUp(false);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
        if (Utils.getBool(this, "keep_screen_on", false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        if (currentTab != null && webView != null) {
            currentTab.browserManager.applySiteSettings(webView.getUrl());
        }
    }

    @Override
    protected void onDestroy() {
        if (Utils.getBool(this, "clear_on_exit", false)) {
            try {
                WebView wv = new WebView(this);
                wv.clearCache(true);
                wv.clearHistory();
                wv.destroy();
            } catch (Exception ignored) {}
            android.webkit.CookieManager.getInstance().removeAllCookies(null);
            Utils.prefs(this).edit().remove("history_json").apply();
        }
        for (TabEntry entry : tabs) destroyTab(entry);
        tabs.clear();
        super.onDestroy();
    }

    public WebView getWebView() { return webView; }
}
