package com.stevenspureclean.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        WebView webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        webView.clearCache(true);
        webView.clearHistory();

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request) {

                String url = request.getUrl().toString();

                // Handle Android intent links
                if (url.startsWith("intent://")) {

                    try {

                        Intent intent = Intent.parseUri(
                                url,
                                Intent.URI_INTENT_SCHEME
                        );

                        startActivity(intent);

                        return true;

                    } catch (Exception e) {

                        return true;
                    }
                }

                // Open Google Maps links outside the app
                if (url.contains("google.com/maps")
                        || url.contains("maps.google.com")) {

                    try {

                        Intent mapIntent =
                                new Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(url)
                                );

                        startActivity(mapIntent);

                    } catch (Exception e) {

                        Intent browserIntent =
                                new Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(url)
                                );

                        startActivity(browserIntent);
                    }

                    return true;
                }

                // Keep normal web content inside the app
                return false;
            }
        });

        webView.loadUrl(
                "file:///android_asset/pureclean.html"
        );
    }
}
