package com.wanderingmiles.app.data;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Seth on 12/16/2017.
 */

public class LocationContract {

    public static final String CONTENT_AUTHORITY = "com.wanderingmiles.app";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_LOCATION = "location";

    public static final class LocationEntry implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_LOCATION)
                .build();

        public static final String TABLE_NAME = "location";

        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_LATITUDE = "latitude";
        public static final String COLUMN_LONGITUDE = "longitude";

    }
}
