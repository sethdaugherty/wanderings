package com.wanderingmiles.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.wanderingmiles.app.data.LocationContract.LocationEntry;

/**
 * Created by Seth on 12/16/2017.
 */

public class LocationDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "location.db";
    private static final int DATABASE_VERSION = 1;

    public LocationDbHelper(Context context) {
        super(context, null, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String createSql = new StringBuilder()
                .append("CREATE TABLE ").append(LocationEntry.TABLE_NAME).append(" (")
                .append(LocationEntry.COLUMN_DATE).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ")
                .append(LocationEntry.COLUMN_LATITUDE).append(" REAL NOT NULL, ")
                .append(LocationEntry.COLUMN_LONGITUDE).append(" REAL NOT NULL, ")
                .append(" UNIQUE (").append(LocationEntry.COLUMN_DATE).append(") ON CONFLICT REPLACE);")
                .toString();

        sqLiteDatabase.execSQL(createSql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
