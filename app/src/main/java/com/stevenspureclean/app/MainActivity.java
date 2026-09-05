package com.stevenspureclean.app;

import android.app.Activity;
import android.app.AlertDialog;
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

    private void saveFullBackup(
            Uri uri) {

        try {

            OutputStream rawOutput =
                    getContentResolver()
                            .openOutputStream(
                                    uri
                            );

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
                        "restore_receipts"
                );

        deleteFolder(
                restoreFolder
        );

        restoreFolder.mkdirs();

        String restoredJson = null;

        try {

            InputStream rawInput =
                    getContentResolver()
                            .openInputStream(
                                    uri
                            );

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
                    (entry = zip.getNextEntry()) != null
            ) {

                String name =
                        entry.getName();

                if (
                        "business.json".equals(
                                name
                        )
                ) {

                    StringBuilder jsonText =
                            new StringBuilder();

                    int length;

                    while (
                            (length = zip.read(buffer)) > 0
                    ) {

                        jsonText.append(
                                new String(
                                        buffer,
                                        0,
                                        length,
                                        "UTF-8"
                                )
                        );
                    }

                    restoredJson =
                            jsonText.toString();

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
                            new File(
                                    name
                            ).getName();

                    File destination =
                            new File(
                                    restoreFolder,
                                    safeName
                            );

                    FileOutputStream output =
                            new FileOutputStream(
                                    destination
                            );

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

                zip.closeEntry();
            }

            zip.close();

            if (
                    restoredJson == null
                            ||
                    restoredJson.trim().isEmpty()
            ) {

                Toast.makeText(
                        this,
                        "This backup does not contain business data.",
                        Toast.LENGTH_LONG
                ).show();

                deleteFolder(
                        restoreFolder
                );

                return;
            }

            File receiptFolder =
                    receiptFolder();

            File[] restoredReceipts =
                    restoreFolder.listFiles();

            if (restoredReceipts != null) {

                for (File restored : restoredReceipts) {

                    if (
                            restored != null
                                    &&
                            restored.isFile()
                    ) {

                        copyFile(
                                restored,
                                new File(
                                        receiptFolder,
                                        restored.getName()
                                )
                        );
                    }
                }
            }

            final String finalRestoredJson =
                    restoredJson;

            webView.evaluateJavascript(
                    "restoreBusinessBackup("
                            +
                            JSONObject.quote(
                                    finalRestoredJson
                            )
                            +
                            ");",
                    null
            );

            deleteFolder(
                    restoreFolder
            );

            Toast.makeText(
                    this,
                    "Full backup restored.",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            deleteFolder(
                    restoreFolder
            );

            Toast.makeText(
                    this,
                    "Full backup could not be restored.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /*
     * =========================================================
     * NORMAL BACKUP
     * =========================================================
     */

    private void startBackup(
            String json) {

        if (
                json == null
                        ||
                json.trim().isEmpty()
        ) {
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

        intent.setType(
                "*/*"
        );

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

                if (
                        savedReceipt != null
                ) {

                    notifyExpenseReceiptSaved(
                            pendingExpenseId,
                            savedReceipt.getName()
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
                requestCode == SAVE_BACKUP
        ) {

            saveBackup(
                    uri
            );

        } else if (
                requestCode == RESTORE_BACKUP
        ) {

            loadBackup(
                    uri
            );

        } else if (
                requestCode == SAVE_FULL_BACKUP
        ) {

            saveFullBackup(
                    uri
            );

        } else if (
                requestCode == RESTORE_FULL_BACKUP
        ) {

            restoreFullBackup(
                    uri
            );
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
                return;
            }

            output.write(
                    backupJson.getBytes(
                            "UTF-8"
                    )
            );

            output.close();

            backupJson = "";

        } catch (Exception ignored) {
        }
    }

    private void loadBackup(
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

            StringBuilder text =
                    new StringBuilder();

            String line;

            while (
                    (line = reader.readLine()) != null
            ) {

                text.append(
                        line
                );
            }

            reader.close();

            String json =
                    text.toString();

            String javascript =
                    "restoreBusinessBackup("
                            +
                            JSONObject.quote(
                                    json
                            )
                            +
                            ");";

            webView.evaluateJavascript(
                    javascript,
                    null
            );

        } catch (Exception ignored) {
        }
    }

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
     * EMAIL / PDF
     * =========================================================
     */

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

        if (!validEmail(email)) {

            Toast.makeText(
                    this,
                    "The quote email address is not valid.",
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

            String cleanAmount =
                    amount == null
                            ?
                            "0.00"
                            :
                            amount
                                    .replace(
                                            "£",
                                            ""
                                    )
                                    .trim();

            intent.putExtra(
                    Intent.EXTRA_TEXT,
                    "Hi "
                            +
                            customerName
                            +
                            ",\n\nPlease find attached Quote Q"
                            +
                            quoteNumber
                            +
                            " from Steven's Pure Clean Exteriors."
                            +
                            "\n\nQuote total: £"
                            +
                            cleanAmount
                            +
                            "\n\nIf you would like to go ahead, please get in touch."
                            +
                            "\n\nMany thanks,\nSteven"
                            +
                            "\nSteven's Pure Clean Exteriors"
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

            try {

                intent.setPackage(
                        "com.google.android.gm"
                );

                startActivity(
                        intent
                );

            } catch (Exception e) {

                intent.setPackage(
                        null
                );

                startActivity(
                        Intent.createChooser(
                                intent,
                                "Email Quote"
                        )
                );
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not create quote email.",
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
            String notes)
            throws Exception {

        File folder =
                new File(
                        getCacheDir(),
                        "quotes"
                );

        if (!folder.exists()) {
            folder.mkdirs();
        }

        /*
         * Remove previous temporary quote PDFs.
         * This stops Samsung/Gmail/PDF viewers displaying an old
         * attachment when a quote number has been reused or edited.
         */
        File[] oldQuoteFiles =
                folder.listFiles();

        if (oldQuoteFiles != null) {

            for (File oldFile : oldQuoteFiles) {

                if (
                        oldFile != null
                                &&
                        oldFile.isFile()
                ) {

                    oldFile.delete();
                }
            }
        }

        /*
         * IMPORTANT FIX:
         * Each generated quote now has a unique physical filename.
         */
        File file =
                new File(
                        folder,
                        "PureClean-Quote-Q"
                                +
                                quoteNumber
                                +
                                "-"
                                +
                                System.currentTimeMillis()
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

        int black =
                Color.rgb(
                        12,
                        12,
                        12
                );

        int lime =
                Color.rgb(
                        155,
                        214,
                        0
                );

        int dark =
                Color.rgb(
                        28,
                        28,
                        28
                );

        int grey =
                Color.rgb(
                        100,
                        100,
                        100
                );

        int light =
                Color.rgb(
                        245,
                        245,
                        245
                );

        int border =
                Color.rgb(
                        220,
                        220,
                        220
                );

        canvas.drawColor(
                Color.WHITE
        );

        paint.setColor(
                black
        );

        canvas.drawRect(
                0,
                0,
                595,
                155,
                paint
        );

        paint.setColor(
                lime
        );

        canvas.drawRect(
                0,
                151,
                595,
                155,
                paint
        );

        try {

            InputStream input =
                    getAssets()
                            .open(
                                    "logo.jpg"
                            );

            Bitmap logo =
                    BitmapFactory.decodeStream(
                            input
                    );

            if (logo != null) {

                RectF logoRect =
                        new RectF(
                                28,
                                22,
                                123,
                                117
                        );

                canvas.drawBitmap(
                        logo,
                        null,
                        logoRect,
                        paint
                );

                logo.recycle();
            }

            input.close();

        } catch (Exception ignored) {
        }

        paint.setFakeBoldText(
                true
        );

        paint.setColor(
                lime
        );

        paint.setTextSize(
                22
        );

        canvas.drawText(
                "Steven's Pure Clean Exteriors",
                145,
                55,
                paint
        );

        paint.setFakeBoldText(
                false
        );

        paint.setColor(
                Color.WHITE
        );

        paint.setTextSize(
                12
        );

        canvas.drawText(
                "Pure Results • Clean Exteriors",
                145,
                80,
                paint
        );

        paint.setFakeBoldText(
                true
        );

        paint.setTextSize(
                31
        );

        paint.setTextAlign(
                Paint.Align.RIGHT
        );

        canvas.drawText(
                "QUOTE",
                558,
                113,
                paint
        );

        paint.setTextAlign(
                Paint.Align.LEFT
        );

        float infoTop =
                185;

        paint.setColor(
                dark
        );

        paint.setFakeBoldText(
                true
        );

        paint.setTextSize(
                11
        );

        canvas.drawText(
                "QUOTE NUMBER",
                45,
                infoTop,
                paint
        );

        canvas.drawText(
                "QUOTE DATE",
                225,
                infoTop,
                paint
        );

        canvas.drawText(
                "VALID FOR",
                410,
                infoTop,
                paint
        );

        paint.setFakeBoldText(
                false
        );

        paint.setColor(
                grey
        );

        paint.setTextSize(
                13
        );

        canvas.drawText(
                "Q"
                        +
                        quoteNumber,
                45,
                infoTop + 25,
                paint
        );

        canvas.drawText(
                quoteDate == null
                        ?
                        ""
                        :
                        quoteDate,
                225,
                infoTop + 25,
                paint
        );

        canvas.drawText(
                "30 days",
                410,
                infoTop + 25,
                paint
        );

        paint.setColor(
                border
        );

        canvas.drawRect(
                35,
                232,
                560,
                234,
                paint
        );

        paint.setColor(
                dark
        );

        paint.setFakeBoldText(
                true
        );

        paint.setTextSize(
                15
        );

        canvas.drawText(
                "QUOTE FOR",
                45,
                270,
                paint
        );

        paint.setFakeBoldText(
                false
        );

        paint.setTextSize(
                16
        );

        canvas.drawText(
                customerName == null
                        ?
                        ""
                        :
                        customerName,
                45,
                298,
                paint
        );

        paint.setColor(
                grey
        );

        paint.setTextSize(
                12
        );

        if (
                customerAddress != null
                        &&
                !customerAddress.trim().isEmpty()
        ) {

            canvas.drawText(
                    customerAddress.trim(),
                    45,
                    320,
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
                    45,
                    340,
                    paint
            );
        }

        paint.setColor(
                light
        );

        RectF serviceBox =
                new RectF(
                        35,
                        370,
                        560,
                        565
                );

        canvas.drawRoundRect(
                serviceBox,
                14,
                14,
                paint
        );

        paint.setColor(
                dark
        );

        paint.setFakeBoldText(
                true
        );

        paint.setTextSize(
                13
        );

        canvas.drawText(
                "SERVICE",
                55,
                400,
                paint
        );

        paint.setTextAlign(
                Paint.Align.RIGHT
        );

        canvas.drawText(
                "PRICE",
                535,
                400,
                paint
        );

        paint.setTextAlign(
                Paint.Align.LEFT
        );

        paint.setFakeBoldText(
                false
        );

        String cleanDescription =
                description == null
                        ?
                        ""
                        :
                        description.trim();

        String[] lines =
                cleanDescription.isEmpty()
                        ?
                        new String[]{
                                "Exterior cleaning service"
                        }
                        :
                        cleanDescription.split(
                                "\\n"
                        );

        float lineY =
                432;

        for (String line : lines) {

            if (
                    line == null
                            ||
                    line.trim().isEmpty()
            ) {
                continue;
            }

            String cleanLine =
                    line.trim();

            String service =
                    cleanLine;

            String price =
                    "";

            /*
             * Accepts BOTH:
             * Window Cleaning - £25.00
             * and
             * Window Cleaning - 25.00
             */
            int split =
                    cleanLine.lastIndexOf(
                            " - "
                    );

            if (split >= 0) {

                service =
                        cleanLine.substring(
                                0,
                                split
                        ).trim();

                price =
                        cleanLine.substring(
                                split + 3
                        ).trim();
            }

            /*
             * Remove any existing pound signs.
             * PDF adds exactly one below.
             */
            price =
                    price.replace(
                            "£",
                            ""
                    ).trim();

            paint.setColor(
                    dark
            );

            paint.setTextSize(
                    13
            );

            canvas.drawText(
                    service,
                    55,
                    lineY,
                    paint
            );

            if (!price.isEmpty()) {

                paint.setTextAlign(
                        Paint.Align.RIGHT
                );

                canvas.drawText(
                        "£"
                                +
                                price,
                        535,
                        lineY,
                        paint
                );

                paint.setTextAlign(
                        Paint.Align.LEFT
                );
            }

            lineY +=
                    27;

            if (lineY > 530) {
                break;
            }
        }

        String cleanAmount =
                amount == null
                        ?
                        "0.00"
                        :
                        amount.replace(
                                "£",
                                ""
                        ).trim();

        paint.setColor(
                black
        );

        RectF amountBox =
                new RectF(
                        315,
                        590,
                        560,
                        680
                );

        canvas.drawRoundRect(
                amountBox,
                14,
                14,
                paint
        );

        paint.setColor(
                Color.WHITE
        );

        paint.setFakeBoldText(
                true
        );

        paint.setTextSize(
                12
        );

        canvas.drawText(
                "QUOTE TOTAL",
                338,
                620,
                paint
        );

        paint.setColor(
                lime
        );

        paint.setTextSize(
                31
        );

        canvas.drawText(
                "£"
                        +
                        cleanAmount,
                338,
                660,
                paint
        );

        paint.setColor(
                dark
        );

        paint.setFakeBoldText(
                true
        );

        paint.setTextSize(
                14
        );

        canvas.drawText(
                "NOTES",
                45,
                710,
                paint
        );

        paint.setFakeBoldText(
                false
        );

        paint.setColor(
                grey
        );

        paint.setTextSize(
                11
        );

        String cleanNotes =
                notes == null
                        ?
                        ""
                        :
                        notes.trim();

        if (cleanNotes.isEmpty()) {

            cleanNotes =
                    "Please get in touch if you would like to proceed with this quotation.";
        }

        if (
                cleanNotes.length() > 95
        ) {

            cleanNotes =
                    cleanNotes.substring(
                            0,
                            95
                    )
                            +
                            "...";
        }

        canvas.drawText(
                cleanNotes,
                45,
                735,
                paint
        );

        paint.setColor(
                border
        );

        canvas.drawRect(
                35,
                792,
                560,
                794,
                paint
        );

        paint.setColor(
                grey
        );

        paint.setTextSize(
                11
        );

        paint.setTextAlign(
                Paint.Align.LEFT
        );

        canvas.drawText(
                "Thank you for the opportunity to provide this quotation.",
                35,
                818,
                paint
        );

        paint.setTextAlign(
                Paint.Align.RIGHT
        );

        paint.setFakeBoldText(
                true
        );

        canvas.drawText(
                "Steven's Pure Clean Exteriors",
                560,
                818,
                paint
        );

        paint.setTextAlign(
                Paint.Align.LEFT
        );

        document.finishPage(
                page
        );

        FileOutputStream output =
                new FileOutputStream(
                        file
                );

        document.writeTo(
                output
        );

        output.close();

        document.close();

        return file;
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

        if (!validEmail(email)) {

            Toast.makeText(
                    this,
                    "The invoice email address is not valid.",
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

            String cleanAmount =
                    amount == null
                            ?
                            "0.00"
                            :
                            amount
                                    .replace(
                                            "£",
                                            ""
                                    )
                                    .trim();

            intent.putExtra(
                    Intent.EXTRA_TEXT,
                    "Hi "
                            +
                            customerName
                            +
                            ",\n\nPlease find attached invoice #"
                            +
                            invoiceNumber
                            +
                            " from Steven's Pure Clean Exteriors."
                            +
                            "\n\nAmount due: £"
                            +
                            cleanAmount
                            +
                            "\nDue date: "
                            +
                            dueDate
                            +
                            "\n\nPlease make payment within 7 days."
                            +
                            "\n\nMany thanks,\nSteven"
                            +
                            "\nSteven's Pure Clean Exteriors"
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

                startActivity(
                        intent
                );

            } catch (Exception e) {

                intent.setPackage(
                        null
                );

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
                        ".\n\nAmount due: £"
                        +
                        amount
                                .replace(
                                        "£",
                                        ""
                                )
                        +
                        "\nDue date: "
                        +
                        dueDate
                        +
                        "\n\nThank you,\n"
                        +
                        "Steven's Pure Clean Exteriors";

        try {

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
                                            Uri.encode(
                                                    subject
                                            )
                                            +
                                            "&body="
                                            +
                                            Uri.encode(
                                                    message
                                            )
                            )
                    );

            startActivity(
                    intent
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not open email.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /*
     * =========================================================
     * INVOICE PDF
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
                                "-"
                                +
                                System.currentTimeMillis()
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

        int black =
                Color.rgb(
                        12,
                        12,
                        12
                );

        int lime =
                Color.rgb(
                        155,
                        214,
                        0
                );

        int dark =
                Color.rgb(
                        28,
                        28,
                        28
                );

        int grey =
                Color.rgb(
                        100,
                        100,
                        100
                );

        int light =
                Color.rgb(
                        245,
                        245,
                        245
                );

        int border =
                Color.rgb(
                        220,
                        220,
                        220
                );

        canvas.drawColor(
                Color.WHITE
        );

        paint.setColor(
                black
        );

        canvas.drawRect(
                0,
                0,
                595,
                155,
                paint
        );

        paint.setColor(
                lime
        );

        canvas.drawRect(
                0,
                151,
                595,
                155,
                paint
        );

        try {

            InputStream input =
                    getAssets()
                            .open(
                                    "logo.jpg"
                            );

            Bitmap logo =
                    BitmapFactory.decodeStream(
                            input
                    );

            if (logo != null) {

                RectF logoRect =
                        new RectF(
                                28,
                                22,
                                123,
                                117
                        );

                canvas.drawBitmap(
                        logo,
                        null,
                        logoRect,
                        paint
                );

                logo.recycle();
            }

            input.close();

        } catch (Exception ignored) {
        }

        paint.setFakeBoldText(
                true
        );

        paint.setColor(
                lime
        );

        paint.setTextSize(
                22
        );

        canvas.drawText(
                "Steven's Pure Clean Exteriors",
                145,
                55,
                paint
        );

        paint.setFakeBoldText(
                false
        );

        paint.setColor(
                Color.WHITE
        );

        paint.setTextSize(
                12
        );

        canvas.drawText(
                "Pure Results • Clean Exteriors",
                145,
                80,
                paint
        );

        paint.setFakeBoldText(
                true
        );

        paint.setTextSize(
                30
        );

        paint.setTextAlign(
                Paint.Align.RIGHT
        );

        canvas.drawText(
                "INVOICE",
                558,
                113,
                paint
        );

        paint.setTextAlign(
                Paint.Align.LEFT
        );

        paint.setColor(
                dark
        );

        paint.setTextSize(
                11
        );

        canvas.drawText(
                "INVOICE NUMBER",
                45,
                185,
                paint
        );

        canvas.drawText(
                "INVOICE DATE",
                220,
                185,
                paint
        );

        canvas.drawText(
                "DUE DATE",
                400,
                185,
                paint
        );

        paint.setFakeBoldText(
                false
        );

        paint.setColor(
                grey
        );

        paint.setTextSize(
                13
        );

        canvas.drawText(
                invoiceNumber == null
                        ?
                        ""
                        :
                        invoiceNumber,
                45,
                210,
                paint
        );

        canvas.drawText(
                invoiceDate == null
                        ?
                        ""
                        :
                        invoiceDate,
                220,
                210,
                paint
        );

        canvas.drawText(
                dueDate == null
                        ?
                        ""
                        :
                        dueDate,
                400,
                210,
                paint
        );

        paint.setColor(
                border
        );

        canvas.drawRect(
                35,
                232,
                560,
                234,
                paint
        );

        paint.setColor(
                dark
        );

        paint.setFakeBoldText(
                true
        );

        paint.setTextSize(
                15
        );

        canvas.drawText(
                "INVOICE TO",
                45,
                270,
                paint
        );

        paint.setFakeBoldText(
                false
        );

        paint.setTextSize(
                16
        );

        canvas.drawText(
                customerName == null
                        ?
                        ""
                        :
                        customerName,
                45,
                298,
                paint
        );

        paint.setColor(
                grey
        );

        paint.setTextSize(
                12
        );

        if (
                customerAddress != null
                        &&
                !customerAddress.trim().isEmpty()
        ) {

            canvas.drawText(
                    customerAddress.trim(),
                    45,
                    320,
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
                    45,
                    340,
                    paint
            );
        }

        paint.setColor(
                light
        );

        RectF serviceBox =
                new RectF(
                        35,
                        370,
                        560,
                        525
                );

        canvas.drawRoundRect(
                serviceBox,
                14,
                14,
                paint
        );

        paint.setColor(
                dark
        );

        paint.setFakeBoldText(
                true
        );

        paint.setTextSize(
                13
        );

        canvas.drawText(
                "DESCRIPTION",
                55,
                405,
                paint
        );

        paint.setFakeBoldText(
                false
        );

        String cleanDescription =
                description == null
                        ||
                description.trim().isEmpty()
                        ?
                        "Exterior cleaning service"
                        :
                        description.trim();

        if (
                cleanDescription.length() > 90
        ) {

            cleanDescription =
                    cleanDescription.substring(
                            0,
                            90
                    )
                            +
                            "...";
        }

        paint.setTextSize(
                13
        );

        canvas.drawText(
                cleanDescription,
                55,
                445,
                paint
        );

        String cleanAmount =
                amount == null
                        ?
                        "0.00"
                        :
                        amount.replace(
                                "£",
                                ""
                        ).trim();

        paint.setColor(
                black
        );

        RectF amountBox =
                new RectF(
                        315,
                        565,
                        560,
                        655
                );

        canvas.drawRoundRect(
                amountBox,
                14,
                14,
                paint
        );

        paint.setColor(
                Color.WHITE
        );

        paint.setFakeBoldText(
                true
        );

        paint.setTextSize(
                12
        );

        canvas.drawText(
                "AMOUNT DUE",
                338,
                595,
                paint
        );

        paint.setColor(
                lime
        );

        paint.setTextSize(
                31
        );

        canvas.drawText(
                "£"
                        +
                        cleanAmount,
                338,
                635,
                paint
        );

        paint.setColor(
                dark
        );

        paint.setFakeBoldText(
                true
        );

        paint.setTextSize(
                14
        );

        canvas.drawText(
                "PAYMENT TERMS",
                45,
                705,
                paint
        );

        paint.setFakeBoldText(
                false
        );

        paint.setColor(
                grey
        );

        paint.setTextSize(
                12
        );

        canvas.drawText(
                "Please make payment within 7 days.",
                45,
                732,
                paint
        );

        paint.setColor(
                border
        );

        canvas.drawRect(
                35,
                792,
                560,
                794,
                paint
        );

        paint.setColor(
                grey
        );

        paint.setTextSize(
                11
        );

        paint.setTextAlign(
                Paint.Align.LEFT
        );

        canvas.drawText(
                "Thank you for your business.",
                35,
                818,
                paint
        );

        paint.setTextAlign(
                Paint.Align.RIGHT
        );

        paint.setFakeBoldText(
                true
        );

        canvas.drawText(
                "Steven's Pure Clean Exteriors",
                560,
                818,
                paint
        );

        paint.setTextAlign(
                Paint.Align.LEFT
        );

        document.finishPage(
                page
        );

        FileOutputStream output =
                new FileOutputStream(
                        file
                );

        document.writeTo(
                output
        );

        output.close();

        document.close();

        return file;
    }
                    }
