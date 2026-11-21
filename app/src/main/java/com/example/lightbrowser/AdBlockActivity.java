package com.example.lightbrowser;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 广告拦截页面 - 重构版
 * 主要改动：
 * 1. 修复资源泄漏（BufferedReader未关闭）
 * 2. 添加常量定义
 * 3. 优化异常处理
 * 4. 完全兼容Java 7
 */
public class AdBlockActivity extends Activity {
    private static final String PREFS_NAME = "LightBrowserPrefs";
    private static final String KEY_ADBLOCK = "adblock_enabled";
    private static final String ADBLOCK_FILE = "LightBrowser/adblock.txt";

    private CheckBox cbAdBlock;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_adblock);

        cbAdBlock = (CheckBox) findViewById(R.id.cb_adblock_enable);
        tvStatus = (TextView) findViewById(R.id.tv_adblock_status);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(KEY_ADBLOCK, false);
        cbAdBlock.setChecked(enabled);

        updateStatus();

        cbAdBlock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putBoolean(KEY_ADBLOCK, isChecked);
                editor.commit();
                Toast.makeText(AdBlockActivity.this,
                        isChecked ? R.string.adblock_enabled : R.string.adblock_disabled,
                        Toast.LENGTH_SHORT).show();
                updateStatus();
            }
        });
    }

    /**
     * 更新状态 - 修复资源泄漏
     */
    private void updateStatus() {
        List<String> rules = new ArrayList<String>();
        File ruleFile = new File(Environment.getExternalStorageDirectory(), ADBLOCK_FILE);
        if (ruleFile.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(ruleFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.length() > 0 && !line.startsWith("!")) {
                        rules.add(line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (rules.isEmpty()) {
            tvStatus.setText(R.string.no_adblock_rules);
        } else {
            tvStatus.setText(rules.size() + " " + getString(R.string.adblock_rules_loaded));
        }
    }
}
