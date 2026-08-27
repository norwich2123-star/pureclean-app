package com.stevenspureclean.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

import android.content.Intent;
import android.content.ClipData;

import android.net.Uri;

import android.util.Patterns;
import android.widget.Toast;

import android.webkit.JavascriptInterface;
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

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        requestWindowFeature(
                Window.FEATURE_NO_TITLE
        );

        webView = new WebView(this);

        setContentView(webView);

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        webView.addJavascriptInterface(
                new AndroidBridge(),
                "Android"
        );

        webView.setWebViewClient(
                new WebViewClient() {

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
                }
        );

        webView.loadUrl(
                "file:///android_asset/pureclean.html"
        );
    }

    private boolean handleUrl(String url) {

        if (url == null) {
            return false;
        }

        if (
                url.contains("google.com/maps")
                        ||
                url.contains("maps.google.com")
        ) {

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

    public class AndroidBridge {

        @JavascriptInterface
        public void emailCustomer(
                String email,
                String customerName,
                String invoiceNumber,
                String invoiceDate,
                String dueDate,
                String amount,
                String description) {

            runOnUiThread(
                    () -> emailInvoiceWithPdf(
                            email,
                            customerName,
                            invoiceNumber,
                            invoiceDate,
                            dueDate,
                            amount,
                            description
                    )
            );
        }

        @JavascriptInterface
        public void sendReminder(
                String email,
                String customerName,
                String invoiceNumber,
                String invoiceDate,
                String dueDate,
                String amount,
                String description,
                boolean overdue) {

            runOnUiThread(
                    () -> openReminderEmail(
                            email,
                            customerName,
                            invoiceNumber,
                            invoiceDate,
                            dueDate,
                            amount,
                            description,
                            overdue
                    )
            );
        }
    }

    private void emailInvoiceWithPdf(
            String email,
            String customerName,
            String invoiceNumber,
            String invoiceDate,
            String dueDate,
            String amount,
            String description) {

        try {

            if (email == null) {
                email = "";
            }

            email = email.trim();

            if (
                    email.isEmpty()
                            ||
                    !Patterns.EMAIL_ADDRESS
                    .matcher(email)
                    .matches()
            ) {

                Toast.makeText(
                        this,
                        "Invalid customer email: " + email,
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

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
                    customerName,
                    invoiceNumber,
                    invoiceDate,
                    dueDate,
                    description,
                    amount
            );

            Uri pdfUri =
                    FileProvider.getUriForFile(
                            this,
                            getPackageName()
                                    + ".fileprovider",
                            pdf
                    );

            String subject =
                    "Invoice #"
                            + invoiceNumber
                            + " - Steven's Pure Clean Exteriors";

            String message =
                    "Hi "
                            + customerName
                            + ",\n\n"
                            + "Please find attached invoice #"
                            + invoiceNumber
                            + " for £"
                            + amount
                            + ".\n\n"
                            + "Invoice date: "
                            + invoiceDate
                            + "\n"
                            + "Payment due: "
                            + dueDate
                            + "\n";

            if (
                    description != null
                            &&
                    !description.trim().isEmpty()
            ) {

                message +=
                        "Description: "
                                + description.trim()
                                + "\n";
            }

            message +=
                    "\nPlease make payment within 7 days.\n\n"
                            + "Thank you for your custom.\n\n"
                            + "Steven's Pure Clean Exteriors";

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
                    message
            );

            emailIntent.putExtra(
                    Intent.EXTRA_STREAM,
                    pdfUri
            );

            emailIntent.setClipData(
                    ClipData.newRawUri(
                            "Invoice PDF",
                            pdfUri
                    )
            );

            emailIntent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            try {

                emailIntent.setPackage(
                        "com.google.android.gm"
                );

                startActivity(
                        emailIntent
                );

            } catch (Exception gmailError) {

                emailIntent.setPackage(null);

                startActivity(
                        Intent.createChooser(
                                emailIntent,
                                "Email Invoice"
                        )
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            Toast.makeText(
                    this,
                    "Could not create or email invoice.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void openReminderEmail(
            String email,
            String customerName,
            String invoiceNumber,
            String invoiceDate,
            String dueDate,
            String amount,
            String description,
            boolean overdue) {

        try {

            if (email == null) {
                email = "";
            }

            email = email.trim();

            if (
                    email.isEmpty()
                            ||
                    !Patterns.EMAIL_ADDRESS
                    .matcher(email)
                    .matches()
            ) {

                Toast.makeText(
                        this,
                        "Invalid customer email: "
                                + email,
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            String subject;

            if (overdue) {

                subject =
                        "Overdue Invoice #"
                                + invoiceNumber
                                + " - Steven's Pure Clean Exteriors";

            } else {

                subject =
                        "Invoice Reminder #"
                                + invoiceNumber
                                + " - Steven's Pure Clean Exteriors";
            }

            String message =
                    "Hi "
                            + customerName
                            + ",\n\n"
                            + "This is a friendly reminder regarding invoice #"
                            + invoiceNumber
                            + ".\n\n"
                            + "Invoice date: "
                            + invoiceDate
                            + "\n"
                            + "Due date: "
                            + dueDate
                            + "\n"
                            + "Amount due: £"
                            + amount
                            + "\n";

            if (
                    description != null
                            &&
                    !description.trim().isEmpty()
            ) {

                message +=
                        "Description: "
                                + description.trim()
                                + "\n";
            }

            if (overdue) {

                message +=
                        "\nThis invoice is now overdue.";

            } else {

                message +=
                        "\nPlease make payment by the due date.";
            }

            message +=
                    "\n\nThank you,\n"
                            + "Steven's Pure Clean Exteriors";

            String mailto =
                    "mailto:"
                            + email
                            + "?subject="
                            + Uri.encode(subject)
                            + "&body="
                            + Uri.encode(message);

            Intent reminderIntent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(mailto)
                    );

            try {

                reminderIntent.setPackage(
                        "com.google.android.gm"
                );

                startActivity(
                        reminderIntent
                );

            } catch (Exception gmailError) {

                reminderIntent.setPackage(null);

                startActivity(
                        Intent.createChooser(
                                reminderIntent,
                                "Send Reminder"
                        )
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            Toast.makeText(
                    this,
                    "Could not open reminder.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void makePdf(
            File file,
            String customerName,
            String invoiceNumber,
            String invoiceDate,
            String dueDate,
            String description,
            String amount) {

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

        paint.setTextSize(15);

        canvas.drawText(
                "Invoice #"
                        + invoiceNumber,
                360,
                145,
                paint
        );

        paint.setFakeBoldText(false);
        paint.setTextSize(12);

        canvas.drawText(
                "Invoice date:",
                360,
                170,
                paint
        );

        canvas.drawText(
                invoiceDate,
                360,
                188,
                paint
        );

        paint.setColor(green);
        paint.setFakeBoldText(true);

        canvas.drawText(
                "Due:",
                360,
                213,
                paint
        );

        canvas.drawText(
                dueDate,
                400,
                213,
                paint
        );

        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(true);
        paint.setTextSize(15);

        canvas.drawText(
                "Bill To",
                40,
                235,
                paint
        );

        paint.setFakeBoldText(false);

        canvas.drawText(
                customerName,
                40,
                260,
                paint
        );

        paint.setFakeBoldText(true);

        canvas.drawText(
                "Description",
                40,
                315,
                paint
        );

        paint.setFakeBoldText(false);

        String cleanDescription =
                description == null
                        ? ""
                        : description.trim();

        if (cleanDescription.isEmpty()) {

            cleanDescription =
                    "Exterior cleaning service";
        }

        canvas.drawText(
                cleanDescription,
                40,
                345,
                paint
        );

        paint.setColor(green);
        paint.setFakeBoldText(true);
        paint.setTextSize(24);

        canvas.drawText(
                "Amount Due: £"
                        + amount,
                40,
                405,
                paint
        );

        paint.setColor(Color.BLACK);
        paint.setTextSize(15);

        canvas.drawText(
                "Payment terms",
                40,
                520,
                paint
        );

        paint.setFakeBoldText(false);

        canvas.drawText(
                "Please make payment within 7 days",
                40,
                545,
                paint
        );

        paint.setFakeBoldText(true);
        paint.setTextSize(16);

        canvas.drawText(
                "Bank Transfer Details",
                40,
                605,
                paint
        );

        paint.setFakeBoldText(false);
        paint.setTextSize(14);

        canvas.drawText(
                "Account name: Steven B Attew",
                40,
                635,
                paint
        );

        canvas.drawText(
                "Bank: Monzo",
                40,
                660,
                paint
        );

        canvas.drawText(
                "Sort code: 04-00-06",
                40,
                685,
                paint
        );

        canvas.drawText(
                "Account number: 34121651",
                40,
                710,
                paint
        );

        paint.setColor(Color.DKGRAY);
        paint.setTextSize(12);

        canvas.drawText(
                "Thank you for your custom.",
                40,
                790,
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
