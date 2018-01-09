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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// TODO: use a LoaderManager to handle this asyncTask
public class SyncDataTask extends AsyncTask<Void, Void, String> {

    private static final String TAG = MainActivity.class.getSimpleName();


    //private LocationDbHelper mLocationDbHelper;
    private Context mParentContext;

    private boolean mSyncAll;

    public SyncDataTask(Context parentContext, boolean syncAll) {
        mParentContext = parentContext;
        mSyncAll = syncAll;
        //this.mLocationDbHelper = locationDbHelper;
    }

    @Override
    protected String doInBackground(Void... voids) {
        Log.d(TAG, "starting async task");
        SQLiteDatabase writableDatabase = new LocationDbHelper(mParentContext).getWritableDatabase();

        // TODO: break this sync up into batches

        // Step 1: Find all positions that haven't been synced yet
        Cursor unsyncedPositionsCursor;
        if (mSyncAll) {
            unsyncedPositionsCursor = writableDatabase.query(LocationContract.LocationEntry.TABLE_NAME, null, null, null, null, null, null);
        }
        else {
            unsyncedPositionsCursor = writableDatabase.query(LocationContract.LocationEntry.TABLE_NAME, null, LocationContract.LocationEntry.COLUMN_SYNCED + " = 'false'", null, null, null, null);
        }

        // Step 2: Convert the resultset to a JSON payload. Each String in the result is the raw json payload for a batch of positions
        Collection<String> payloads = convertResultsToJson(unsyncedPositionsCursor);

        // Step 3: Post the payload
        for (String payload : payloads) {
            boolean postStatus = NetworkUtils.postRawJsonString(payload);
            Log.d(TAG, "posting position json returned status=" + postStatus);
        }
        if (payloads.isEmpty()) {
            boolean updateStatuse = markPositionsAsSynced(unsyncedPositionsCursor, writableDatabase);

        }

        return "successful";
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

    private Collection<String> convertResultsToJson(Cursor unsyncedPositionsCursor) {
        List<String> resultsList = new ArrayList<>();
        int maxBatchSize = 100;
        int currentBatchSize = 0;
        JSONArray positionsArray = new JSONArray();
        if (unsyncedPositionsCursor.getCount() > 0) {
            Log.d(TAG, "Found " + unsyncedPositionsCursor.getCount() + " position records to sync");
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
                currentBatchSize++;
                if (currentBatchSize >= maxBatchSize) {
                    resultsList.add(positionsArray.toString());
                    positionsArray = new JSONArray();

                    currentBatchSize = 0;
                }
            } while (unsyncedPositionsCursor.moveToNext());


            if (positionsArray.length() > 0) {
                resultsList.add(positionsArray.toString());
            }
        }

        return resultsList;
    }
}
