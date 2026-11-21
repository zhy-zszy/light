package com.example.lightbrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.HashSet;

public class SettingsActivity extends Activity {
    private static final String DEFAULT_HOMEPAGE = Utils.DEFAULT_HOMEPAGE;

    private EditText etHomepage;
    private CheckBox cbBlockImages, cbForceZoom, cbVolumeScroll, cbKeepScreenOn, cbIncognito, cbClearOnExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_settings);

        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        etHomepage = (EditText) findViewById(R.id.et_homepage);
        cbBlockImages = (CheckBox) findViewById(R.id.cb_block_images);
        cbForceZoom = (CheckBox) findViewById(R.id.cb_force_zoom);
        cbVolumeScroll = (CheckBox) findViewById(R.id.cb_volume_scroll);
        cbKeepScreenOn = (CheckBox) findViewById(R.id.cb_keep_screen_on);
        cbIncognito = (CheckBox) findViewById(R.id.cb_incognito);
        cbClearOnExit = (CheckBox) findViewById(R.id.cb_clear_on_exit);
    }

    private void loadSettings() {
        SharedPreferences prefs = Utils.prefs(this);
        etHomepage.setText(prefs.getString("homepage", DEFAULT_HOMEPAGE));
        cbBlockImages.setChecked(prefs.getBoolean("block_images", false));
        cbForceZoom.setChecked(prefs.getBoolean("force_zoom", true));
        cbVolumeScroll.setChecked(prefs.getBoolean("volume_scroll", true));
        cbKeepScreenOn.setChecked(prefs.getBoolean("keep_screen_on", false));
        cbIncognito.setChecked(prefs.getBoolean("incognito", false));
        cbClearOnExit.setChecked(prefs.getBoolean("clear_on_exit", false));

        String engine = prefs.getString("search_engine", "https://www.bing.com/search?q=");
        if (engine.contains("baidu")) {
            ((RadioButton) findViewById(R.id.rb_baidu)).setChecked(true);
        } else if (engine.contains("google")) {
            ((RadioButton) findViewById(R.id.rb_google)).setChecked(true);
        } else {
            ((RadioButton) findViewById(R.id.rb_bing)).setChecked(true);
        }
    }

    private void setupListeners() {
        findViewById(R.id.btn_save_homepage).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String url = etHomepage.getText().toString().trim();
                if (url.length() == 0) url = DEFAULT_HOMEPAGE;
                if (!url.startsWith("http://") && !url.startsWith("https://")
                        && !url.startsWith("https://lightbrowser")) {
                    url = "https://" + url;
                }
                Utils.putString(SettingsActivity.this, "homepage", url);
                Toast.makeText(SettingsActivity.this, "主页已保存", Toast.LENGTH_SHORT).show();
            }
        });

        ((RadioGroup) findViewById(R.id.rg_search_engine)).setOnCheckedChangeListener(
            new RadioGroup.OnCheckedChangeListener() {
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    SharedPreferences.Editor editor = Utils.prefs(SettingsActivity.this).edit();
                    if (checkedId == R.id.rb_baidu) {
                        editor.putString("search_engine", "https://www.baidu.com/s?wd=");
                    } else if (checkedId == R.id.rb_google) {
                        editor.putString("search_engine", "https://www.google.com/search?q=");
                    } else {
                        editor.putString("search_engine", "https://www.bing.com/search?q=");
                    }
                    editor.apply();
                }
            });

        findViewById(R.id.btn_text_small).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { setTextSize(75); }
        });
        findViewById(R.id.btn_text_normal).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { setTextSize(100); }
        });
        findViewById(R.id.btn_text_large).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { setTextSize(150); }
        });

        CompoundButton.OnCheckedChangeListener toggleListener = new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = Utils.prefs(SettingsActivity.this).edit();
                int id = buttonView.getId();
                if (id == R.id.cb_block_images) editor.putBoolean("block_images", isChecked);
                else if (id == R.id.cb_force_zoom) editor.putBoolean("force_zoom", isChecked);
                else if (id == R.id.cb_volume_scroll) editor.putBoolean("volume_scroll", isChecked);
                else if (id == R.id.cb_keep_screen_on) editor.putBoolean("keep_screen_on", isChecked);
                else if (id == R.id.cb_incognito) editor.putBoolean("incognito", isChecked);
                else if (id == R.id.cb_clear_on_exit) editor.putBoolean("clear_on_exit", isChecked);
                editor.apply();
            }
        };

        cbBlockImages.setOnCheckedChangeListener(toggleListener);
        cbForceZoom.setOnCheckedChangeListener(toggleListener);
        cbVolumeScroll.setOnCheckedChangeListener(toggleListener);
        cbKeepScreenOn.setOnCheckedChangeListener(toggleListener);
        cbIncognito.setOnCheckedChangeListener(toggleListener);
        cbClearOnExit.setOnCheckedChangeListener(toggleListener);

        findViewById(R.id.btn_clear_cache).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { clearCache(); }
        });
        findViewById(R.id.btn_clear_cookies).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { clearCookies(); }
        });
        findViewById(R.id.btn_clear_history).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { clearHistory(); }
        });
        findViewById(R.id.btn_clear_data).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showClearAllDataDialog(); }
        });
    }

    private void setTextSize(int percent) {
        Utils.putInt(this, "text_size", percent);
        Toast.makeText(this, "字体大小已更改", Toast.LENGTH_SHORT).show();
    }

    private void clearCache() {
        WebView webView = new WebView(this);
        webView.clearCache(true);
        webView.destroy();
        Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show();
    }

    private void clearCookies() {
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(null);
        } else {
            cookieManager.removeAllCookie();
        }
        Toast.makeText(this, "Cookies已清除", Toast.LENGTH_SHORT).show();
    }

    private void clearHistory() {
        Utils.prefs(this).edit().remove("history_json").apply();
        Toast.makeText(this, "历史记录已清除", Toast.LENGTH_SHORT).show();
    }

    private void showClearAllDataDialog() {
        new AlertDialog.Builder(this)
            .setTitle("恢复出厂设置")
            .setMessage("将清除所有缓存、Cookies、历史记录、书签以及设置，是否继续？")
            .setPositiveButton("彻底清除", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) { clearAllData(); }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void clearAllData() {
        clearCache();
        clearCookies();
        Utils.prefs(this).edit().clear().apply();
        Toast.makeText(this, "已重置所有数据", Toast.LENGTH_LONG).show();
        finish();
    }
}
