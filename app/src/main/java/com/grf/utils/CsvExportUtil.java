package com.grf.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class CsvExportUtil {

    public static Uri saveCsvToDownloads(
            Context context,
            String fileName,
            List<String> headers,
            List<List<String>> rows
    ) {
        Uri fileUri = null;

        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            ContentResolver resolver = context.getContentResolver();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            }

            if (fileUri == null) return null;

            try (OutputStream outputStream = resolver.openOutputStream(fileUri)) {

                if (outputStream == null) return null;

                StringBuilder builder = new StringBuilder();

                // ---- Center headers ----
                try {
                    List<String> centered = new ArrayList<>();
                    for (String h : headers) {
                        centered.add(centerText(h, 20));
                    }
                    builder.append(String.join(",", centered)).append("\n");
                } catch (Exception e) {
                    builder.append(String.join(",", headers)).append("\n");
                }

                // ---- Write rows ----
                try {
                    for (List<String> r : rows) {
                        builder.append(String.join(",", r)).append("\n");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                outputStream.write(builder.toString().getBytes());
                outputStream.flush();

            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return fileUri;
    }

    private static String centerText(String text, int width) {
        try {
            int pad = (width - text.length()) / 2;
            if (pad < 0) pad = 0;
            String p = new String(new char[pad]).replace("\0", " ");
            return p + text + p;
        } catch (Exception e) {
            return text;
        }
    }
}
