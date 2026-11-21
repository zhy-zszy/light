package com.example.lightbrowser;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebSettings;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

public class SiteSettingsActivity extends Activity {
    private static final String PREFS_NAME = "LightBrowserPrefs";

    private TextView tvSiteHost;
    private CheckBox cbJavaScript, cbDomStorage, cbAllowImages, cbAllowPopup, cbDesktopMode, cbSafeBrowsing;
    private Button btnSave, btnReset, btnBack;

    private String currentUrl;
    private SiteSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_site_settings);

        currentUrl = getIntent().getStringExtra("url");
        if (currentUrl == null) {
            Toast.makeText(this, "无效网址", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        settings = SiteSettings.load(this, currentUrl);

        tvSiteHost = (TextView) findViewById(R.id.tv_site_host);
        cbJavaScript = (CheckBox) findViewById(R.id.cb_javascript);
        cbDomStorage = (CheckBox) findViewById(R.id.cb_dom_storage);
        cbAllowImages = (CheckBox) findViewById(R.id.cb_allow_images);
        cbAllowPopup = (CheckBox) findViewById(R.id.cb_allow_popup);
        cbDesktopMode = (CheckBox) findViewById(R.id.cb_desktop_mode);
        cbSafeBrowsing = (CheckBox) findViewById(R.id.cb_safe_browsing);
        btnSave = (Button) findViewById(R.id.btn_save_site_settings);
        btnReset = (Button) findViewById(R.id.btn_reset_site_settings);
        btnBack = (Button) findViewById(R.id.btn_back);

        try {
            java.net.URI uri = new java.net.URI(currentUrl);
            tvSiteHost.setText("网站: " + uri.getHost());
        } catch (Exception e) {
            tvSiteHost.setText("网站: " + currentUrl);
        }

        cbJavaScript.setChecked(settings.javascript);
        cbDomStorage.setChecked(settings.domStorage);
        cbAllowImages.setChecked(settings.allowImages);
        cbAllowPopup.setChecked(settings.allowPopup);
        cbDesktopMode.setChecked(settings.desktopMode);
        cbSafeBrowsing.setChecked(settings.safeBrowsing);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            cbSafeBrowsing.setEnabled(false);
        }

        btnSave.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) { saveSettings(); }
        });
        btnReset.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) { resetSettings(); }
        });
        btnBack.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) { finish(); }
        });
    }

    private void saveSettings() {
        settings.javascript = cbJavaScript.isChecked();
        settings.domStorage = cbDomStorage.isChecked();
        settings.allowImages = cbAllowImages.isChecked();
        settings.allowPopup = cbAllowPopup.isChecked();
        settings.desktopMode = cbDesktopMode.isChecked();
        settings.safeBrowsing = cbSafeBrowsing.isChecked();
        settings.save(this);

        setResult(RESULT_OK);
        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void resetSettings() {
        SiteSettings.reset(this, currentUrl);
        Toast.makeText(this, "已恢复默认", Toast.LENGTH_SHORT).show();

        cbJavaScript.setChecked(true);
        cbDomStorage.setChecked(true);
        cbAllowImages.setChecked(true);
        cbAllowPopup.setChecked(false);
        cbDesktopMode.setChecked(false);
        cbSafeBrowsing.setChecked(true);

        setResult(RESULT_OK);
    }
}
