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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private WebView webView;

    private static final int CREATE_BACKUP_FILE = 5001;
    private static final int OPEN_BACKUP_FILE = 5002;

    private String pendingBackupJson = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        webView = new WebView(this);

        setContentView(webView);

        WebSettings settings = webView.getSettings();

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

        @JavascriptInterface
        public void backupBusinessData(
                String jsonData) {

            runOnUiThread(
                    () -> createBackupFile(
                            jsonData
                    )
            );
        }

        @JavascriptInterface
        public void restoreBusinessData() {

            runOnUiThread(
                    this::openRestorePicker
            );
        }

        private void openRestorePicker() {

            chooseBackupFile();
        }
    }

    private void createBackupFile(
            String jsonData) {

        if (
                jsonData == null
                        ||
                jsonData.trim().isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "There is no business data to back up.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        pendingBackupJson = jsonData;

        String date =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.UK
                ).format(
                        new Date()
                );

        String fileName =
                "PureClean-Backup-"
                        + date
                        + ".json";

        Intent intent =
                new Intent(
                        Intent.ACTION_CREATE_DOCUMENT
                );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.setType(
                "application/json"
        );

        intent.putExtra(
                Intent.EXTRA_TITLE,
                fileName
        );

        try {

            startActivityForResult(
                    intent,
                    CREATE_BACKUP_FILE
            );

        } catch (Exception e) {

            e.printStackTrace();

            Toast.makeText(
                    this,
                    "Could not open backup save screen.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void chooseBackupFile() {

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.setType(
                "*/*"
        );

        String[] mimeTypes = {
                "application/json",
                "text/plain",
                "application/octet-stream"
        };

        intent.putExtra(
                Intent.EXTRA_MIME_TYPES,
                mimeTypes
        );

        try {

            startActivityForResult(
                    intent,
                    OPEN_BACKUP_FILE
            );

        } catch (Exception e) {

            e.printStackTrace();

            Toast.makeText(
                    this,
                    "Could not open restore file picker.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (
                resultCode != RESULT_OK
                        ||
                data == null
                        ||
                data.getData() == null
        ) {

            return;
        }

        Uri uri = data.getData();

        if (
                requestCode
                        ==
                CREATE_BACKUP_FILE
        ) {

            saveBackupToUri(uri);

            return;
        }

        if (
                requestCode
                        ==
                OPEN_BACKUP_FILE
        ) {

            restoreBackupFromUri(uri);
        }
    }

    private void saveBackupToUri(
            Uri uri) {

        try {

            OutputStream outputStream =
                    getContentResolver()
                            .openOutputStream(
                                    uri
                            );

            if (
                    outputStream
                            ==
                    null
            ) {

                Toast.makeText(
                        this,
                        "Could not save backup.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            outputStream.write(
                    pendingBackupJson
                            .getBytes(
                                    "UTF-8"
                            )
            );

            outputStream.flush();
            outputStream.close();

            pendingBackupJson = "";

            Toast.makeText(
                    this,
                    "Business backup saved successfully.",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            e.printStackTrace();

            Toast.makeText(
                    this,
                    "Backup could not be saved.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void restoreBackupFromUri(
            Uri uri) {

        try {

            InputStream inputStream =
                    getContentResolver()
                            .openInputStream(
                                    uri
                            );

            if (
                    inputStream
                            ==
                    null
            ) {

                Toast.makeText(
                        this,
                        "Could not read backup file.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    inputStream,
                                    "UTF-8"
                            )
                    );

            StringBuilder builder =
                    new StringBuilder();

            String line;

            while (
                    (line = reader.readLine())
                            !=
                    null
            ) {

                builder.append(line);
            }

            reader.close();
            inputStream.close();

            String backupJson =
                    builder.toString()
                            .trim();

            if (
                    backupJson.isEmpty()
            ) {

                Toast.makeText(
                        this,
                        "The selected backup file is empty.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            JSONObject backup =
                    new JSONObject(
                            backupJson
                    );

            JSONArray customers =
                    backup.optJSONArray(
                            "customers"
                    );

            JSONArray invoices =
                    backup.optJSONArray(
                            "invoices"
                    );

            if (
                    customers == null
                            ||
                    invoices == null
            ) {

                Toast.makeText(
                        this,
                        "This is not a valid Pure Clean backup file.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            String javascript =
                    "restoreBusinessBackup("
                            + JSONObject.quote(
                                    backupJson
                            )
                            + ");";

            webView.post(
                    () -> webView.evaluateJavascript(
                            javascript,
                            null
                    )
            );

            Toast.makeText(
                    this,
                    "Backup loaded: "
                            + customers.length()
                            + " customers, "
                            + invoices.length()
                            + " invoices",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            e.printStackTrace();

            Toast.makeText(
                    this,
                    "Could not restore this backup file.",
                    Toast.LENGTH_LONG
            ).show();
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
                        "Invalid customer email: "
                                + email,
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
                    new String[]{
                            email
                    }
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
                            + "This is a friendly reminder regarding invoice
