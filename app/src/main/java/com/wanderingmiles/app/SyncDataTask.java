package com.wanderingmiles.app;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import com.wanderingmiles.app.data.LocationContract;
import com.wanderingmiles.app.data.LocationDbHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
        // Step 1: Find all positions that haven't been synced yet
        //Cursor unsyncedPositionsCursor = mLocationDbHelper.getWritableDatabase().query(LocationContract.LocationEntry.TABLE_NAME, null, LocationContract.LocationEntry.COLUMN_SYNCED + " = 0", null, null, null, null);
        Cursor unsyncedPositionsCursor = new LocationDbHelper(mParentContext).getWritableDatabase().query(LocationContract.LocationEntry.TABLE_NAME, null, null, null, null, null, null);

        // Step 2: Convert the resultset to a JSON payload
        String positionsJson = convertResultsToJson(unsyncedPositionsCursor);

        // Step 3: Post the payload
        if (positionsJson != "") {
            String status = NetworkUtils.postRawJsonString(positionsJson);
            // Step 5: Mark all the positions as synced

            return "success";
        }
        else {
            return "no unsynced positions found";
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
