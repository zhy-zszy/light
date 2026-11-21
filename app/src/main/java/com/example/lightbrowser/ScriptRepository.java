package com.example.lightbrowser;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 单例脚本仓库：后台线程加载 /sdcard/LightBrowser/scripts/*.js，带 TTL 缓存。
 * 严格 Java 7。
 */
public final class ScriptRepository {
    private static final String TAG = "ScriptRepository";
    private static final long CACHE_TTL = 10 * 1000L;

    private static ScriptRepository instance;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile List<String> cachedScripts = new ArrayList<String>();
    private volatile long lastLoadTime = 0L;

    public interface Callback {
        void onLoaded(List<String> scripts);
    }

    public static synchronized ScriptRepository get() {
        if (instance == null) instance = new ScriptRepository();
        return instance;
    }

    private ScriptRepository() {}

    /** 异步加载并回调；callback 可为 null。 */
    public void loadAsync(final Callback callback) {
        executor.execute(new Runnable() {
                public void run() {
                    List<String> scripts = loadInternal();
                    cachedScripts = scripts;
                    lastLoadTime = System.currentTimeMillis();
                    if (callback != null) {
                        try { callback.onLoaded(scripts); } catch (Exception e) {
                            Log.e(TAG, "callback error", e);
                        }
                    }
                }
            });
    }

    /** 同步获取（带 TTL）；可能返回过期缓存，不阻塞 UI 太久。 */
    public List<String> getCached() {
        if (System.currentTimeMillis() - lastLoadTime > CACHE_TTL) {
            cachedScripts = loadInternal();
            lastLoadTime = System.currentTimeMillis();
        }
        return cachedScripts;
    }

    public void invalidate() {
        lastLoadTime = 0L;
    }

    private List<String> loadInternal() {
        List<String> result = new ArrayList<String>();
        File dir = new File(Environment.getExternalStorageDirectory(), "LightBrowser/scripts");
        if (!dir.exists() || !dir.isDirectory()) return result;
        File[] files = dir.listFiles();
        if (files == null) return result;
        for (File f : files) {
            if (!f.getName().endsWith(".js")) continue;
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(f));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line).append("\n");
                result.add(sb.toString());
            } catch (Exception e) {
                Log.e(TAG, "load failed: " + f.getName(), e);
            } finally {
                if (reader != null) {
                    try { reader.close(); } catch (Exception ignored) {}
                }
            }
        }
        return result;
    }
}
