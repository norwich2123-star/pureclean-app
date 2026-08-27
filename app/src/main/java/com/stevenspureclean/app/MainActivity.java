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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

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

                return handleUrl(
                        request.getUrl().toString()
                );
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    String url) {

                return handleUrl(url);
            }
        });

        webView.loadUrl(
                "file:///android_asset/pureclean.html"
        );
    }


    private boolean handleUrl(String url) {

        if (url == null) {
            return false;
        }

        if (url.startsWith("mailto:")) {

            createAndSendInvoice(url);

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
                e.printStackTrace();
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
                e.printStackTrace();
            }

            return true;
        }

        return false;
    }


    private String getMailValue(
            String fullUrl,
            String key) {

        try {

            int question =
                    fullUrl.indexOf("?");

            if (question < 0) {
                return "";
            }

            String query =
                    fullUrl.substring(
                            question + 1
                    );

            String[] parts =
                    query.split("&");

            for (String part : parts) {

                String prefix =
                        key + "=";

                if (part.startsWith(prefix)) {

                    return Uri.decode(
                            part.substring(
                                    prefix.length()
                            )
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }


    private void createAndSendInvoice(
            String mailUrl) {

        try {

            int emailStart =
                    "mailto:".length();

            int question =
                    mailUrl.indexOf("?");

            String email;

            if (question > emailStart) {

                email =
                        Uri.decode(
                                mailUrl.substring(
                                        emailStart,
                                        question
                                )
                        );

            } else {

                email =
                        Uri.decode(
                                mailUrl.substring(
                                        emailStart
                                )
                        );
            }


            String subject =
                    getMailValue(
                            mailUrl,
                            "subject"
                    );

            String body =
                    getMailValue(
                            mailUrl,
                            "body"
                    );


            if (subject.isEmpty()) {

                subject =
                        "Invoice from Steven's Pure Clean Exteriors";
            }


            File invoiceFolder =
                    new File(
                            getCacheDir(),
                            "invoices"
                    );

            if (!invoiceFolder.exists()) {

                invoiceFolder.mkdirs();
            }


            File pdfFile =
                    new File(
                            invoiceFolder,
                            "Steven-Pure-Clean-Invoice.pdf"
                    );


            createInvoicePdf(
                    pdfFile,
                    body
            );


            Uri pdfUri =
                    FileProvider.getUriForFile(
                            this,
                            getPackageName()
                                    + ".fileprovider",
                            pdfFile
                    );


            Intent emailIntent =
                    new Intent(
                            Intent.ACTION_SEND
                    );

            emailIntent.setType(
                    "application/pdf"
            );

            emailIntent.putExtra(
                    Intent.EXTRA_EMAIL,
                    new String[]{email}
            );

            emailIntent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    subject
            );

            emailIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    "Hi,\n\nPlease find your invoice attached from Steven's Pure Clean Exteriors.\n\nThank you for your custom."
            );

            emailIntent.putExtra(
                    Intent.EXTRA_STREAM,
                    pdfUri
            );

            emailIntent.addFlags(
                   
