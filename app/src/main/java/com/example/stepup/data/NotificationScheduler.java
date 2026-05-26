package com.example.stepup.data;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.stepup.GoalDetailsActivity;
import com.example.stepup.R;
import com.example.stepup.SplashActivity;
import com.example.stepup.data.entities.Goal;
import com.example.stepup.data.entities.Reminder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class NotificationScheduler {

    private static final String TAG = "NotificationFlow";
    private static final String CHANNEL_ID = "stepup_goal_channel";


    public static void showImmediateNotification(Context context, String title, String message, Goal goal) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "StepUp Goal Reminders";
            String description = "Channel for all StepUp goal reminder notifications";
            int importance = goal.notificationType.equals("TOUGH") ? NotificationManager.IMPORTANCE_HIGH : NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, GoalDetailsActivity.class);
        intent.putExtra(GoalDetailsActivity.EXTRA_GOAL_ID, (long)goal.id);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                goal.id,
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_runner)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(goal.notificationType.equals("TOUGH") ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(goal.id, builder.build());
        Log.i(TAG, "Showing immediate notification for goal ID: " + goal.id);
    }


    public static void scheduleNotification(Context context, Goal goal, Reminder reminder) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (goal == null || reminder == null || reminder.days == null || reminder.days.isEmpty()) {
            Log.w(TAG, "Aborting schedule. Goal, reminder, or days are null/empty.");
            return;
        }

        Calendar nextNotificationTime = getNextNotificationTime(reminder);
        if (nextNotificationTime == null) {
            Log.w(TAG, "Aborting schedule. Could not calculate next notification time.");
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Log.i(TAG, "Scheduling for goal ID " + goal.id + " at: " + sdf.format(nextNotificationTime.getTime()));

        Intent intent = new Intent(context, GoalNotificationReceiver.class);
        intent.putExtra("goal_id", (long)goal.id);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                goal.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Check for permission to schedule exact alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "No exact alarm permission. Falling back to inexact alarm.");
            // Fallback for devices that cannot schedule exact alarms
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextNotificationTime.getTimeInMillis(),
                    pendingIntent
            );
        } else {
            // Schedule exact alarm for devices that have permission
            Log.i(TAG, "Scheduling exact alarm.");
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextNotificationTime.getTimeInMillis(),
                    pendingIntent
            );
        }
    }

    public static void cancelNotification(Context context, Goal goal) {
        Log.i(TAG, "Cancelling notification for goal ID " + goal.id);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, GoalNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                goal.id,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private static Calendar getNextNotificationTime(Reminder reminder) {
        Calendar now = Calendar.getInstance();
        if(reminder.minuteOfDay == null) return null;
        
        int reminderHour = reminder.minuteOfDay / 60;
        int reminderMinute = reminder.minuteOfDay % 60;

        for (int i = 0; i < 14; i++) {
            Calendar temp = (Calendar) now.clone();
            temp.add(Calendar.DAY_OF_YEAR, i);
            int dayOfWeek = temp.get(Calendar.DAY_OF_WEEK) - 1;

            if (reminder.days.contains(dayOfWeek)) {
                temp.set(Calendar.HOUR_OF_DAY, reminderHour);
                temp.set(Calendar.MINUTE, reminderMinute);
                temp.set(Calendar.SECOND, 0);
                temp.set(Calendar.MILLISECOND, 0);

                if (temp.after(now)) {
                    return temp;
                }
            }
        }
        return null;
    }
}
