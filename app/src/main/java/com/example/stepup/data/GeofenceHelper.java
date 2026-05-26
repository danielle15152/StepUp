package com.example.stepup.data;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.stepup.data.entities.Goal;
import com.example.stepup.data.entities.Reminder; // The missing import
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

public class GeofenceHelper {

    private static final String TAG = "GeofenceHelper";
    private static final float GEOFENCE_RADIUS_IN_METERS = 100; // 100 meters radius

    private final Context context;
    private final GeofencingClient geofencingClient;

    public GeofenceHelper(Context context) {
        this.context = context;
        this.geofencingClient = LocationServices.getGeofencingClient(context);
    }

    public void addGeofence(Goal goal, Reminder reminder) {
        if (reminder == null || reminder.latitude == null || reminder.longitude == null) {
            return; // No location to set a geofence for
        }

        String geofenceId = String.valueOf(goal.id);

        Geofence geofence = new Geofence.Builder()
                .setRequestId(geofenceId)
                .setCircularRegion(
                        reminder.latitude,
                        reminder.longitude,
                        GEOFENCE_RADIUS_IN_METERS
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build();

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Cannot add geofence: ACCESS_FINE_LOCATION permission not granted.");
            return;
        }

        geofencingClient.addGeofences(geofencingRequest, getGeofencePendingIntent())
                .addOnSuccessListener(aVoid -> Log.i(TAG, "Geofence added for goal ID: " + geofenceId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to add geofence for goal ID: " + geofenceId, e));
    }

    public void removeGeofence(String geofenceId) {
        geofencingClient.removeGeofences(java.util.Collections.singletonList(geofenceId))
                .addOnSuccessListener(aVoid -> Log.i(TAG, "Geofence removed for goal ID: " + geofenceId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to remove geofence for goal ID: " + geofenceId, e));
    }

    private PendingIntent getGeofencePendingIntent() {
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    }
}
