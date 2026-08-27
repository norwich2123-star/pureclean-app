package com.stevenspureclean.app;

import android.app.Activity;
import android.os.Bundle;
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

    private static final int SAVE_BACKUP = 1001;
    private static final int RESTORE_BACKUP = 1002;

    private String backupJson = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

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

                        return openExternalLink(
                                request.getUrl().toString()
                        );
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(
                            WebView view,
                            String url) {

                        return openExternalLink(url);
                    }
                }
        );

        webView.loadUrl(
                "file:///android_asset/pureclean.html"
        );
    }

    private boolean openExternalLink(String url) {

        if (url == null) {
            return false;
        }

        if (
                url.startsWith("tel:")
                        ||
                url.startsWith("sms:")
                        ||
                url.startsWith("smsto:")
                        ||
                url.contains("google.com/maps")
                        ||
                url.contains("maps.google.com")
        ) {

            try {

                Intent intent =
                        new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(url)
                        );

                startActivity(intent);

            } catch (Exception e) {

                Toast.makeText(
                        this,
                        "Could not open this action.",
                        Toast.LENGTH_SHORT
                ).show();
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
                    () -> emailInvoice(
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
                    () -> reminderEmail(
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
        public void backupBusinessData(String json) {

            runOnUiThread(
                    () -> startBackup(json)
            );
        }

        @JavascriptInterface
        public void restoreBusinessData() {

            runOnUiThread(
                    () -> startRestore()
            );
        }

        @JavascriptInterface
        public void callPhone(String phone) {

            runOnUiThread(
                    () -> openDialler(phone)
            );
        }

        @JavascriptInterface
        public void textPhone(String phone) {

            runOnUiThread(
                    () -> openTextMessage(phone)
            );
        }
    }

    private void openDialler(String phone) {

        if (
                phone == null
                        ||
                phone.trim().isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "No phone number saved.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_DIAL
                    );

            intent.setData(
                    Uri.parse(
                            "tel:"
                                    + phone.trim()
                    )
            );

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not open phone.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void openTextMessage(String phone) {

        if (
                phone == null
                        ||
                phone.trim().isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "No phone number saved.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_SENDTO
                    );

            intent.setData(
                    Uri.parse(
                            "smsto:"
                                    + phone.trim()
                    )
            );

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not open messages.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void startBackup(String json) {

        if (
                json == null
                        ||
                json.trim().isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "No business data to back up.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        backupJson = json;

        String date =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.UK
                ).format(new Date());

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
                "PureClean-Backup-"
                        + date
                        + ".json"
        );

        startActivityForResult(
                intent,
                SAVE_BACKUP
        );
    }

    private void startRestore() {

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.setType("*/*");

        startActivityForResult(
                intent,
                RESTORE_BACKUP
        );
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

        if (
                requestCode
                        ==
                SAVE_BACKUP
        ) {

            saveBackup(uri);

        } else if (
                requestCode
                        ==
                RESTORE_BACKUP
        ) {

            loadBackup(uri);
        }
    }

    private void saveBackup(Uri uri) {

        try {

            OutputStream output =
                    getContentResolver()
                            .openOutputStream(uri);

            if (output == null) {
                return;
            }

            output.write(
                    backupJson.getBytes(
                            "UTF-8"
                    )
            );

            output.flush();
            output.close();

            backupJson = "";

            Toast.makeText(
                    this,
                    "Business backup saved successfully.",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Backup could not be saved.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void loadBackup(Uri uri) {

        try {

            InputStream input =
                    getContentResolver()
                            .openInputStream(uri);

            if (input == null) {
                return;
            }

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    input,
                                    "UTF-8"
                            )
                    );

            StringBuilder text =
                    new StringBuilder();

            String line;

            while (
                    (line = reader.readLine())
                            != null
            ) {

                text.append(line);
            }

            reader.close();
            input.close();

            String json =
                    text.toString().trim();

            JSONObject backup =
                    new JSONObject(json);

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
                        "This is not a Pure Clean backup.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            String javascript =
                    "restoreBusinessBackup("
                            +
                            JSONObject.quote(json)
                            +
                            ");";

            webView.evaluateJavascript(
                    javascript,
                    null
            );

            Toast.makeText(
                    this,
                    "Backup loaded: "
                            +
                            customers.length()
                            +
                            " customers, "
                            +
                            invoices.length()
                            +
                            " invoices",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not restore backup.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private boolean validEmail(
            String email) {

        return email != null
                &&
                !email.trim().isEmpty()
                &&
                Patterns.EMAIL_ADDRESS
                        .matcher(
                                email.trim()
                        )
                        .matches();
    }

    private void emailInvoice(
            String email,
            String customerName,
            String invoiceNumber,
            String invoiceDate,
            String dueDate,
            String amount,
            String description) {

        if (!validEmail(email)) {

            Toast.makeText(
                    this,
                    "Invalid customer email.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        try {

            File pdf =
                    createInvoicePdf(
                            customerName,
                            invoiceNumber,
                            invoiceDate,
                            dueDate,
                            amount,
                            description
                    );

            Uri pdfUri =
                    FileProvider.getUriForFile(
                            this,
                            getPackageName()
                                    +
                                    ".fileprovider",
                            pdf
                    );

            String subject =
                    "Invoice #"
                            +
                            invoiceNumber
                            +
                            " - Steven's Pure Clean Exteriors";

            String message =
                    "Hi "
                            +
                            customerName
                            +
                            ",\n\n"
                            +
                            "Please find attached invoice #"
                            +
                            invoiceNumber
                            +
                            " for £"
                            +
                            amount
                            +
                            ".\n\n"
                            +
                            "Invoice date: "
                            +
                            invoiceDate
                            +
                            "\n"
                            +
                            "Payment due: "
                            +
                            dueDate
                            +
                            "\n\n"
                            +
                            "Please make payment within 7 days."
                            +
                            "\n\nThank you for your custom."
                            +
                            "\n\nSteven's Pure Clean Exteriors";

            Intent intent =
                    new Intent(
                            Intent.ACTION_SEND
                    );

            intent.setType(
                    "application/pdf"
            );

            intent.putExtra(
                    Intent.EXTRA_EMAIL,
                    new String[]{
                            email.trim()
                    }
            );

            intent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    subject
            );

            intent.putExtra(
                    Intent.EXTRA_TEXT,
                    message
            );

            intent.putExtra(
                    Intent.EXTRA_STREAM,
                    pdfUri
            );

            intent.setClipData(
                    ClipData.newRawUri(
                            "Invoice PDF",
                            pdfUri
                    )
            );

            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            try {

                intent.setPackage(
                        "com.google.android.gm"
                );

                startActivity(intent);

            } catch (Exception e) {

                intent.setPackage(null);

                startActivity(
                        Intent.createChooser(
                                intent,
                                "Email Invoice"
                        )
                );
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not create invoice email.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void reminderEmail(
            String email,
            String customerName,
            String invoiceNumber,
            String invoiceDate,
            String dueDate,
            String amount,
            String description,
            boolean overdue) {

        if (!validEmail(email)) {
            return;
        }

        String subject =
                overdue
                        ?
                        "Overdue Invoice #"
                                +
                                invoiceNumber
                        :
                        "Invoice Reminder #"
                                +
                                invoiceNumber;

        String message =
                "Hi "
                        +
                        customerName
                        +
                        ",\n\n"
                        +
                        "This is a friendly reminder regarding invoice #"
                        +
                        invoiceNumber
                        +
                        ".\n\n"
                        +
                        "Amount due: £"
                        +
                        amount
                        +
                        "\n"
                        +
                        "Due date: "
                        +
                        dueDate
                        +
                        "\n\n"
                        +
                        "Thank you,\n"
                        +
                        "Steven's Pure Clean Exteriors";

        Intent intent =
                new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                                "mailto:"
                                        +
                                        email.trim()
                                        +
                                        "?subject="
                                        +
                                        Uri.encode(subject)
                                        +
                                        "&body="
                                        +
                                        Uri.encode(message)
                        )
                );

        try {

            intent.setPackage(
                    "com.google.android.gm"
            );

            startActivity(intent);

        } catch (Exception e) {

            intent.setPackage(null);

            startActivity(intent);
        }
    }

    private File createInvoicePdf(
            String customerName,
            String invoiceNumber,
            String invoiceDate,
            String dueDate,
            String amount,
            String description)
            throws Exception {

        File folder =
                new File(
                        getCacheDir(),
                        "invoices"
                );

        if (!folder.exists()) {
            folder.mkdirs();
        }

        File file =
                new File(
                        folder,
                        "PureClean-Invoice-"
                                +
                                invoiceNumber
                                +
                                ".pdf"
                );

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

        int lime =
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

                Bitmap resized =
                        Bitmap.createScaledBitmap(
                                logo,
                                85,
                                85,
                                true
                        );

                canvas.drawBitmap(
                        resized,
                        40,
                        35,
                        paint
                );
            }

            input.close();

        } catch (Exception ignored) {
        }

        paint.setColor(lime);
        paint.setFakeBoldText(true);
        paint.setTextSize(22);

        canvas.drawText(
                "Steven's Pure Clean Exteriors",
                145,
                65,
                paint
        );

        paint.setFakeBoldText(false);
        paint.setTextSize(13);

        canvas.drawText(
                "Pure Results • Clean Exteriors",
                145,
                90,
                paint
        );

        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(true);
        paint.setTextSize(28);

        canvas.drawText(
                "INVOICE",
                40,
                160,
                paint
        );

        paint.setTextSize(15);

        canvas.drawText(
                "Invoice #"
                        +
                        invoiceNumber,
                365,
                145,
                paint
        );

        paint.setFakeBoldText(false);
        paint.setTextSize(12);

        canvas.drawText(
                "Invoice date: "
                        +
                        invoiceDate,
                365,
                175,
                paint
        );

        canvas.drawText(
                "Due date: "
                        +
                        dueDate,
                365,
                200,
                paint
        );

        paint.setFakeBoldText(true);
        paint.setTextSize(15);

        canvas.drawText(
                "Bill To",
                40,
                245,
                paint
        );

        paint.setFakeBoldText(false);

        canvas.drawText(
                customerName,
                40,
                270,
                paint
        );

        paint.setFakeBoldText(true);

        canvas.drawText(
                "Description",
                40,
                325,
                paint
        );

        paint.setFakeBoldText(false);

        String cleanDescription =
                description == null
                        ?
                        ""
                        :
                        description.trim();

        if (cleanDescription.isEmpty()) {

            cleanDescription =
                    "Exterior cleaning service";
        }

        canvas.drawText(
                cleanDescription,
                40,
                350,
                paint
        );

        paint.setColor(lime);
        paint.setFakeBoldText(true);
        paint.setTextSize(24);

        canvas.drawText(
                "Amount Due: £"
                        +
                        amount,
                40,
                415,
                paint
        );

        paint.setColor(Color.BLACK);
        paint.setTextSize(15);

        canvas.drawText(
                "Payment terms",
                40,
                510,
                paint
        );

        paint.setFakeBoldText(false);

        canvas.drawText(
                "Please make payment within 7 days",
                40,
                535,
                paint
        );

        paint.setFakeBoldText(true);
        paint.setTextSize(16);

        canvas.drawText(
                "Bank Transfer Details",
                40,
                595,
                paint
        );

        paint.setFakeBoldText(false);
        paint.setTextSize(14);

        canvas.drawText(
                "Account name: Steven B Attew",
                40,
                625,
                paint
        );

        canvas.drawText(
                "Bank: Monzo",
                40,
                650,
                paint
        );

        canvas.drawText(
                "Sort code: 04-00-06",
                40,
                675,
                paint
        );

        canvas.drawText(
                "Account number: 34121651",
                40,
                700,
                paint
        );

        document.finishPage(page);

        FileOutputStream output =
                new FileOutputStream(
                        file
                );

        document.writeTo(output);

        output.close();

        document.close();

        return file;
    }
}
