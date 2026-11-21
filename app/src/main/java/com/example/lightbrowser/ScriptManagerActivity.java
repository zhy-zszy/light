package com.example.lightbrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class ScriptManagerActivity extends Activity {
    private static final String TAG = "LightBrowserScripts";

    private ListView lvScripts;
    private Button btnRefreshScripts;
    private ArrayAdapter<String> adapter;
    private List<String> scriptNames = new ArrayList<String>();
    private List<File> scriptFiles = new ArrayList<File>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_script_manager);

        lvScripts = (ListView) findViewById(R.id.lv_scripts);
        btnRefreshScripts = (Button) findViewById(R.id.btn_refresh_scripts);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, scriptNames);
        lvScripts.setAdapter(adapter);

        btnRefreshScripts.setOnClickListener(new android.view.View.OnClickListener() {
            public void onClick(android.view.View v) { loadScripts(); }
        });
        lvScripts.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            public void onItemClick(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                showScriptOptions(position);
            }
        });

        File scriptsDir = new File(Environment.getExternalStorageDirectory(), "LightBrowser/scripts");
        if (!scriptsDir.exists()) {
            scriptsDir.mkdirs();
        }
        loadScripts();
    }

    private void loadScripts() {
        scriptNames.clear();
        scriptFiles.clear();

        File scriptsDir = new File(Environment.getExternalStorageDirectory(), "LightBrowser/scripts");
        if (scriptsDir.exists() && scriptsDir.isDirectory()) {
            File[] files = scriptsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".js")) {
                        scriptNames.add(file.getName());
                        scriptFiles.add(file);
                    }
                }
            }
        }

        if (scriptNames.isEmpty()) {
            scriptNames.add(getString(R.string.no_scripts));
        }
        adapter.notifyDataSetChanged();
    }

    private void showScriptOptions(int position) {
        if (position >= scriptFiles.size()) return;
        final File scriptFile = scriptFiles.get(position);

        final String[] items = new String[] {
            getString(R.string.view_content),
            getString(R.string.delete_script),
            getString(R.string.copy_to_clipboard)
        };

        new AlertDialog.Builder(this)
            .setTitle(scriptFile.getName())
            .setItems(items, new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface dialog, int which) {
                    switch (which) {
                        case 0: viewScriptContent(scriptFile); break;
                        case 1: deleteScript(scriptFile); break;
                        case 2: copyScriptToClipboard(scriptFile); break;
                    }
                }
            })
            .show();
    }

    private void viewScriptContent(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            String content = new String(data, "UTF-8");

            new AlertDialog.Builder(this)
                .setTitle(file.getName())
                .setMessage(content)
                .setPositiveButton(R.string.close, null)
                .show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to read script", e);
            Toast.makeText(this, R.string.read_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteScript(final File file) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.delete_script)
            .setMessage(getString(R.string.confirm_delete) + " " + file.getName() + "?")
            .setPositiveButton(R.string.delete, new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface dialog, int which) {
                    if (file.delete()) {
                        Toast.makeText(ScriptManagerActivity.this, R.string.deleted, Toast.LENGTH_SHORT).show();
                        loadScripts();
                    } else {
                        Toast.makeText(ScriptManagerActivity.this, R.string.delete_failed, Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void copyScriptToClipboard(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            String content = new String(data, "UTF-8");

            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText(file.getName(), content);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy script", e);
            Toast.makeText(this, R.string.copy_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
