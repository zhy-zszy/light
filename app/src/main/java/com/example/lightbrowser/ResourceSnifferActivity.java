package com.example.lightbrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResourceSnifferActivity extends Activity {
    private static final String TAG = "LightBrowserSniffer";
    private static final String KEY_SNIFFED = "sniffed_resources";
    private static final String DESKTOP_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    /** 最大读取 HTML / JS 的字节数，避免超大页面卡死 */
    private static final int MAX_READ_BYTES = 2 * 1024 * 1024;

    private EditText etUrl;
    private Button btnSniff, btnClear;
    private Button btnFilterAll, btnFilterVideo, btnFilterAudio, btnFilterImage, btnFilterOther;
    private TextView tvCount;
    private ListView lvResources;
    private ResourceAdapter adapter;
    private List<ResourceInfo> resources = new ArrayList<ResourceInfo>();
    private List<ResourceInfo> filteredResources = new ArrayList<ResourceInfo>();
    private String currentFilter = "all";

    private static final Pattern[] PATTERNS = {
        Pattern.compile("https?://[^\\s\"'<>]+\\.(mp4|webm|ogg|m3u8|flv|avi|mkv|mov)(\\?[^\\s\"'<>]*)?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("https?://[^\\s\"'<>]+\\.(mp3|m4a|aac|wav|flac|ogg|wma)(\\?[^\\s\"'<>]*)?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("https?://[^\\s\"'<>]+\\.(jpg|jpeg|png|gif|webp|svg|bmp|ico)(\\?[^\\s\"'<>]*)?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("https?://[^\\s\"'<>]+\\.(js)(\\?[^\\s\"'<>]*)?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("https?://[^\\s\"'<>]+\\.(css)(\\?[^\\s\"'<>]*)?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("https?://[^\\s\"'<>]+\\.(pdf|doc|docx|xls|xlsx|ppt|pptx|txt|zip|rar|7z|apk)(\\?[^\\s\"'<>]*)?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("https?://[^\\s\"'<>]+\\.m3u8(\\?[^\\s\"'<>]*)?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("blob:https?://[^\\s\"'<>]+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("https?://[^\\s\"'<>]+/(video|audio|image|media|stream|play)/[^\\s\"'<>]+", Pattern.CASE_INSENSITIVE),
    };

    private static final Pattern[] VIDEO_PATTERNS = {
        Pattern.compile("https?://[^\\s\"'<>]+douyinvod[^\\s\"'<>]+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("https?://[^\\s\"'<>]+tiktok[^\\s\"'<>]+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("https?://[^\\s\"'<>]+youtube[^\\s\"'<>]+videoplayback[^\\s\"'<>]+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("https?://[^\\s\"'<>]+bilibili[^\\s\"'<>]+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("https?://[^\\s\"'<>]+upos-sz[^\\s\"'<>]+", Pattern.CASE_INSENSITIVE),
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_resource_sniffer);

        etUrl = (EditText) findViewById(R.id.et_sniff_url);
        btnSniff = (Button) findViewById(R.id.btn_sniff);
        btnClear = (Button) findViewById(R.id.btn_clear_sniffed);
        lvResources = (ListView) findViewById(R.id.lv_resources);
        tvCount = (TextView) findViewById(R.id.tv_sniff_count);
        btnFilterAll = (Button) findViewById(R.id.btn_filter_all);
        btnFilterVideo = (Button) findViewById(R.id.btn_filter_video);
        btnFilterAudio = (Button) findViewById(R.id.btn_filter_audio);
        btnFilterImage = (Button) findViewById(R.id.btn_filter_image);
        btnFilterOther = (Button) findViewById(R.id.btn_filter_other);

        adapter = new ResourceAdapter();
        lvResources.setAdapter(adapter);

        String url = getIntent().getStringExtra("url");
        if (url != null && url.length() > 0) {
            etUrl.setText(url);
            startSniff(url);
        }

        btnSniff.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String input = etUrl.getText().toString().trim();
                    if (input.length() == 0) {
                        Toast.makeText(ResourceSnifferActivity.this, "请输入网址", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!input.startsWith("http://") && !input.startsWith("https://")) {
                        input = "https://" + input;
                        etUrl.setText(input);
                    }
                    startSniff(input);
                }
            });

        btnClear.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    resources.clear();
                    filteredResources.clear();
                    adapter.notifyDataSetChanged();
                    updateCount();
                    Toast.makeText(ResourceSnifferActivity.this, "已清空", Toast.LENGTH_SHORT).show();
                }
            });

        btnFilterAll.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { setFilter("all"); }
            });
        btnFilterVideo.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { setFilter("video"); }
            });
        btnFilterAudio.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { setFilter("audio"); }
            });
        btnFilterImage.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { setFilter("image"); }
            });
        btnFilterOther.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { setFilter("other"); }
            });

        loadSniffedHistory();
        updateCount();
    }

    private void startSniff(final String url) {
        resources.clear();
        filteredResources.clear();
        adapter.notifyDataSetChanged();
        updateCount();
        Toast.makeText(this, "正在嗅探资源...", Toast.LENGTH_SHORT).show();

        new Thread(new Runnable() {
                public void run() {
                    try {
                        sniffPage(url);
                        Set<String> seen = new HashSet<String>();
                        List<ResourceInfo> unique = new ArrayList<ResourceInfo>();
                        for (ResourceInfo r : resources) {
                            if (!seen.contains(r.url)) {
                                seen.add(r.url);
                                unique.add(r);
                            }
                        }
                        resources = unique;
                        applyFilter();
                        runOnUiThread(new Runnable() {
                                public void run() {
                                    adapter.notifyDataSetChanged();
                                    updateCount();
                                    Toast.makeText(ResourceSnifferActivity.this,
                                                   "嗅探完成，找到 " + resources.size() + " 个资源", Toast.LENGTH_SHORT).show();
                                    saveSniffedHistory();
                                }
                            });
                    } catch (final Exception e) {
                        Log.e(TAG, "Sniff failed", e);
                        runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(ResourceSnifferActivity.this,
                                                   "嗅探失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                    }
                }
            }).start();
    }

    private void sniffPage(String pageUrl) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(pageUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", DESKTOP_UA);
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            conn.setInstanceFollowRedirects(true);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) throw new Exception("HTTP " + responseCode);

            String contentType = conn.getContentType();
            if (contentType != null) contentType = contentType.toLowerCase();

            if (contentType != null && (contentType.contains("video") || contentType.contains("audio")
                || contentType.contains("image") || contentType.contains("application"))) {
                ResourceInfo info = new ResourceInfo();
                info.url = pageUrl;
                info.type = guessTypeFromMime(contentType);
                info.size = conn.getContentLength();
                info.name = extractFileName(pageUrl);
                resources.add(info);
            }

            // 关键修改：限制最大读取字节数
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            int total = 0;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
                total += line.length();
                if (total > MAX_READ_BYTES) break;
            }
            reader.close();
            String html = sb.toString();

            extractResources(html, pageUrl);
            extractJsResources(html, pageUrl);
            extractStreamResources(html, pageUrl);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void extractResources(String html, String baseUrl) {
        Set<String> found = new HashSet<String>();

        for (Pattern p : PATTERNS) {
            Matcher m = p.matcher(html);
            while (m.find()) {
                String url = cleanUrl(m.group());
                if (url.length() > 0 && !found.contains(url)) {
                    found.add(url);
                    ResourceInfo info = new ResourceInfo();
                    info.url = url;
                    info.type = guessTypeFromUrl(url);
                    info.name = extractFileName(url);
                    resources.add(info);
                }
            }
        }

        for (Pattern p : VIDEO_PATTERNS) {
            Matcher m = p.matcher(html);
            while (m.find()) {
                String url = cleanUrl(m.group());
                if (url.length() > 0 && !found.contains(url)) {
                    found.add(url);
                    ResourceInfo info = new ResourceInfo();
                    info.url = url;
                    info.type = "video";
                    info.name = extractFileName(url);
                    resources.add(info);
                }
            }
        }

        Pattern srcPattern = Pattern.compile("(?:src|href)=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher srcMatcher = srcPattern.matcher(html);
        while (srcMatcher.find()) {
            String url = resolveUrl(srcMatcher.group(1), baseUrl);
            if (url != null && isMediaUrl(url) && !found.contains(url)) {
                found.add(url);
                ResourceInfo info = new ResourceInfo();
                info.url = url;
                info.type = guessTypeFromUrl(url);
                info.name = extractFileName(url);
                resources.add(info);
            }
        }

        Pattern dataSrcPattern = Pattern.compile("data-src=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher dataSrcMatcher = dataSrcPattern.matcher(html);
        while (dataSrcMatcher.find()) {
            String url = resolveUrl(dataSrcMatcher.group(1), baseUrl);
            if (url != null && isMediaUrl(url) && !found.contains(url)) {
                found.add(url);
                ResourceInfo info = new ResourceInfo();
                info.url = url;
                info.type = guessTypeFromUrl(url);
                info.name = extractFileName(url);
                resources.add(info);
            }
        }
    }

    private void extractJsResources(String html, String baseUrl) {
        Pattern scriptPattern = Pattern.compile("<script[^>]*src=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher m = scriptPattern.matcher(html);
        while (m.find()) {
            String url = resolveUrl(m.group(1), baseUrl);
            if (url != null && url.endsWith(".js")) {
                try { sniffJsForResources(url, baseUrl); } catch (Exception ignored) {}
            }
        }
    }

    private void sniffJsForResources(String jsUrl, String baseUrl) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(jsUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", DESKTOP_UA);

            if (conn.getResponseCode() != 200) return;

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            int total = 0;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
                total += line.length();
                if (total > MAX_READ_BYTES) break;
            }
            reader.close();
            String js = sb.toString();

            for (Pattern p : PATTERNS) {
                Matcher m = p.matcher(js);
                while (m.find()) {
                    String foundUrl = cleanUrl(m.group());
                    if (foundUrl.length() > 0) {
                        ResourceInfo info = new ResourceInfo();
                        info.url = foundUrl;
                        info.type = guessTypeFromUrl(foundUrl);
                        info.name = extractFileName(foundUrl);
                        resources.add(info);
                    }
                }
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void extractStreamResources(String html, String baseUrl) {
        Pattern[] streamPatterns = {
            Pattern.compile("https?://[^\\s\"'<>]+\\.ts(\\?[^\\s\"'<>]*)?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("https?://[^\\s\"'<>]+/playlist[^\\s\"'<>]*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("https?://[^\\s\"'<>]+/master[^\\s\"'<>]*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("https?://[^\\s\"'<>]+/index[^\\s\"'<>]*\\.m3u8", Pattern.CASE_INSENSITIVE),
        };

        for (Pattern p : streamPatterns) {
            Matcher m = p.matcher(html);
            while (m.find()) {
                String url = cleanUrl(m.group());
                if (url.length() > 0) {
                    ResourceInfo info = new ResourceInfo();
                    info.url = url;
                    info.type = "video";
                    info.name = extractFileName(url);
                    resources.add(info);
                }
            }
        }
    }

    private String cleanUrl(String url) {
        if (url == null) return "";
        url = url.trim();
        while (url.length() > 0 && "\"'<>);,".indexOf(url.charAt(url.length() - 1)) >= 0) {
            url = url.substring(0, url.length() - 1);
        }
        while (url.length() > 0 && "\"'<>);,".indexOf(url.charAt(0)) >= 0) {
            url = url.substring(1);
        }
        return url;
    }

    private String resolveUrl(String relative, String base) {
        if (relative == null || relative.length() == 0) return null;
        if (relative.startsWith("http://") || relative.startsWith("https://")) return relative;
        if (relative.startsWith("//")) {
            return base.startsWith("https") ? "https:" + relative : "http:" + relative;
        }
        try {
            java.net.URL baseUrl = new java.net.URL(base);
            if (relative.startsWith("/")) {
                return baseUrl.getProtocol() + "://" + baseUrl.getHost()
                    + (baseUrl.getPort() != -1 ? ":" + baseUrl.getPort() : "") + relative;
            }
            String basePath = baseUrl.getPath();
            int lastSlash = basePath.lastIndexOf("/");
            if (lastSlash >= 0) basePath = basePath.substring(0, lastSlash + 1);
            return baseUrl.getProtocol() + "://" + baseUrl.getHost()
                + (baseUrl.getPort() != -1 ? ":" + baseUrl.getPort() : "")
                + basePath + relative;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isMediaUrl(String url) {
        String lower = url.toLowerCase();
        String[] exts = {".mp4", ".webm", ".ogg", ".m3u8", ".mp3", ".m4a", ".aac", ".wav",
            ".flac", ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg", ".css", ".js",
            ".pdf", ".doc", ".docx", ".zip", ".rar", ".apk", ".ts", ".flv"};
        for (String ext : exts) if (lower.contains(ext)) return true;
        return false;
    }

    private String guessTypeFromUrl(String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".mp4") || lower.contains(".webm") || lower.contains(".m3u8")
            || lower.contains(".flv") || lower.contains(".ts") || lower.contains("video")
            || lower.contains("vod") || lower.contains("play")) return "video";
        if (lower.contains(".mp3") || lower.contains(".m4a") || lower.contains(".aac")
            || lower.contains(".wav") || lower.contains(".flac") || lower.contains("audio")) return "audio";
        if (lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains(".png")
            || lower.contains(".gif") || lower.contains(".webp") || lower.contains(".svg")
            || lower.contains(".bmp") || lower.contains(".ico")) return "image";
        if (lower.contains(".js")) return "js";
        if (lower.contains(".css")) return "css";
        if (lower.contains(".pdf") || lower.contains(".doc") || lower.contains(".xls")
            || lower.contains(".ppt") || lower.contains(".txt")) return "doc";
        if (lower.contains(".zip") || lower.contains(".rar") || lower.contains(".7z")) return "archive";
        if (lower.contains(".apk")) return "apk";
        return "other";
    }

    private String guessTypeFromMime(String mime) {
        String lower = mime.toLowerCase();
        if (lower.contains("video")) return "video";
        if (lower.contains("audio")) return "audio";
        if (lower.contains("image")) return "image";
        if (lower.contains("javascript")) return "js";
        if (lower.contains("css")) return "css";
        if (lower.contains("pdf") || lower.contains("word") || lower.contains("excel")) return "doc";
        if (lower.contains("zip") || lower.contains("rar")) return "archive";
        return "other";
    }

    private String extractFileName(String url) {
        try {
            int qIdx = url.indexOf("?");
            String path = qIdx > 0 ? url.substring(0, qIdx) : url;
            int lastSlash = path.lastIndexOf("/");
            if (lastSlash >= 0 && lastSlash < path.length() - 1) {
                String name = path.substring(lastSlash + 1);
                if (name.length() > 80) name = name.substring(0, 80) + "...";
                return name;
            }
        } catch (Exception ignored) {}
        return url;
    }

    private void applyFilter() {
        filteredResources.clear();
        if ("all".equals(currentFilter)) {
            filteredResources.addAll(resources);
        } else {
            for (ResourceInfo r : resources) {
                if (currentFilter.equals(r.type)) filteredResources.add(r);
            }
        }
        Collections.sort(filteredResources, new Comparator<ResourceInfo>() {
                public int compare(ResourceInfo a, ResourceInfo b) {
                    int typeCompare = a.type.compareTo(b.type);
                    if (typeCompare != 0) return typeCompare;
                    return a.url.compareTo(b.url);
                }
            });
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        applyFilter();
        adapter.notifyDataSetChanged();
        updateCount();
    }

    private void updateCount() {
        tvCount.setText("找到 " + filteredResources.size() + " 个资源"
                        + (filteredResources.size() != resources.size() ? " (共 " + resources.size() + ")" : ""));
    }

    private void saveSniffedHistory() {
        try {
            JSONArray arr = new JSONArray();
            int count = Math.min(resources.size(), 100);
            for (int i = 0; i < count; i++) {
                ResourceInfo r = resources.get(i);
                JSONObject obj = new JSONObject();
                obj.put("url", r.url);
                obj.put("type", r.type);
                obj.put("name", r.name);
                obj.put("size", r.size);
                arr.put(obj);
            }
            Utils.prefs(this).edit().putString(KEY_SNIFFED, arr.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadSniffedHistory() {
        String json = Utils.prefs(this).getString(KEY_SNIFFED, "");
        if (json.length() == 0) return;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                ResourceInfo info = new ResourceInfo();
                info.url = obj.optString("url", "");
                info.type = obj.optString("type", "other");
                info.name = obj.optString("name", "");
                info.size = obj.optLong("size", 0);
                if (info.url.length() > 0) resources.add(info);
            }
            applyFilter();
            adapter.notifyDataSetChanged();
            updateCount();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void downloadResource(ResourceInfo info) {
        try {
            String fileName = info.name;
            if (fileName == null || fileName.length() == 0) fileName = "resource_" + System.currentTimeMillis();
            fileName = Utils.sanitizeFileName(fileName);

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(info.url));
            request.setTitle("下载: " + fileName);
            request.setDescription(info.url);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.addRequestHeader("User-Agent", DESKTOP_UA);
            request.addRequestHeader("Accept", "*/*");

            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                long id = dm.enqueue(request);
                DownloadManagerActivity.addDownloadRecord(this, id, fileName, info.url);
                Toast.makeText(this, "开始下载: " + fileName, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "下载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void copyUrl(ResourceInfo info) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(info.name, info.url));
            Toast.makeText(this, "链接已复制", Toast.LENGTH_SHORT).show();
        }
    }

    private void openInBrowser(ResourceInfo info) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(info.url));
        startActivity(intent);
    }

    private void showResourceOptions(final ResourceInfo info) {
        final String[] items = new String[]{"下载资源", "复制链接", "浏览器打开", "查看完整URL"};
        new AlertDialog.Builder(this)
            .setTitle(info.name)
            .setItems(items, new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface dialog, int which) {
                    switch (which) {
                        case 0: downloadResource(info); break;
                        case 1: copyUrl(info); break;
                        case 2: openInBrowser(info); break;
                        case 3:
                            new AlertDialog.Builder(ResourceSnifferActivity.this)
                                .setTitle("完整URL")
                                .setMessage(info.url)
                                .setPositiveButton("复制", new android.content.DialogInterface.OnClickListener() {
                                    public void onClick(android.content.DialogInterface d, int w) {
                                        copyUrl(info);
                                    }
                                })
                                .setNegativeButton("关闭", null)
                                .show();
                            break;
                    }
                }
            })
            .show();
    }

    private void setTypeBadge(TextView tv, String type) {
        tv.setText(getTypeLabel(type));
        tv.setTextColor(0xffffffff);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(getTypeColor(type));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            tv.setBackground(drawable);
        } else {
            tv.setBackgroundDrawable(drawable);
        }
    }

    private int getTypeColor(String type) {
        if ("video".equals(type)) return 0xfff44336;
        if ("audio".equals(type)) return 0xff9c27b0;
        if ("image".equals(type)) return 0xff4caf50;
        if ("js".equals(type)) return 0xffff9800;
        if ("css".equals(type)) return 0xff2196f3;
        if ("doc".equals(type)) return 0xff795548;
        if ("archive".equals(type)) return 0xff607d8b;
        if ("apk".equals(type)) return 0xff4caf50;
        return 0xffff4081;
    }

    private String getTypeLabel(String type) {
        if ("video".equals(type)) return "视频";
        if ("audio".equals(type)) return "音频";
        if ("image".equals(type)) return "图片";
        if ("js".equals(type)) return "JS";
        if ("css".equals(type)) return "CSS";
        if ("doc".equals(type)) return "文档";
        if ("archive".equals(type)) return "压缩包";
        if ("apk".equals(type)) return "APK";
        return "其他";
    }

    private class ResourceAdapter extends BaseAdapter {
        public int getCount() { return filteredResources.size(); }
        public Object getItem(int position) { return filteredResources.get(position); }
        public long getItemId(int position) { return position; }

        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(ResourceSnifferActivity.this)
                    .inflate(R.layout.item_resource, parent, false);
                holder = new ViewHolder();
                holder.tvType = (TextView) convertView.findViewById(R.id.tv_resource_type);
                holder.tvName = (TextView) convertView.findViewById(R.id.tv_resource_name);
                holder.tvUrl = (TextView) convertView.findViewById(R.id.tv_resource_url);
                holder.tvSize = (TextView) convertView.findViewById(R.id.tv_resource_size);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final ResourceInfo info = filteredResources.get(position);
            setTypeBadge(holder.tvType, info.type);
            holder.tvName.setText(info.name);
            holder.tvUrl.setText(info.url);
            if (info.size > 0) {
                holder.tvSize.setText(Utils.formatSize(info.size));
                holder.tvSize.setVisibility(View.VISIBLE);
            } else {
                holder.tvSize.setVisibility(View.GONE);
            }

            convertView.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { showResourceOptions(info); }
                });
            return convertView;
        }
    }

    private static class ViewHolder {
        TextView tvType, tvName, tvUrl, tvSize;
    }

    private static class ResourceInfo {
        String url;
        String type;
        String name;
        long size;
    }
}
