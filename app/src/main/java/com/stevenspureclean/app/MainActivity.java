package com.stevenspureclean.app;

import android.app.Activity;
import android.os.Bundle;

import android.content.Intent;
import android.content.ClipData;

import android.content.pm.ResolveInfo;

import android.net.Uri;

import android.provider.MediaStore;

import android.util.Patterns;

import android.view.Window;
import android.view.WindowInsets;
import android.view.View;

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

    /*
     * Camera uses a temporary cache file first.
     */
    private File pendingCameraFile = null;

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
                                receiptResponse(url);

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

    private boolean openExternalLink(String url) {

        if (url == null) {
            return false;
        }

        if (url.startsWith("appreceipt:")) {
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
        public void backupBusinessDataWithPhotos(
                String json) {

            runOnUiThread(
                    () -> startFullBackup(json)
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
                    () -> openReceiptPhoto(
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

    /*
     * =========================================================
     * EXPENSE RECEIPT STORAGE
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
                    Uri.parse(url);

            String name =
                    uri.getLastPathSegment();

            if (name == null) {
                return null;
            }

            name =
                    Uri.decode(name);

            File file =
                    receiptFile(name);

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

                if (!cameraFolder.mkdirs()) {

                    Toast.makeText(
                            this,
                            "Could not prepare camera storage.",
                            Toast.LENGTH_LONG
                    ).show();

                    return;
                }
            }

            pendingCameraFile =
                    File.createTempFile(
                            "receipt_camera_",
                            ".jpg",
                            cameraFolder
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
                        resolveInfo
                                .activityInfo
                                .packageName;

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
                    )
                            ==
                    null
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
                    (length = input.read(buffer))
                            >
                    0
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
        Bitmap scaled = null;

        try {

            BitmapFactory.Options bounds =
                    new BitmapFactory.Options();

            bounds.inJustDecodeBounds = true;

            BitmapFactory.decodeFile(
                    source.getAbsolutePath(),
                    bounds
            );

            if (
                    bounds.outWidth <= 0
                            ||
                    bounds.outHeight <= 0
            ) {
                return false;
            }

            int maxDecodeSize = 2200;

            int sample = 1;

            while (
                    bounds.outWidth / sample
                            >
                    maxDecodeSize
                            ||
                    bounds.outHeight / sample
                            >
                    maxDecodeSize
            ) {

                sample *= 2;
            }

            BitmapFactory.Options options =
                    new BitmapFactory.Options();

            options.inSampleSize = sample;

            bitmap =
                    BitmapFactory.decodeFile(
                            source.getAbsolutePath(),
                            options
                    );

            if (bitmap == null) {
                return false;
            }

            int rotation = 0;

            try {

                ExifInterface exif =
                        new ExifInterface(
                                source.getAbsolutePath()
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

                    rotation = 90;

                } else if (
                        orientation
                                ==
                        ExifInterface.ORIENTATION_ROTATE_180
                ) {

                    rotation = 180;

                } else if (
                        orientation
                                ==
                        ExifInterface.ORIENTATION_ROTATE_270
                ) {

                    rotation = 270;
                }

            } catch (Exception ignored) {
            }

            rotated = bitmap;

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
            }

            int width =
                    rotated.getWidth();

            int height =
                    rotated.getHeight();

            int maxSide = 1600;

            if (
                    width > maxSide
                            ||
                    height > maxSide
            ) {

                float scale =
                        Math.min(
                                (float) maxSide
                                        /
                                width,
                                (float) maxSide
                                        /
                                height
                        );

                int newWidth =
                        Math.max(
                                1,
                                Math.round(
                                        width * scale
                                )
                        );

                int newHeight =
                        Math.max(
                                1,
                                Math.round(
                                        height * scale
                                )
                        );

                scaled =
                        Bitmap.createScaledBitmap(
                                rotated,
                                newWidth,
                                newHeight,
                                true
                        );

            } else {

                scaled = rotated;
            }

            FileOutputStream output =
                    new FileOutputStream(
                            destination
                    );

            boolean success =
                    scaled.compress(
                            Bitmap.CompressFormat.JPEG,
                            78,
                            output
                    );

            output.flush();
            output.close();

            return success;

        } catch (Exception e) {

            return false;

        } finally {

            if (
                    scaled != null
                            &&
                    scaled != rotated
                            &&
                    !scaled.isRecycled()
            ) {

                scaled.recycle();
            }

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

            return null;
        }

        File destination =
                new File(
                        receiptFolder(),
                        newReceiptFileName(
                                pendingExpenseId
                        )
                );

        try {

            boolean compressed =
                    compressReceiptImage(
                            pendingCameraFile,
                            destination
                    );

            if (!compressed) {

                copyFile(
                        pendingCameraFile,
                        destination
                );
            }

            if (
                    destination.exists()
                            &&
                    destination.length() > 0
            ) {

                return destination;
            }

        } catch (Exception ignored) {
        }

        if (destination.exists()) {
            destination.delete();
        }

        return null;
    }

    private void notifyExpenseReceiptSaved(
            String expenseId,
            String fileName) {

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

    private void openReceiptPhoto(
            String fileName) {

        File file =
                receiptFile(
                        fileName
                );

        if (!file.exists()) {

            Toast.makeText(
                    this,
                    "Receipt photo could not be found.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        try {

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
                            Intent.ACTION_VIEW
                    );

            intent.setDataAndType(
                    uri,
                    "image/jpeg"
            );

            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not open receipt photo.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /*
     * =========================================================
     * FULL BUSINESS BACKUP WITH PHOTOS
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

        /*
         * Make sure this really is a complete current backup
         * before allowing it to be saved.
         */
        try {

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

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Business data could not be prepared for backup.",
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
                "application/zip"
        );

        intent.putExtra(
                Intent.EXTRA_TITLE,
                "PureClean-Full-Backup-"
                        +
                        date
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

    private void saveFullBackup(
            Uri uri) {

        try {

            OutputStream rawOutput =
                    getContentResolver()
                            .openOutputStream(uri);

            if (rawOutput == null) {
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

            byte[] jsonBytes =
                    backupJson.getBytes(
                            "UTF-8"
                    );

            zip.write(
                    jsonBytes
            );

            zip.closeEntry();

            File[] receipts =
                    receiptFolder()
                            .listFiles();

            int receiptCount = 0;

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
                            (length = input.read(buffer))
                                    >
                            0
                    ) {

                        zip.write(
                                buffer,
                                0,
                                length
                        );
                    }

                    input.close();

                    zip.closeEntry();

                    receiptCount++;
                }
            }

            zip.finish();
            zip.close();

            backupJson = "";

            Toast.makeText(
                    this,
                    "Full backup saved with "
                            +
                            receiptCount
                            +
                            " receipt photo"
                            +
                            (
                                    receiptCount
                                            ==
                                    1
                                            ?
                                    ""
                                            :
                                    "s"
                            )
                            +
                            ".",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

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
                        "restore_receipts"
                );

        deleteFolder(
                restoreFolder
        );

        restoreFolder.mkdirs();

        String restoredJson = null;
        int receiptCount = 0;

        try {

            InputStream rawInput =
                    getContentResolver()
                            .openInputStream(uri);

            if (rawInput == null) {
                return;
            }

            ZipInputStream zip =
                    new ZipInputStream(
                            rawInput
                    );

            ZipEntry entry;

            byte[] buffer =
                    new byte[8192];

            while (
                    (entry = zip.getNextEntry())
                            !=
                    null
            ) {

                String name =
                        entry.getName();

                if (
                        "business.json"
                                .equals(name)
                ) {

                    StringBuilder jsonText =
                            new StringBuilder();

                    byte[] textBuffer =
                            new byte[8192];

                    int length;

                    while (
                            (length = zip.read(textBuffer))
                                    >
                            0
                    ) {

                        jsonText.append(
                                new String(
                                        textBuffer,
                                        0,
                                        length,
                                        "UTF-8"
                                )
                        );
                    }

                    restoredJson =
                            jsonText
                                    .toString()
                                    .trim();

                } else if (
                        name != null
                                &&
                        name.startsWith(
                                "receipts/"
                        )
                                &&
                        !entry.isDirectory()
                ) {

                    String safeName =
                            new File(name)
                                    .getName();

                    if (
                            safeName.trim()
                                    .isEmpty()
                    ) {

                        zip.closeEntry();
                        continue;
                    }

                    File receipt =
                            new File(
                                    restoreFolder,
                                    safeName
                            );

                    FileOutputStream output =
                            new FileOutputStream(
                                    receipt
                            );

                    int length;

                    while (
                            (length = zip.read(buffer))
                                    >
                            0
                    ) {

                        output.write(
                                buffer,
                                0,
                                length
                        );
                    }

                    output.flush();
                    output.close();

                    receiptCount++;
                }

                zip.closeEntry();
            }

            zip.close();
            rawInput.close();

            if (
                    restoredJson == null
                            ||
                    restoredJson.trim()
                            .isEmpty()
            ) {

                deleteFolder(
                        restoreFolder
                );

                Toast.makeText(
                        this,
                        "This is not a Pure Clean full backup.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            JSONObject backup =
                    new JSONObject(
                            restoredJson
                    );

            JSONArray customers =
                    backup.optJSONArray(
                            "customers"
                    );

            JSONArray invoices =
                    backup.optJSONArray(
                            "invoices"
                    );

            JSONArray expenses =
                    backup.optJSONArray(
                            "expenses"
                    );

            /*
             * IMPORTANT:
             * A valid current backup must contain all three.
             * This prevents an older or incomplete backup from
             * appearing to restore successfully while expenses
             * silently disappear.
             */
            if (
                    customers == null
                            ||
                    invoices == null
                            ||
                    expenses == null
            ) {

                deleteFolder(
                        restoreFolder
                );

                Toast.makeText(
                        this,
                        "This backup is incomplete. Customers, invoices and expenses are all required.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            File liveReceipts =
                    receiptFolder();

            File[] existing =
                    liveReceipts.listFiles();

            if (existing != null) {

                for (File file : existing) {

                    if (
                            file != null
                                    &&
                            file.isFile()
                    ) {

                        file.delete();
                    }
                }
            }

            File[] restoredFiles =
                    restoreFolder.listFiles();

            if (restoredFiles != null) {

                for (File file : restoredFiles) {

                    if (
                            file != null
                                    &&
                            file.isFile()
                    ) {

                        copyFile(
                                file,
                                new File(
                                        liveReceipts,
                                        file.getName()
                                )
                        );
                    }
                }
            }

            deleteFolder(
                    restoreFolder
            );

            String javascript =
                    "restoreBusinessBackup("
                            +
                            JSONObject.quote(
                                    restoredJson
                            )
                            +
                            ");";

            webView.evaluateJavascript(
                    javascript,
                    null
            );

            Toast.makeText(
                    this,
                    "Restore complete: "
                            +
                            customers.length()
                            +
                            " customer"
                            +
                            (
                                    customers.length() == 1
                                            ?
                                    ""
                                            :
                                    "s"
                            )
                            +
                            ", "
                            +
                            invoices.length()
                            +
                            " invoice"
                            +
                            (
                                    invoices.length() == 1
                                            ?
                                    ""
                                            :
                                    "s"
                            )
                            +
                            ", "
                            +
                            expenses.length()
                            +
                            " expense"
                            +
                            (
                                    expenses.length() == 1
                                            ?
                                    ""
                                            :
                                    "s"
                            )
                            +
                            ", "
                            +
                            receiptCount
                            +
                            " receipt photo"
                            +
                            (
                                    receiptCount == 1
                                            ?
                                    ""
                                            :
                                    "s"
                            )
                            +
                            ".",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            deleteFolder(
                    restoreFolder
            );

            Toast.makeText(
                    this,
                    "Could not restore full backup.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /*
     * =========================================================
     * EXPENSE ACCOUNTANT ZIP
     * =========================================================
     */
    private void shareExpensePack(
            String fileName,
            String csvText,
            String receiptNamesJson) {

        if (
                csvText == null
                        ||
                csvText.trim().isEmpty()
        ) {

            Toast.makeText(
                    this,
                    "There is no expense data to export.",
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
                        "PureClean-Expenses.zip";
            }

            if (
                    !safeFileName
                            .toLowerCase()
                            .endsWith(".zip")
            ) {

                safeFileName += ".zip";
            }

            File folder =
                    new File(
                            getCacheDir(),
                            "expense_exports"
                    );

            if (!folder.exists()) {
                folder.mkdirs();
            }

            File zipFile =
                    new File(
                            folder,
                            safeFileName
                    );

            ZipOutputStream zip =
                    new ZipOutputStream(
                            new FileOutputStream(
                                    zipFile
                            )
                    );

            zip.putNextEntry(
                    new ZipEntry(
                            "expenses.csv"
                    )
            );

            zip.write(
                    csvText.getBytes(
                            "UTF-8"
                    )
            );

            zip.closeEntry();

            JSONArray receiptNames;

            try {

                receiptNames =
                        new JSONArray(
                                receiptNamesJson == null
                                        ?
                                        "[]"
                                        :
                                        receiptNamesJson
                        );

            } catch (Exception e) {

                receiptNames =
                        new JSONArray();
            }

            byte[] buffer =
                    new byte[8192];

            for (
                    int i = 0;
                    i < receiptNames.length();
                    i++
            ) {

                String name =
                        receiptNames.optString(
                                i,
                                ""
                        );

                File receipt =
                        receiptFile(name);

                if (
                        !receipt.exists()
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
                        (length = input.read(buffer))
                                >
                        0
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

            zip.finish();
            zip.close();

            Uri zipUri =
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
                    zipUri
            );

            shareIntent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Pure Clean Expenses"
            );

            shareIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    "Expense export and receipt photos attached."
            );

            shareIntent.setClipData(
                    ClipData.newRawUri(
                            "Expense Export",
                            zipUri
                    )
            );

            shareIntent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(
                    Intent.createChooser(
                            shareIntent,
                            "Share Expenses"
                    )
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not create expense accountant pack.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /*
     * =========================================================
     * PHONE / TEXT
     * =========================================================
     */

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

    /*
     * =========================================================
     * CSV
     * =========================================================
     */

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

    /*
     * =========================================================
     * JSON BACKUP
     * =========================================================
     */

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

        try {

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

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Business data could not be prepared for backup.",
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

                File savedReceipt =
                        saveCameraReceipt();

                if (savedReceipt != null) {

                    notifyExpenseReceiptSaved(
                            pendingExpenseId,
                            savedReceipt.getName()
                    );

                    Toast.makeText(
                            this,
                            "Receipt photo saved.",
                            Toast.LENGTH_SHORT
                    ).show();

                } else {

                    Toast.makeText(
                            this,
                            "The camera photo could not be saved.",
                            Toast.LENGTH_LONG
                    ).show();
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

        } else if (
                requestCode
                        ==
                SAVE_FULL_BACKUP
        ) {

            saveFullBackup(uri);

        } else if (
                requestCode
                        ==
                RESTORE_FULL_BACKUP
        ) {

            restoreFullBackup(uri);
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
                        "This backup is incomplete. Customers, invoices and expenses are all required.",
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
                    "Backup restored: "
                            +
                            customers.length()
                            +
                            " customer"
                            +
                            (
                                    customers.length() == 1
                                            ?
                                    ""
                                            :
                                    "s"
                            )
                            +
                            ", "
                            +
                            invoices.length()
                            +
                            " invoice"
                            +
                            (
                                    invoices.length() == 1
                                            ?
                                    ""
                                            :
                                    "s"
                            )
                            +
                            ", "
                            +
                            expenses.length()
                            +
                            " expense"
                            +
                            (
                                    expenses.length() == 1
                                            ?
                                    ""
                                            :
                                    "s"
                            )
                            +
                            ".",
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
                (length = input.read(buffer))
                        >
                0
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
            File folder) {

        if (
                folder == null
                        ||
                !folder.exists()
        ) {
            return;
        }

        File[] files =
                folder.listFiles();

        if (files != null) {

            for (File file : files) {

                if (file.isDirectory()) {

                    deleteFolder(
                            file
                    );

                } else {

                    file.delete();
                }
            }
        }

        folder.delete();
    }

    /*
     * =========================================================
     * EMAIL / INVOICES
     * =========================================================
     */

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

    /*
     * =========================================================
     * PDF INVOICE
     * =========================================================
     */

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
