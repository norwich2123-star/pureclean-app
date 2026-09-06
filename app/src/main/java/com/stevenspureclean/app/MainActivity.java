package com.stevenspureclean.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.content.Intent;
import android.content.ClipData;

import android.content.pm.ResolveInfo;

import android.net.Uri;

import android.provider.MediaStore;

import android.util.Patterns;

import android.view.Window;
import android.view.WindowInsets;
import android.view.View;

import android.widget.ImageView;
import android.widget.Toast;

import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;

import android.media.ExifInterface;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {

    private WebView webView;

    private static final int SAVE_BACKUP = 1001;
    private static final int RESTORE_BACKUP = 1002;

    private static final int SAVE_FULL_BACKUP = 1003;
    private static final int RESTORE_FULL_BACKUP = 1004;

    private static final int TAKE_EXPENSE_PHOTO = 2001;
    private static final int CHOOSE_EXPENSE_PHOTO = 2002;

    private String backupJson = "";

    private String pendingExpenseId = "";

    private File pendingCameraFile = null;

    /*
     * =========================================================
     * PAYMENT REMINDER CONFIRMATION
     * =========================================================
     */

    private boolean waitingForReminderReturn = false;
    private boolean reminderAppActuallyOpened = false;

    private String pendingReminderId = "";
    private String pendingReminderCustomer = "";

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

                        return openExternalLink(
                                url
                        );
                    }

                    @Override
                    public WebResourceResponse shouldInterceptRequest(
                            WebView view,
                            WebResourceRequest request) {

                        WebResourceResponse receipt =
                                receiptResponse(
                                        request
                                                .getUrl()
                                                .toString()
                                );

                        if (receipt != null) {
                            return receipt;
                        }

                        return super.shouldInterceptRequest(
                                view,
                                request
                        );
                    }

                    @Override
                    public WebResourceResponse shouldInterceptRequest(
                            WebView view,
                            String url) {

                        WebResourceResponse receipt =
                                receiptResponse(
                                        url
                                );

                        if (receipt != null) {
                            return receipt;
                        }

                        return super.shouldInterceptRequest(
                                view,
                                url
                        );
                    }
                }
        );

        webView.loadUrl(
                "file:///android_asset/pureclean.html"
        );
    }

    /*
     * =========================================================
     * REMINDER RETURN CHECK
     * =========================================================
     */

    @Override
    protected void onResume() {

        super.onResume();

        if (
                waitingForReminderReturn
                        &&
                reminderAppActuallyOpened
                        &&
                pendingReminderId != null
                        &&
                !pendingReminderId.trim().isEmpty()
        ) {

            waitingForReminderReturn = false;

            new Handler(
                    Looper.getMainLooper()
            ).postDelayed(
                    () -> showReminderSentConfirmation(),
                    450
            );
        }
    }

    private void showReminderSentConfirmation() {

        if (
                pendingReminderId == null
                        ||
                pendingReminderId.trim().isEmpty()
        ) {
            return;
        }

        String name =
                pendingReminderCustomer == null
                        ?
                        ""
                        :
                        pendingReminderCustomer.trim();

        String message =
                name.isEmpty()
                        ?
                        "Did you send the payment reminder email?"
                        :
                        "Did you send the payment reminder to "
                                +
                                name
                                +
                                "?";

        new AlertDialog.Builder(
                this
        )
                .setTitle(
                        "Payment Reminder"
                )
                .setMessage(
                        message
                )
                .setCancelable(
                        false
                )
                .setPositiveButton(
                        "Yes, Sent",
                        (dialog, which) -> {

                            String reminderId =
                                    pendingReminderId;

                            clearPendingReminder();

                            callJavascript(
                                    "reminderConfirmedFromAndroid("
                                            +
                                            JSONObject.quote(
                                                    reminderId
                                            )
                                            +
                                            ");"
                            );
                        }
                )
                .setNegativeButton(
                        "No",
                        (dialog, which) -> {

                            String reminderId =
                                    pendingReminderId;

                            clearPendingReminder();

                            callJavascript(
                                    "reminderCancelledFromAndroid("
                                            +
                                            JSONObject.quote(
                                                    reminderId
                                            )
                                            +
                                            ");"
                            );
                        }
                )
                .show();
    }

    private void clearPendingReminder() {

        waitingForReminderReturn = false;
        reminderAppActuallyOpened = false;

        pendingReminderId = "";
        pendingReminderCustomer = "";
    }

    private void callJavascript(
            String javascript) {

        if (
                webView == null
                        ||
                javascript == null
                        ||
                javascript.trim().isEmpty()
        ) {
            return;
        }

        webView.post(
                () -> webView.evaluateJavascript(
                        javascript,
                        null
                )
        );
    }

    /*
     * =========================================================
     * EXTERNAL LINKS
     * =========================================================
     */

    private boolean openExternalLink(
            String url) {

        if (url == null) {
            return false;
        }

        if (
                url.startsWith(
                        "appreceipt:"
                )
        ) {
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

                startActivity(
                        intent
                );

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

    /*
     * =========================================================
     * ANDROID BRIDGE
     * =========================================================
     */

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
        public void emailQuoteWithAddress(
                String email,
                String customerName,
                String customerAddress,
                String customerPostcode,
                String quoteNumber,
                String quoteDate,
                String amount,
                String description,
                String notes) {

            runOnUiThread(
                    () -> emailQuote(
                            email,
                            customerName,
                            customerAddress,
                            customerPostcode,
                            quoteNumber,
                            quoteDate,
                            amount,
                            description,
                            notes
                    )
            );
        }

        /*
         * Kept for compatibility with older HTML versions.
         */
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
                            "",
                            email,
                            customerName,
                            invoiceNumber,
                            invoiceDate,
                            dueDate,
                            amount,
                            description,
                            overdue,
                            false
                    )
            );
        }

        /*
         * New reminder method.
         *
         * HTML sends the invoice ID so the reminder is only
         * recorded after Steven confirms that it was sent.
         */
        @JavascriptInterface
        public void sendReminderWithId(
                String reminderId,
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
                            reminderId,
                            email,
                            customerName,
                            invoiceNumber,
                            invoiceDate,
                            dueDate,
                            amount,
                            description,
                            overdue,
                            true
                    )
            );
        }

        /*
         * Opens customer directions using an Android maps app.
         */
        @JavascriptInterface
        public void openMapDirections(
                String query) {

            runOnUiThread(
                    () -> openDirections(
                            query
                    )
            );
        }

        @JavascriptInterface
        public void backupBusinessData(
                String json) {

            runOnUiThread(
                    () -> startBackup(
                            json
                    )
            );
        }

        @JavascriptInterface
        public void restoreBusinessData() {

            runOnUiThread(
                    () -> startRestore()
            );
        }

        @JavascriptInterface
        public void backupBusinessDataWithPhotos(
                String json) {

            runOnUiThread(
                    () -> startFullBackup(
                            json
                    )
            );
        }

        @JavascriptInterface
        public void restoreBusinessDataWithPhotos() {

            runOnUiThread(
                    () -> startFullRestore()
            );
        }

        @JavascriptInterface
        public void takeExpenseReceipt(
                String expenseId) {

            runOnUiThread(
                    () -> startExpenseCamera(
                            expenseId
                    )
            );
        }

        @JavascriptInterface
        public void chooseExpenseReceipt(
                String expenseId) {

            runOnUiThread(
                    () -> startExpensePhotoPicker(
                            expenseId
                    )
            );
        }

        @JavascriptInterface
        public String getExpenseReceiptUrl(
                String fileName) {

            if (
                    fileName == null
                            ||
                    fileName.trim().isEmpty()
            ) {
                return "";
            }

            return "appreceipt://receipt/"
                    +
                    Uri.encode(
                            new File(
                                    fileName
                            ).getName()
                    );
        }

        @JavascriptInterface
        public boolean expenseReceiptExists(
                String fileName) {

            File file =
                    receiptFile(
                            fileName
                    );

            return file.exists()
                    &&
                    file.isFile();
        }

        @JavascriptInterface
        public void openExpenseReceipt(
                String fileName) {

            runOnUiThread(
                    () -> showReceiptInApp(
                            fileName
                    )
            );
        }

        @JavascriptInterface
        public boolean deleteExpenseReceipt(
                String fileName) {

            File file =
                    receiptFile(
                            fileName
                    );

            if (!file.exists()) {
                return true;
            }

            return file.delete();
        }

        @JavascriptInterface
        public void shareExpenseAccountantPack(
                String fileName,
                String csvText,
                String receiptNamesJson) {

            runOnUiThread(
                    () -> shareExpensePack(
                            fileName,
                            csvText,
                            receiptNamesJson
                    )
            );
        }

        @JavascriptInterface
        public void callPhone(
                String phone) {

            runOnUiThread(
                    () -> openDialler(
                            phone
                    )
            );
        }

        @JavascriptInterface
        public void textPhone(
                String phone) {

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

    /*
     * =========================================================
     * DIRECTIONS
     * =========================================================
     */

    private void openDirections(
            String query) {

        if (
                query == null
                        ||
                query.trim().isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "No customer address is saved.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String cleanQuery =
                query.trim();

        /*
         * First try a normal Android geo intent.
         * This lets Google Maps, Waze or another mapping app
         * handle the address.
         */
        try {

            Uri geoUri =
                    Uri.parse(
                            "geo:0,0?q="
                                    +
                                    Uri.encode(
                                            cleanQuery
                                    )
                    );

            Intent mapIntent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            geoUri
                    );

            if (
                    mapIntent.resolveActivity(
                            getPackageManager()
                    ) != null
            ) {

                startActivity(
                        mapIntent
                );

                return;
            }

        } catch (Exception ignored) {
        }

        /*
         * Browser fallback.
         */
        try {

            Uri webUri =
                    Uri.parse(
                            "https://www.google.com/maps/search/?api=1&query="
                                    +
                                    Uri.encode(
                                            cleanQuery
                                    )
                    );

            Intent browserIntent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            webUri
                    );

            startActivity(
                    browserIntent
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not open directions.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /*
     * =========================================================
     * RECEIPT FILES
     * =========================================================
     */

    private File receiptFolder() {

        File folder =
                new File(
                        getFilesDir(),
                        "expense_receipts"
                );

        if (!folder.exists()) {
            folder.mkdirs();
        }

        return folder;
    }

    private File receiptFile(
            String fileName) {

        String safeName =
                new File(
                        fileName == null
                                ?
                                ""
                                :
                                fileName
                ).getName();

        return new File(
                receiptFolder(),
                safeName
        );
    }

    private int countReceiptPhotos() {

        File[] files =
                receiptFolder()
                        .listFiles();

        if (files == null) {
            return 0;
        }

        int count = 0;

        for (File file : files) {

            if (
                    file != null
                            &&
                    file.isFile()
            ) {
                count++;
            }
        }

        return count;
    }

    private String safeExpenseId(
            String expenseId) {

        if (expenseId == null) {
            return "expense";
        }

        String clean =
                expenseId.replaceAll(
                        "[^A-Za-z0-9_-]",
                        "_"
                );

        if (clean.trim().isEmpty()) {
            clean = "expense";
        }

        return clean;
    }

    private String newReceiptFileName(
            String expenseId) {

        return "receipt_"
                +
                safeExpenseId(
                        expenseId
                )
                +
                "_"
                +
                System.currentTimeMillis()
                +
                ".jpg";
    }

    private WebResourceResponse receiptResponse(
            String url) {

        if (
                url == null
                        ||
                !url.startsWith(
                        "appreceipt://receipt/"
                )
        ) {
            return null;
        }

        try {

            Uri uri =
                    Uri.parse(
                            url
                    );

            String name =
                    uri.getLastPathSegment();

            if (name == null) {
                return null;
            }

            name =
                    Uri.decode(
                            name
                    );

            File file =
                    receiptFile(
                            name
                    );

            if (
                    !file.exists()
                            ||
                    !file.isFile()
            ) {
                return null;
            }

            InputStream input =
                    new FileInputStream(
                            file
                    );

            return new WebResourceResponse(
                    "image/jpeg",
                    null,
                    input
            );

        } catch (Exception e) {

            return null;
        }
    }

    private void showReceiptInApp(
            String fileName) {

        File file =
                receiptFile(
                        fileName
                );

        if (
                !file.exists()
                        ||
                !file.isFile()
                        ||
                file.length() <= 0
        ) {

            Toast.makeText(
                    this,
                    "Receipt photo could not be found.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        Bitmap bitmap =
                BitmapFactory.decodeFile(
                        file.getAbsolutePath()
                );

        if (bitmap == null) {

            Toast.makeText(
                    this,
                    "Receipt photo could not be displayed.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        ImageView imageView =
                new ImageView(
                        this
                );

        int padding =
                (int) (
                        16
                                *
                        getResources()
                                .getDisplayMetrics()
                                .density
                );

        imageView.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        imageView.setAdjustViewBounds(
                true
        );

        imageView.setScaleType(
                ImageView.ScaleType.FIT_CENTER
        );

        imageView.setImageBitmap(
                bitmap
        );

        AlertDialog dialog =
                new AlertDialog.Builder(
                        this
                )
                        .setTitle(
                                "Receipt Photo"
                        )
                        .setView(
                                imageView
                        )
                        .setPositiveButton(
                                "Close",
                                null
                        )
                        .create();

        dialog.setOnDismissListener(
                d -> {

                    imageView.setImageDrawable(
                            null
                    );

                    if (!bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                }
        );

        dialog.show();
    }

    /*
     * =========================================================
     * CAMERA
     * =========================================================
     */

    private void startExpenseCamera(
            String expenseId) {

        pendingExpenseId =
                expenseId == null
                        ?
                        ""
                        :
                        expenseId;

        try {

            File cameraFolder =
                    new File(
                            getCacheDir(),
                            "camera_receipts"
                    );

            if (!cameraFolder.exists()) {
                cameraFolder.mkdirs();
            }

            pendingCameraFile =
                    new File(
                            cameraFolder,
                            "receipt_camera_"
                                    +
                                    System.currentTimeMillis()
                                    +
                                    ".jpg"
                    );

            Uri photoUri =
                    FileProvider.getUriForFile(
                            this,
                            getPackageName()
                                    +
                                    ".fileprovider",
                            pendingCameraFile
                    );

            Intent intent =
                    new Intent(
                            MediaStore.ACTION_IMAGE_CAPTURE
                    );

            intent.putExtra(
                    MediaStore.EXTRA_OUTPUT,
                    photoUri
            );

            intent.setClipData(
                    ClipData.newRawUri(
                            "Expense Receipt",
                            photoUri
                    )
            );

            intent.addFlags(
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            |
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            List<ResolveInfo> cameraApps =
                    getPackageManager()
                            .queryIntentActivities(
                                    intent,
                                    0
                            );

            for (ResolveInfo resolveInfo : cameraApps) {

                if (
                        resolveInfo == null
                                ||
                        resolveInfo.activityInfo == null
                ) {
                    continue;
                }

                String packageName =
                        resolveInfo.activityInfo.packageName;

                grantUriPermission(
                        packageName,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                |
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            }

            if (
                    intent.resolveActivity(
                            getPackageManager()
                    ) == null
            ) {

                if (
                        pendingCameraFile != null
                                &&
                        pendingCameraFile.exists()
                ) {
                    pendingCameraFile.delete();
                }

                pendingCameraFile = null;

                Toast.makeText(
                        this,
                        "No camera app was found.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            startActivityForResult(
                    intent,
                    TAKE_EXPENSE_PHOTO
            );

        } catch (Exception e) {

            if (
                    pendingCameraFile != null
                            &&
                    pendingCameraFile.exists()
            ) {
                pendingCameraFile.delete();
            }

            pendingCameraFile = null;

            Toast.makeText(
                    this,
                    "Could not open the camera.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void startExpensePhotoPicker(
            String expenseId) {

        pendingExpenseId =
                expenseId == null
                        ?
                        ""
                        :
                        expenseId;

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_OPEN_DOCUMENT
                    );

            intent.addCategory(
                    Intent.CATEGORY_OPENABLE
            );

            intent.setType(
                    "image/*"
            );

            startActivityForResult(
                    intent,
                    CHOOSE_EXPENSE_PHOTO
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not open your photos.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private File saveCameraReceipt() {

        if (
                pendingCameraFile == null
                        ||
                !pendingCameraFile.exists()
                        ||
                pendingCameraFile.length() <= 0
        ) {

            Toast.makeText(
                    this,
                    "Receipt photo was not saved.",
                    Toast.LENGTH_LONG
            ).show();

            return null;
        }

        try {

            File destination =
                    new File(
                            receiptFolder(),
                            newReceiptFileName(
                                    pendingExpenseId
                            )
                    );

            if (
                    !compressReceiptImage(
                            pendingCameraFile,
                            destination
                    )
            ) {

                copyFile(
                        pendingCameraFile,
                        destination
                );
            }

            if (
                    !destination.exists()
                            ||
                    destination.length() <= 0
            ) {

                Toast.makeText(
                        this,
                        "Receipt file was not saved correctly.",
                        Toast.LENGTH_LONG
                ).show();

                return null;
            }

            Toast.makeText(
                    this,
                    "Receipt photo saved.",
                    Toast.LENGTH_SHORT
            ).show();

            return destination;

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not save the receipt photo.",
                    Toast.LENGTH_LONG
            ).show();

            return null;
        }
    }

    private void saveChosenExpensePhoto(
            Uri sourceUri) {

        File tempFile = null;

        try {

            tempFile =
                    new File(
                            getCacheDir(),
                            "expense_photo_"
                                    +
                                    System.currentTimeMillis()
                                    +
                                    ".jpg"
                    );

            InputStream input =
                    getContentResolver()
                            .openInputStream(
                                    sourceUri
                            );

            if (input == null) {

                Toast.makeText(
                        this,
                        "Could not read that photo.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            FileOutputStream tempOutput =
                    new FileOutputStream(
                            tempFile
                    );

            byte[] buffer =
                    new byte[8192];

            int length;

            while (
                    (length = input.read(buffer)) > 0
            ) {

                tempOutput.write(
                        buffer,
                        0,
                        length
                );
            }

            tempOutput.flush();
            tempOutput.close();
            input.close();

            File destination =
                    new File(
                            receiptFolder(),
                            newReceiptFileName(
                                    pendingExpenseId
                            )
                    );

            if (
                    !compressReceiptImage(
                            tempFile,
                            destination
                    )
            ) {

                copyFile(
                        tempFile,
                        destination
                );
            }

            if (
                    !destination.exists()
                            ||
                    destination.length() <= 0
            ) {

                Toast.makeText(
                        this,
                        "Receipt file was not saved correctly.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            notifyExpenseReceiptSaved(
                    pendingExpenseId,
                    destination.getName()
            );

            Toast.makeText(
                    this,
                    "Receipt photo saved.",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not save that receipt photo.",
                    Toast.LENGTH_LONG
            ).show();

        } finally {

            if (
                    tempFile != null
                            &&
                    tempFile.exists()
            ) {
                tempFile.delete();
            }
        }
    }

    private boolean compressReceiptImage(
            File source,
            File destination) {

        Bitmap bitmap = null;
        Bitmap rotated = null;

        try {

            BitmapFactory.Options bounds =
                    new BitmapFactory.Options();

            bounds.inJustDecodeBounds = true;

            BitmapFactory.decodeFile(
                    source.getAbsolutePath(),
                    bounds
            );

            int sample = 1;

            while (
                    bounds.outWidth / sample > 1800
                            ||
                    bounds.outHeight / sample > 1800
            ) {

                sample *= 2;
            }

            BitmapFactory.Options options =
                    new BitmapFactory.Options();

            options.inSampleSize =
                    Math.max(
                            1,
                            sample
                    );

            bitmap =
                    BitmapFactory.decodeFile(
                            source.getAbsolutePath(),
                            options
                    );

            if (bitmap == null) {
                return false;
            }

            int rotation =
                    getPhotoRotation(
                            source
                    );

            if (rotation != 0) {

                Matrix matrix =
                        new Matrix();

                matrix.postRotate(
                        rotation
                );

                rotated =
                        Bitmap.createBitmap(
                                bitmap,
                                0,
                                0,
                                bitmap.getWidth(),
                                bitmap.getHeight(),
                                matrix,
                                true
                        );

            } else {

                rotated =
                        bitmap;
            }

            FileOutputStream output =
                    new FileOutputStream(
                            destination
                    );

            boolean result =
                    rotated.compress(
                            Bitmap.CompressFormat.JPEG,
                            82,
                            output
                    );

            output.flush();
            output.close();

            if (
                    rotated != bitmap
                            &&
                    rotated != null
                            &&
                    !rotated.isRecycled()
            ) {

                rotated.recycle();
            }

            if (
                    bitmap != null
                            &&
                    !bitmap.isRecycled()
            ) {

                bitmap.recycle();
            }

            return result;

        } catch (Exception e) {

            try {

                if (
                        rotated != null
                                &&
                        rotated != bitmap
                                &&
                        !rotated.isRecycled()
                ) {
                    rotated.recycle();
                }

                if (
                        bitmap != null
                                &&
                        !bitmap.isRecycled()
                ) {
                    bitmap.recycle();
                }

            } catch (Exception ignored) {
            }

            return false;
        }
    }

    private int getPhotoRotation(
            File file) {

        try {

            ExifInterface exif =
                    new ExifInterface(
                            file.getAbsolutePath()
                    );

            int orientation =
                    exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                    );

            if (
                    orientation
                            ==
                    ExifInterface.ORIENTATION_ROTATE_90
            ) {
                return 90;
            }

            if (
                    orientation
                            ==
                    ExifInterface.ORIENTATION_ROTATE_180
            ) {
                return 180;
            }

            if (
                    orientation
                            ==
                    ExifInterface.ORIENTATION_ROTATE_270
            ) {
                return 270;
            }

        } catch (Exception ignored) {
        }

        return 0;
    }

    private void notifyExpenseReceiptSaved(
            String expenseId,
            String fileName) {

        if (webView == null) {
            return;
        }

        String javascript =
                "if(typeof expenseReceiptSaved==='function'){"
                        +
                        "expenseReceiptSaved("
                        +
                        JSONObject.quote(
                                expenseId == null
                                        ?
                                        ""
                                        :
                                        expenseId
                        )
                        +
                        ","
                        +
                        JSONObject.quote(
                                fileName == null
                                        ?
                                        ""
                                        :
                                        fileName
                        )
                        +
                        ");"
                        +
                        "}";

        webView.evaluateJavascript(
                javascript,
                null
        );
    }

    /*
     * =========================================================
     * PHONE / TEXT
     * =========================================================
     */

    private void openDialler(
            String phone) {

        if (
                phone == null
                        ||
                phone.trim().isEmpty()
        ) {
            return;
        }

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_DIAL,
                            Uri.parse(
                                    "tel:"
                                            +
                                            Uri.encode(
                                                    phone.trim()
                                            )
                            )
                    );

            startActivity(
                    intent
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not open the phone.",
                    Toast.LENGTH_SHORT
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
                                    Uri.encode(
                                            phone.trim()
                                    )
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

            startActivity(
                    intent
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not open messages.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /*
     * =========================================================
     * CSV
     * =========================================================
     */

    private void shareCsvFile(
            String fileName,
            String csvText) {

        try {

            String safeFileName =
                    fileName == null
                            ||
                    fileName.trim().isEmpty()
                            ?
                            "PureClean-Export.csv"
                            :
                            fileName.trim();

            if (
                    !safeFileName
                            .toLowerCase(
                                    Locale.UK
                            )
                            .endsWith(
                                    ".csv"
                            )
            ) {

                safeFileName +=
                        ".csv";
            }

            File folder =
                    new File(
                            getCacheDir(),
                            "exports"
                    );

            if (!folder.exists()) {
                folder.mkdirs();
            }

            File file =
                    new File(
                            folder,
                            safeFileName
                    );

            FileOutputStream output =
                    new FileOutputStream(
                            file
                    );

            output.write(
                    (
                            csvText == null
                                    ?
                                    ""
                                    :
                                    csvText
                    ).getBytes(
                            "UTF-8"
                    )
            );

            output.flush();
            output.close();

            Uri uri =
                    FileProvider.getUriForFile(
                            this,
                            getPackageName()
                                    +
                                    ".fileprovider",
                            file
                    );

            Intent intent =
                    new Intent(
                            Intent.ACTION_SEND
                    );

            intent.setType(
                    "text/csv"
            );

            intent.putExtra(
                    Intent.EXTRA_STREAM,
                    uri
            );

            intent.setClipData(
                    ClipData.newRawUri(
                            "CSV Export",
                            uri
                    )
            );

            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(
                    Intent.createChooser(
                            intent,
                            "Share CSV"
                    )
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not create CSV.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void shareExpensePack(
            String fileName,
            String csvText,
            String receiptNamesJson) {

        try {

            File folder =
                    new File(
                            getCacheDir(),
                            "accountant_pack"
                    );

            deleteFolder(
                    folder
            );

            folder.mkdirs();

            String safeFileName =
                    fileName == null
                            ||
                    fileName.trim().isEmpty()
                            ?
                            "PureClean-Expenses.csv"
                            :
                            fileName.trim();

            if (
                    !safeFileName
                            .toLowerCase(
                                    Locale.UK
                            )
                            .endsWith(
                                    ".csv"
                            )
            ) {

                safeFileName +=
                        ".csv";
            }

            File csv =
                    new File(
                            folder,
                            safeFileName
                    );

            FileOutputStream csvOutput =
                    new FileOutputStream(
                            csv
                    );

            csvOutput.write(
                    (
                            csvText == null
                                    ?
                                    ""
                                    :
                                    csvText
                    ).getBytes(
                            "UTF-8"
                    )
            );

            csvOutput.flush();
            csvOutput.close();

            File zipFile =
                    new File(
                            getCacheDir(),
                            "PureClean-Accountant-Pack-"
                                    +
                                    System.currentTimeMillis()
                                    +
                                    ".zip"
                    );

            ZipOutputStream zip =
                    new ZipOutputStream(
                            new FileOutputStream(
                                    zipFile
                            )
                    );

            addFileToZip(
                    zip,
                    csv,
                    csv.getName()
            );

            JSONArray names;

            try {

                names =
                        new JSONArray(
                                receiptNamesJson == null
                                        ?
                                        "[]"
                                        :
                                        receiptNamesJson
                        );

            } catch (Exception e) {

                names =
                        new JSONArray();
            }

            for (
                    int i = 0;
                    i < names.length();
                    i++
            ) {

                String receiptName =
                        names.optString(
                                i,
                                ""
                        );

                if (
                        receiptName == null
                                ||
                        receiptName.trim().isEmpty()
                ) {
                    continue;
                }

                File receipt =
                        receiptFile(
                                receiptName
                        );

                if (
                        receipt.exists()
                                &&
                        receipt.isFile()
                ) {

                    addFileToZip(
                            zip,
                            receipt,
                            "receipts/"
                                    +
                                    receipt.getName()
                    );
                }
            }

            zip.finish();
            zip.close();

            Uri uri =
                    FileProvider.getUriForFile(
                            this,
                            getPackageName()
                                    +
                                    ".fileprovider",
                            zipFile
                    );

            Intent shareIntent =
                    new Intent(
                            Intent.ACTION_SEND
                    );

            shareIntent.setType(
                    "application/zip"
            );

            shareIntent.putExtra(
                    Intent.EXTRA_STREAM,
                    uri
            );

            shareIntent.setClipData(
                    ClipData.newRawUri(
                            "Accountant Pack",
                            uri
                    )
            );

            shareIntent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(
                    Intent.createChooser(
                            shareIntent,
                            "Share Accountant Pack"
                    )
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not create accountant pack.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void addFileToZip(
            ZipOutputStream zip,
            File file,
            String entryName)
            throws Exception {

        zip.putNextEntry(
                new ZipEntry(
                        entryName
                )
        );

        FileInputStream input =
                new FileInputStream(
                        file
                );

        byte[] buffer =
                new byte[8192];

        int length;

        while (
                (length = input.read(buffer)) > 0
        ) {

            zip.write(
                    buffer,
                    0,
                    length
            );
        }

        input.close();

        zip.closeEntry();
    }

    /*
     * =========================================================
     * FULL BACKUP
     * =========================================================
     */

    private void startFullBackup(
            String json) {

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

        try {

            JSONObject backup =
                    new JSONObject(
                            json
                    );

            JSONArray customers =
                    backup.optJSONArray(
                            "customers"
                    );

            JSONArray invoices =
                    backup.optJSONArray(
                            "invoices"
                    );

            JSONArray quotes =
                    backup.optJSONArray(
                            "quotes"
                    );

            JSONArray expenses =
                    backup.optJSONArray(
                            "expenses"
                    );

            if (
                    customers == null
                            ||
                    invoices == null
                            ||
                    expenses == null
            ) {

                Toast.makeText(
                        this,
                        "Backup could not be created because some business data is missing.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            int quoteCount =
                    quotes == null
                            ?
                            0
                            :
                            quotes.length();

            int receiptCount =
                    countReceiptPhotos();

            String message =
                    "This backup will contain:\n\n"
                            +
                            customers.length()
                            +
                            " customers\n"
                            +
                            invoices.length()
                            +
                            " invoices\n"
                            +
                            quoteCount
                            +
                            " quotes\n"
                            +
                            expenses.length()
                            +
                            " expenses\n"
                            +
                            receiptCount
                            +
                            " receipt photos"
                            +
                            "\n\nSave this backup now?";

            new AlertDialog.Builder(
                    this
            )
                    .setTitle(
                            "Backup Everything"
                    )
                    .setMessage(
                            message
                    )
                    .setNegativeButton(
                            "Cancel",
                            null
                    )
                    .setPositiveButton(
                            "Save Backup",
                            (dialog, which) -> {

                                backupJson =
                                        json;

                                openFullBackupSaveScreen();
                            }
                    )
                    .show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Business data could not be prepared for backup.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void openFullBackupSaveScreen() {

        String stamp =
                new SimpleDateFormat(
                        "yyyy-MM-dd-HHmmss",
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
                "application/zip"
        );

        intent.putExtra(
                Intent.EXTRA_TITLE,
                "PureClean-Full-Backup-"
                        +
                        stamp
                        +
                        ".zip"
        );

        try {

            startActivityForResult(
                    intent,
                    SAVE_FULL_BACKUP
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not open full backup screen.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void startFullRestore() {

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.setType(
                "application/zip"
        );

        try {

            startActivityForResult(
                    intent,
                    RESTORE_FULL_BACKUP
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not open full restore screen.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /*
     * BUG FIX:
     * Android only tells the HTML that the backup completed
     * AFTER the ZIP has actually been written successfully.
     */
    private void saveFullBackup(
            Uri uri) {

        try {

            OutputStream rawOutput =
                    getContentResolver()
                            .openOutputStream(
                                    uri
                            );

            if (rawOutput == null) {

                Toast.makeText(
                        this,
                        "Full backup could not be saved.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            ZipOutputStream zip =
                    new ZipOutputStream(
                            rawOutput
                    );

            zip.putNextEntry(
                    new ZipEntry(
                            "business.json"
                    )
            );

            zip.write(
                    backupJson.getBytes(
                            "UTF-8"
                    )
            );

            zip.closeEntry();

            File[] receipts =
                    receiptFolder()
                            .listFiles();

            if (receipts != null) {

                byte[] buffer =
                        new byte[8192];

                for (File receipt : receipts) {

                    if (
                            receipt == null
                                    ||
                            !receipt.isFile()
                    ) {
                        continue;
                    }

                    zip.putNextEntry(
                            new ZipEntry(
                                    "receipts/"
                                            +
                                            receipt.getName()
                            )
                    );

                    FileInputStream input =
                            new FileInputStream(
                                    receipt
                            );

                    int length;

                    while (
                            (length = input.read(buffer)) > 0
                    ) {

                        zip.write(
                                buffer,
                                0,
                                length
                        );
                    }

                    input.close();

                    zip.closeEntry();
                }
            }

            zip.finish();
            zip.close();

            backupJson = "";

            callJavascript(
                    "if(typeof backupCompletedFromAndroid==='function'){backupCompletedFromAndroid();}"
            );

            Toast.makeText(
                    this,
                    "Full backup saved.",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            backupJson = "";

            Toast.makeText(
                    this,
                    "Full backup could not be saved.",
                    Toast.LENGTH_LONG
            ).show();
        }
        }
        private void restoreFullBackup(
            Uri uri) {

        File restoreFolder =
                new File(
                        getCacheDir(),
                        "full_restore"
                );

        try {

            deleteFolder(
                    restoreFolder
            );

            restoreFolder.mkdirs();

            InputStream rawInput =
                    getContentResolver()
                            .openInputStream(
                                    uri
                            );

            if (rawInput == null) {

                Toast.makeText(
                        this,
                        "Backup could not be opened.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            ZipInputStream zip =
                    new ZipInputStream(
                            rawInput
                    );

            ZipEntry entry;

            String businessJson = null;

            while (
                    (entry = zip.getNextEntry()) != null
            ) {

                String entryName =
                        entry.getName();

                if (
                        entryName == null
                                ||
                        entryName.trim().isEmpty()
                ) {

                    zip.closeEntry();
                    continue;
                }

                if (
                        entryName.equals(
                                "business.json"
                        )
                ) {

                    StringBuilder text =
                            new StringBuilder();

                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            zip,
                                            "UTF-8"
                                    )
                            );

                    char[] buffer =
                            new char[4096];

                    int length;

                    while (
                            (length = reader.read(buffer)) > 0
                    ) {

                        text.append(
                                buffer,
                                0,
                                length
                        );
                    }

                    businessJson =
                            text.toString();

                } else if (
                        entryName.startsWith(
                                "receipts/"
                        )
                ) {

                    String fileName =
                            new File(
                                    entryName
                            ).getName();

                    if (
                            !fileName.trim().isEmpty()
                    ) {

                        File destination =
                                receiptFile(
                                        fileName
                                );

                        FileOutputStream output =
                                new FileOutputStream(
                                        destination
                                );

                        byte[] buffer =
                                new byte[8192];

                        int length;

                        while (
                                (length = zip.read(buffer)) > 0
                        ) {

                            output.write(
                                    buffer,
                                    0,
                                    length
                            );
                        }

                        output.flush();
                        output.close();
                    }
                }

                zip.closeEntry();
            }

            zip.close();

            if (
                    businessJson == null
                            ||
                    businessJson.trim().isEmpty()
            ) {

                Toast.makeText(
                        this,
                        "This does not look like a Pure Clean full backup.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            JSONObject test =
                    new JSONObject(
                            businessJson
                    );

            if (
                    test.optJSONArray(
                            "customers"
                    ) == null
                            ||
                    test.optJSONArray(
                            "invoices"
                    ) == null
            ) {

                Toast.makeText(
                        this,
                        "Backup business data is invalid.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            String finalBusinessJson =
                    businessJson;

            new AlertDialog.Builder(
                    this
            )
                    .setTitle(
                            "Restore Full Backup"
                    )
                    .setMessage(
                            "This will replace the business data currently stored in the app.\n\nContinue?"
                    )
                    .setNegativeButton(
                            "Cancel",
                            null
                    )
                    .setPositiveButton(
                            "Restore",
                            (dialog, which) -> {

                                callJavascript(
                                        "restoreBusinessBackup("
                                                +
                                                JSONObject.quote(
                                                        finalBusinessJson
                                                )
                                                +
                                                ");"
                                );

                                Toast.makeText(
                                        this,
                                        "Full backup restored.",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                    )
                    .show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Full backup could not be restored.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /*
     * =========================================================
     * STANDARD DATA BACKUP
     * =========================================================
     */

    private void startBackup(
            String json) {

        if (
                json == null
                        ||
                json.trim().isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "No data to back up.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        backupJson =
                json;

        String stamp =
                new SimpleDateFormat(
                        "yyyy-MM-dd-HHmmss",
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
                        stamp
                        +
                        ".json"
        );

        try {

            startActivityForResult(
                    intent,
                    SAVE_BACKUP
            );

        } catch (Exception e) {

            backupJson = "";

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

        intent.setType(
                "application/json"
        );

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

    private void saveBackup(
            Uri uri) {

        try {

            OutputStream output =
                    getContentResolver()
                            .openOutputStream(
                                    uri
                            );

            if (output == null) {

                Toast.makeText(
                        this,
                        "Backup could not be saved.",
                        Toast.LENGTH_LONG
                ).show();

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
                    "Backup saved.",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            backupJson = "";

            Toast.makeText(
                    this,
                    "Backup could not be saved.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void restoreBackup(
            Uri uri) {

        try {

            InputStream input =
                    getContentResolver()
                            .openInputStream(
                                    uri
                            );

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

            StringBuilder builder =
                    new StringBuilder();

            String line;

            while (
                    (line = reader.readLine()) != null
            ) {

                builder.append(
                        line
                );

                builder.append(
                        "\n"
                );
            }

            reader.close();
            input.close();

            String json =
                    builder.toString();

            new JSONObject(
                    json
            );

            callJavascript(
                    "restoreBusinessBackup("
                            +
                            JSONObject.quote(
                                    json
                            )
                            +
                            ");"
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Backup could not be restored.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /*
     * =========================================================
     * ACTIVITY RESULTS
     * =========================================================
     */

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

        /*
         * Camera receipt
         */
        if (
                requestCode
                        ==
                TAKE_EXPENSE_PHOTO
        ) {

            if (
                    resultCode
                            ==
                    RESULT_OK
            ) {

                File saved =
                        saveCameraReceipt();

                if (saved != null) {

                    notifyExpenseReceiptSaved(
                            pendingExpenseId,
                            saved.getName()
                    );
                }
            }

            if (
                    pendingCameraFile != null
                            &&
                    pendingCameraFile.exists()
            ) {

                pendingCameraFile.delete();
            }

            pendingCameraFile = null;
            pendingExpenseId = "";

            return;
        }

        /*
         * Existing receipt photo
         */
        if (
                requestCode
                        ==
                CHOOSE_EXPENSE_PHOTO
        ) {

            if (
                    resultCode
                            ==
                    RESULT_OK
                    &&
                    data != null
                    &&
                    data.getData() != null
            ) {

                saveChosenExpensePhoto(
                        data.getData()
                );
            }

            pendingExpenseId = "";

            return;
        }

        /*
         * BUG FIX:
         * If Full Backup Save is cancelled,
         * throw away the pending JSON rather than leaving it
         * hanging around for a future backup.
         */
        if (
                requestCode
                        ==
                SAVE_FULL_BACKUP
                &&
                resultCode
                        !=
                RESULT_OK
        ) {

            backupJson = "";
            return;
        }

        if (
                resultCode
                        !=
                RESULT_OK
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

            saveBackup(
                    uri
            );

        } else if (
                requestCode
                        ==
                RESTORE_BACKUP
        ) {

            restoreBackup(
                    uri
            );

        } else if (
                requestCode
                        ==
                SAVE_FULL_BACKUP
        ) {

            saveFullBackup(
                    uri
            );

        } else if (
                requestCode
                        ==
                RESTORE_FULL_BACKUP
        ) {

            restoreFullBackup(
                    uri
            );
        }
    }

    /*
     * =========================================================
     * PAYMENT REMINDER EMAIL
     * =========================================================
     */

    private void reminderEmail(
            String reminderId,
            String email,
            String customerName,
            String invoiceNumber,
            String invoiceDate,
            String dueDate,
            String amount,
            String description,
            boolean overdue,
            boolean requireConfirmation) {

        if (
                email == null
                        ||
                email.trim().isEmpty()
                        ||
                !Patterns.EMAIL_ADDRESS
                        .matcher(
                                email.trim()
                        )
                        .matches()
        ) {

            Toast.makeText(
                    this,
                    "Customer email address is missing or invalid.",
                    Toast.LENGTH_LONG
            ).show();

            if (requireConfirmation) {

                callJavascript(
                        "reminderCancelledFromAndroid("
                                +
                                JSONObject.quote(
                                        reminderId
                                )
                                +
                                ");"
                );
            }

            return;
        }

        String subject =
                overdue
                        ?
                        "Payment Reminder - Invoice #"
                                +
                                invoiceNumber
                        :
                        "Invoice Reminder - Invoice #"
                                +
                                invoiceNumber;

        String firstName =
                customerName == null
                        ?
                        ""
                        :
                        customerName.trim();

        if (
                firstName.contains(
                        " "
                )
        ) {

            firstName =
                    firstName.substring(
                            0,
                            firstName.indexOf(
                                    " "
                            )
                    );
        }

        StringBuilder body =
                new StringBuilder();

        body.append(
                "Hi"
        );

        if (
                !firstName.isEmpty()
        ) {

            body.append(
                    " "
            );

            body.append(
                    firstName
            );
        }

        body.append(
                ",\n\n"
        );

        if (overdue) {

            body.append(
                    "Just a friendly reminder that payment for the following invoice is now overdue."
            );

        } else {

            body.append(
                    "Just a friendly reminder regarding the following unpaid invoice."
            );
        }

        body.append(
                "\n\nInvoice #"
        );

        body.append(
                invoiceNumber
        );

        if (
                invoiceDate != null
                        &&
                !invoiceDate.trim().isEmpty()
        ) {

            body.append(
                    "\nInvoice date: "
            );

            body.append(
                    invoiceDate
            );
        }

        if (
                dueDate != null
                        &&
                !dueDate.trim().isEmpty()
        ) {

            body.append(
                    "\nDue date: "
            );

            body.append(
                    dueDate
            );
        }

        body.append(
                "\nAmount: £"
        );

        body.append(
                cleanMoney(
                        amount
                )
        );

        if (
                description != null
                        &&
                !description.trim().isEmpty()
        ) {

            body.append(
                    "\n"
            );

            body.append(
                    description.trim()
            );
        }

        body.append(
                "\n\nPlease make payment as soon as convenient."
        );

        body.append(
                "\n\nMany thanks,\nSteven's Pure Clean Exteriors"
        );

        try {

            Uri uri =
                    Uri.parse(
                            "mailto:"
                                    +
                                    Uri.encode(
                                            email.trim()
                                    )
                    );

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            uri
                    );

            intent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    subject
            );

            intent.putExtra(
                    Intent.EXTRA_TEXT,
                    body.toString()
            );

            if (
                    intent.resolveActivity(
                            getPackageManager()
                    ) == null
            ) {

                Toast.makeText(
                        this,
                        "No email app was found.",
                        Toast.LENGTH_LONG
                ).show();

                if (requireConfirmation) {

                    callJavascript(
                            "reminderCancelledFromAndroid("
                                    +
                                    JSONObject.quote(
                                            reminderId
                                    )
                                    +
                                    ");"
                    );
                }

                return;
            }

            if (requireConfirmation) {

                pendingReminderId =
                        reminderId == null
                                ?
                                ""
                                :
                                reminderId;

                pendingReminderCustomer =
                        customerName == null
                                ?
                                ""
                                :
                                customerName;

                waitingForReminderReturn =
                        true;

                reminderAppActuallyOpened =
                        true;
            }

            startActivity(
                    intent
            );

        } catch (Exception e) {

            clearPendingReminder();

            Toast.makeText(
                    this,
                    "Could not open the email app.",
                    Toast.LENGTH_LONG
            ).show();

            if (requireConfirmation) {

                callJavascript(
                        "reminderCancelledFromAndroid("
                                +
                                JSONObject.quote(
                                        reminderId
                                )
                                +
                                ");"
                );
            }
        }
    }

    /*
     * =========================================================
     * QUOTE EMAIL
     * =========================================================
     */

    private void emailQuote(
            String email,
            String customerName,
            String customerAddress,
            String customerPostcode,
            String quoteNumber,
            String quoteDate,
            String amount,
            String description,
            String notes) {

        if (
                email == null
                        ||
                email.trim().isEmpty()
                        ||
                !Patterns.EMAIL_ADDRESS
                        .matcher(
                                email.trim()
                        )
                        .matches()
        ) {

            Toast.makeText(
                    this,
                    "Quote email address is missing or invalid.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        try {

            File pdf =
                    createQuotePdf(
                            customerName,
                            customerAddress,
                            customerPostcode,
                            quoteNumber,
                            quoteDate,
                            amount,
                            description,
                            notes
                    );

            if (
                    pdf == null
                            ||
                    !pdf.exists()
            ) {

                Toast.makeText(
                        this,
                        "Quote PDF could not be created.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            Uri pdfUri =
                    FileProvider.getUriForFile(
                            this,
                            getPackageName()
                                    +
                                    ".fileprovider",
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
                    new String[]{
                            email.trim()
                    }
            );

            intent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Quote Q"
                            +
                            quoteNumber
                            +
                            " - Steven's Pure Clean Exteriors"
            );

            String body =
                    "Hi "
                            +
                            safePersonName(
                                    customerName
                            )
                            +
                            ",\n\n"
                            +
                            "Please find your quote attached.\n\n"
                            +
                            "Quote total: £"
                            +
                            cleanMoney(
                                    amount
                            )
                            +
                            "\n\n"
                            +
                            "This quote is valid for 30 days."
                            +
                            "\n\n"
                            +
                            "Many thanks,\n"
                            +
                            "Steven's Pure Clean Exteriors";

            intent.putExtra(
                    Intent.EXTRA_TEXT,
                    body
            );

            intent.putExtra(
                    Intent.EXTRA_STREAM,
                    pdfUri
            );

            intent.setClipData(
                    ClipData.newRawUri(
                            "Quote PDF",
                            pdfUri
                    )
            );

            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(
                    Intent.createChooser(
                            intent,
                            "Email Quote"
                    )
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not open quote email.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private File createQuotePdf(
            String customerName,
            String customerAddress,
            String customerPostcode,
            String quoteNumber,
            String quoteDate,
            String amount,
            String description,
            String notes) {

        PdfDocument document =
                new PdfDocument();

        try {

            File folder =
                    new File(
                            getCacheDir(),
                            "quotes"
                    );

            if (!folder.exists()) {
                folder.mkdirs();
            }

            File[] oldFiles =
                    folder.listFiles();

            if (oldFiles != null) {

                for (File old : oldFiles) {

                    if (
                            old != null
                                    &&
                            old.isFile()
                                    &&
                            old.getName()
                                    .startsWith(
                                            "PureClean-Quote-"
                                    )
                    ) {

                        old.delete();
                    }
                }
            }

            File outputFile =
                    new File(
                            folder,
                            "PureClean-Quote-"
                                    +
                                    cleanFilePart(
                                            quoteNumber
                                    )
                                    +
                                    "-"
                                    +
                                    System.currentTimeMillis()
                                    +
                                    ".pdf"
                    );

            PdfDocument.PageInfo pageInfo =
                    new PdfDocument.PageInfo.Builder(
                            595,
                            842,
                            1
                    )
                            .create();

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

            /*
             * Black top banner
             */
            paint.setColor(
                    Color.rgb(
                            8,
                            8,
                            8
                    )
            );

            canvas.drawRect(
                    0,
                    0,
                    595,
                    180,
                    paint
            );

            Bitmap logo =
                    loadCompanyLogo();

            if (logo != null) {

                RectF logoRect =
                        new RectF(
                                34,
                                28,
                                144,
                                138
                        );

                canvas.drawBitmap(
                        logo,
                        null,
                        logoRect,
                        paint
                );
            }

            paint.setColor(
                    Color.rgb(
                            155,
                            214,
                            0
                    )
            );

            paint.setTextSize(
                    24
            );

            paint.setFakeBoldText(
                    true
            );

            canvas.drawText(
                    "STEVEN'S PURE CLEAN",
                    165,
                    62,
                    paint
            );

            canvas.drawText(
                    "EXTERIORS",
                    165,
                    91,
                    paint
            );

            paint.setTextSize(
                    12
            );

            paint.setFakeBoldText(
                    false
            );

            canvas.drawText(
                    "Pure Results • Clean Exteriors",
                    165,
                    117,
                    paint
            );

            paint.setColor(
                    Color.WHITE
            );

            paint.setTextSize(
                    28
            );

            paint.setFakeBoldText(
                    true
            );

            canvas.drawText(
                    "QUOTE",
                    432,
                    155,
                    paint
            );

            paint.setFakeBoldText(
                    false
            );

            /*
             * Quote details
             */
            paint.setColor(
                    Color.rgb(
                            35,
                            35,
                            35
                    )
            );

            paint.setTextSize(
                    11
            );

            canvas.drawText(
                    "Quote Number",
                    36,
                    213,
                    paint
            );

            canvas.drawText(
                    "Quote Date",
                    210,
                    213,
                    paint
            );

            canvas.drawText(
                    "Valid For",
                    380,
                    213,
                    paint
            );

            paint.setTextSize(
                    14
            );

            paint.setFakeBoldText(
                    true
            );

            canvas.drawText(
                    "Q"
                            +
                            safePdfText(
                                    quoteNumber
                            ),
                    36,
                    235,
                    paint
            );

            canvas.drawText(
                    safePdfText(
                            quoteDate
                    ),
                    210,
                    235,
                    paint
            );

            canvas.drawText(
                    "30 days",
                    380,
                    235,
                    paint
            );

            paint.setFakeBoldText(
                    false
            );

            /*
             * Customer
             */
            paint.setColor(
                    Color.rgb(
                            155,
                            214,
                            0
                    )
            );

            paint.setTextSize(
                    14
            );

            paint.setFakeBoldText(
                    true
            );

            canvas.drawText(
                    "QUOTE FOR",
                    36,
                    284,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            25,
                            25,
                            25
                    )
            );

            paint.setTextSize(
                    14
            );

            canvas.drawText(
                    safePdfText(
                            customerName
                    ),
                    36,
                    310,
                    paint
            );

            paint.setFakeBoldText(
                    false
            );

            paint.setTextSize(
                    12
            );

            int customerY =
                    332;

            if (
                    customerAddress != null
                            &&
                    !customerAddress.trim().isEmpty()
            ) {

                canvas.drawText(
                        safePdfText(
                                customerAddress
                        ),
                        36,
                        customerY,
                        paint
                );

                customerY +=
                        20;
            }

            if (
                    customerPostcode != null
                            &&
                    !customerPostcode.trim().isEmpty()
            ) {

                canvas.drawText(
                        safePdfText(
                                customerPostcode
                        ),
                        36,
                        customerY,
                        paint
                );
            }

            /*
             * Services table
             */
            int tableTop =
                    386;

            paint.setColor(
                    Color.rgb(
                            20,
                            20,
                            20
                    )
            );

            canvas.drawRect(
                    36,
                    tableTop,
                    559,
                    tableTop + 34,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            155,
                            214,
                            0
                    )
            );

            paint.setTextSize(
                    12
            );

            paint.setFakeBoldText(
                    true
            );

            canvas.drawText(
                    "SERVICE",
                    50,
                    tableTop + 22,
                    paint
            );

            canvas.drawText(
                    "PRICE",
                    484,
                    tableTop + 22,
                    paint
            );

            paint.setFakeBoldText(
                    false
            );

            int lineY =
                    448;

            String[] lines =
                    (
                            description == null
                                    ?
                                    ""
                                    :
                                    description
                    )
                            .split(
                                    "\\r?\\n"
                            );

            paint.setTextSize(
                    12
            );

            for (String line : lines) {

                if (
                        line == null
                                ||
                        line.trim().isEmpty()
                ) {
                    continue;
                }

                String service =
                        line.trim();

                String price =
                        "";

                int separator =
                        service.lastIndexOf(
                                " - £"
                        );

                if (separator >= 0) {

                    price =
                            service.substring(
                                    separator + 4
                            )
                                    .trim();

                    service =
                            service.substring(
                                    0,
                                    separator
                            )
                                    .trim();
                }

                paint.setColor(
                        Color.rgb(
                                35,
                                35,
                                35
                        )
                );

                canvas.drawText(
                        safePdfText(
                                service
                        ),
                        50,
                        lineY,
                        paint
                );

                if (!price.isEmpty()) {

                    paint.setFakeBoldText(
                            true
                    );

                    canvas.drawText(
                            "£"
                                    +
                                    cleanMoney(
                                            price
                                    ),
                            484,
                            lineY,
                            paint
                    );

                    paint.setFakeBoldText(
                            false
                    );
                }

                paint.setColor(
                        Color.rgb(
                                220,
                                220,
                                220
                        )
                );

                canvas.drawLine(
                        42,
                        lineY + 10,
                        553,
                        lineY + 10,
                        paint
                );

                lineY +=
                        22;

                /*
                 * Fits six service rows on the existing PDF.
                 */
                if (lineY > 580) {
                    break;
                }
            }

            /*
             * Total
             */
            int totalTop =
                    Math.max(
                            lineY + 18,
                            612
                    );

            paint.setColor(
                    Color.rgb(
                            240,
                            240,
                            240
                    )
            );

            canvas.drawRoundRect(
                    new RectF(
                            338,
                            totalTop,
                            559,
                            totalTop + 60
                    ),
                    8,
                    8,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            40,
                            40,
                            40
                    )
            );

            paint.setTextSize(
                    13
            );

            paint.setFakeBoldText(
                    true
            );

            canvas.drawText(
                    "TOTAL",
                    357,
                    totalTop + 25,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            72,
                            145,
                            34
                    )
            );

            paint.setTextSize(
                    20
            );

            canvas.drawText(
                    "£"
                            +
                            cleanMoney(
                                    amount
                            ),
                    445,
                    totalTop + 38,
                    paint
            );

            paint.setFakeBoldText(
                    false
            );

            /*
             * Notes
             */
            if (
                    notes != null
                            &&
                    !notes.trim().isEmpty()
            ) {

                paint.setColor(
                        Color.rgb(
                                45,
                                45,
                                45
                        )
                );

                paint.setTextSize(
                        11
                );

                paint.setFakeBoldText(
                        true
                );

                canvas.drawText(
                        "NOTES",
                        36,
                        704,
                        paint
                );

                paint.setFakeBoldText(
                        false
                );

                drawWrappedText(
                        canvas,
                        paint,
                        notes,
                        36,
                        725,
                        500,
                        14,
                        3
                );
            }

            /*
             * Footer
             */
            paint.setColor(
                    Color.rgb(
                            155,
                            214,
                            0
                    )
            );

            canvas.drawRect(
                    0,
                    792,
                    595,
                    842,
                    paint
            );

            paint.setColor(
                    Color.BLACK
            );

            paint.setTextSize(
                    11
            );

            paint.setFakeBoldText(
                    true
            );

            canvas.drawText(
                    "Steven's Pure Clean Exteriors",
                    36,
                    817,
                    paint
            );

            paint.setFakeBoldText(
                    false
            );

            paint.setTextSize(
                    9
            );

            canvas.drawText(
                    "Thank you for the opportunity to provide this quote.",
                    36,
                    833,
                    paint
            );

            document.finishPage(
                    page
            );

            FileOutputStream output =
                    new FileOutputStream(
                            outputFile
                    );

            document.writeTo(
                    output
            );

            output.flush();
            output.close();

            document.close();

            return outputFile;

        } catch (Exception e) {

            try {
                document.close();
            } catch (Exception ignored) {
            }

            return null;
        }
    }

    /*
     * =========================================================
     * INVOICE EMAIL
     * =========================================================
     */

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

        if (
                email == null
                        ||
                email.trim().isEmpty()
                        ||
                !Patterns.EMAIL_ADDRESS
                        .matcher(
                                email.trim()
                        )
                        .matches()
        ) {

            Toast.makeText(
                    this,
                    "Customer email address is missing or invalid.",
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

            if (
                    pdf == null
                            ||
                    !pdf.exists()
            ) {

                Toast.makeText(
                        this,
                        "Invoice PDF could not be created.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            Uri pdfUri =
                    FileProvider.getUriForFile(
                            this,
                            getPackageName()
                                    +
                                    ".fileprovider",
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
                    new String[]{
                            email.trim()
                    }
            );

            intent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Invoice #"
                            +
                            invoiceNumber
                            +
                            " - Steven's Pure Clean Exteriors"
            );

            String body =
                    "Hi "
                            +
                            safePersonName(
                                    customerName
                            )
                            +
                            ",\n\n"
                            +
                            "Please find your invoice attached."
                            +
                            "\n\n"
                            +
                            "Amount due: £"
                            +
                            cleanMoney(
                                    amount
                            )
                            +
                            "\n"
                            +
                            "Please make payment within 7 days."
                            +
                            "\n\n"
                            +
                            "Many thanks,\n"
                            +
                            "Steven's Pure Clean Exteriors";

            intent.putExtra(
                    Intent.EXTRA_TEXT,
                    body
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

            startActivity(
                    Intent.createChooser(
                            intent,
                            "Email Invoice"
                    )
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not open invoice email.",
                    Toast.LENGTH_LONG
            ).show();
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
            String description) {

        PdfDocument document =
                new PdfDocument();

        try {

            File folder =
                    new File(
                            getCacheDir(),
                            "invoices"
                    );

            if (!folder.exists()) {
                folder.mkdirs();
            }

            /*
             * Remove old temporary invoice PDFs so Samsung/Gmail
             * does not accidentally show a stale version.
             */
            File[] oldFiles =
                    folder.listFiles();

            if (oldFiles != null) {

                for (File old : oldFiles) {

                    if (
                            old != null
                                    &&
                            old.isFile()
                                    &&
                            old.getName()
                                    .startsWith(
                                            "PureClean-Invoice-"
                                    )
                    ) {

                        old.delete();
                    }
                }
            }

            File outputFile =
                    new File(
                            folder,
                            "PureClean-Invoice-"
                                    +
                                    cleanFilePart(
                                            invoiceNumber
                                    )
                                    +
                                    "-"
                                    +
                                    System.currentTimeMillis()
                                    +
                                    ".pdf"
                    );

            PdfDocument.PageInfo pageInfo =
                    new PdfDocument.PageInfo.Builder(
                            595,
                            842,
                            1
                    )
                            .create();

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

            /*
             * Header
             */
            paint.setColor(
                    Color.rgb(
                            8,
                            8,
                            8
                    )
            );

            canvas.drawRect(
                    0,
                    0,
                    595,
                    180,
                    paint
            );

            Bitmap logo =
                    loadCompanyLogo();

            if (logo != null) {

                canvas.drawBitmap(
                        logo,
                        null,
                        new RectF(
                                34,
                                28,
                                144,
                                138
                        ),
                        paint
                );
            }

            paint.setColor(
                    Color.rgb(
                            155,
                            214,
                            0
                    )
            );

            paint.setTextSize(
                    24
            );

            paint.setFakeBoldText(
                    true
            );

            canvas.drawText(
                    "STEVEN'S PURE CLEAN",
                    165,
                    62,
                    paint
            );

            canvas.drawText(
                    "EXTERIORS",
                    165,
                    91,
                    paint
            );

            paint.setTextSize(
                    12
            );

            paint.setFakeBoldText(
                    false
            );

            canvas.drawText(
                    "Pure Results • Clean Exteriors",
                    165,
                    117,
                    paint
            );

            paint.setColor(
                    Color.WHITE
            );

            paint.setTextSize(
                    28
            );

            paint.setFakeBoldText(
                    true
            );

            canvas.drawText(
                    "INVOICE",
                    405,
                    155,
                    paint
            );

            /*
             * Invoice data
             */
            paint.setColor(
                    Color.rgb(
                            35,
                            35,
                            35
                    )
            );

            paint.setTextSize(
                    11
            );

            paint.setFakeBoldText(
                    false
            );

            canvas.drawText(
                    "Invoice Number",
                    36,
                    213,
                    paint
            );

            canvas.drawText(
                    "Invoice Date",
                    210,
                    213,
                    paint
            );

            canvas.drawText(
                    "Due Date",
                    380,
                    213,
                    paint
            );

            paint.setTextSize(
                    14
            );

            paint.setFakeBoldText(
                    true
            );

            canvas.drawText(
                    "#"
                            +
                            safePdfText(
                                    invoiceNumber
                            ),
                    36,
                    235,
                    paint
            );

            canvas.drawText(
                    safePdfText(
                            invoiceDate
                    ),
                    210,
                    235,
                    paint
            );

            canvas.drawText(
                    safePdfText(
                            dueDate
                    ),
                    380,
                    235,
                    paint
            );

            /*
             * Bill to
             */
            paint.setColor(
                    Color.rgb(
                            155,
                            214,
                            0
                    )
            );

            paint.setTextSize(
                    14
            );

            canvas.drawText(
                    "BILL TO",
                    36,
                    284,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            30,
                            30,
                            30
                    )
            );

            paint.setTextSize(
                    14
            );

            canvas.drawText(
                    safePdfText(
                            customerName
                    ),
                    36,
                    310,
                    paint
            );

            paint.setTextSize(
                    12
            );

            paint.setFakeBoldText(
                    false
            );

            int y =
                    332;

            if (
                    customerAddress != null
                            &&
                    !customerAddress.trim().isEmpty()
            ) {

                canvas.drawText(
                        safePdfText(
                                customerAddress
                        ),
                        36,
                        y,
                        paint
                );

                y +=
                        20;
            }

            if (
                    customerPostcode != null
                            &&
                    !customerPostcode.trim().isEmpty()
            ) {

                canvas.drawText(
                        safePdfText(
                                customerPostcode
                        ),
                        36,
                        y,
                        paint
                );
            }

            /*
             * Invoice table
             */
            paint.setColor(
                    Color.rgb(
                            20,
                            20,
                            20
                    )
            );

            canvas.drawRect(
                    36,
                    386,
                    559,
                    420,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            155,
                            214,
                            0
                    )
            );

            paint.setTextSize(
                    12
            );

            paint.setFakeBoldText(
                    true
            );

            canvas.drawText(
                    "DESCRIPTION",
                    50,
                    408,
                    paint
            );

            canvas.drawText(
                    "AMOUNT",
                    474,
                    408,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            35,
                            35,
                            35
                    )
            );

            paint.setFakeBoldText(
                    false
            );

            paint.setTextSize(
                    12
            );

            String invoiceDescription =
                    description == null
                            ||
                    description.trim().isEmpty()
                            ?
                            "Exterior Cleaning Service"
                            :
                            description.trim();

            drawWrappedText(
                    canvas,
                    paint,
                    invoiceDescription,
                    50,
                    452,
                    385,
                    17,
                    5
            );

            paint.setFakeBoldText(
                    true
            );

            canvas.drawText(
                    "£"
                            +
                            cleanMoney(
                                    amount
                            ),
                    475,
                    452,
                    paint
            );

            /*
             * Total
             */
            paint.setColor(
                    Color.rgb(
                            240,
                            240,
                            240
                    )
            );

            canvas.drawRoundRect(
                    new RectF(
                            338,
                            565,
                            559,
                            630
                    ),
                    8,
                    8,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            40,
                            40,
                            40
                    )
            );

            paint.setTextSize(
                    13
            );

            canvas.drawText(
                    "TOTAL DUE",
                    355,
                    592,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            72,
                            145,
                            34
                    )
            );

            paint.setTextSize(
                    21
            );

            canvas.drawText(
                    "£"
                            +
                            cleanMoney(
                                    amount
                            ),
                    445,
                    612,
                    paint
            );

            /*
             * Payment terms
             */
            paint.setColor(
                    Color.rgb(
                            55,
                            55,
                            55
                    )
            );

            paint.setTextSize(
                    12
            );

            paint.setFakeBoldText(
                    true
            );

            canvas.drawText(
                    "PAYMENT TERMS",
                    36,
                    687,
                    paint
            );

            paint.setFakeBoldText(
                    false
            );

            paint.setTextSize(
                    11
            );

            canvas.drawText(
                    "Please make payment within 7 days.",
                    36,
                    710,
                    paint
            );

            /*
             * Footer
             */
            paint.setColor(
                    Color.rgb(
                            155,
                            214,
                            0
                    )
            );

            canvas.drawRect(
                    0,
                    792,
                    595,
                    842,
                    paint
            );

            paint.setColor(
                    Color.BLACK
            );

            paint.setTextSize(
                    11
            );

            paint.setFakeBoldText(
                    true
            );

            canvas.drawText(
                    "Steven's Pure Clean Exteriors",
                    36,
                    817,
                    paint
            );

            paint.setFakeBoldText(
                    false
            );

            paint.setTextSize(
                    9
            );

            canvas.drawText(
                    "Thank you for your business.",
                    36,
                    833,
                    paint
            );

            document.finishPage(
                    page
            );

            FileOutputStream output =
                    new FileOutputStream(
                            outputFile
                    );

            document.writeTo(
                    output
            );

            output.flush();
            output.close();

            document.close();

            return outputFile;

        } catch (Exception e) {

            try {
                document.close();
            } catch (Exception ignored) {
            }

            return null;
        }
    }

    /*
     * =========================================================
     * PDF HELPERS
     * =========================================================
     */

    private Bitmap loadCompanyLogo() {

        try {

            InputStream input =
                    getAssets()
                            .open(
                                    "logo.jpg"
                            );

            Bitmap bitmap =
                    BitmapFactory.decodeStream(
                            input
                    );

            input.close();

            return bitmap;

        } catch (Exception e) {

            return null;
        }
    }

    private void drawWrappedText(
            Canvas canvas,
            Paint paint,
            String text,
            float x,
            float startY,
            float maxWidth,
            float lineHeight,
            int maxLines) {

        if (
                text == null
                        ||
                text.trim().isEmpty()
        ) {
            return;
        }

        String clean =
                text.replace(
                        "\r",
                        " "
                )
                        .replace(
                                "\n",
                                " "
                        )
                        .trim();

        String[] words =
                clean.split(
                        "\\s+"
                );

        StringBuilder line =
                new StringBuilder();

        float y =
                startY;

        int lines =
                0;

        for (String word : words) {

            String trial =
                    line.length() == 0
                            ?
                            word
                            :
                            line
                                    +
                                    " "
                                    +
                                    word;

            if (
                    paint.measureText(
                            trial
                    )
                            >
                    maxWidth
            ) {

                if (
                        line.length() > 0
                ) {

                    canvas.drawText(
                            safePdfText(
                                    line.toString()
                            ),
                            x,
                            y,
                            paint
                    );

                    y +=
                            lineHeight;

                    lines++;

                    if (
                            lines >= maxLines
                    ) {
                        return;
                    }
                }

                line =
                        new StringBuilder(
                                word
                        );

            } else {

                line =
                        new StringBuilder(
                                trial
                        );
            }
        }

        if (
                line.length() > 0
                        &&
                lines < maxLines
        ) {

            canvas.drawText(
                    safePdfText(
                            line.toString()
                    ),
                    x,
                    y,
                    paint
            );
        }
    }

    private String safePdfText(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace(
                        "\n",
                        " "
                )
                .replace(
                        "\r",
                        " "
                )
                .trim();
    }

    private String safePersonName(
            String name) {

        if (
                name == null
                        ||
                name.trim().isEmpty()
        ) {

            return "there";
        }

        String clean =
                name.trim();

        int space =
                clean.indexOf(
                        " "
                );

        if (space > 0) {

            clean =
                    clean.substring(
                            0,
                            space
                    );
        }

        return clean;
    }

    /*
     * Prevents the double-£ problem.
     */
    private String cleanMoney(
            String amount) {

        if (amount == null) {
            return "0.00";
        }

        String clean =
                amount
                        .replace(
                                "£",
                                ""
                        )
                        .replace(
                                ",",
                                ""
                        )
                        .trim();

        try {

            double value =
                    Double.parseDouble(
                            clean
                    );

            return String.format(
                    Locale.UK,
                    "%.2f",
                    value
            );

        } catch (Exception e) {

            return clean.isEmpty()
                    ?
                    "0.00"
                    :
                    clean;
        }
    }

    private String cleanFilePart(
            String value) {

        if (value == null) {
            return "file";
        }

        String clean =
                value.replaceAll(
                        "[^A-Za-z0-9_-]",
                        "_"
                );

        if (
                clean.trim().isEmpty()
        ) {

            clean =
                    "file";
        }

        return clean;
    }

    /*
     * =========================================================
     * FILE HELPERS
     * =========================================================
     */

    private void copyFile(
            File source,
            File destination)
            throws Exception {

        FileInputStream input =
                new FileInputStream(
                        source
                );

        FileOutputStream output =
                new FileOutputStream(
                        destination
                );

        byte[] buffer =
                new byte[8192];

        int length;

        while (
                (length = input.read(buffer)) > 0
        ) {

            output.write(
                    buffer,
                    0,
                    length
            );
        }

        output.flush();

        input.close();
        output.close();
    }

    private void deleteFolder(
            File file) {

        if (
                file == null
                        ||
                !file.exists()
        ) {
            return;
        }

        if (file.isDirectory()) {

            File[] children =
                    file.listFiles();

            if (children != null) {

                for (File child : children) {

                    deleteFolder(
                            child
                    );
                }
            }
        }

        file.delete();
    }
                                }
