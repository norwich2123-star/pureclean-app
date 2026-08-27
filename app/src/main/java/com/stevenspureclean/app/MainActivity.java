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
                    () -> chooseBackupFile()
            );
        }
    }

    /*
     * ============================================================
     * BACKUP BUSINESS DATA
     * ============================================================
     */

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
                    "Could not open the backup file picker.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /*
     * ============================================================
     * RESTORE BUSINESS DATA
     * ============================================================
     */

    private void chooseBackupFile() {

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.setType(
                "application/json"
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
                    "Could not open the restore file picker.",
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

        Uri uri =
                data.getData();

        /*
         * SAVE BACKUP
         */
        if (
                requestCode
                        ==
                CREATE_BACKUP_FILE
        ) {

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

            return;
        }

        /*
         * LOAD BACKUP
         */
        if (
                requestCode
                        ==
                OPEN_BACKUP_FILE
        ) {

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

                    builder.append(
                            line
                    );

                    builder.append(
                            "\n"
                    );
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

                /*
                 * Pass the complete backup text
                 * safely back into JavaScript.
                 */
                String javascript =
                        "restoreBusinessBackup("
                                + JSONObject.quote(
                                        backupJson
                                )
                                + ");";

                webView.evaluateJavascript(
                        javascript,
                        null
                );

            } catch (Exception e) {

                e.printStackTrace();

                Toast.makeText(
                        this,
                        "Backup could not be restored.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    /*
     * ============================================================
     * EMAIL INVOICE WITH PDF
     * ============================================================
     */

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

            email =
                    email.trim();

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

                emailIntent.setPackage(
                        null
                );

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

    /*
     * ============================================================
     * REMINDER EMAIL
     * ============================================================
     */

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

            email =
                    email.trim();

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

                reminderIntent.setPackage(
                        null
                );

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

    /*
     * ============================================================
     * PDF
     * ============================================================
     */

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
                            .open(
                                    "logo.jpg"
                            );

            Bitmap logo =
                    BitmapFactory
                            .decodeStream(
                                    input
                            );

            if (
                    logo != null
            ) {

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

        paint.setColor(
                green
        );

        paint.setTextSize(
                22
        );

        paint.setFakeBoldText(
                true
        );

        canvas.drawText(
                "Steven's Pure Clean Exteriors",
                150,
                65,
                paint
        );

        paint.setTextSize(
                13
        );

        paint.setFakeBoldText(
                false
        );

        canvas.drawText(
                "Pure Results • Clean Exteriors",
                150,
                90,
                paint
        );

        paint.setColor(
                Color.BLACK
        );

        paint.setTextSize(
                28
        );

        paint.setFakeBoldText(
                true
        );

        canvas.drawText(
                "INVOICE",
                40,
                160,
                paint
        );

        paint.setTextSize(
                15
        );

        canvas.drawText(
                "Invoice #"
                        + invoiceNumber,
                360,
                145,
                paint
        );

        paint.setFakeBoldText(
                false
        );

        paint.setTextSize(
                12
        );

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

        paint.setColor(
                green
        );

        paint.setFakeBoldText(
                true
        );

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

        paint.setColor(
                Color.BLACK
        );

        paint.setFakeBoldText(
                true
        );

        paint.setTextSize(
                15
        );

        canvas.drawText(
                "Bill To",
                40,
                235,
                paint
        );

        paint.setFakeBoldText(
                false
        );

        canvas.drawText(
                customerName,
                40,
                260,
                paint
        );

        paint.setFakeBoldText(
                true
        );

        canvas.drawText(
                "Description",
                40,
                315,
                paint
        );

        paint.setFakeBoldText(
                false
        );

        String cleanDescription =
                description == null
                        ? ""
                        : description.trim();

        if (
                cleanDescription.isEmpty()
        ) {

            cleanDescription =
                    "Exterior cleaning service";
        }

        canvas.drawText(
                cleanDescription,
                40,
                345,
                paint
        );

        paint.setColor(
                green
        );

        paint.setFakeBoldText(
                true
        );

        paint.setTextSize(
                24
        );

        canvas.drawText(
                "Amount Due: £"
                        + amount,
                40,
                405,
                paint
        );

        paint.setColor(
                Color.BLACK
        );

        paint.setTextSize(
                15
        );

        canvas.drawText(
                "Payment terms",
                40,
                520,
                paint
        );

        paint.setFakeBoldText(
                false
        );

        canvas.drawText(
                "Please make payment within 7 days",
                40,
                545,
                paint
        );

        paint.setFakeBoldText(
                true
        );

        paint.setTextSize(
                16
        );

        canvas.drawText(
                "Bank Transfer Details",
                40,
                605,
                paint
        );

        paint.setFakeBoldText(
                false
        );

        paint.setTextSize(
                14
        );

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

        paint.setColor(
                Color.DKGRAY
        );

        paint.setTextSize(
                12
        );

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
