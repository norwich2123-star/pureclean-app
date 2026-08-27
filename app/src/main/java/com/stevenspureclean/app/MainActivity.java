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

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request) {

                String url = request.getUrl().toString();

                if (url.startsWith("mailto:")) {
                    try {
                        Intent emailIntent =
                                new Intent(
                                        Intent.ACTION_SENDTO,
                                        Uri.parse(url)
                                );

                        startActivity(emailIntent);

                    } catch (Exception e) {
                    }

                    return true;
                }

                if (url.startsWith("intent://")) {
                    try {
                        Intent intent =
                                Intent.parseUri(
                                        url,
                                        Intent.URI_INTENT_SCHEME
                                );

                        startActivity(intent);

                    } catch (Exception e) {
                    }

                    return true;
                }

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
                    }

                    return true;
                }

                return false;
            }
        });

        webView.loadUrl(
                "file:///android_asset/pureclean.html"
        );
    }
}
