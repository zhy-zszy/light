package com.example.lightbrowser;

import android.content.Context;

public class HomePageHtml {
    public static String getHtml(Context context) {
        String engine = Utils.getString(context, "search_engine", "https://www.bing.com/search?q=");
        return build(engine);
    }

    public static String getHtml() {
        return build("https://www.bing.com/search?q=");
    }

    private static String build(String engine) {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">" +
            "<title>主页</title><style>" +
            "* { margin: 0; padding: 0; box-sizing: border-box; -webkit-tap-highlight-color: transparent; }" +
            "body { background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%); min-height: 100vh; font-family: system-ui, -apple-system, sans-serif; padding: 50px 20px; color: #fff; display: flex; flex-direction: column; }" +
            ".header { text-align: center; margin-bottom: 40px; }" +
            ".time { font-size: 48px; font-weight: 200; letter-spacing: 2px; text-shadow: 0 4px 10px rgba(0,0,0,0.3); }" +
            ".greeting { font-size: 16px; opacity: 0.8; margin-top: 8px; }" +
            ".search-box { position: relative; width: 100%; max-width: 600px; margin: 0 auto 40px auto; }" +
            ".search-input { width: 100%; height: 56px; border-radius: 28px; border: 1px solid rgba(255,255,255,0.2); padding: 0 60px 0 24px; font-size: 16px; outline: none; background: rgba(255,255,255,0.15); backdrop-filter: blur(20px); -webkit-backdrop-filter: blur(20px); box-shadow: 0 8px 32px rgba(0,0,0,0.2); color: #fff; transition: all 0.3s ease; }" +
            ".search-input:focus { background: rgba(255,255,255,0.25); border-color: rgba(255,255,255,0.5); box-shadow: 0 8px 32px rgba(0,0,0,0.4); }" +
            ".search-input::placeholder { color: rgba(255,255,255,0.6); }" +
            ".search-btn { position: absolute; right: 8px; top: 8px; width: 40px; height: 40px; border-radius: 50%; background: rgba(255,255,255,0.2); color: white; border: none; font-size: 18px; cursor: pointer; transition: background 0.2s; }" +
            ".search-btn:hover { background: rgba(255,255,255,0.4); }" +
            ".grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(70px, 1fr)); gap: 20px; width: 100%; max-width: 600px; margin: 0 auto; }" +
            ".item { text-decoration: none; color: #fff; display: flex; flex-direction: column; align-items: center; transition: transform 0.2s; }" +
            ".item:active { transform: scale(0.9); }" +
            ".icon { width: 56px; height: 56px; border-radius: 18px; background: rgba(255,255,255,0.15); backdrop-filter: blur(10px); -webkit-backdrop-filter: blur(10px); margin-bottom: 8px; display: flex; align-items: center; justify-content: center; font-size: 24px; font-weight: bold; box-shadow: 0 4px 15px rgba(0,0,0,0.2); border: 1px solid rgba(255,255,255,0.1); }" +
            ".name { font-size: 12px; opacity: 0.9; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 100%; text-align: center; }" +
            "</style></head><body>" +
            "<div class=\"header\">" +
            "<div class=\"time\" id=\"clock\">00:00</div>" +
            "<div class=\"greeting\" id=\"greeting\">你好</div>" +
            "</div>" +
            "<div class=\"search-box\">" +
            "<input type=\"text\" class=\"search-input\" id=\"searchInput\" placeholder=\"搜索或输入网址...\">" +
            "<button class=\"search-btn\" onclick=\"doSearch()\">➔</button></div>" +
            "<div class=\"grid\">" +
            "<a href=\"https://www.bilibili.com\" class=\"item\"><div class=\"icon\" style=\"color:#fb7299\">B</div><div class=\"name\">哔哩哔哩</div></a>" +
            "<a href=\"https://www.baidu.com\" class=\"item\"><div class=\"icon\" style=\"color:#2932e1\">百</div><div class=\"name\">百度</div></a>" +
            "<a href=\"https://www.zhihu.com\" class=\"item\"><div class=\"icon\" style=\"color:#0066ff\">知</div><div class=\"name\">知乎</div></a>" +
            "<a href=\"https://github.com\" class=\"item\"><div class=\"icon\" style=\"color:#fff\">G</div><div class=\"name\">GitHub</div></a>" +
            "<a href=\"https://www.taobao.com\" class=\"item\"><div class=\"icon\" style=\"color:#ff5000\">淘</div><div class=\"name\">淘宝</div></a>" +
            "<a href=\"https://www.bing.com\" class=\"item\"><div class=\"icon\" style=\"color:#008373\">B</div><div class=\"name\">Bing</div></a>" +
            "<a href=\"https://weibo.com\" class=\"item\"><div class=\"icon\" style=\"color:#e6162d\">微</div><div class=\"name\">微博</div></a>" +
            "<a href=\"https://www.douyin.com\" class=\"item\"><div class=\"icon\" style=\"color:#fff\">抖</div><div class=\"name\">抖音</div></a>" +
            "</div>" +
            "<script>" +
            "function updateTime() {" +
            "  var d = new Date();" +
            "  var h = d.getHours().toString(); if(h.length<2)h='0'+h;" +
            "  var m = d.getMinutes().toString(); if(m.length<2)m='0'+m;" +
            "  document.getElementById('clock').innerText = h + ':' + m;" +
            "  var hour = d.getHours(); var g = '你好';" +
            "  if(hour>=5&&hour<9)g='早上好';else if(hour>=9&&hour<12)g='上午好';else if(hour>=12&&hour<14)g='中午好';else if(hour>=14&&hour<18)g='下午好';else g='晚上好';" +
            "  document.getElementById('greeting').innerText = g;" +
            "}" +
            "setInterval(updateTime, 1000); updateTime();" +
            "function doSearch(){var q=document.getElementById('searchInput').value.trim();if(!q)return;" +
            "if(q.indexOf('http')===0||(q.indexOf('.')>0&&q.indexOf(' ')<0)){if(q.indexOf('http')!==0)q='https://'+q;window.location.href=q;}" +
            "else{window.location.href='" + engine + "'+encodeURIComponent(q);}}" +
            "document.getElementById('searchInput').addEventListener('keydown',function(e){if(e.key==='Enter')doSearch()});" +
            "</script></body></html>";
    }
}
