package com.example.stepup.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.stepup.data.entities.Goal;
import com.example.stepup.data.entities.GoalWithReminder;
import com.example.stepup.data.entities.Reminder;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "GeofenceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = "Geofence error code: " + geofencingEvent.getErrorCode();
            Log.e(TAG, errorMessage);
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            for (Geofence geofence : triggeringGeofences) {
                String requestId = geofence.getRequestId();
                Log.i(TAG, "Geofence ENTER transition for goal ID: " + requestId);
                
                sendNotificationForGeofence(context, requestId);
            }

        } else {
            Log.e(TAG, "Invalid geofence transition type: " + geofenceTransition);
        }
    }

    private void sendNotificationForGeofence(Context context, String goalIdStr) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Dao dao = AppDatabase.getDatabase(context).dao();
            try {
                long goalId = Long.parseLong(goalIdStr);
                GoalWithReminder goalWithReminder = dao.getGoalWithReminderById(goalId);

                if (goalWithReminder == null || !goalWithReminder.goal.active || goalWithReminder.reminder == null) {
                     Log.i(TAG, "Notification check failed: Goal is null, inactive, or has no reminder.");
                    return;
                }
                
                Reminder reminder = goalWithReminder.reminder;

                // 1. Check if today is a scheduled day
                Calendar today = Calendar.getInstance();
                int dayOfWeek = today.get(Calendar.DAY_OF_WEEK) - 1; // 0=Sunday
                if (reminder.days == null || !reminder.days.contains(dayOfWeek)) {
                    Log.i(TAG, "Notification check failed: Not a scheduled day for goal ID " + goalId);
                    return;
                }

                // 2. Check if a notification was already sent today
                long lastNotificationTime = reminder.lastLocationNotificationTimestamp;
                long now = System.currentTimeMillis();
                if (TimeUnit.MILLISECONDS.toDays(now) == TimeUnit.MILLISECONDS.toDays(lastNotificationTime)) {
                     Log.i(TAG, "Notification check failed: Already sent today for goal ID " + goalId);
                    return;
                }

                // All checks passed! Send notification
                // The NotificationScheduler will now use the Goal object's notificationType
                // to determine the title and message.
                NotificationScheduler.showImmediateNotification(context, goalWithReminder.goal); // Pass the Goal object

                // Update the timestamp in the database
                reminder.lastLocationNotificationTimestamp = now;
                dao.updateReminder(reminder);
                Log.i(TAG, "Notification sent and timestamp updated for goal ID " + goalId);

            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing goal ID from geofence request ID", e);
            }
        });
    }
}
