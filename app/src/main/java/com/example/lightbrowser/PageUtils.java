package com.example.lightbrowser;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Environment;
import android.webkit.WebView;

import java.io.File;
import java.io.FileOutputStream;

public final class PageUtils {
    private PageUtils() {}

    /** 地址栏快捷搜索解析：@bd 内容 -> 百度；@g 内容 -> Google */
    public static String resolveInput(Context context, String input) {
        String defaultEngine = Utils.getString(context, "search_engine", "https://www.bing.com/search?q=");
        if (input.startsWith("@bd ")) {
            return "https://www.baidu.com/s?wd=" + Uri.encode(input.substring(4).trim());
        } else if (input.startsWith("@g ")) {
            return "https://www.google.com/search?q=" + Uri.encode(input.substring(3).trim());
        } else if (input.startsWith("@b ")) {
            return "https://www.bing.com/search?q=" + Uri.encode(input.substring(3).trim());
        }
        
        if (input.startsWith("http://") || input.startsWith("https://")) {
            return input;
        } else if (input.contains(".") && !input.contains(" ")) {
            return "https://" + input;
        } else {
            return defaultEngine + Uri.encode(input);
        }
    }

    /** 注入阅读模式 JS，提取正文 */
    public static final String READER_MODE_JS = 
        "javascript:(function(){" +
        "var article = document.querySelector('article') || document.querySelector('.article') || document.querySelector('.content') || document.body;" +
        "var text = article.innerHTML;" +
        "document.body.innerHTML = '<div id=\"lb-reader\" style=\"padding:20px;max-width:800px;margin:0 auto;font-size:18px;line-height:1.8;color:#333;background:#fff;\">' + text + '</div>';" +
        "document.body.style.background = '#fff';" +
        "})()";

    /** 隐藏顶部/底部栏实现全屏沉浸 */
    public static final String IMMERSIVE_JS = 
        "javascript:(function(){var css='header,nav,.header,.nav,.footer,footer{display:none !important;} body{padding-top:0 !important;}';" +
        "var s=document.createElement('style');s.innerHTML=css;document.head.appendChild(s);})()";

    /** 长截图核心逻辑 */
    public interface OnScreenshotListener {
        void onSuccess(String path);
        void onFailed();
    }

    public static void captureLongScreenshot(final WebView webView, final OnScreenshotListener listener) {
        if (webView == null) return;
        try {
            // 获取整个网页的高度
            float scale = webView.getScale();
            int contentHeight = (int) (webView.getContentHeight() * scale);
            int viewWidth = webView.getWidth();
            
            // 限制最大高度防止 OOM
            if (contentHeight > 8000) contentHeight = 8000;

            final Bitmap bitmap = Bitmap.createBitmap(viewWidth, contentHeight, Bitmap.Config.RGB_565);
            final Canvas canvas = new Canvas(bitmap);
            
            // 分批绘制，避免阻塞主线程
            webView.draw(canvas);

            new Thread(new Runnable() {
                public void run() {
                    try {
                        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "LightBrowser");
                        if (!dir.exists()) dir.mkdirs();
                        File file = new File(dir, "Screenshot_" + System.currentTimeMillis() + ".jpg");
                        FileOutputStream fos = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                        fos.close();
                        if (listener != null) listener.onSuccess(file.getAbsolutePath());
                    } catch (Exception e) {
                        if (listener != null) listener.onFailed();
                    }
                }
            }).start();
        } catch (Throwable e) {
            // 可能发生 OOM
            if (listener != null) listener.onFailed();
        }
    }
}
