package com.example.lightbrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DeveloperActivity extends Activity {
    private static final String TAG = "LightBrowserDev";
    private static final String PREFS_NAME = "LightBrowserPrefs";
    private static final String KEY_DEV_ENABLED = "dev_mode_enabled";
    private static final String KEY_LOG_ENABLED = "dev_log_enabled";
    private static final String KEY_INSPECT_ENABLED = "dev_inspect_enabled";
    private static final String KEY_OVERRIDE_UA = "dev_override_ua";
    private static final String KEY_CUSTOM_UA = "dev_custom_ua";
    private static final String KEY_BLOCK_IMAGES = "dev_block_images";
    private static final String KEY_BLOCK_JS = "dev_block_js";
    private static final String KEY_FORCE_DESKTOP = "dev_force_desktop";
    private static final String KEY_CACHE_MODE = "dev_cache_mode";
    private static final String KEY_CONSOLE_LOG = "dev_console_log";
    private static final String KEY_NETWORK_LOG = "dev_network_log";
    private static final String LOG_FILE = "LightBrowser/dev_log.txt";

    private CheckBox cbDevMode, cbLogEnabled, cbInspectEnabled, cbBlockImages,
        cbBlockJs, cbForceDesktop, cbConsoleLog, cbNetworkLog;
    private EditText etCustomUa;
    private Button btnSaveUa, btnClearLog, btnViewLog, btnExportLog,
        btnInjectJs, btnViewSource, btnClearCache, btnTestUa, btnBack;
    private TextView tvDevInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_developer);

        initViews();
        loadSettings();
        setupListeners();
        updateDevInfo();
    }

    private void initViews() {
        cbDevMode = (CheckBox) findViewById(R.id.cb_dev_mode);
        cbLogEnabled = (CheckBox) findViewById(R.id.cb_log_enabled);
        cbInspectEnabled = (CheckBox) findViewById(R.id.cb_inspect_enabled);
        cbBlockImages = (CheckBox) findViewById(R.id.cb_block_images);
        cbBlockJs = (CheckBox) findViewById(R.id.cb_block_js);
        cbForceDesktop = (CheckBox) findViewById(R.id.cb_force_desktop);
        cbConsoleLog = (CheckBox) findViewById(R.id.cb_console_log);
        cbNetworkLog = (CheckBox) findViewById(R.id.cb_network_log);
        etCustomUa = (EditText) findViewById(R.id.et_custom_ua);
        btnSaveUa = (Button) findViewById(R.id.btn_save_ua);
        btnClearLog = (Button) findViewById(R.id.btn_clear_log);
        btnViewLog = (Button) findViewById(R.id.btn_view_log);
        btnExportLog = (Button) findViewById(R.id.btn_export_log);
        btnInjectJs = (Button) findViewById(R.id.btn_inject_js);
        btnViewSource = (Button) findViewById(R.id.btn_view_source);
        btnClearCache = (Button) findViewById(R.id.btn_clear_dev_cache);
        btnTestUa = (Button) findViewById(R.id.btn_test_ua);
        btnBack = (Button) findViewById(R.id.btn_dev_back);
        tvDevInfo = (TextView) findViewById(R.id.tv_dev_info);
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        cbDevMode.setChecked(prefs.getBoolean(KEY_DEV_ENABLED, false));
        cbLogEnabled.setChecked(prefs.getBoolean(KEY_LOG_ENABLED, false));
        cbInspectEnabled.setChecked(prefs.getBoolean(KEY_INSPECT_ENABLED, false));
        cbBlockImages.setChecked(prefs.getBoolean(KEY_BLOCK_IMAGES, false));
        cbBlockJs.setChecked(prefs.getBoolean(KEY_BLOCK_JS, false));
        cbForceDesktop.setChecked(prefs.getBoolean(KEY_FORCE_DESKTOP, false));
        cbConsoleLog.setChecked(prefs.getBoolean(KEY_CONSOLE_LOG, false));
        cbNetworkLog.setChecked(prefs.getBoolean(KEY_NETWORK_LOG, false));
        etCustomUa.setText(prefs.getString(KEY_CUSTOM_UA, ""));
    }

    private void setupListeners() {
        cbDevMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                saveBool(KEY_DEV_ENABLED, isChecked);
                Toast.makeText(DeveloperActivity.this,
                    isChecked ? "开发者模式已开启" : "开发者模式已关闭", Toast.LENGTH_SHORT).show();
                updateDevInfo();
            }
        });

        cbLogEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                saveBool(KEY_LOG_ENABLED, isChecked);
                Toast.makeText(DeveloperActivity.this,
                    isChecked ? "日志记录已开启" : "日志记录已关闭", Toast.LENGTH_SHORT).show();
            }
        });

        cbInspectEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                saveBool(KEY_INSPECT_ENABLED, isChecked);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    WebView.setWebContentsDebuggingEnabled(isChecked);
                    Toast.makeText(DeveloperActivity.this,
                        isChecked ? "WebView调试已开启 (chrome://inspect)" : "WebView调试已关闭",
                        Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(DeveloperActivity.this, "系统版本过低，不支持WebView调试",
                        Toast.LENGTH_SHORT).show();
                }
            }
        });

        cbBlockImages.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                saveBool(KEY_BLOCK_IMAGES, isChecked);
                Toast.makeText(DeveloperActivity.this,
                    isChecked ? "已禁用图片加载" : "已恢复图片加载", Toast.LENGTH_SHORT).show();
            }
        });

        cbBlockJs.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                saveBool(KEY_BLOCK_JS, isChecked);
                Toast.makeText(DeveloperActivity.this,
                    isChecked ? "已禁用JavaScript" : "已恢复JavaScript", Toast.LENGTH_SHORT).show();
            }
        });

        cbForceDesktop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                saveBool(KEY_FORCE_DESKTOP, isChecked);
                Toast.makeText(DeveloperActivity.this,
                    isChecked ? "已强制桌面模式" : "已取消强制桌面模式", Toast.LENGTH_SHORT).show();
            }
        });

        cbConsoleLog.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                saveBool(KEY_CONSOLE_LOG, isChecked);
                Toast.makeText(DeveloperActivity.this,
                    isChecked ? "控制台日志已开启" : "控制台日志已关闭", Toast.LENGTH_SHORT).show();
            }
        });

        cbNetworkLog.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                saveBool(KEY_NETWORK_LOG, isChecked);
                Toast.makeText(DeveloperActivity.this,
                    isChecked ? "网络请求日志已开启" : "网络请求日志已关闭", Toast.LENGTH_SHORT).show();
            }
        });

        btnSaveUa.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String ua = etCustomUa.getText().toString().trim();
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putBoolean(KEY_OVERRIDE_UA, ua.length() > 0);
                editor.putString(KEY_CUSTOM_UA, ua);
                editor.commit();
                Toast.makeText(DeveloperActivity.this,
                    ua.length() > 0 ? "自定义UA已保存" : "已清除自定义UA", Toast.LENGTH_SHORT).show();
            }
        });

        btnTestUa.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String ua = etCustomUa.getText().toString().trim();
                if (ua.length() == 0) {
                    Toast.makeText(DeveloperActivity.this, "请输入UA再测试", Toast.LENGTH_SHORT).show();
                    return;
                }
                new AlertDialog.Builder(DeveloperActivity.this)
                    .setTitle("UA测试")
                    .setMessage("当前UA:\n" + ua)
                    .setPositiveButton("复制", new android.content.DialogInterface.OnClickListener() {
                        public void onClick(android.content.DialogInterface dialog, int which) {
                            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            if (clipboard != null) {
                                clipboard.setPrimaryClip(ClipData.newPlainText("UA", ua));
                                Toast.makeText(DeveloperActivity.this, "已复制", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .setNegativeButton("关闭", null)
                    .show();
            }
        });

        btnClearLog.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { clearLogFile(); }
        });

        btnViewLog.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { viewLogFile(); }
        });

        btnExportLog.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { exportLogFile(); }
        });

        btnInjectJs.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showInjectJsDialog(); }
        });

        btnViewSource.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { viewPageSource(); }
        });

        btnClearCache.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { clearDevCache(); }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
    }

    private void saveBool(String key, boolean value) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    private void updateDevInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("设备信息:\n");
        sb.append("Android版本: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("设备型号: ").append(Build.MODEL).append("\n");
        sb.append("制造商: ").append(Build.MANUFACTURER).append("\n");
        sb.append("\nWebView信息:\n");
        try {
            WebView wv = new WebView(this);
            WebSettings ws = wv.getSettings();
            sb.append("UserAgent: ").append(ws.getUserAgentString()).append("\n");
            sb.append("JavaScript: ").append(ws.getJavaScriptEnabled()).append("\n");
            sb.append("DomStorage: ").append(ws.getDomStorageEnabled()).append("\n");
            sb.append("Database: ").append(ws.getDatabaseEnabled()).append("\n");
            sb.append("CacheMode: ").append(ws.getCacheMode()).append("\n");
            wv.destroy();
        } catch (Exception e) {
            sb.append("获取失败: ").append(e.getMessage()).append("\n");
        }
        sb.append("\n开发者状态:\n");
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        sb.append("开发者模式: ").append(prefs.getBoolean(KEY_DEV_ENABLED, false) ? "开启" : "关闭").append("\n");
        sb.append("日志记录: ").append(prefs.getBoolean(KEY_LOG_ENABLED, false) ? "开启" : "关闭").append("\n");
        sb.append("WebView调试: ").append(prefs.getBoolean(KEY_INSPECT_ENABLED, false) ? "开启" : "关闭").append("\n");
        sb.append("自定义UA: ").append(prefs.getBoolean(KEY_OVERRIDE_UA, false) ? "已启用" : "未启用").append("\n");
        tvDevInfo.setText(sb.toString());
    }

    private void clearLogFile() {
        try {
            File file = new File(Environment.getExternalStorageDirectory(), LOG_FILE);
            if (file.exists()) {
                FileWriter writer = new FileWriter(file, false);
                writer.write("");
                writer.close();
            }
            Toast.makeText(this, "日志已清空", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "清空失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void viewLogFile() {
        try {
            File file = new File(Environment.getExternalStorageDirectory(), LOG_FILE);
            if (!file.exists() || file.length() == 0) {
                Toast.makeText(this, "暂无日志", Toast.LENGTH_SHORT).show();
                return;
            }
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < 500) {
                sb.append(line).append("\n");
                count++;
            }
            reader.close();
            new AlertDialog.Builder(this)
                .setTitle("日志 (最近" + count + "条)")
                .setMessage(sb.toString().length() > 0 ? sb.toString() : "空")
                .setPositiveButton("关闭", null)
                .show();
        } catch (Exception e) {
            Toast.makeText(this, "读取失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportLogFile() {
        try {
            File src = new File(Environment.getExternalStorageDirectory(), LOG_FILE);
            if (!src.exists() || src.length() == 0) {
                Toast.makeText(this, "暂无日志可导出", Toast.LENGTH_SHORT).show();
                return;
            }
            File dst = new File(Environment.getExternalStorageDirectory(),
                "LightBrowser/dev_log_" + System.currentTimeMillis() + ".txt");
            java.nio.channels.FileChannel srcChannel = new java.io.FileInputStream(src).getChannel();
            java.nio.channels.FileChannel dstChannel = new java.io.FileOutputStream(dst).getChannel();
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
            srcChannel.close();
            dstChannel.close();
            Toast.makeText(this, "日志已导出到:\n" + dst.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showInjectJsDialog() {
        final EditText input = new EditText(this);
        input.setHint("输入要注入的JavaScript代码");
        input.setMinLines(3);
        input.setGravity(android.view.Gravity.TOP);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 10, 20, 10);
        layout.addView(input);

        new AlertDialog.Builder(this)
            .setTitle("注入JavaScript")
            .setView(layout)
            .setPositiveButton("注入", new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface dialog, int which) {
                    String js = input.getText().toString().trim();
                    if (js.length() > 0) {
                        Intent result = new Intent();
                        result.putExtra("inject_js", js);
                        setResult(RESULT_OK, result);
                        Toast.makeText(DeveloperActivity.this, "代码已准备，返回页面后注入", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void viewPageSource() {
        Intent result = new Intent();
        result.putExtra("view_source", true);
        setResult(RESULT_OK, result);
        Toast.makeText(this, "返回页面后查看源码", Toast.LENGTH_SHORT).show();
    }

    private void clearDevCache() {
        try {
            WebView wv = new WebView(this);
            wv.clearCache(true);
            wv.destroy();
            Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "清除失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 静态方法供其他Activity调用
    public static boolean isDevModeEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DEV_ENABLED, false);
    }

    public static boolean isLogEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_LOG_ENABLED, false);
    }

    public static boolean isInspectEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_INSPECT_ENABLED, false);
    }

    public static boolean isBlockImages(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_BLOCK_IMAGES, false);
    }

    public static boolean isBlockJs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_BLOCK_JS, false);
    }

    public static boolean isForceDesktop(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_FORCE_DESKTOP, false);
    }

    public static boolean isConsoleLog(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_CONSOLE_LOG, false);
    }

    public static boolean isNetworkLog(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NETWORK_LOG, false);
    }

    public static String getCustomUa(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_OVERRIDE_UA, false)) {
            return prefs.getString(KEY_CUSTOM_UA, "");
        }
        return null;
    }

    public static void log(Context context, String tag, String message) {
        if (!isLogEnabled(context)) return;
        try {
            File dir = new File(Environment.getExternalStorageDirectory(), "LightBrowser");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "dev_log.txt");
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault());
            String line = sdf.format(new Date()) + " [" + tag + "] " + message + "\n";
            FileWriter writer = new FileWriter(file, true);
            writer.write(line);
            writer.close();
        } catch (Exception e) {
            Log.e(TAG, "Log write failed", e);
        }
    }
}
