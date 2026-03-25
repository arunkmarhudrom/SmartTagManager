package com.grf.utils;


import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * FilePickerUtils (StartActivityForResult approach)
 * - Single file only
 * - Passes EXTRA_MIME_TYPES to chooser
 * - Returns file DATA (List<String>) via callback:
 *     - Text-like files => list of lines
 *     - Binary spreadsheets => single "BASE64:<data>" string
 */
public class FilePickerUtils {
    private static final String TAG = "FilePickerUtils";

    public interface Callback {
        void onFilesPicked(@NonNull List<String> fileData);
        void onError(@NonNull String message);
    }

    private final Callback callback;
    private ActivityResultLauncher<Intent> launcher;

    // MIME types to show in picker
    private final String[] spreadsheetMimeTypes = new String[]{
            "text/csv",
            "text/comma-separated-values",
            "application/csv",
            "text/plain",
            "application/vnd.ms-excel", // .xls
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
            "application/vnd.oasis.opendocument.spreadsheet" // .ods
    };

    public FilePickerUtils(@NonNull Callback callback) {
        this.callback = callback;
    }

    /**
     * Register the launcher using StartActivityForResult. Call in Fragment.onCreate or onAttach.
     */
    public void register(@NonNull final Fragment fragment) {
        try {
            launcher = fragment.registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    (ActivityResult result) -> {
                        try {
                            if (result == null || result.getResultCode() != Activity.RESULT_OK) {
                                // user cancelled or no result
                                callback.onFilesPicked(new ArrayList<String>());
                                return;
                            }
                            Intent data = result.getData();
                            if (data == null) {
                                callback.onFilesPicked(new ArrayList<String>());
                                return;
                            }
                            Uri uri = data.getData(); // single selection
                            handleResult(fragment, uri);
                        } catch (Exception e) {
                            try {
                                callback.onError("Error processing activity result: " + e.getMessage());
                            } catch (Exception inner) {
                                Log.e(TAG, "callback.onError threw", inner);
                            }
                            Log.e(TAG, "ActivityResult handler error", e);
                        }
                    }
            );
        } catch (Exception e) {
            try {
                callback.onError("Failed to register file picker: " + e.getMessage());
            } catch (Exception inner) {
                Log.e(TAG, "callback.onError threw", inner);
            }
            Log.e(TAG, "register() error", e);
        }
    }

    /**
     * Launch the single-file picker with MIME filters.
     */
    public void launchPicker() {
        try {
            if (launcher == null) {
                callback.onError("FilePicker not registered. Call register(fragment) first.");
                return;
            }

            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*"); // generic type; use EXTRA_MIME_TYPES to filter
            intent.putExtra(Intent.EXTRA_MIME_TYPES, spreadsheetMimeTypes);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false); // force single selection

            launcher.launch(intent);
        } catch (Exception e) {
            try {
                callback.onError("Failed to launch file picker: " + e.getMessage());
            } catch (Exception inner) {
                Log.e(TAG, "callback.onError threw", inner);
            }
            Log.e(TAG, "launchPicker() error", e);
        }
    }

    /**
     * Handle the picked URI: read content and return data (lines or base64).
     */
    private void handleResult(@NonNull Fragment fragment, @Nullable Uri uri) {
        try {
            List<String> out = new ArrayList<>();
            if (uri == null) {
                callback.onFilesPicked(out);
                return;
            }

            ContentResolver cr = fragment.requireContext().getContentResolver();

            // Persist read permission if possible
            try {
                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                cr.takePersistableUriPermission(uri, takeFlags);
            } catch (SecurityException se) {
                Log.w(TAG, "couldn't take persistable permission", se);
            } catch (Exception e) {
                Log.w(TAG, "takePersistableUriPermission unexpected", e);
            }

            String mime = cr.getType(uri);
            if (mime == null) mime = "";

            // If text-like -> return list of lines
            if (mime.startsWith("text") || mime.equals("application/csv") || mime.contains("comma-separated")) {
                BufferedReader reader = null;
                try {
                    InputStream is = cr.openInputStream(uri);
                    if (is == null) {
                        callback.onError("Unable to open selected file.");
                        return;
                    }
                    reader = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        out.add(line);
                    }
                    callback.onFilesPicked(out);
                    return;
                } catch (Exception e) {
                    Log.e(TAG, "error reading text file", e);
                    callback.onError("Error reading text file: " + e.getMessage());
                    return;
                } finally {
                    try { if (reader != null) reader.close(); } catch (Exception ignored) {}
                }
            }

            // Binary spreadsheet -> return BASE64
            boolean isBinarySpreadsheet = mime.contains("spreadsheet") ||
                    mime.contains("officedocument") ||
                    mime.equals("application/vnd.ms-excel") ||
                    uri.toString().toLowerCase().endsWith(".xls") ||
                    uri.toString().toLowerCase().endsWith(".xlsx") ||
                    uri.toString().toLowerCase().endsWith(".ods");

            if (isBinarySpreadsheet) {
                InputStream is = null;
                ByteArrayOutputStream baos = null;
                try {
                    is = cr.openInputStream(uri);
                    if (is == null) {
                        callback.onError("Unable to open selected file.");
                        return;
                    }
                    baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        baos.write(buffer, 0, read);
                    }
                    String base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
                    out.add("BASE64:" + base64);
                    callback.onFilesPicked(out);
                    return;
                } catch (Exception e) {
                    Log.e(TAG, "error reading binary file", e);
                    callback.onError("Error reading binary file: " + e.getMessage());
                    return;
                } finally {
                    try { if (is != null) is.close(); } catch (Exception ignored) {}
                    try { if (baos != null) baos.close(); } catch (Exception ignored) {}
                }
            }

            // Fallback: read as text
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(cr.openInputStream(uri)));
                String line;
                while ((line = reader.readLine()) != null) out.add(line);
                try { reader.close(); } catch (Exception ignored) {}
                callback.onFilesPicked(out);
            } catch (Exception e) {
                Log.e(TAG, "fallback read failed", e);
                callback.onError("Unsupported file type or read error: " + e.getMessage());
            }
        } catch (Exception e) {
            try {
                callback.onError("Error handling picked file: " + e.getMessage());
            } catch (Exception inner) {
                Log.e(TAG, "callback.onError threw", inner);
            }
            Log.e(TAG, "handleResult() error", e);
        }
    }

    /**
     * Optional helper to get display name.
     */
    @Nullable
    public static String getDisplayName(@NonNull Context ctx, @NonNull Uri uri) {
        try {
            String name = null;
            android.database.Cursor cursor = null;
            try {
                cursor = ctx.getContentResolver().query(uri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx != -1) name = cursor.getString(idx);
                }
            } finally {
                if (cursor != null) cursor.close();
            }
            return name;
        } catch (Exception e) {
            Log.w(TAG, "getDisplayName error", e);
            return null;
        }
    }
}
