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

                return handleUrl(request.getUrl().toString());
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
            sendInvoice(url);
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
                Intent intent =
                        new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(url)
                        );

                startActivity(intent);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return true;
        }

        return false;
    }

    private String getQueryValue(
            String url,
            String name) {

        try {

            int q = url.indexOf("?");

            if (q < 0) {
                return "";
            }

            String query =
                    url.substring(q + 1);

            String[] parts =
                    query.split("&");

            for (String part : parts) {

                String prefix =
                        name + "=";

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

    private int getNextInvoiceNumber() {

        android.content.SharedPreferences prefs =
                getSharedPreferences(
                        "PureCleanPrefs",
                        MODE_PRIVATE
                );

        int current =
                prefs.getInt(
                        "nextInvoiceNumber",
                        1001
                );

        prefs.edit()
                .putInt(
                        "nextInvoiceNumber",
                        current + 1
                )
                .apply();

        return current;
    }

    private void sendInvoice(String mailUrl) {

        try {

            int start =
                    "mailto:".length();

            int question =
                    mailUrl.indexOf("?");

            String email;

            if (question > start) {

                email =
                        Uri.decode(
                                mailUrl.substring(
                                        start,
                                        question
                                )
                        );

            } else {

                email =
                        Uri.decode(
                                mailUrl.substring(start)
                        );
            }

            String subject =
                    getQueryValue(
                            mailUrl,
                            "subject"
                    );

            String body =
                    getQueryValue(
                            mailUrl,
                            "body"
                    );

            int invoiceNumber =
                    getNextInvoiceNumber();

            File folder =
                    new File(
                            getCacheDir(),
                            "invoices"
                    );

            if (!folder.exists()) {
                folder.mkdirs();
            }

            File pdf =
                    new File(
                            folder,
                            "PureClean-Invoice-"
                                    + invoiceNumber
                                    + ".pdf"
                    );

            makePdf(
                    pdf,
                    body,
                    invoiceNumber
            );

            Uri pdfUri =
                    FileProvider.getUriForFile(
                            this,
                            getPackageName()
                                    + ".fileprovider",
                            pdf
                    );

            Intent intent =
                    new Intent(
                            Intent.ACTION_SEND
                    );

            intent.setType(
                    "application/pdf"
            );

            intent.putExtra(
                    Intent.EXTRA_EMAIL,
                    new String[]{email}
            );

            intent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Invoice #"
                            + invoiceNumber
                            + " - Steven's Pure Clean Exteriors"
            );

            intent.putExtra(
                    Intent.EXTRA_TEXT,
                    "Hi,\n\n"
                            + "Please find invoice #"
                            + invoiceNumber
                            + " attached.\n\n"
                            + "Please make payment within 7 days.\n\n"
                            + "Thank you for your custom.\n\n"
                            + "Steven's Pure Clean Exteriors"
            );

            intent.putExtra(
                    Intent.EXTRA_STREAM,
                    pdfUri
            );

            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(
                    Intent.createChooser(
                            intent,
                            "Send Invoice"
                    )
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void makePdf(
            File file,
            String text,
            int invoiceNumber) {

        PdfDocument document =
                new PdfDocument();

        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(
                        595,
                        842,
                        1
                ).create();

        PdfDocument.Page page =
                document.startPage(pageInfo);

        Canvas canvas =
                page.getCanvas();

        Paint paint =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        canvas.drawColor(
                Color.WHITE
        );

        int green =
                Color.rgb(
                        145,
                        205,
                        0
                );

        try {

            InputStream input =
                    getAssets()
                            .open("logo.jpg");

            Bitmap logo =
                    BitmapFactory
                            .decodeStream(input);

            if (logo != null) {

                Bitmap scaled =
                        Bitmap.createScaledBitmap(
                                logo,
                                90,
                                90,
                                true
                        );

                canvas.drawBitmap(
                        scaled,
                        40,
                        35,
                        paint
                );
            }

            input.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        paint.setColor(green);
        paint.setTextSize(22);
        paint.setFakeBoldText(true);

        canvas.drawText(
                "Steven's Pure Clean Exteriors",
                150,
                65,
                paint
        );

        paint.setTextSize(13);
        paint.setFakeBoldText(false);

        canvas.drawText(
                "Pure Results • Clean Exteriors",
                150,
                90,
                paint
        );

        paint.setColor(Color.BLACK);
        paint.setTextSize(28);
        paint.setFakeBoldText(true);

        canvas.drawText(
                "INVOICE",
                40,
                160,
                paint
        );

        paint.setTextSize(16);

        canvas.drawText(
                "Invoice #"
                        + invoiceNumber,
                390,
                160,
                paint
        );

        paint.setFakeBoldText(false);
        paint.setTextSize(15);

        float y = 210;

        String[] lines =
                text.split("\n");

        for (String line : lines) {

            if (line.trim().isEmpty()) {
                y += 15;
                continue;
            }

            if (line.startsWith("Amount due:")) {

                paint.setColor(green);
                paint.setTextSize(22);
                paint.setFakeBoldText(true);

                canvas.drawText(
                        line,
                        40,
                        y,
                        paint
                );

                paint.setColor(Color.BLACK);
                paint.setTextSize(15);
                paint.setFakeBoldText(false);

                y += 35;

            } else {

                canvas.drawText(
                        line,
                        40,
                        y,
                        paint
                );

                y += 24;
            }

            if (y > 500) {
                break;
            }
        }

        paint.setColor(Color.BLACK);
        paint.setTextSize(15);
        paint.setFakeBoldText(true);

        canvas.drawText(
                "Payment terms",
                40,
                560,
                paint
        );

        paint.setFakeBoldText(false);

        canvas.drawText(
                "Please make payment within 7 days",
                40,
                585,
                paint
        );

        paint.setFakeBoldText(true);
        paint.setTextSize(16);

        canvas.drawText(
                "Bank Transfer Details",
                40,
                635,
                paint
        );

        paint.setFakeBoldText(false);
        paint.setTextSize(14);

        canvas.drawText(
                "Account name: Steven B Attew",
                40,
                665,
                paint
        );

        canvas.drawText(
                "Bank: Monzo",
                40,
                690,
                paint
        );

        canvas.drawText(
                "Sort code: 04-00-06",
                40,
                715,
                paint
        );

        canvas.drawText(
                "Account number: 34121651",
                40,
                740,
                paint
        );

        paint.setColor(Color.DKGRAY);
        paint.setTextSize(12);

        canvas.drawText(
                "Thank you for your custom.",
                40,
                795,
                paint
        );

        document.finishPage(page);

        try {

            FileOutputStream output =
                    new FileOutputStream(file);

            document.writeTo(output);
            output.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        document.close();
    }
}
