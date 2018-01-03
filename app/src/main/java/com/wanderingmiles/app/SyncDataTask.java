package com.wanderingmiles.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.wanderingmiles.app.data.LocationContract;
import com.wanderingmiles.app.data.LocationDbHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// TODO: use a LoaderManager to handle this asyncTask
public class SyncDataTask extends AsyncTask<Void, Void, String> {

    private static final String TAG = MainActivity.class.getSimpleName();


    //private LocationDbHelper mLocationDbHelper;
    private Context mParentContext;

    public SyncDataTask(Context parentContext) {
        mParentContext = parentContext;
        //this.mLocationDbHelper = locationDbHelper;
    }

    @Override
    protected String doInBackground(Void... voids) {
        Log.d(TAG, "starting async task");
        SQLiteDatabase writableDatabase = new LocationDbHelper(mParentContext).getWritableDatabase();

        // Step 1: Find all positions that haven't been synced yet
        Cursor unsyncedPositionsCursor = writableDatabase.query(LocationContract.LocationEntry.TABLE_NAME, null, LocationContract.LocationEntry.COLUMN_SYNCED + " = 'false'", null, null, null, null);
        //Cursor unsyncedPositionsCursor = writableDatabase.query(LocationContract.LocationEntry.TABLE_NAME, null, null, null, null, null, null);

        // Step 2: Convert the resultset to a JSON payload
        String positionsJson = convertResultsToJson(unsyncedPositionsCursor);

        // Step 3: Post the payload
        if (positionsJson != "") {
            boolean postStatus = NetworkUtils.postRawJsonString(positionsJson);

            // Step 5: Mark all the positions as synced
            boolean updateStatuse = markPositionsAsSynced(unsyncedPositionsCursor, writableDatabase);

            return "success";
        }
        else {
            return "no unsynced positions found";
        }

    }

    private boolean markPositionsAsSynced(Cursor unsyncedPositionsCursor, SQLiteDatabase writableDatabase) {
        if (unsyncedPositionsCursor.getCount() > 0) {
            unsyncedPositionsCursor.moveToFirst();

            List<Long> idList = new ArrayList<>();
            do {
                idList.add(unsyncedPositionsCursor.getLong(unsyncedPositionsCursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_DATE)));
            } while (unsyncedPositionsCursor.moveToNext());


            ContentValues newValues = new ContentValues();
            newValues.put(LocationContract.LocationEntry.COLUMN_SYNCED, true);
            String whereClause = LocationContract.LocationEntry.COLUMN_DATE + " in (" + idList.stream().map(Object::toString).collect(Collectors.joining(", ")) + ")";
            writableDatabase.update(LocationContract.LocationEntry.TABLE_NAME, newValues, whereClause, null);

            return true;
        }
        else {
            return false;
        }
    }

    private String convertResultsToJson(Cursor unsyncedPositionsCursor) {
        JSONArray positionsArray = new JSONArray();
        if (unsyncedPositionsCursor.getCount() > 0) {
            unsyncedPositionsCursor.moveToFirst();
            do {
                // TODO: Figure out how to do this json serialization more gracefully. It's probably obvious.
                JSONObject rawObject = new JSONObject();
                try {
                    rawObject.put("timestamp", unsyncedPositionsCursor.getLong(unsyncedPositionsCursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_DATE)));
                    rawObject.put("latitude", unsyncedPositionsCursor.getDouble(unsyncedPositionsCursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_LATITUDE)));
                    rawObject.put("longitude", unsyncedPositionsCursor.getDouble(unsyncedPositionsCursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_LONGITUDE)));
                } catch (JSONException e) {
                    // TODO: this is shit
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }

                positionsArray.put(rawObject);
            } while (unsyncedPositionsCursor.moveToNext());

            return positionsArray.toString();
        }
        else {
            return "";
        }
    }
}
