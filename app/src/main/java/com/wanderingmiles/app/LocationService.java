package com.wanderingmiles.app;

import android.Manifest;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.wanderingmiles.app.data.LocationContract;
import com.wanderingmiles.app.data.LocationDbHelper;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.Executor;

import static android.content.ContentValues.TAG;

/**
 * Created by Seth on 12/22/2017.
 */

public class LocationService extends Service {
    private static final String NOTIFICATION_CHANNEL_ID = "location_update_channel";
    private static final int ONGOING_NOTIFICATION_ID = 981234;

    /**
     * Code used in requesting runtime permissions.
     */
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    /**
     * Constant used in the location settings dialog.
     */
    private static final int REQUEST_CHECK_SETTINGS = 0x1;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Keys for storing activity state in the Bundle.
    private final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    private final static String KEY_LOCATION = "location";
    private final static String KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string";
    public static final String START = "start_updates";
    public static final String STOP = "stop_updates";


    private FusedLocationProviderClient mFusedLocationClient;

    private SettingsClient mSettingsClient;

    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;

    private Boolean mRequestingLocationUpdates;
    private String mLastUpdateTime;


    private Location mCurrentLocation;


    // Database
    private LocationDbHelper mLocationDbHelper;

    public LocationService() {
        //super("LocationService");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Add a binder here?
        return null;
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action != null && action.equals(LocationService.START)) {
            // TODO: move all this stuff to a method
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(this, 0, notificationIntent, 0);


            Notification notification =
                    new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                            .setContentTitle(getText(R.string.location_notification_title))
                            .setContentText(getText(R.string.location_notification_message))
                            .setSmallIcon(R.drawable.ic_drink_notification) // TODO: get a real icon here
                            .setContentIntent(pendingIntent)
                            .setTicker(getText(R.string.ticker_text))
                            .build();

            startForeground(ONGOING_NOTIFICATION_ID, notification);
            Log.d("BLAH", "finished onHandleIntent");

            mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                    .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                        @Override
                        public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                            Log.i(TAG, "All location settings are satisfied.");

                            //noinspection MissingPermission
                            if (ActivityCompat.checkSelfPermission(LocationService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(LocationService.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                // TODO: Consider calling
                                //    ActivityCompat#requestPermissions
                                // here to request the missing permissions, and then overriding
                                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                //                                          int[] grantResults)
                                // to handle the case where the user grants the permission. See the documentation
                                // for ActivityCompat#requestPermissions for more details.
                                Log.d(TAG, "returning from permissions");
                                return;
                            }
                            Log.d(TAG, "requesting location updates");
                            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                    mLocationCallback, Looper.myLooper());

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            int statusCode = ((ApiException) e).getStatusCode();
                            switch (statusCode) {
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                            "location settings ");
                                    // TODO: Fix this
                                /*)
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                */
                                    break;
                                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                    String errorMessage = "Location settings are inadequate, and cannot be " +
                                            "fixed here. Fix in Settings.";
                                    Log.e(TAG, errorMessage);
                                    Toast.makeText(LocationService.this, errorMessage, Toast.LENGTH_LONG).show();
                                    mRequestingLocationUpdates = false;
                            }
                        }
                    });
        }
        else if (action.equals(STOP)) {
            Log.d(TAG, "Stopping updates");

            stopSelf();
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        mLocationDbHelper = new LocationDbHelper(this);

        // Kick off the process of building the LocationCallback, LocationRequest, and
        // LocationSettingsRequest objects.
        mLocationCallback = createLocationCallback();
        mLocationRequest = createLocationRequest();
        mLocationSettingsRequest = buildLocationSettingsRequest();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // TODO: stop recording location data here
        Log.d(TAG, "called onDestroy");

        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    mRequestingLocationUpdates = false;
                }
            });
    }

    private LocationCallback createLocationCallback() {
        return new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                Date now = new Date();
                mCurrentLocation = locationResult.getLastLocation();
                mLastUpdateTime = DateFormat.getTimeInstance().format(now);
                persistCurrentLocation(now, mCurrentLocation);
            }
        };
    }

    private LocationRequest createLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();

        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        return locationRequest;
    }

    private LocationSettingsRequest buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        return builder.build();
    }

    private void persistCurrentLocation(Date now, android.location.Location location) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(LocationContract.LocationEntry.COLUMN_DATE, now.getTime());
        contentValues.put(LocationContract.LocationEntry.COLUMN_LATITUDE, location.getLatitude());
        contentValues.put(LocationContract.LocationEntry.COLUMN_LONGITUDE, location.getLongitude());
        mLocationDbHelper.getWritableDatabase().insert(LocationContract.LocationEntry.TABLE_NAME, null, contentValues);
        Log.d(TAG, "Wrote current postion to DB: " + now.getTime() + " " + location.getLatitude() + ", " + location.getLongitude());
    }
}
