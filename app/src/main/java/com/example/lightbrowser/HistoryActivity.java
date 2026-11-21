package com.example.lightbrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends Activity {
    private ListView lvHistory;
    private HistoryAdapter adapter;
    private final List<HistoryInfo> historyList = new ArrayList<HistoryInfo>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_history);

        lvHistory = (ListView) findViewById(R.id.lv_history);
        adapter = new HistoryAdapter();
        lvHistory.setAdapter(adapter);
        loadHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }

    private void loadHistory() {
        historyList.clear();
        String json = Utils.prefs(this).getString("history_json", "");
        if (json.length() > 0) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    HistoryInfo info = new HistoryInfo();
                    info.time = obj.optLong("time", 0);
                    info.title = obj.optString("title", "");
                    info.url = obj.optString("url", "");
                    if (info.url.length() > 0) historyList.add(info);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Collections.sort(historyList, new Comparator<HistoryInfo>() {
                public int compare(HistoryInfo a, HistoryInfo b) {
                    if (b.time < a.time) return -1;
                    if (b.time > a.time) return 1;
                    return 0;
                }
            });
        if (historyList.isEmpty()) {
            HistoryInfo empty = new HistoryInfo();
            empty.title = getString(R.string.no_history);
            empty.url = "";
            empty.time = 0;
            historyList.add(empty);
        }
        adapter.notifyDataSetChanged();
    }

    private void deleteHistoryItem(final int position) {
        if (position < 0 || position >= historyList.size()) return;
        final HistoryInfo info = historyList.get(position);
        if (info.url.length() == 0) return;

        new AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(getString(R.string.confirm_delete) + " \"" + info.title + "\"?")
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        JSONArray arr = new JSONArray(Utils.prefs(HistoryActivity.this).getString("history_json", "[]"));
                        JSONArray newArr = new JSONArray();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            if (!(info.url.equals(obj.optString("url"))
                                && info.time == obj.optLong("time", 0))) {
                                newArr.put(obj);
                            }
                        }
                        Utils.prefs(HistoryActivity.this).edit().putString("history_json", newArr.toString()).apply();
                        loadHistory();
                        Toast.makeText(HistoryActivity.this, R.string.deleted, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void openHistoryItem(HistoryInfo info) {
        if (info.url.length() == 0) return;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(info.url));
        intent.setClass(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private class HistoryAdapter extends BaseAdapter {
        private final LayoutInflater inflater = LayoutInflater.from(HistoryActivity.this);

        public int getCount() { return historyList.size(); }
        public Object getItem(int position) { return historyList.get(position); }
        public long getItemId(int position) { return position; }

        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
                holder = new ViewHolder();
                holder.tvTitle = (TextView) convertView.findViewById(android.R.id.text1);
                holder.tvSubtitle = (TextView) convertView.findViewById(android.R.id.text2);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final HistoryInfo info = historyList.get(position);
            holder.tvTitle.setText(info.title);
            if (info.time > 0) {
                holder.tvSubtitle.setText(sdf.format(new Date(info.time)) + "  " + info.url);
            } else {
                holder.tvSubtitle.setText(info.url);
            }
            holder.tvSubtitle.setSingleLine(true);
            holder.tvSubtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);

            convertView.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { openHistoryItem(info); }
                });
            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                    public boolean onLongClick(View v) {
                        deleteHistoryItem(position);
                        return true;
                    }
                });
            return convertView;
        }

        class ViewHolder {
            TextView tvTitle;
            TextView tvSubtitle;
        }
    }

    private static class HistoryInfo {
        long time;
        String title;
        String url;
    }
}
