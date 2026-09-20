// [Endpoint Capture] — bundled by si firda lovely
// Source: original (MIT)
// Usage: frida -D local -f <package> -l endpoint_capture.js

Java.perform(function () {
    console.log("[CAPTURE] Hooking network layers...");

    // OkHttp3 Request
    try {
        var Request = Java.use("okhttp3.Request");
        var Buffer = null;
        try { Buffer = Java.use("okio.Buffer"); } catch (e) {}

        Request.headers.implementation = function () {
            var url = this.url().toString();
            var method = this.method();
            console.log("\n[REQ] " + method + " " + url);
            var hdrs = this.headers();
            var size = hdrs.size();
            for (var i = 0; i < size; i++) {
                console.log("  > " + hdrs.name(i) + ": " + hdrs.value(i));
            }
            return this.headers();
        };
        console.log("[CAPTURE] okhttp3.Request hooked");
    } catch (e) {
        console.log("[CAPTURE] okhttp3 not found: " + e);
    }

    // HttpURLConnection
    try {
        var URL = Java.use("java.net.URL");
        URL.openConnection.overload().implementation = function () {
            console.log("[REQ] URL.openConnection: " + this.toString());
            return this.openConnection();
        };
        console.log("[CAPTURE] HttpURLConnection hooked");
    } catch (e) {
        console.log("[CAPTURE] URL hook failed: " + e);
    }

    // WebView loadUrl
    try {
        var WebView = Java.use("android.webkit.WebView");
        WebView.loadUrl.overload("java.lang.String").implementation = function (url) {
            console.log("[WEBVIEW] loadUrl: " + url);
            return this.loadUrl(url);
        };
        console.log("[CAPTURE] WebView hooked");
    } catch (e) {
        console.log("[CAPTURE] WebView hook failed: " + e);
    }

    console.log("[CAPTURE] All hooks installed — use the app now");
});
