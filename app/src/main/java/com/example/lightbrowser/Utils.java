package com.example.lightbrowser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 通用工具：常量、Prefs、FileProvider、MIME、文件名、尺寸
 * 严格 Java 7
 */
public final class Utils {
    public static final String PREFS_NAME = "LightBrowserPrefs";
    public static final String SITE_PREFS_NAME = "LightBrowserSiteSettings";
    public static final String DEFAULT_HOMEPAGE = "https://lightbrowser.homepage";
    public static final String DESKTOP_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    public static final int REQ_OPEN_HTML = 1001;
    public static final int REQ_OVERLAY_PERM = 1002;
    public static final int REQ_FILE_CHOOSER = 1003;
    public static final int REQ_TAB_MANAGER = 1004;
    public static final int REQ_SITE_SETTINGS = 1005;
    public static final int REQ_DEVELOPER = 1007;
    public static final int REQ_STORAGE_PERM = 2001;

    public static final Map<String, String> MIME_MAP = new HashMap<String, String>();
    static {
        MIME_MAP.put("lrc", "application/lrc");
        MIME_MAP.put("apk", "application/vnd.android.package-archive");
        MIME_MAP.put("torrent", "application/x-bittorrent");
        MIME_MAP.put("7z", "application/x-7z-compressed");
        MIME_MAP.put("rar", "application/x-rar-compressed");
        MIME_MAP.put("zip", "application/zip");
        MIME_MAP.put("mp3", "audio/mpeg");
        MIME_MAP.put("flac", "audio/flac");
        MIME_MAP.put("m4a", "audio/mp4");
        MIME_MAP.put("mp4", "video/mp4");
        MIME_MAP.put("mkv", "video/x-matroska");
        MIME_MAP.put("pdf", "application/pdf");
        MIME_MAP.put("doc", "application/msword");
        MIME_MAP.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        MIME_MAP.put("txt", "text/plain");
        MIME_MAP.put("csv", "text/csv");
        MIME_MAP.put("json", "application/json");
        MIME_MAP.put("js", "application/javascript");
        MIME_MAP.put("css", "text/css");
        MIME_MAP.put("jpg", "image/jpeg");
        MIME_MAP.put("png", "image/png");
    }

    private Utils() {}

    public static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static SharedPreferences sitePrefs(Context c) {
        return c.getSharedPreferences(SITE_PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean getBool(Context c, String k, boolean d) {
        return prefs(c).getBoolean(k, d);
    }
    public static void putBool(Context c, String k, boolean v) {
        prefs(c).edit().putBoolean(k, v).apply();
    }
    public static String getString(Context c, String k, String d) {
        return prefs(c).getString(k, d);
    }
    public static void putString(Context c, String k, String v) {
        prefs(c).edit().putString(k, v).apply();
    }
    public static int getInt(Context c, String k, int d) {
        return prefs(c).getInt(k, d);
    }
    public static void putInt(Context c, String k, int v) {
        prefs(c).edit().putInt(k, v).apply();
    }

    public static String sanitizeFileName(String name) {
        if (name == null) name = "file_" + System.currentTimeMillis();
        name = name.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        if (name.length() > 200) {
            String ext = "";
            int dot = name.lastIndexOf('.');
            if (dot > 0) ext = name.substring(dot);
            name = name.substring(0, 200 - ext.length()) + ext;
        }
        return name;
    }

    public static String guessMime(String url, String mimetype, String fileName) {
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0 && dot < fileName.length() - 1) {
            ext = fileName.substring(dot + 1).toLowerCase();
        }
        if (ext.length() > 0 && MIME_MAP.containsKey(ext)) {
            String correct = MIME_MAP.get(ext);
            if (mimetype == null || mimetype.length() == 0
                || "application/octet-stream".equals(mimetype)
                || "text/plain".equals(mimetype)
                || "application/x-msdownload".equals(mimetype)) {
                return correct;
            }
        }
        if (mimetype == null || mimetype.length() == 0) {
            String urlExt = MimeTypeMap.getFileExtensionFromUrl(url);
            if (urlExt != null && urlExt.length() > 0) {
                String inferred = MimeTypeMap.getSingleton().getMimeTypeFromExtension(urlExt.toLowerCase());
                if (inferred != null && inferred.length() > 0) return inferred;
            }
            return "application/octet-stream";
        }
        return mimetype;
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    public static Uri getUriForFile(Context c, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(c, c.getPackageName() + ".fileprovider", file);
        }
        return Uri.fromFile(file);
    }

    public static void openFile(Context c, File file, String mime) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(getUriForFile(c, file), mime);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(intent);
    }

    public static void installApk(Context c, File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(getUriForFile(c, file), "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(intent);
    }

    public static void shareFile(Context c, File file, String mime) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mime);
        intent.putExtra(Intent.EXTRA_STREAM, getUriForFile(c, file));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        c.startActivity(Intent.createChooser(intent, "分享"));
    }

    public static int dp2px(Context c, int dp) {
        return (int) (dp * c.getResources().getDisplayMetrics().density + 0.5f);
    }
}
