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

                String url =
                        request.getUrl().toString();

                if (url.startsWith("mailto:")) {

                    createAndSendInvoice(
                            request.getUrl()
                    );

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


    private void createAndSendInvoice(Uri mailUri) {

        try {

            String full =
                    mailUri.toString();

            String email = "";

            int start =
                    full.indexOf("mailto:") + 7;

            int question =
                    full.indexOf("?");

            if (question > start) {

                email =
                        Uri.decode(
                                full.substring(
                                        start,
                                        question
                                )
                        );

            } else {

                email =
                        Uri.decode(
                                full.substring(start)
                        );
            }


            String subject =
                    mailUri.getQueryParameter(
                            "subject"
                    );

            String body =
                    mailUri.getQueryParameter(
                            "body"
                    );


            if (subject == null) {

                subject =
                        "Invoice from Steven's Pure Clean Exteriors";
            }

            if (body == null) {

                body = "";
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
                            "PureClean-Invoice-" +
                            System.currentTimeMillis() +
                            ".pdf"
                    );


            createInvoicePdf(
                    pdfFile,
                    body
            );


            Uri pdfUri =
                    FileProvider.getUriForFile(
                            this,
                            getPackageName() +
                            ".fileprovider",
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
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );


            startActivity(
                    Intent.createChooser(
                            emailIntent,
                            "Send Invoice"
                    )
            );


        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    private void createInvoicePdf(
            File file,
            String invoiceText) {

        PdfDocument document =
                new PdfDocument();


        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(
                        595,
                        842,
                        1
                ).create();


        PdfDocument.Page page =
                document.startPage(
                        pageInfo
                );


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
                            .open(
                                    "logo.jpg"
                            );

            Bitmap logo =
                    BitmapFactory
                            .decodeStream(
                                    input
                            );

            if (logo != null) {

                Bitmap scaled =
                        Bitmap.createScaledBitmap(
                                logo,
                                95,
                                95,
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
        }


        paint.setColor(green);
        paint.setTextSize(24);
        paint.setFakeBoldText(true);

        canvas.drawText(
                "Steven's Pure Clean Exteriors",
                155,
                65,
                paint
        );


        paint.setTextSize(13);
        paint.setFakeBoldText(false);

        canvas.drawText(
                "Pure Results • Clean Exteriors",
                155,
                91,
                paint
        );


        paint.setColor(
                Color.BLACK
        );

        paint.setTextSize(28);
        paint.setFakeBoldText(true);

        canvas.drawText(
                "INVOICE",
                40,
                165,
                paint
        );


        paint.setFakeBoldText(false);

        paint.setStrokeWidth(2);

        canvas.drawLine(
                40,
                180,
                555,
                180,
                paint
        );


        paint.setTextSize(15);

        float y = 220;


        String[] lines =
                invoiceText.split(
                        "\n"
                );


        for (String line : lines) {

            if (line.trim().isEmpty()) {

                y += 16;

                continue;
            }


            if (line.startsWith(
                    "Amount due:"
            )) {

                paint.setColor(green);
                paint.setTextSize(22);
                paint.setFakeBoldText(true);

                canvas.drawText(
                        line,
                        40,
                        y,
                        paint
                );

                paint.setColor(
                        Color.BLACK
                );

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


            if (y > 770) {

                break;
            }
        }


        paint.setColor(
                Color.DKGRAY
        );

        paint.setTextSize(12);

        canvas.drawText(
                "Thank you for your custom.",
                40,
                790,
                paint
        );


        document.finishPage(
                page
        );


        try {

            FileOutputStream output =
                    new FileOutputStream(
                            file
                    );

            document.writeTo(
                    output
            );

            output.close();

        } catch (Exception e) {

            e.printStackTrace();
        }


        document.close();
    }
}
