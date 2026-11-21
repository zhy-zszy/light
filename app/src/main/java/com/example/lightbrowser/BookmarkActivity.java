package com.example.lightbrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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

import java.util.ArrayList;
import java.util.List;

public class BookmarkActivity extends Activity {
    private ListView lvBookmarks;
    private BookmarkAdapter adapter;
    private final List<BookmarkInfo> bookmarks = new ArrayList<BookmarkInfo>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_bookmark);

        lvBookmarks = (ListView) findViewById(R.id.lv_bookmarks);
        adapter = new BookmarkAdapter();
        lvBookmarks.setAdapter(adapter);
        loadBookmarks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBookmarks();
    }

    private void loadBookmarks() {
        bookmarks.clear();
        String json = Utils.prefs(this).getString("bookmarks_json", "");
        if (json.length() > 0) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    BookmarkInfo info = new BookmarkInfo();
                    info.title = obj.optString("title", "");
                    info.url = obj.optString("url", "");
                    if (info.url.length() > 0) bookmarks.add(info);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (bookmarks.isEmpty()) {
            BookmarkInfo empty = new BookmarkInfo();
            empty.title = getString(R.string.no_bookmarks);
            empty.url = "";
            bookmarks.add(empty);
        }
        adapter.notifyDataSetChanged();
    }

    private void deleteBookmark(final int position) {
        if (position < 0 || position >= bookmarks.size()) return;
        final BookmarkInfo info = bookmarks.get(position);
        if (info.url.length() == 0) return;

        new AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(getString(R.string.confirm_delete) + " \"" + info.title + "\"?")
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        JSONArray arr = new JSONArray(Utils.prefs(BookmarkActivity.this).getString("bookmarks_json", "[]"));
                        JSONArray newArr = new JSONArray();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            if (!info.url.equals(obj.optString("url"))) {
                                newArr.put(obj);
                            }
                        }
                        Utils.prefs(BookmarkActivity.this).edit().putString("bookmarks_json", newArr.toString()).apply();
                        loadBookmarks();
                        Toast.makeText(BookmarkActivity.this, R.string.deleted, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void openBookmark(BookmarkInfo info) {
        if (info.url.length() == 0) return;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(info.url));
        intent.setClass(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void copyBookmarkUrl(BookmarkInfo info) {
        if (info.url.length() == 0) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("URL", info.url));
            Toast.makeText(this, R.string.url_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void showBookmarkOptions(final int position) {
        if (position < 0 || position >= bookmarks.size()) return;
        final BookmarkInfo info = bookmarks.get(position);
        if (info.url.length() == 0) return;

        final String[] options = new String[]{
            getString(R.string.open), getString(R.string.copy_url), getString(R.string.delete)
        };

        new AlertDialog.Builder(this)
            .setTitle(info.title)
            .setItems(options, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0: openBookmark(info); break;
                        case 1: copyBookmarkUrl(info); break;
                        case 2: deleteBookmark(position); break;
                    }
                }
            })
            .show();
    }

    private class BookmarkAdapter extends BaseAdapter {
        private final LayoutInflater inflater = LayoutInflater.from(BookmarkActivity.this);

        public int getCount() { return bookmarks.size(); }
        public Object getItem(int position) { return bookmarks.get(position); }
        public long getItemId(int position) { return position; }

        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
                holder = new ViewHolder();
                holder.tvTitle = (TextView) convertView.findViewById(android.R.id.text1);
                holder.tvUrl = (TextView) convertView.findViewById(android.R.id.text2);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final BookmarkInfo info = bookmarks.get(position);
            holder.tvTitle.setText(info.title);
            holder.tvUrl.setText(info.url);
            holder.tvUrl.setSingleLine(true);
            holder.tvUrl.setEllipsize(android.text.TextUtils.TruncateAt.END);

            convertView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { openBookmark(info); }
            });
            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    showBookmarkOptions(position);
                    return true;
                }
            });
            return convertView;
        }

        class ViewHolder {
            TextView tvTitle;
            TextView tvUrl;
        }
    }

    private static class BookmarkInfo {
        String title;
        String url;
    }
}
