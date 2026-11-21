package com.example.lightbrowser;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

public class SiteSettings {
    private static final String PREFS_NAME = "LightBrowserSiteSettings";

    public boolean javascript = true;
    public boolean domStorage = true;
    public boolean allowImages = true;
    public boolean allowPopup = false;
    public boolean desktopMode = false;
    public boolean safeBrowsing = true;

    private String host;

    public static SiteSettings load(Context context, String url) {
        SiteSettings settings = new SiteSettings();
        String host = getHost(url);
        settings.host = host;
        if (host == null || host.length() == 0) return settings;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(host, "");
        if (json.length() > 0) {
            try {
                JSONObject obj = new JSONObject(json);
                settings.javascript = obj.optBoolean("javascript", true);
                settings.domStorage = obj.optBoolean("domStorage", true);
                settings.allowImages = obj.optBoolean("allowImages", true);
                settings.allowPopup = obj.optBoolean("allowPopup", false);
                settings.desktopMode = obj.optBoolean("desktopMode", false);
                settings.safeBrowsing = obj.optBoolean("safeBrowsing", true);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return settings;
    }

    public void save(Context context) {
        if (host == null || host.length() == 0) return;
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        JSONObject obj = new JSONObject();
        try {
            obj.put("javascript", javascript);
            obj.put("domStorage", domStorage);
            obj.put("allowImages", allowImages);
            obj.put("allowPopup", allowPopup);
            obj.put("desktopMode", desktopMode);
            obj.put("safeBrowsing", safeBrowsing);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        editor.putString(host, obj.toString());
        editor.commit();
    }

    public static void reset(Context context, String url) {
        String host = getHost(url);
        if (host == null || host.length() == 0) return;
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.remove(host);
        editor.commit();
    }

    private static String getHost(String url) {
        if (url == null) return "";
        try {
            java.net.URI uri = new java.net.URI(url);
            return uri.getHost();
        } catch (Exception e) {
            return "";
        }
    }
}
