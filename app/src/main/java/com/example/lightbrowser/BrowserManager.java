package com.example.lightbrowser;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BrowserManager {
    private static final String TAG = "BrowserManager";

    private Context context;
    private WebView webView;
    private ProgressBar progressBar;
    private EditText etAddress;
    private List<String> adBlockRules = new ArrayList<String>();
    private Set<String> adBlockSet = new HashSet<String>();
    private boolean isDesktopMode = false;
    private boolean isAdBlockEnabled = false;
    private String mobileUserAgent;
    private boolean isReaderMode = false;

    public BrowserManager(Context context, WebView webView, ProgressBar progressBar, EditText etAddress) {
        this.context = context;
        this.webView = webView;
        this.progressBar = progressBar;
        this.etAddress = etAddress;
    }

    public String getMobileUserAgent() {
        return mobileUserAgent;
    }

    public void init() {
        setupWebView();
        isAdBlockEnabled = Utils.getBool(context, "adblock_enabled", false);
        new Thread(new Runnable() {
            public void run() {
                loadAdBlockRules();
                ScriptRepository.get().loadAsync(new ScriptRepository.Callback() {
                    public void onLoaded(List<String> scripts) {
                        if (webView != null) {
                            final List<String> copy = scripts;
                            webView.post(new Runnable() {
                                public void run() {
                                    for (String s : copy) {
                                        try { webView.loadUrl("javascript:" + s); } catch (Exception ignored) {}
                                    }
                                }
                            });
                        }
                    }
                });
            }
        }).start();
    }

    private void setupWebView() {
        WebSettings ws = webView.getSettings();
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
        ws.setSupportMultipleWindows(false);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);

        if (Utils.getBool(context, "block_images", false)) ws.setBlockNetworkImage(true);
        if (Utils.getBool(context, "force_zoom", true)) {
            ws.setUseWideViewPort(true);
            ws.setLoadWithOverviewMode(true);
        }
        int textZoom = Utils.getInt(context, "text_size", 100);
        ws.setTextZoom(textZoom);
        if (Utils.getBool(context, "incognito", false)) {
            android.webkit.CookieManager.getInstance().setAcceptCookie(false);
            ws.setSaveFormData(false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ws.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ws.setSafeBrowsingEnabled(true);
        }

        webView.removeJavascriptInterface("searchBoxJavaBridge_");
        webView.removeJavascriptInterface("accessibility");
        webView.removeJavascriptInterface("accessibilityTraversal");
        mobileUserAgent = ws.getUserAgentString();

        isDesktopMode = Utils.getBool(context, "desktop_mode", false);
        if (isDesktopMode) ws.setUserAgentString(Utils.DESKTOP_UA);

        webView.setWebViewClient(new SafeWebViewClient());

        webView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                final WebView.HitTestResult result = webView.getHitTestResult();
                if (result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                    final String url = result.getExtra();
                    new AlertDialog.Builder(context)
                        .setItems(new String[]{"复制链接", "新标签页打开"}, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    android.content.ClipboardManager cm =
                                        (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                    if (cm != null) {
                                        cm.setPrimaryClip(android.content.ClipData.newPlainText("url", url));
                                        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    if (context instanceof MainActivity) {
                                        ((MainActivity) context).addNewTab(url);
                                    }
                                }
                            }
                        }).show();
                    return true;
                }
                return false;
            }
        });

        applyDevSettings();
    }

    private class SafeWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            etAddress.setText(url);
            progressBar.setVisibility(View.VISIBLE);
            if (isReaderMode) {
                isReaderMode = false; // 切换页面时重置阅读模式
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar.setVisibility(View.GONE);

            List<String> scripts = ScriptRepository.get().getCached();
            for (String script : scripts) {
                try { view.loadUrl("javascript:" + script); } catch (Exception ignored) {}
            }

            if (Utils.getBool(context, "night_mode", false)) {
                view.loadUrl(NIGHT_MODE_JS_ON);
            }
            if (isReaderMode) {
                view.loadUrl(PageUtils.READER_MODE_JS);
            }

            if (!Utils.getBool(context, "incognito", false)) {
                recordHistory(view.getTitle(), url);
            }
        }

        @Override
        public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
            final String url = error.getUrl();
            final String host = extractHost(url);
            if (Utils.prefs(context).getBoolean("ssl_allow_" + host, false)) {
                handler.proceed();
                return;
            }
            new AlertDialog.Builder(context)
                .setTitle(R.string.ssl_error_title)
                .setMessage(R.string.ssl_error_message + "\n" + url)
                .setPositiveButton(R.string.continue_text, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { handler.proceed(); }
                })
                .setNeutralButton("始终允许此站点", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        Utils.prefs(context).edit().putBoolean("ssl_allow_" + host, true).apply();
                        handler.proceed();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { handler.cancel(); }
                })
                .setCancelable(false)
                .show();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url == null || url.length() == 0) return false;
            if (url.equals(Utils.DEFAULT_HOMEPAGE)) {
                if (context instanceof MainActivity) {
                    ((MainActivity) context).loadHomePage();
                }
                return true;
            }
            if (url.startsWith("http://") || url.startsWith("https://")
                    || url.startsWith("file://") || url.startsWith("about:")) return false;
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return true;
            } catch (Exception e) {
                Toast.makeText(context, "无法打开外部应用", Toast.LENGTH_SHORT).show();
                return true;
            }
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (isAdBlockEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (shouldBlockUrl(request.getUrl().toString())) {
                    return new WebResourceResponse("text/plain", "UTF-8", null);
                }
            }
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            if (isAdBlockEnabled && shouldBlockUrl(url)) {
                return new WebResourceResponse("text/plain", "UTF-8", null);
            }
            return super.shouldInterceptRequest(view, url);
        }
    }

    public static final String NIGHT_MODE_JS_ON =
        "javascript:(function(){" +
        "var s=document.getElementById('lb-night-mode');" +
        "if(!s){s=document.createElement('style');s.id='lb-night-mode';" +
        "s.innerHTML='html,body{background-color:#1a1a2e!important;color:#e0e0e0!important;} " +
        "div,span,p,h1,h2,h3,h4,h5,h6,li,td,th{color:#e0e0e0!important;} " +
        "a{color:#64b5f6!important;} " +
        "input,textarea{background:#16213e!important;color:#e0e0e0!important;} " +
        "img,video{opacity:0.8!important;filter:invert(1) hue-rotate(180deg)!important;}' " +
        "document.head.appendChild(s);}})()";

    public static final String NIGHT_MODE_JS_OFF =
        "javascript:(function(){var s=document.getElementById('lb-night-mode');if(s)s.remove();})()";

    public void toggleReaderMode() {
        isReaderMode = !isReaderMode;
        if (webView != null) {
            if (isReaderMode) {
                webView.loadUrl(PageUtils.READER_MODE_JS);
                Toast.makeText(context, "阅读模式已开启", Toast.LENGTH_SHORT).show();
            } else {
                webView.reload();
                Toast.makeText(context, "阅读模式已关闭", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void toggleImmersive() {
        if (webView != null) {
            webView.loadUrl(PageUtils.IMMERSIVE_JS);
            Toast.makeText(context, "页面元素已隐藏", Toast.LENGTH_SHORT).show();
        }
    }

    public void applySiteSettings(String url) {
        if (url == null || url.length() == 0) return;
        SiteSettings settings = SiteSettings.load(context, url);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(settings.javascript);
        ws.setDomStorageEnabled(settings.domStorage);
        ws.setBlockNetworkImage(!settings.allowImages);
        ws.setJavaScriptCanOpenWindowsAutomatically(settings.allowPopup);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ws.setSafeBrowsingEnabled(settings.safeBrowsing);
        }
        if (settings.desktopMode) {
            ws.setUserAgentString(Utils.DESKTOP_UA);
        } else {
            ws.setUserAgentString(mobileUserAgent);
        }
    }

    public void applyDevSettings() {
        if (!DeveloperActivity.isDevModeEnabled(context)) return;
        WebSettings ws = webView.getSettings();
        ws.setBlockNetworkImage(DeveloperActivity.isBlockImages(context));
        ws.setJavaScriptEnabled(!DeveloperActivity.isBlockJs(context));
        if (DeveloperActivity.isForceDesktop(context)) {
            ws.setUserAgentString(Utils.DESKTOP_UA);
        }
        String customUa = DeveloperActivity.getCustomUa(context);
        if (customUa != null && customUa.length() > 0) ws.setUserAgentString(customUa);
    }

    public void setDesktopMode(boolean desktop) {
        isDesktopMode = desktop;
        WebSettings ws = webView.getSettings();
        if (desktop) {
            ws.setUserAgentString(Utils.DESKTOP_UA);
            ws.setUseWideViewPort(true);
            ws.setLoadWithOverviewMode(false);
        } else {
            ws.setUserAgentString(mobileUserAgent);
            ws.setLoadWithOverviewMode(true);
        }
    }

    private void loadAdBlockRules() {
        adBlockRules.clear();
        adBlockSet.clear();
        File ruleFile = new File(Environment.getExternalStorageDirectory(), "LightBrowser/adblock.txt");
        if (!ruleFile.exists()) return;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(ruleFile));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0 && !line.startsWith("!")) {
                    adBlockRules.add(line);
                    adBlockSet.add(line);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "load adblock rules failed", e);
        } finally {
            if (reader != null) try { reader.close(); } catch (Exception ignored) {}
        }
    }

    private boolean shouldBlockUrl(String url) {
        if (url == null || adBlockSet.isEmpty()) return false;
        for (String rule : adBlockSet) {
            if (url.contains(rule)) return true;
        }
        return false;
    }

    private void recordHistory(String title, String url) {
        if (url == null || url.length() == 0 || url.equals(Utils.DEFAULT_HOMEPAGE)) return;
        if (title == null || title.length() == 0) title = url;
        try {
            SharedPreferences prefs = Utils.prefs(context);
            String json = prefs.getString("history_json", "");
            JSONArray arr = json.length() > 0 ? new JSONArray(json) : new JSONArray();
            JSONObject obj = new JSONObject();
            obj.put("time", System.currentTimeMillis());
            obj.put("title", title);
            obj.put("url", url);
            arr.put(obj);
            while (arr.length() > 1000) {
                arr.remove(0);
            }
            prefs.edit().putString("history_json", arr.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "record history failed", e);
        }
    }

    private static String extractHost(String url) {
        try {
            return new java.net.URI(url).getHost();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
