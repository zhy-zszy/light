package com.example.lightbrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadManagerActivity extends Activity {
    private ListView lvDownloads;
    private Button btnClearCompleted, btnClearAll;
    private DownloadAdapter adapter;
    private List<DownloadInfo> downloads = new ArrayList<DownloadInfo>();

    private BroadcastReceiver downloadReceiver;
    private boolean receiverRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_download_manager);

        lvDownloads = (ListView) findViewById(R.id.lv_downloads);
        btnClearCompleted = (Button) findViewById(R.id.btn_clear_completed);
        btnClearAll = (Button) findViewById(R.id.btn_clear_all);

        adapter = new DownloadAdapter();
        lvDownloads.setAdapter(adapter);

        loadDownloads();
        refreshDownloadStatus();

        btnClearCompleted.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { clearCompleted(); }
            });
        btnClearAll.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { clearAll(); }
            });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerDownloadReceiver();
        refreshDownloadStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterDownloadReceiver();
    }

    private void registerDownloadReceiver() {
        if (receiverRegistered) return;
        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                    refreshDownloadStatus();
                }
            }
        };
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadReceiver, filter);
        receiverRegistered = true;
    }

    private void unregisterDownloadReceiver() {
        if (receiverRegistered && downloadReceiver != null) {
            try { unregisterReceiver(downloadReceiver); } catch (Exception ignored) {}
            downloadReceiver = null;
            receiverRegistered = false;
        }
    }

    public static void addDownloadRecord(Context context, long downloadId, String fileName, String url) {
        android.content.SharedPreferences prefs = Utils.prefs(context);
        String json = prefs.getString("downloads_json", "");
        try {
            JSONArray arr = json.length() > 0 ? new JSONArray(json) : new JSONArray();
            JSONObject obj = new JSONObject();
            obj.put("id", downloadId);
            obj.put("fileName", fileName);
            obj.put("url", url);
            obj.put("status", "downloading");
            obj.put("totalBytes", 0);
            obj.put("downloadedBytes", 0);
            obj.put("timestamp", System.currentTimeMillis());
            arr.put(obj);
            prefs.edit().putString("downloads_json", arr.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadDownloads() {
        downloads.clear();
        String json = Utils.prefs(this).getString("downloads_json", "");
        if (json.length() > 0) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    DownloadInfo info = new DownloadInfo();
                    info.downloadId = obj.optLong("id", -1);
                    info.fileName = obj.optString("fileName", "");
                    info.url = obj.optString("url", "");
                    info.status = obj.optString("status", "downloading");
                    info.totalBytes = obj.optLong("totalBytes", 0);
                    info.downloadedBytes = obj.optLong("downloadedBytes", 0);
                    info.timestamp = obj.optLong("timestamp", 0);
                    downloads.add(info);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void saveDownloads() {
        JSONArray arr = new JSONArray();
        for (DownloadInfo info : downloads) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", info.downloadId);
                obj.put("fileName", info.fileName);
                obj.put("url", info.url);
                obj.put("status", info.status);
                obj.put("totalBytes", info.totalBytes);
                obj.put("downloadedBytes", info.downloadedBytes);
                obj.put("timestamp", info.timestamp);
                arr.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Utils.prefs(this).edit().putString("downloads_json", arr.toString()).apply();
    }

    private void refreshDownloadStatus() {
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) return;
        boolean changed = false;
        for (DownloadInfo info : downloads) {
            if (!"completed".equals(info.status) && !"failed".equals(info.status)) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(info.downloadId);
                Cursor cursor = dm.query(query);
                if (cursor != null && cursor.moveToFirst()) {
                    int statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                    int downloadedIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                    if (statusIdx >= 0) {
                        int status = cursor.getInt(statusIdx);
                        info.totalBytes = totalIdx >= 0 ? cursor.getLong(totalIdx) : 0;
                        info.downloadedBytes = downloadedIdx >= 0 ? cursor.getLong(downloadedIdx) : 0;

                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            info.status = "completed";
                            changed = true;
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            info.status = "failed";
                            changed = true;
                        } else if (status == DownloadManager.STATUS_PAUSED) {
                            info.status = "paused";
                        } else if (status == DownloadManager.STATUS_RUNNING) {
                            info.status = "downloading";
                        } else if (status == DownloadManager.STATUS_PENDING) {
                            info.status = "pending";
                        }
                    }
                    cursor.close();
                }
            }
        }
        if (changed) saveDownloads();
        adapter.notifyDataSetChanged();
    }

    private void clearCompleted() {
        List<DownloadInfo> toRemove = new ArrayList<DownloadInfo>();
        for (DownloadInfo info : downloads) {
            if ("completed".equals(info.status) || "failed".equals(info.status)) {
                toRemove.add(info);
            }
        }
        if (toRemove.isEmpty()) {
            Toast.makeText(this, "没有已完成项", Toast.LENGTH_SHORT).show();
            return;
        }
        downloads.removeAll(toRemove);
        saveDownloads();
        adapter.notifyDataSetChanged();
        Toast.makeText(this, "已清理 " + toRemove.size() + " 项", Toast.LENGTH_SHORT).show();
    }

    private void clearAll() {
        new AlertDialog.Builder(this)
            .setTitle("清除所有下载记录")
            .setMessage("确定要清除所有下载记录吗？（不会删除已下载的文件）")
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    downloads.clear();
                    saveDownloads();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(DownloadManagerActivity.this, "已清除所有记录", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void showDownloadOptions(final DownloadInfo info) {
        final List<String> options = new ArrayList<String>();
        final List<Integer> actions = new ArrayList<Integer>(); // 1:打开, 2:暂停, 3:继续, 4:取消, 5:删除

        if ("completed".equals(info.status)) {
            options.add("打开文件");
            actions.add(1);
        } else if ("downloading".equals(info.status) || "pending".equals(info.status)) {
            options.add("暂停下载");
            actions.add(2);
        } else if ("paused".equals(info.status)) {
            options.add("继续下载");
            actions.add(3);
        }

        if (!"completed".equals(info.status) && !"failed".equals(info.status)) {
            options.add("取消下载");
            actions.add(4);
        }

        options.add("删除记录");
        actions.add(5);

        new AlertDialog.Builder(this)
            .setTitle(info.fileName)
            .setItems(options.toArray(new String[0]), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which) {
                    int action = actions.get(which);
                    if (action == 1) openDownloadFile(info);
                    else if (action == 2) pauseDownload(info);
                    else if (action == 3) resumeDownload(info);
                    else if (action == 4) cancelDownload(info);
                    else if (action == 5) deleteDownload(info);
                }
            }).show();
    }

    private void pauseDownload(DownloadInfo info) {
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm != null) {
            try {
                // 使用反射调用，因为部分旧版本 SDK 中 DownloadManager 没有公开 pauseDownload
                java.lang.reflect.Method method = dm.getClass().getMethod("pauseDownload", long.class);
                method.invoke(dm, info.downloadId);
                Toast.makeText(this, "尝试暂停中...", Toast.LENGTH_SHORT).show();
                // 刷新状态稍有延迟
                lvDownloads.postDelayed(new Runnable() {
                        public void run() { refreshDownloadStatus(); }
                    }, 1000);
            } catch (Exception e) {
                Toast.makeText(this, "当前系统不支持暂停操作", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void resumeDownload(DownloadInfo info) {
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm != null) {
            try {
                java.lang.reflect.Method method = dm.getClass().getMethod("resumeDownload", long.class);
                method.invoke(dm, info.downloadId);
                Toast.makeText(this, "尝试继续中...", Toast.LENGTH_SHORT).show();
                lvDownloads.postDelayed(new Runnable() {
                        public void run() { refreshDownloadStatus(); }
                    }, 1000);
            } catch (Exception e) {
                Toast.makeText(this, "当前系统不支持继续操作", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void cancelDownload(final DownloadInfo info) {
        new AlertDialog.Builder(this)
            .setTitle("取消下载")
            .setMessage("确定要取消并删除已下载的部分吗？")
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (dm != null) {
                        dm.remove(info.downloadId);
                        downloads.remove(info);
                        saveDownloads();
                        adapter.notifyDataSetChanged();
                        Toast.makeText(DownloadManagerActivity.this, "已取消", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("否", null)
            .show();
    }

    private void openDownloadFile(DownloadInfo info) {
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(
                                     Environment.DIRECTORY_DOWNLOADS), info.fileName);
            if (file.exists()) {
                String mime = Utils.guessMime(info.url, null, info.fileName);
                Utils.openFile(this, file, mime);
            } else {
                Toast.makeText(this, "文件不存在，可能已被删除", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "无法打开文件", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteDownload(final DownloadInfo info) {
        new AlertDialog.Builder(this)
            .setTitle("删除记录")
            .setMessage("确定删除 \"" + info.fileName + "\" 的记录吗？（不会删除已下载的文件）")
            .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    downloads.remove(info);
                    saveDownloads();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(DownloadManagerActivity.this, "已删除记录", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private class DownloadAdapter extends BaseAdapter {
        public int getCount() { return downloads.size(); }
        public Object getItem(int position) { return downloads.get(position); }
        public long getItemId(int position) { return position; }

        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(DownloadManagerActivity.this)
                    .inflate(R.layout.item_download, parent, false);
                holder = new ViewHolder();
                holder.tvFileName = (TextView) convertView.findViewById(R.id.tv_download_name);
                holder.tvUrl = (TextView) convertView.findViewById(R.id.tv_download_url);
                holder.tvStatus = (TextView) convertView.findViewById(R.id.tv_download_status);
                holder.tvSize = (TextView) convertView.findViewById(R.id.tv_download_size);
                holder.progressBar = (ProgressBar) convertView.findViewById(R.id.download_progress);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            final DownloadInfo info = downloads.get(position);
            holder.tvFileName.setText(info.fileName);
            holder.tvUrl.setText(info.url);

            String statusText = info.status;
            int statusColor = 0xff757575;
            boolean showProgress = false;
            int progress = 0;

            if ("completed".equals(info.status)) {
                statusText = "✓ 已完成";
                statusColor = 0xff4caf50;
            } else if ("failed".equals(info.status)) {
                statusText = "✗ 下载失败";
                statusColor = 0xfff44336;
            } else if ("downloading".equals(info.status) || "pending".equals(info.status)) {
                statusText = "⬇ 下载中...";
                statusColor = 0xffff9800;
                showProgress = true;
                if (info.totalBytes > 0) {
                    progress = (int) (info.downloadedBytes * 100 / info.totalBytes);
                }
            } else if ("paused".equals(info.status)) {
                statusText = "⏸ 已暂停";
                statusColor = 0xffff9800;
                showProgress = true;
                if (info.totalBytes > 0) {
                    progress = (int) (info.downloadedBytes * 100 / info.totalBytes);
                }
            }

            holder.tvStatus.setText(statusText);
            holder.tvStatus.setTextColor(statusColor);

            if (showProgress) {
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.progressBar.setProgress(progress);
            } else {
                holder.progressBar.setVisibility(View.GONE);
            }

            if (info.totalBytes > 0) {
                holder.tvSize.setText(Utils.formatSize(info.totalBytes));
                holder.tvSize.setVisibility(View.VISIBLE);
            } else {
                holder.tvSize.setVisibility(View.GONE);
            }

            convertView.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { showDownloadOptions(info); }
                });
            return convertView;
        }
    }

    private static class ViewHolder {
        TextView tvFileName, tvUrl, tvStatus, tvSize;
        ProgressBar progressBar;
    }

    private static class DownloadInfo {
        long downloadId;
        String fileName;
        String url;
        String status;
        long totalBytes;
        long downloadedBytes;
        long timestamp;
    }
}
