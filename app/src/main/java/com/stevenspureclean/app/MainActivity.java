package com.stevenspureclean.app;

import android.app.Activity;
import android.os.Bundle;

import android.content.Intent;
import android.content.ClipData;

import android.net.Uri;

import android.util.Patterns;

import android.view.Window;
import android.view.WindowInsets;
import android.view.View;

import android.widget.Toast;

import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
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

        requestWindowFeature(
                Window.FEATURE_NO_TITLE
        );

        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(
                Color.rgb(20, 20, 20)
        );

        getWindow().setNavigationBarColor(
                Color.BLACK
        );

        webView = new WebView(this);

        webView.setOnApplyWindowInsetsListener(
                new View.OnApplyWindowInsetsListener() {

                    @Override
                    public WindowInsets onApplyWindowInsets(
                            View view,
                            WindowInsets insets) {

                        int top =
                                insets.getSystemWindowInsetTop();

                        int bottom =
                                insets.getSystemWindowInsetBottom();

                        view.setPadding(
                                0,
                                top,
                                0,
                                bottom
                        );

                        return insets;
                    }
                }
        );

        setContentView(webView);

        webView.requestApplyInsets();

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        webView.setWebChromeClient(
                new WebChromeClient()
        );

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
                                request
                                        .getUrl()
                                        .toString()
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
                            "",
                            "",
                            invoiceNumber,
                            invoiceDate,
                            dueDate,
                            amount,
                            description
                    )
            );
        }

        @JavascriptInterface
        public void emailCustomerWithAddress(
                String email,
                String customerName,
                String customerAddress,
                String customerPostcode,
                String invoiceNumber,
                String invoiceDate,
                String dueDate,
                String amount,
                String description) {

            runOnUiThread(
                    () -> emailInvoice(
                            email,
                            customerName,
                            customerAddress,
                            customerPostcode,
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
                    () -> openTextMessage(
                            phone,
                            ""
                    )
            );
        }

        @JavascriptInterface
        public void textPhoneWithMessage(
                String phone,
                String message) {

            runOnUiThread(
                    () -> openTextMessage(
                            phone,
                            message
                    )
            );
        }

        @JavascriptInterface
        public void shareCsv(
                String fileName,
                String csvText) {

            runOnUiThread(
                    () -> shareCsvFile(
                            fileName,
                            csvText
                    )
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
                                    +
                                    phone.trim()
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

    private void openTextMessage(
            String phone,
            String message) {

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
                                    +
                                    phone.trim()
                    )
            );

            if (
                    message != null
                            &&
                    !message.trim().isEmpty()
            ) {

                intent.putExtra(
                        "sms_body",
                        message
                );
            }

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not open messages.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void shareCsvFile(
            String fileName,
            String csvText) {

        if (
                csvText == null
                        ||
                csvText.trim().isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "There is no CSV data to share.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        try {

            String safeFileName =
                    fileName;

            if (
                    safeFileName == null
                            ||
                    safeFileName.trim().isEmpty()
            ) {

                safeFileName =
                        "PureClean-Accountant.csv";
            }

            if (
                    !safeFileName
                            .toLowerCase()
                            .endsWith(".csv")
            ) {

                safeFileName += ".csv";
            }

            File folder =
                    new File(
                            getCacheDir(),
                            "exports"
                    );

            if (!folder.exists()) {
                folder.mkdirs();
            }

            File csvFile =
                    new File(
                            folder,
                            safeFileName
                    );

            FileOutputStream output =
                    new FileOutputStream(
                            csvFile
                    );

            output.write(
                    csvText.getBytes(
                            "UTF-8"
                    )
            );

            output.flush();
            output.close();

            Uri csvUri =
                    FileProvider.getUriForFile(
                            this,
                            getPackageName()
                                    +
                                    ".fileprovider",
                            csvFile
                    );

            Intent shareIntent =
                    new Intent(
                            Intent.ACTION_SEND
                    );

            shareIntent.setType(
                    "text/csv"
            );

            shareIntent.putExtra(
                    Intent.EXTRA_STREAM,
                    csvUri
            );

            shareIntent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Pure Clean Accountant Export"
            );

            shareIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    "Pure Clean accountant invoice export attached."
            );

            shareIntent.setClipData(
                    ClipData.newRawUri(
                            "Accountant CSV",
                            csvUri
                    )
            );

            shareIntent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(
                    Intent.createChooser(
                            shareIntent,
                            "Share Accountant CSV"
                    )
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not create accountant CSV.",
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
                ).format(
                        new Date()
                );

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
                        +
                        date
                        +
                        ".json"
        );

        try {

            startActivityForResult(
                    intent,
                    SAVE_BACKUP
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not open backup screen.",
                    Toast.LENGTH_LONG
            ).show();
        }
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

        try {

            startActivityForResult(
                    intent,
                    RESTORE_BACKUP
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not open restore screen.",
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
                            !=
                    null
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

    private boolean validEmail(String email) {

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
            String customerAddress,
            String customerPostcode,
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
                            customerAddress,
                            customerPostcode,
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
            String customerAddress,
            String customerPostcode,
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

        int lime =
                Color.rgb(
                        155,
                        214,
                        0
                );

        int dark =
                Color.rgb(
                        20,
                        20,
                        20
                );

        int grey =
                Color.rgb(
                        110,
                        110,
                        110
                );

        int lightGrey =
                Color.rgb(
                        238,
                        238,
                        238
                );

        canvas.drawColor(
                Color.WHITE
        );

        paint.setColor(dark);

        canvas.drawRect(
                0,
                0,
                595,
                135,
                paint
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
                                82,
                                82,
                                true
                        );

                canvas.drawBitmap(
                        resized,
                        35,
                        26,
                        paint
                );
            }

            input.close();

        } catch (Exception ignored) {
        }

        paint.setColor(lime);
        paint.setFakeBoldText(true);
        paint.setTextSize(21);

        canvas.drawText(
                "Steven's Pure Clean Exteriors",
                135,
                55,
                paint
        );

        paint.setFakeBoldText(false);
        paint.setTextSize(12);

        canvas.drawText(
                "Pure Results • Clean Exteriors",
                135,
                82,
                paint
        );

        paint.setColor(Color.WHITE);
        paint.setFakeBoldText(true);
        paint.setTextSize(30);

        canvas.drawText(
                "INVOICE",
                430,
                105,
                paint
        );

        paint.setColor(dark);
        paint.setFakeBoldText(true);
        paint.setTextSize(13);

        canvas.drawText(
                "INVOICE NUMBER",
                35,
                175,
                paint
        );

        canvas.drawText(
                "INVOICE DATE",
                220,
                175,
                paint
        );

        canvas.drawText(
                "DUE DATE",
                405,
                175,
                paint
        );

        paint.setFakeBoldText(false);
        paint.setColor(grey);
        paint.setTextSize(13);

        canvas.drawText(
                "#" + invoiceNumber,
                35,
                198,
                paint
        );

        canvas.drawText(
                invoiceDate,
                220,
                198,
                paint
        );

        canvas.drawText(
                dueDate,
                405,
                198,
                paint
        );

        paint.setColor(lightGrey);

        canvas.drawRect(
                35,
                225,
                560,
                227,
                paint
        );

        paint.setColor(dark);
        paint.setFakeBoldText(true);
        paint.setTextSize(15);

        canvas.drawText(
                "BILL TO",
                35,
                260,
                paint
        );

        paint.setFakeBoldText(false);
        paint.setTextSize(16);

        canvas.drawText(
                customerName == null
                        ?
                        ""
                        :
                        customerName,
                35,
                288,
                paint
        );

        paint.setColor(grey);
        paint.setTextSize(12);

        if (
                customerAddress != null
                        &&
                !customerAddress.trim().isEmpty()
        ) {

            canvas.drawText(
                    customerAddress.trim(),
                    35,
                    310,
                    paint
            );
        }

        if (
                customerPostcode != null
                        &&
                !customerPostcode.trim().isEmpty()
        ) {

            canvas.drawText(
                    customerPostcode.trim(),
                    35,
                    329,
                    paint
            );
        }

        paint.setColor(lightGrey);

        RectF serviceBox =
                new RectF(
                        35,
                        355,
                        560,
                        455
                );

        canvas.drawRoundRect(
                serviceBox,
                10,
                10,
                paint
        );

        paint.setColor(dark);
        paint.setFakeBoldText(true);
        paint.setTextSize(13);

        canvas.drawText(
                "DESCRIPTION",
                52,
                385,
                paint
        );

        paint.setFakeBoldText(false);
        paint.setTextSize(14);

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
                52,
                415,
                paint
        );

        paint.setColor(dark);

        RectF amountBox =
                new RectF(
                        325,
                        480,
                        560,
                        555
                );

        canvas.drawRoundRect(
                amountBox,
                10,
                10,
                paint
        );

        paint.setColor(Color.WHITE);
        paint.setTextSize(12);
        paint.setFakeBoldText(false);

        canvas.drawText(
                "AMOUNT DUE",
                345,
                507,
                paint
        );

        paint.setColor(lime);
        paint.setFakeBoldText(true);
        paint.setTextSize(27);

        canvas.drawText(
                "£" + amount,
                345,
                540,
                paint
        );

        paint.setColor(dark);
        paint.setFakeBoldText(true);
        paint.setTextSize(15);

        canvas.drawText(
                "Payment Details",
                35,
                595,
                paint
        );

        paint.setFakeBoldText(false);
        paint.setTextSize(13);

        canvas.drawText(
                "Please make payment within 7 days.",
                35,
                620,
                paint
        );

        paint.setColor(lightGrey);

        RectF bankBox =
                new RectF(
                        35,
                        645,
                        560,
                        765
                );

        canvas.drawRoundRect(
                bankBox,
                10,
                10,
                paint
        );

        paint.setColor(dark);
        paint.setFakeBoldText(true);
        paint.setTextSize(14);

        canvas.drawText(
                "BANK TRANSFER",
                52,
                675,
                paint
        );

        paint.setFakeBoldText(false);
        paint.setTextSize(13);

        canvas.drawText(
                "Account name: Steven B Attew",
                52,
                702,
                paint
        );

        canvas.drawText(
                "Bank: Monzo",
                52,
                726,
                paint
        );

        canvas.drawText(
                "Sort code: 04-00-06",
                310,
                702,
                paint
        );

        canvas.drawText(
                "Account number: 34121651",
                310,
                726,
                paint
        );

        paint.setColor(grey);
        paint.setTextSize(11);

        canvas.drawText(
                "Thank you for your custom.",
                35,
                805,
                paint
        );

        paint.setTextAlign(
                Paint.Align.RIGHT
        );

        canvas.drawText(
                "Steven's Pure Clean Exteriors",
                560,
                805,
                paint
        );

        paint.setTextAlign(
                Paint.Align.LEFT
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
