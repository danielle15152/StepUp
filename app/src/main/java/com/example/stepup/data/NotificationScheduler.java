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


    public static void showImmediateNotification(Context context, Goal goal) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String title = goal.name;
        String message;
        if ("TOUGH".equals(goal.notificationType)) {
            message = context.getString(R.string.notification_message_tough);
        } else {
            message = context.getString(R.string.notification_message_gentle);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.notification_channel_name);
            String description = context.getString(R.string.notification_channel_description);
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
    }


    public static void scheduleNotification(Context context, Goal goal, Reminder reminder) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (goal == null || reminder == null || reminder.days == null || reminder.days.isEmpty()) {
            return;
        }

        Calendar nextNotificationTime = getNextNotificationTime(reminder);
        if (nextNotificationTime == null) {
            return;
        }

        Intent intent = new Intent(context, GoalNotificationReceiver.class);
        intent.putExtra("goal_id", (long)goal.id);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                goal.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // אם אין הרשאה ל-exact alarm, נופלים ל-inexact
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextNotificationTime.getTimeInMillis(),
                    pendingIntent
            );
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextNotificationTime.getTimeInMillis(),
                    pendingIntent
            );
        }
    }

    public static void cancelNotification(Context context, Goal goal) {
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
