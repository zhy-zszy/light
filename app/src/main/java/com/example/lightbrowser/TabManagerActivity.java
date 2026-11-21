package com.example.lightbrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TabManagerActivity extends Activity {
    private static final String PREFS_NAME = "LightBrowserPrefs";
    private static final String KEY_TABS = "tabs";
    private static final String KEY_ACTIVE_TAB = "active_tab";
    public static final String EXTRA_CLEAR_HISTORY = "clear_history";

    private ListView lvTabs;
    private Button btnNewTab, btnCloseAll;
    private TabAdapter adapter;
    private List<TabInfo> tabs = new ArrayList<TabInfo>();
    private int activeTabIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_tab_manager);

        lvTabs = (ListView) findViewById(R.id.lv_tabs);
        btnNewTab = (Button) findViewById(R.id.btn_new_tab);
        btnCloseAll = (Button) findViewById(R.id.btn_close_all);

        adapter = new TabAdapter();
        lvTabs.setAdapter(adapter);

        loadTabs();

        String currentUrl = getIntent().getStringExtra("current_url");
        if (currentUrl != null && activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
            tabs.get(activeTabIndex).url = currentUrl;
            tabs.get(activeTabIndex).title = currentUrl;
            saveTabs();
            adapter.notifyDataSetChanged();
        }

        btnNewTab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { addNewTab(); }
        });
        btnCloseAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { closeAllTabs(); }
        });
    }

    private void loadTabs() {
        tabs.clear();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String tabsJson = prefs.getString(KEY_TABS, "");
        activeTabIndex = prefs.getInt(KEY_ACTIVE_TAB, 0);

        if (tabsJson.length() > 0) {
            try {
                JSONArray arr = new JSONArray(tabsJson);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    TabInfo tab = new TabInfo();
                    tab.id = obj.getLong("id");
                    tab.url = obj.getString("url");
                    tab.title = obj.optString("title", tab.url);
                    tabs.add(tab);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (tabs.isEmpty()) {
            TabInfo tab = new TabInfo();
            tab.id = System.currentTimeMillis();
            tab.url = prefs.getString("homepage", "https://www.bing.com");
            tab.title = tab.url;
            tabs.add(tab);
            activeTabIndex = 0;
            saveTabs();
        }

        adapter.notifyDataSetChanged();
    }

    private void saveTabs() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        JSONArray arr = new JSONArray();
        for (TabInfo tab : tabs) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", tab.id);
                obj.put("url", tab.url);
                obj.put("title", tab.title);
                arr.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        editor.putString(KEY_TABS, arr.toString());
        editor.putInt(KEY_ACTIVE_TAB, activeTabIndex);
        editor.commit();
    }

    private void addNewTab() {
        TabInfo tab = new TabInfo();
        tab.id = System.currentTimeMillis();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        tab.url = prefs.getString("homepage", "https://www.bing.com");
        tab.title = tab.url;
        tabs.add(tab);
        activeTabIndex = tabs.size() - 1;
        saveTabs();
        adapter.notifyDataSetChanged();

        Intent result = new Intent();
        result.putExtra("open_url", tab.url);
        result.putExtra(EXTRA_CLEAR_HISTORY, true);
        setResult(RESULT_OK, result);
        finish();
    }

    private void closeTab(int position) {
        if (position < 0 || position >= tabs.size()) return;
        tabs.remove(position);
        if (activeTabIndex >= tabs.size()) {
            activeTabIndex = tabs.size() - 1;
        }
        if (tabs.isEmpty()) {
            addNewTab();
            return;
        }
        saveTabs();
        adapter.notifyDataSetChanged();

        if (position == activeTabIndex || position < activeTabIndex) {
            Intent result = new Intent();
            result.putExtra("open_url", tabs.get(activeTabIndex).url);
            result.putExtra(EXTRA_CLEAR_HISTORY, true);
            setResult(RESULT_OK, result);
            finish();
        }
    }

    private void closeAllTabs() {
        if (tabs.size() <= 1) {
            Toast.makeText(this, "至少保留一个标签页", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("关闭所有标签页")
                .setMessage("确定要关闭除当前标签页外的所有标签页吗？")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        TabInfo active = tabs.get(activeTabIndex);
                        tabs.clear();
                        tabs.add(active);
                        activeTabIndex = 0;
                        saveTabs();
                        adapter.notifyDataSetChanged();
                        Toast.makeText(TabManagerActivity.this, "已关闭其他标签页", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void switchToTab(int position) {
        if (position < 0 || position >= tabs.size()) return;
        activeTabIndex = position;
        saveTabs();
        Intent result = new Intent();
        result.putExtra("open_url", tabs.get(position).url);
        result.putExtra(EXTRA_CLEAR_HISTORY, true);
        setResult(RESULT_OK, result);
        finish();
    }

    private void editTabUrl(final int position) {
        if (position < 0 || position >= tabs.size()) return;
        final TabInfo tab = tabs.get(position);
        final EditText input = new EditText(this);
        input.setText(tab.url);
        input.setSelection(input.getText().length());
        new AlertDialog.Builder(this)
                .setTitle("编辑网址")
                .setView(input)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        String url = input.getText().toString().trim();
                        if (url.length() > 0) {
                            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                url = "https://" + url;
                            }
                            tab.url = url;
                            tab.title = url;
                            saveTabs();
                            adapter.notifyDataSetChanged();
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private class TabAdapter extends BaseAdapter {
        @Override
        public int getCount() { return tabs.size(); }
        @Override
        public Object getItem(int position) { return tabs.get(position); }
        @Override
        public long getItemId(int position) { return tabs.get(position).id; }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(TabManagerActivity.this)
                        .inflate(R.layout.item_tab, parent, false);
                holder = new ViewHolder();
                holder.tvTitle = (TextView) convertView.findViewById(R.id.tv_tab_title);
                holder.tvUrl = (TextView) convertView.findViewById(R.id.tv_tab_url);
                holder.btnClose = (ImageButton) convertView.findViewById(R.id.btn_tab_close);
                holder.layout = (LinearLayout) convertView.findViewById(R.id.tab_item_layout);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final TabInfo tab = tabs.get(position);
            holder.tvTitle.setText(tab.title);
            holder.tvUrl.setText(tab.url);

            if (position == activeTabIndex) {
                holder.layout.setBackgroundColor(0xffe3f2fd);
            } else {
                holder.layout.setBackgroundColor(0xffffffff);
            }

            holder.layout.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { switchToTab(position); }
            });
            holder.layout.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    editTabUrl(position);
                    return true;
                }
            });
            holder.btnClose.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { closeTab(position); }
            });

            return convertView;
        }
    }

    private static class ViewHolder {
        TextView tvTitle, tvUrl;
        ImageButton btnClose;
        LinearLayout layout;
    }

    private static class TabInfo {
        long id;
        String url;
        String title;
    }
}