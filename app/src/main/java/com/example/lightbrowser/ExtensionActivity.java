package com.example.lightbrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ExtensionActivity extends Activity {
    private static final String EXTENSION_DIR = "LightBrowser/extensions";
    private static final String EXTENSION_SUFFIX = ".apk";

    private ListView lvExtensions;
    private Button btnScanExtensions;
    private ArrayAdapter<String> adapter;
    private final List<String> extensionNames = new ArrayList<String>();
    private final List<File> extensionFiles = new ArrayList<File>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_extension);

        lvExtensions = (ListView) findViewById(R.id.lv_extensions);
        btnScanExtensions = (Button) findViewById(R.id.btn_scan_extensions);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, extensionNames);
        lvExtensions.setAdapter(adapter);

        btnScanExtensions.setOnClickListener(new android.view.View.OnClickListener() {
                public void onClick(android.view.View v) { scanExtensions(); }
            });
        lvExtensions.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
                public void onItemClick(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                    showExtensionOptions(position);
                }
            });

        File extDir = new File(Environment.getExternalStorageDirectory(), EXTENSION_DIR);
        if (!extDir.exists()) extDir.mkdirs();
        scanExtensions();
    }

    private void scanExtensions() {
        extensionNames.clear();
        extensionFiles.clear();

        File extDir = new File(Environment.getExternalStorageDirectory(), EXTENSION_DIR);
        if (extDir.exists() && extDir.isDirectory()) {
            File[] files = extDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(EXTENSION_SUFFIX)) {
                        extensionNames.add(file.getName());
                        extensionFiles.add(file);
                    }
                }
            }
        }

        if (extensionNames.isEmpty()) {
            extensionNames.add(getString(R.string.no_extensions));
        }
        adapter.notifyDataSetChanged();
    }

    private void showExtensionOptions(int position) {
        if (position >= extensionFiles.size()) return;
        final File extFile = extensionFiles.get(position);

        final String[] items = new String[]{
            getString(R.string.install_extension),
            getString(R.string.delete_file),
            getString(R.string.share_file)
        };

        new AlertDialog.Builder(this)
            .setTitle(extFile.getName())
            .setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0: installExtension(extFile); break;
                        case 1: deleteExtension(extFile); break;
                        case 2: shareExtension(extFile); break;
                    }
                }
            })
            .show();
    }

    private void installExtension(File file) {
        if (!file.exists()) {
            Toast.makeText(this, R.string.file_not_found, Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            && !getPackageManager().canRequestPackageInstalls()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                       Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            Toast.makeText(this, "请允许安装未知来源应用后重试", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            Utils.installApk(this, file);
        } catch (Exception e) {
            Toast.makeText(this, "无法安装: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteExtension(final File file) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.delete_extension)
            .setMessage(getString(R.string.confirm_delete) + " " + file.getName() + "?")
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (file.delete()) {
                        Toast.makeText(ExtensionActivity.this, R.string.deleted, Toast.LENGTH_SHORT).show();
                        scanExtensions();
                    } else {
                        Toast.makeText(ExtensionActivity.this, R.string.delete_failed, Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void shareExtension(File file) {
        try {
            Utils.shareFile(this, file, "application/vnd.android.package-archive");
        } catch (Exception e) {
            Toast.makeText(this, "分享失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
