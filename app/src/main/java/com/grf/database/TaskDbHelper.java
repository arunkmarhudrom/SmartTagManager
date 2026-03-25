package com.grf.database;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


import com.google.gson.GsonBuilder;
import com.grf.model.Task;
import com.grf.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;

public class TaskDbHelper {
    private static final String TAG = "TaskDbHelper";

    private final SqliteDbHelper sqliteDbHelper;

    // Create table SQL for TaskList
    private static final String CREATE_TABLE_TASK =
            "CREATE TABLE IF NOT EXISTS " + Task.TABLE_NAME + " (" +
                    Task.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    Task.COL_TITLE + " TEXT, " +
                    Task.COL_TAG_COUNT + " INTEGER, " +
                    Task.COL_TAG_FOUND + " INTEGER, " +
                    Task.COL_TAG_COMPLETE + " INTEGER, " +
                    Task.COL_TAG_ID + " TEXT, " +
                    Task.COL_DATE_TIME + " TEXT, " +
                    Task.COL_BOX_ID + " INTEGER, " +
                    Task.COL_RSS_VALUE + " REAL, " +
                    Task.COL_ZONE_ID + " TEXT, " +
                    Task.COL_MODULE_ID + " TEXT" +
                    ");";




    public TaskDbHelper(Context context) {
        this.sqliteDbHelper = new SqliteDbHelper(context);

        // ensure Task table exists (executes CREATE TABLE IF NOT EXISTS)
        SQLiteDatabase db = null;
        try {
            db = sqliteDbHelper.getWritableDatabase();
            db.execSQL(CREATE_TABLE_TASK);
        } catch (Exception e) {
            Log.e(TAG, "TaskDbHelper: failed to create Task table", e);
        } finally {
            if (db != null && db.isOpen()) db.close();
        }
    }

    public int insertListTasks(List<Task> tasks) {
        SQLiteDatabase db = null;
        int inserted = 0;

        try {
            db = sqliteDbHelper.getWritableDatabase();
            db.beginTransaction();

            for (Task task : tasks) {
                try {
                    ContentValues cv = new ContentValues();
                    cv.put(Task.COL_TITLE, task.getTitle());
                    cv.put(Task.COL_TAG_COUNT, task.getTagCount());
                    cv.put(Task.COL_TAG_ID, task.getTagId());
                    cv.put(Task.COL_BOX_ID, task.getBoxId());
                    cv.put(Task.COL_RSS_VALUE, task.getRssValue());
                    cv.put(Task.COL_ZONE_ID, task.getZoneId());
                    cv.put(Task.COL_MODULE_ID, task.getModuleId());
                    cv.put(Task.COL_TAG_FOUND, task.getTagFound());
                    cv.put(Task.COL_DATE_TIME, task.getDateTime());

                    long row = db.insert(Task.TABLE_NAME, null, cv);
                    if (row != -1) inserted++;

                } catch (Exception e) {
                    Log.e(TAG, "insertTasks: failed inserting item", e);
                }
            }

            db.setTransactionSuccessful();

        } catch (Exception e) {
            Log.e(TAG, "insertTasks: error", e);
        } finally {
            try {
                if (db != null) {
                    db.endTransaction();
                    if (db.isOpen()) db.close();
                }
            } catch (Exception ignored) {
            }
        }

        return inserted;
    }


    // Insert a task; returns inserted row id or -1
    public long insertTask(Task task) {
        SQLiteDatabase db = null;
        long rowId = -1;
        try {
            db = sqliteDbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(Task.COL_TITLE, task.getTitle());
            cv.put(Task.COL_TAG_COUNT, task.getTagCount());
            cv.put(Task.COL_TAG_ID, task.getTagId());
            cv.put(Task.COL_BOX_ID, task.getBoxId());
            cv.put(Task.COL_RSS_VALUE, task.getRssValue());
            cv.put(Task.COL_ZONE_ID, task.getZoneId());
            cv.put(Task.COL_MODULE_ID, task.getModuleId());
            cv.put(Task.COL_DATE_TIME, task.getDateTime());

            rowId = db.insert(Task.TABLE_NAME, null, cv);
        } catch (Exception e) {
            Log.e(TAG, "insertTask: error", e);
        } finally {
            if (db != null && db.isOpen()) db.close();
        }
        return rowId;
    }

    // Get all tasks
    public List<Task> getAllTasks() {
        List<Task> list = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = sqliteDbHelper.getReadableDatabase();

            // ORDER BY COL_ID DESC
            cursor = db.query(
                    Task.TABLE_NAME,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Task.COL_ID + " DESC"   // <-- correct order by
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    list.add(cursorToTask(cursor));
                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            Log.e(TAG, "getAllTasks: error", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) cursor.close();
            if (db != null && db.isOpen()) db.close();
        }

        return list;
    }


    public static class TaskFilter {

        public String title;
        public String tagId;
        public String boxId;
        public String zoneId;
        public String moduleId;
        public Double minRssValue;
        public Double maxRssValue;

        public TaskFilter() {
        }
    }

    public String getTagsByBoxId(int boxId) {
        String result = "";
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = sqliteDbHelper.getReadableDatabase();

            String sql = "SELECT " + Task.COL_TAG_ID +
                    " FROM " + Task.TABLE_NAME +
                    " WHERE " + Task.COL_BOX_ID + " = ?" +
                    " AND (" + Task.COL_TAG_FOUND + " = 0 OR " + Task.COL_TAG_FOUND + " IS NULL)";


            cursor = db.rawQuery(sql, new String[]{String.valueOf(boxId)});

            List<String> tags = new ArrayList<>();

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    try {
                        String tag = cursor.getString(0);
                        if (tag != null && !tag.isEmpty()) {
                            tags.add(tag);
                        }
                    } catch (Exception ignored) {
                    }
                } while (cursor.moveToNext());
            }

            result = android.text.TextUtils.join(",", tags);

        } catch (Exception e) {
            try {
                e.printStackTrace();
            } catch (Exception ignored) {
            }
        } finally {
            try {
                if (cursor != null) cursor.close();
                if (db != null) db.close();
            } catch (Exception ignored) {
            }
        }

        return result;
    }


//    TaskFilter filter = new TaskFilter();
//    filter.tagId = "TAG123";
//    filter.minRssValue = -40.0;


    /**
     * Return one Task per boxId. If multiple rows share the same boxId,
     * the row with the highest Task.COL_ID (latest) is returned.
     */
    public List<Task> getTasksList(TaskFilter filter) { // get task list only unique task list
        List<Task> list = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = sqliteDbHelper.getReadableDatabase();

            // Choose MAX(id) for latest row per boxId. Use MIN(id) for earliest.
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT t.* FROM ").append(Task.TABLE_NAME).append(" t")
                    .append(" INNER JOIN (")
                    .append("  SELECT ").append("MAX(").append(Task.COL_ID).append(") AS keep_id")
                    .append("  FROM ").append(Task.TABLE_NAME)
                    .append("  WHERE ").append(Task.COL_BOX_ID).append(" IS NOT NULL")
                    .append("    AND ").append(Task.COL_BOX_ID).append(" != ''")
                    .append("  GROUP BY ").append(Task.COL_BOX_ID)
                    .append(") u ON t.").append(Task.COL_ID).append(" = u.keep_id");

            List<String> args = new ArrayList<>();

            try {
                if (filter != null) {
                    if (filter.title != null && !filter.title.isEmpty()) {
                        sql.append(" AND t.").append(Task.COL_TITLE).append(" LIKE ?");
                        args.add("%" + filter.title + "%");
                    }
                    if (filter.tagId != null && !filter.tagId.isEmpty()) {
                        sql.append(" AND t.").append(Task.COL_TAG_ID).append(" = ?");
                        args.add(filter.tagId);
                    }
                    if (filter.boxId != null && !filter.boxId.isEmpty()) {
                        // If COL_BOX_ID is INTEGER, ensure filter.boxId contains numeric string
                        sql.append(" AND t.").append(Task.COL_BOX_ID).append(" = ?");
                        args.add(filter.boxId);
                    }
                    if (filter.zoneId != null && !filter.zoneId.isEmpty()) {
                        sql.append(" AND t.").append(Task.COL_ZONE_ID).append(" = ?");
                        args.add(filter.zoneId);
                    }
                    if (filter.moduleId != null && !filter.moduleId.isEmpty()) {
                        sql.append(" AND t.").append(Task.COL_MODULE_ID).append(" = ?");
                        args.add(filter.moduleId);
                    }
                    if (filter.minRssValue != null) {
                        sql.append(" AND t.").append(Task.COL_RSS_VALUE).append(" >= ?");
                        args.add(String.valueOf(filter.minRssValue));
                    }
                    if (filter.maxRssValue != null) {
                        sql.append(" AND t.").append(Task.COL_RSS_VALUE).append(" <= ?");
                        args.add(String.valueOf(filter.maxRssValue));
                    }
                }
            } catch (Exception ignored) {
            }

            sql.append(" ORDER BY t.").append(Task.COL_ID).append(" DESC");

            try {
                Log.d(TAG, "getTasksOnePerBox SQL: " + sql.toString());
                Log.d(TAG, "getTasksOnePerBox ARGS: " + args.toString());
            } catch (Exception ignored) {
            }

            cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]));

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    try {
                        list.add(cursorToTask(cursor));
                    } catch (Exception ignored) {
                    }
                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            try {
                Log.e(TAG, "getTasksOnePerBox: error", e);
            } catch (Exception ignored) {
            }
        } finally {
            try {
                if (cursor != null) cursor.close();
                if (db != null) db.close();
            } catch (Exception ignored) {
            }
        }

        return list;
    }


    public List<Task> getTasksListNoDuplicate(TaskFilter filter) {
        List<Task> list = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = sqliteDbHelper.getReadableDatabase();

            StringBuilder sql = new StringBuilder();
            List<String> args = new ArrayList<>();

            // Build SELECT + WHERE first (no ORDER BY here)
            sql.append("SELECT * FROM ")
                    .append(Task.TABLE_NAME)
                    .append(" WHERE ")
                    .append(Task.COL_ZONE_ID).append(" IS NOT NULL ")
                    .append(" AND ").append(Task.COL_ZONE_ID).append(" != '' ");

            // ---- OPTIONAL FILTERS ----
            if (filter != null) {

                if (filter.title != null && !filter.title.isEmpty()) {
                    sql.append(" AND ").append(Task.COL_TITLE).append(" LIKE ?");
                    args.add("%" + filter.title + "%");
                }

                if (filter.tagId != null && !filter.tagId.isEmpty()) {
                    sql.append(" AND ").append(Task.COL_TAG_ID).append(" = ?");
                    args.add(filter.tagId);
                }

                if (filter.boxId != null && !filter.boxId.isEmpty()) {
                    sql.append(" AND ").append(Task.COL_BOX_ID).append(" = ?");
                    args.add(filter.boxId);
                }

                if (filter.zoneId != null && !filter.zoneId.isEmpty()) {
                    sql.append(" AND ").append(Task.COL_ZONE_ID).append(" = ?");
                    args.add(filter.zoneId);
                }

                if (filter.moduleId != null && !filter.moduleId.isEmpty()) {
                    sql.append(" AND ").append(Task.COL_MODULE_ID).append(" = ?");
                    args.add(filter.moduleId);
                }

                if (filter.minRssValue != null) {
                    sql.append(" AND ").append(Task.COL_RSS_VALUE).append(" >= ?");
                    args.add(String.valueOf(filter.minRssValue));
                }

                if (filter.maxRssValue != null) {
                    sql.append(" AND ").append(Task.COL_RSS_VALUE).append(" <= ?");
                    args.add(String.valueOf(filter.maxRssValue));
                }
            }

            // Append ORDER BY only once, after filters
            sql.append(" ORDER BY ").append(Task.COL_ID).append(" DESC");

            // Debug: log final SQL and args to verify there is no duplicate ORDER BY or misplaced AND
            Log.d(TAG, "getTasksListNoDuplicate SQL: " + sql.toString() + " | args: " + args.toString());

            cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]));

            if (cursor.moveToFirst()) {
                do {
                    try {
                        list.add(cursorToTask(cursor));
                    } catch (Exception ignored) {}
                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            Log.e(TAG, "getTasksListNoDuplicate error", e);
        } finally {
            try {
                if (cursor != null) cursor.close();
                if (db != null) db.close();
            } catch (Exception ignored) {}
        }

        return list;
    }


    // Get single Task by id
    public Task getTaskById(long id) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        Task t = null;
        try {
            db = sqliteDbHelper.getReadableDatabase();
            String selection = Task.COL_ID + " = ?";
            String[] args = {String.valueOf(id)};
            cursor = db.query(Task.TABLE_NAME, null, selection, args, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                t = cursorToTask(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "getTaskById: error", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) cursor.close();
            if (db != null && db.isOpen()) db.close();
        }
        return t;
    }


    public static class TaskUpdate {
        // nullable wrappers so we can tell which fields to update
        public Long id;            // required for WHERE clause (use Long to allow null-check)
        public String title;
        public Integer tagCount;
        public Integer tagFound;
        public Integer taskComplete;
        public String tagId;
        public Integer boxId;      // use Integer (nullable) since column is INTEGER
        public Double rssValue;
        public String zoneId;
        public String moduleId;

        public String getDateTime() {
            return dateTime;
        }

        public void setDateTime(String dateTime) {
            this.dateTime = dateTime;
        }

        public String dateTime;

        public TaskUpdate() {
        }

        // convenience fluent setters (optional)
        public TaskUpdate id(long id) {
            this.id = id;
            return this;
        }

        public TaskUpdate title(String title) {
            this.title = title;
            return this;
        }

        public TaskUpdate tagCount(Integer tagCount) {
            this.tagCount = tagCount;
            return this;
        }

        public TaskUpdate tagFound(Integer tagFound) {
            this.tagFound = tagFound;
            return this;
        }

        public TaskUpdate taskComplete(Integer taskComplete) {
            this.taskComplete = taskComplete;
            return this;
        }

        public TaskUpdate tagId(String tagId) {
            this.tagId = tagId;
            return this;
        }

        public TaskUpdate boxId(Integer boxId) {
            this.boxId = boxId;
            return this;
        }

        public TaskUpdate rssValue(Double rssValue) {
            this.rssValue = rssValue;
            return this;
        }

        public TaskUpdate zoneId(String zoneId) {
            this.zoneId = zoneId;
            return this;
        }

        public TaskUpdate moduleId(String moduleId) {
            this.moduleId = moduleId;
            return this;
        }
    }

    public int updateTask(TaskUpdate update, String where, String[] args) {
        SQLiteDatabase db = null;
        int rows = 0;

        try {
            db = sqliteDbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();

            if (update.title != null) cv.put(Task.COL_TITLE, update.title);
            if (update.tagCount != null) cv.put(Task.COL_TAG_COUNT, update.tagCount);
            if (update.tagFound != null) cv.put(Task.COL_TAG_FOUND, update.tagFound);
            if (update.taskComplete != null) cv.put(Task.COL_TAG_COMPLETE, update.taskComplete);
            if (update.tagId != null) cv.put(Task.COL_TAG_ID, update.tagId);
            if (update.boxId != null) cv.put(Task.COL_BOX_ID, update.boxId);
            if (update.rssValue != null) cv.put(Task.COL_RSS_VALUE, update.rssValue);
            if (update.zoneId != null) cv.put(Task.COL_ZONE_ID, update.zoneId);
            if (update.moduleId != null) cv.put(Task.COL_MODULE_ID, update.moduleId);
            if (update.dateTime != null) cv.put(Task.COL_DATE_TIME, update.dateTime);

            if (cv.size() == 0) return 0;

            rows = db.update(Task.TABLE_NAME, cv, where, args);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (db != null) db.close();
        }

        return rows;
    }


    // Delete task by id - returns true if deleted
    public boolean deleteTask(long id) {
        SQLiteDatabase db = null;
        int deleted = 0;
        try {
            db = sqliteDbHelper.getWritableDatabase();
            String where = Task.COL_BOX_ID + " = ?";
            String[] args = {String.valueOf(id)};
            deleted = db.delete(Task.TABLE_NAME, where, args);
        } catch (Exception e) {
            Log.e(TAG, "deleteTask: error", e);
        } finally {
            if (db != null && db.isOpen()) db.close();
        }
        return deleted > 0;
    }

    // Helper: convert cursor -> Task
    private Task cursorToTask(Cursor cursor) {
        Task t = new Task();
        try {
            t.setId(cursor.getLong(cursor.getColumnIndexOrThrow(Task.COL_ID)));
            t.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(Task.COL_TITLE)));
            t.setTagCount(cursor.getInt(cursor.getColumnIndexOrThrow(Task.COL_TAG_COUNT)));
            t.setTagFound(cursor.getInt(cursor.getColumnIndexOrThrow(Task.COL_TAG_FOUND)));
            t.setTaskComplete(cursor.getInt(cursor.getColumnIndexOrThrow(Task.COL_TAG_COMPLETE)));
            t.setTagId(cursor.getString(cursor.getColumnIndexOrThrow(Task.COL_TAG_ID)));
            t.setDateTime(cursor.getString(cursor.getColumnIndexOrThrow(Task.COL_DATE_TIME)));
            t.setBoxId(cursor.getInt(cursor.getColumnIndexOrThrow(Task.COL_BOX_ID)));
            t.setRssValue(cursor.getDouble(cursor.getColumnIndexOrThrow(Task.COL_RSS_VALUE)));
            t.setZoneId(cursor.getString(cursor.getColumnIndexOrThrow(Task.COL_ZONE_ID)));
            t.setModuleId(cursor.getString(cursor.getColumnIndexOrThrow(Task.COL_MODULE_ID)));

        } catch (Exception e) {
            Log.e(TAG, "cursorToTask: error", e);
        }
        return t;
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
