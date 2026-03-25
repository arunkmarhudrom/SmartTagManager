package com.grf.utils;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * JsonToCsvExporter
 *
 * - Convert JSONArray (or List<JSONObject>) to CSV text
 * - Proper quoting/escaping for CSV consumers (Excel-friendly)
 * - Optional title row and BOM for Excel
 * - Write to Downloads (MediaStore on Android Q+) or app-files fallback
 * - Share helper (Intent)
 *
 * Usage examples at bottom of file.
 */
public class JsonToCsvExporter {

    private static final String TAG = "JsonToCsvExporter";

    // UTF-8 BOM — helps Excel recognize UTF-8
    private static final byte[] UTF8_BOM = {(byte)0xEF, (byte)0xBB, (byte)0xBF};

    /**
     * Export a JSONArray to a CSV file in Downloads (or app files fallback).
     *
     * @param ctx         Context (app context preferred)
     * @param jsonArray   JSONArray to export
     * @param fileName    desired file name, e.g. "my_export.csv"
     * @param columnOrder if non-null, will be used as headers/column order. If null, columns are auto-derived.
     * @param titleRow    optional title row placed above headers (pass null to skip)
     * @param includeBom  whether to include UTF-8 BOM for Excel
     * @return Uri of saved file on success, null on failure
     */
    @Nullable
    public static Uri exportJsonArrayToCsv(@NonNull Context ctx,
                                           @NonNull JSONArray jsonArray,
                                           @NonNull String fileName,
                                           @Nullable List<String> columnOrder,
                                           @Nullable String titleRow,
                                           boolean includeBom) {
        try {
            // 1) Derive columns if not provided
            List<String> columns = columnOrder != null ? new ArrayList<>(columnOrder) : deriveColumns(jsonArray);

            // 2) Build CSV content
            String csvText = buildCsvText(jsonArray, columns, titleRow);

            // 3) Write to file (Downloads preferred)
            Uri saved = writeCsvToDownloads(ctx, fileName, csvText, includeBom);
            return saved;
        } catch (Exception e) {
            Log.e(TAG, "Failed to export JSON to CSV", e);
            return null;
        }
    }

    /**
     * Build CSV string from JSONArray and explicit columns.
     * Adds a blank line between optional title and headers.
     */
    @NonNull
    private static String buildCsvText(@NonNull JSONArray jsonArray,
                                       @NonNull List<String> columns,
                                       @Nullable String titleRow) {
        StringBuilder sb = new StringBuilder();

        try {
            // Optional title row (as first row, single cell)
            if (titleRow != null && !titleRow.trim().isEmpty()) {
                sb.append(escapeCsvCell(titleRow)).append("\n\n"); // title row + blank line
            }

            // Header row (human-friendly: Title Case)
            for (int i = 0; i < columns.size(); i++) {
                String header = beautifyHeader(columns.get(i));
                sb.append(escapeCsvCell(header));
                if (i < columns.size() - 1) sb.append(",");
            }
            sb.append("\n");

            // Data rows
            for (int r = 0; r < jsonArray.length(); r++) {
                JSONObject obj = jsonArray.optJSONObject(r);
                if (obj == null) {
                    // if item isn't an object, write its toString
                    sb.append(escapeCsvCell(jsonArray.opt(r))).append("\n");
                    continue;
                }
                for (int c = 0; c < columns.size(); c++) {
                    String key = columns.get(c);
                    Object val = extractValueForKey(obj, key);
                    sb.append(escapeCsvCell(val));
                    if (c < columns.size() - 1) sb.append(",");
                }
                sb.append("\n");
            }
        } catch (Exception e) {
            // Already caught above in caller; keep method defensive.
            Log.e(TAG, "Error building CSV text", e);
        }

        return sb.toString();
    }

    /**
     * Escape a value for CSV:
     * - wrap in double-quotes if it contains comma, newline or quote
     * - double internal quotes
     * - null -> empty
     */
    @NonNull
    private static String escapeCsvCell(@Nullable Object raw) {
        try {
            if (raw == null || raw == JSONObject.NULL) return "";
            String s = String.valueOf(raw);
            boolean mustQuote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
            if (s.contains("\"")) {
                s = s.replace("\"", "\"\"");
            }
            if (mustQuote) {
                return "\"" + s + "\"";
            } else {
                return s;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error escaping CSV cell", e);
            return "";
        }
    }

    /**
     * Derive union of keys across all JSON objects (maintain insertion order of first occurrence).
     */
    @NonNull
    private static List<String> deriveColumns(@NonNull JSONArray arr) {
        try {
            Set<String> keys = new LinkedHashSet<>();
            for (int i = 0; i < arr.length(); i++) {
                try {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) continue;
                    Iterator<String> it = o.keys();
                    while (it.hasNext()) {
                        keys.add(it.next());
                    }
                } catch (Exception ignore) {}
            }
            return new ArrayList<>(keys);
        } catch (Exception e) {
            Log.e(TAG, "Error deriving columns", e);
            return Collections.emptyList();
        }
    }

    /**
     * Flatten extraction: if value is JSONObject or JSONArray, stringify it compactly.
     * Supports dot-notation access for nested keys (if user provided column like "address.city").
     */
    @Nullable
    private static Object extractValueForKey(@NonNull JSONObject obj, @NonNull String key) {
        try {
            if (key.contains(".")) {
                String[] parts = key.split("\\.");
                JSONObject cur = obj;
                for (int i = 0; i < parts.length - 1; i++) {
                    String p = parts[i];
                    if (!cur.has(p) || cur.isNull(p)) return null;
                    Object nested = cur.opt(p);
                    if (!(nested instanceof JSONObject)) return null;
                    cur = (JSONObject) nested;
                }
                String last = parts[parts.length - 1];
                return cur.opt(last);
            } else {
                Object v = obj.opt(key);
                if (v instanceof JSONObject || v instanceof JSONArray) {
                    // stringify nested structures compactly
                    return v.toString();
                } else {
                    return v;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting value for key: " + key, e);
            return null;
        }
    }

    /**
     * Convert column key into a prettier header label, e.g. "first_name" -> "First Name"
     */
    @NonNull
    private static String beautifyHeader(@NonNull String rawKey) {
        try {
            String s = rawKey.replace("_", " ").replace(".", " - ");
            String[] parts = s.split(" ");
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                String p = parts[i];
                if (p.length() == 0) continue;
                out.append(Character.toUpperCase(p.charAt(0)));
                if (p.length() > 1) out.append(p.substring(1));
                if (i < parts.length - 1) out.append(" ");
            }
            return out.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error beautifying header: " + rawKey, e);
            return rawKey;
        }
    }

    /**
     * Write CSV string to Downloads (MediaStore for API >= 29). Returns file Uri or null.
     */
    @Nullable
    private static Uri writeCsvToDownloads(@NonNull Context ctx,
                                           @NonNull String fileName,
                                           @NonNull String csvText,
                                           boolean includeBom) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore -> Downloads
                ContentResolver resolver = ctx.getContentResolver();
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                Uri itemUri = resolver.insert(collection, values);
                if (itemUri == null) {
                    Log.e(TAG, "Failed to create MediaStore entry for " + fileName);
                    return null;
                }

                try (OutputStream out = resolver.openOutputStream(itemUri)) {
                    if (out == null) return null;
                    try (BufferedOutputStream bos = new BufferedOutputStream(out)) {
                        if (includeBom) bos.write(UTF8_BOM);
                        bos.write(csvText.getBytes(StandardCharsets.UTF_8));
                        bos.flush();
                    }
                }

                // mark not pending
                values.clear();
                values.put(MediaStore.Downloads.IS_PENDING, 0);
                resolver.update(itemUri, values, null, null);
                return itemUri;
            } else {
                // Pre-Q: Write to public Downloads directory (requires WRITE_EXTERNAL_STORAGE on <Android Q)
                File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloads.exists()) downloads.mkdirs();
                File outFile = new File(downloads, fileName);
                try (FileOutputStream fos = new FileOutputStream(outFile);
                     BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                    if (includeBom) bos.write(UTF8_BOM);
                    bos.write(csvText.getBytes(StandardCharsets.UTF_8));
                    bos.flush();
                }
                return Uri.fromFile(outFile);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error writing CSV to downloads", e);
            return null;
        }
    }

    /**
     * Share a CSV file Uri with other apps using ACTION_SEND.
     * Make sure file Uri has proper permissions (content:// is preferred).
     */
    public static void shareCsv(@NonNull Context ctx, @NonNull Uri csvUri, @NonNull String chooserTitle) {
        try {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/csv");
            share.putExtra(Intent.EXTRA_STREAM, csvUri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooser = Intent.createChooser(share, chooserTitle);
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(chooser);
        } catch (Exception e) {
            Log.e(TAG, "Error sharing CSV", e);
        }
    }

    /* ---------------------------
       Small convenience examples
       --------------------------- */

    /**
     * Example: convert a List<JSONObject> to JSONArray.
     */
    @NonNull
    public static JSONArray listToJsonArray(@NonNull List<JSONObject> list) {
        JSONArray arr = new JSONArray();
        try {
            for (JSONObject o : list) {
                arr.put(o);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error building JSONArray", e);
        }
        return arr;
    }
}
