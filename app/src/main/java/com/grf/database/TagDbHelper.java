package com.grf.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.grf.model.Tag;

import java.util.ArrayList;
import java.util.List;

public class TagDbHelper {
    private static final String TAG = "TagDbHelper";

    private final SqliteDbHelper sqliteDbHelper;

    // Create table SQL
    private static final String CREATE_TABLE_TAG =
            "CREATE TABLE IF NOT EXISTS " + Tag.TABLE_NAME + " (" +
                    Tag.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    Tag.COL_SL_NO + " INTEGER, " +
                    Tag.COL_TOTE_BARCODE + " TEXT, " +
                    Tag.COL_TAG_CODE + " TEXT, " +
                    Tag.COL_DATE_TIME + " TEXT, " +
                    Tag.COL_ACTIVE + " INTEGER, " +
                    Tag.COL_ZONE + " INTEGER, " +
                    Tag.COL_MODULE + " TEXT" +
                    ");";

    public TagDbHelper(Context context) {
        this.sqliteDbHelper = new SqliteDbHelper(context);

        SQLiteDatabase db = null;
        try {
            db = sqliteDbHelper.getWritableDatabase();
            db.execSQL(CREATE_TABLE_TAG);
        } catch (Exception e) {
            Log.e(TAG, "TagDbHelper: failed to create Tag table", e);
        } finally {
            try {
                if (db != null && db.isOpen()) db.close();
            } catch (Exception ignored) {
            }
        }
    }

    // Insert single Tag
    public long insertTag(Tag tag) {
        SQLiteDatabase db = null;
        long rowId = -1;
        try {
            db = sqliteDbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(Tag.COL_SL_NO, tag.getSlNo());
            cv.put(Tag.COL_TOTE_BARCODE, tag.getToteBarcode());
            cv.put(Tag.COL_TAG_CODE, tag.getTagCode());
            cv.put(Tag.COL_DATE_TIME, tag.getDateTime());
            cv.put(Tag.COL_ACTIVE, tag.getActive());
            cv.put(Tag.COL_ZONE, tag.getZone());
            cv.put(Tag.COL_MODULE, tag.getModule());

            rowId = db.insert(Tag.TABLE_NAME, null, cv);
        } catch (Exception e) {
            Log.e(TAG, "insertTag: error", e);
        } finally {
            try {
                if (db != null && db.isOpen()) db.close();
            } catch (Exception ignored) {
            }
        }
        return rowId;
    }

    public int insertTags(List<Tag> tags) {
        SQLiteDatabase db = null;
        int affected = 0;

        try {
            db = sqliteDbHelper.getWritableDatabase();
            db.beginTransaction();

            for (Tag tag : tags) {
                try {
                    if (tag.getTagCode().isEmpty())
                        continue;
                    long existingId = findExistingId(db, tag.getToteBarcode(), tag.getTagCode());

                    ContentValues cv = new ContentValues();
                    cv.put(Tag.COL_SL_NO, tag.getSlNo());
                    cv.put(Tag.COL_TOTE_BARCODE, tag.getToteBarcode());
                    cv.put(Tag.COL_TAG_CODE, tag.getTagCode());
                    cv.put(Tag.COL_DATE_TIME, tag.getDateTime());
                    cv.put(Tag.COL_ACTIVE, tag.getActive());
                    cv.put(Tag.COL_ZONE, tag.getZone());
                    cv.put(Tag.COL_MODULE, tag.getModule());

                    if (existingId == -1) {
                        // INSERT NEW
                        long row = db.insert(Tag.TABLE_NAME, null, cv);
                        if (row != -1) affected++;
                    } else {
                        // UPDATE EXISTING
                        int rows = db.update(
                                Tag.TABLE_NAME,
                                cv,
                                Tag.COL_ID + "=?",
                                new String[]{String.valueOf(existingId)}
                        );
                        affected += rows;
                    }

                } catch (Exception e) {
                    Log.e(TAG, "insertTags: error in loop", e);
                }
            }

            db.setTransactionSuccessful();

        } catch (Exception e) {
            Log.e(TAG, "insertTags: transaction error", e);
        } finally {
            try {
                if (db != null) {
                    db.endTransaction();
                    if (db.isOpen()) db.close();
                }
            } catch (Exception ignored) {
            }
        }

        return affected; // number of inserted + updated rows
    }

    private long findExistingId(SQLiteDatabase db, String tote, String tagCode) {
        long id = -1;
        Cursor cursor = null;
        try {
            cursor = db.query(
                    Tag.TABLE_NAME,
                    new String[]{Tag.COL_ID},
                    Tag.COL_TOTE_BARCODE + "=? AND " + Tag.COL_TAG_CODE + "=?",
                    new String[]{tote, tagCode},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                id = cursor.getLong(cursor.getColumnIndexOrThrow(Tag.COL_ID));
            }
        } catch (Exception e) {
            Log.e(TAG, "findExistingId error", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return id;
    }

    public String getAllTagIdAsync(String TagCodes) {
        List<String> tagCodeList = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            // Convert "TAG1,TAG2" → ["TAG1", "TAG2"]
            String[] tagArray = TagCodes.split(",");
            String placeholders = makePlaceholders(tagArray.length); // ?,?

            db = sqliteDbHelper.getReadableDatabase();

            cursor = db.query(
                    Tag.TABLE_NAME,
                    new String[]{Tag.COL_TAG_CODE},
                    Tag.COL_TOTE_BARCODE + " IN (" + placeholders + ")",
                    tagArray,
                    null,
                    null,
                    Tag.COL_ID + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String tagCode = cursor.getString(cursor.getColumnIndexOrThrow(Tag.COL_TAG_CODE));
                    if (tagCode != null && !tagCode.trim().isEmpty()) {
                        tagCodeList.add(tagCode);
                    }
                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            Log.e(TAG, "getAllTags error", e);
        } finally {
            try {
                if (cursor != null) cursor.close();
                if (db != null) db.close();
            } catch (Exception ignored) {
            }
        }

        // Return like: TAG1,TAG2,TAG3
        return TextUtils.join(",", tagCodeList);
    }

    private String makePlaceholders(int count) {
        if (count < 1) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append("?");
            if (i < count - 1) sb.append(",");
        }
        return sb.toString();
    }

    // Get all tags ordered by Id DESC
    public List<Tag> getAllTags() {
        List<Tag> list = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = sqliteDbHelper.getReadableDatabase();
            cursor = db.query(
                    Tag.TABLE_NAME,
                    null,
                    Tag.COL_TAG_CODE + " IS NOT NULL AND " + Tag.COL_TAG_CODE + " != ''",
                    null,
                    null,
                    null,
                    Tag.COL_ID + " DESC"
            );


            if (cursor != null && cursor.moveToFirst()) {
                do {
                    list.add(cursorToTag(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "getAllTags: error", e);
        } finally {
            try {
                if (cursor != null && !cursor.isClosed()) cursor.close();
                if (db != null && db.isOpen()) db.close();
            } catch (Exception ignored) {
            }
        }
        return list;
    }

    // Helper: convert cursor -> Tag
    private Tag cursorToTag(Cursor cursor) {
        Tag t = new Tag();
        try {
            t.setId(cursor.getLong(cursor.getColumnIndexOrThrow(Tag.COL_ID)));
            t.setSlNo(cursor.getInt(cursor.getColumnIndexOrThrow(Tag.COL_SL_NO)));
            t.setToteBarcode(cursor.getString(cursor.getColumnIndexOrThrow(Tag.COL_TOTE_BARCODE)));
            t.setTagCode(cursor.getString(cursor.getColumnIndexOrThrow(Tag.COL_TAG_CODE)));
            t.setDateTime(cursor.getString(cursor.getColumnIndexOrThrow(Tag.COL_DATE_TIME)));
            t.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(Tag.COL_ACTIVE)));
            t.setZone(cursor.getInt(cursor.getColumnIndexOrThrow(Tag.COL_ZONE)));
            t.setModule(cursor.getString(cursor.getColumnIndexOrThrow(Tag.COL_MODULE)));
        } catch (Exception e) {
            Log.e(TAG, "cursorToTag: error", e);
        }
        return t;
    }

    // Update wrapper similar to TaskUpdate
    public static class TagUpdate {
        public Long id; // required for WHERE usually
        public Integer slNo;
        public String toteBarcode;
        public String tagCode;
        public String dateTime;
        public Integer active;
        public Integer zone;
        public String module;

        public TagUpdate() {
        }

        public TagUpdate id(long id) {
            this.id = id;
            return this;
        }

        public TagUpdate slNo(Integer slNo) {
            this.slNo = slNo;
            return this;
        }

        public TagUpdate toteBarcode(String toteBarcode) {
            this.toteBarcode = toteBarcode;
            return this;
        }

        public TagUpdate tagCode(String tagCode) {
            this.tagCode = tagCode;
            return this;
        }

        public TagUpdate dateTime(String dateTime) {
            this.dateTime = dateTime;
            return this;
        }

        public TagUpdate active(Integer active) {
            this.active = active;
            return this;
        }

        public TagUpdate zone(Integer zone) {
            this.zone = zone;
            return this;
        }

        public TagUpdate module(String module) {
            this.module = module;
            return this;
        }
    }

    // Update tags using where clause and args (flexible)
    public int updateTag(TagUpdate update, String where, String[] args) {
        SQLiteDatabase db = null;
        int rows = 0;
        try {
            db = sqliteDbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();

            if (update.slNo != null) cv.put(Tag.COL_SL_NO, update.slNo);
            if (update.toteBarcode != null) cv.put(Tag.COL_TOTE_BARCODE, update.toteBarcode);
            if (update.tagCode != null) cv.put(Tag.COL_TAG_CODE, update.tagCode);
            if (update.dateTime != null) cv.put(Tag.COL_DATE_TIME, update.dateTime);
            if (update.active != null) cv.put(Tag.COL_ACTIVE, update.active);
            if (update.zone != null) cv.put(Tag.COL_ZONE, update.zone);
            if (update.module != null) cv.put(Tag.COL_MODULE, update.module);

            if (cv.size() == 0) return 0;

            rows = db.update(Tag.TABLE_NAME, cv, where, args);
        } catch (Exception e) {
            Log.e(TAG, "updateTag: error", e);
        } finally {
            try {
                if (db != null && db.isOpen()) db.close();
            } catch (Exception ignored) {
            }
        }
        return rows;
    }

    public void CloseDb() {
        try {
            if (sqliteDbHelper != null) {
                sqliteDbHelper.close();
            }
        } catch (Exception ignored) {
        }
    }
}
