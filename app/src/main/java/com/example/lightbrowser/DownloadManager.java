package com.example.lightbrowser;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URLDecoder;

/**
 * WebView 下载处理器（原类名保留，内部使用系统 DownloadManager 全限定名）。
 */
public class DownloadManager implements DownloadListener {
    private final Context context;
    private final WebView webView;

    public DownloadManager(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
    }

    @Override
    public void onDownloadStart(final String url, final String userAgent,
                                String contentDisposition, final String mimetype, long contentLength) {
        String fileName = guessFileName(url, contentDisposition, mimetype);
        final String finalFileName = fileName;

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        TextView tvInfo = new TextView(context);
        tvInfo.setText("文件格式: " + (mimetype == null ? "未知" : mimetype));
        tvInfo.setPadding(0, 0, 0, 15);

        final EditText etFileName = new EditText(context);
        etFileName.setText(fileName);
        etFileName.setSelectAllOnFocus(true);

        layout.addView(tvInfo);
        layout.addView(etFileName);

        new AlertDialog.Builder(context)
            .setTitle("下载文件")
            .setView(layout)
            .setPositiveButton("开始下载", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String customName = etFileName.getText().toString().trim();
                    if (customName.length() == 0) customName = finalFileName;
                    startDownload(url, userAgent, mimetype, customName);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void startDownload(String url, String userAgent, String mimetype, String fileName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "需要存储权限才能下载", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        try {
            android.app.DownloadManager.Request request =
                new android.app.DownloadManager.Request(Uri.parse(url));
            request.setMimeType(Utils.guessMime(url, mimetype, fileName));
            if (userAgent != null) request.addRequestHeader("User-Agent", userAgent);
            request.addRequestHeader("Accept", "*/*");
            if (webView != null && webView.getUrl() != null) {
                request.addRequestHeader("Referer", webView.getUrl());
            }
            request.setTitle(fileName);
            request.setNotificationVisibility(
                android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            android.app.DownloadManager dm = (android.app.DownloadManager)
                context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                long id = dm.enqueue(request);
                DownloadManagerActivity.addDownloadRecord(context, id, fileName, url);
                Toast.makeText(context, "开始下载: " + fileName, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "下载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String guessFileName(String url, String contentDisposition, String mimetype) {
        String fileName = null;
        if (contentDisposition != null) {
            int idx = contentDisposition.toLowerCase().indexOf("filename=");
            if (idx >= 0) {
                fileName = contentDisposition.substring(idx + 9).replace("\"", "").replace("'", "");
                int semicolon = fileName.indexOf(";");
                if (semicolon > 0) fileName = fileName.substring(0, semicolon);
                try { fileName = URLDecoder.decode(fileName, "UTF-8"); } catch (Exception ignored) {}
            }
        }
        if (fileName == null || fileName.length() == 0) {
            try {
                String path = url.split("\\?")[0];
                fileName = path.substring(path.lastIndexOf("/") + 1);
                fileName = URLDecoder.decode(fileName, "UTF-8");
            } catch (Exception ignored) {}
        }
        if (fileName == null || fileName.length() == 0 || !fileName.contains(".")) {
            fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
        }
        return Utils.sanitizeFileName(fileName);
    }

    public void retryPending() {}
}
