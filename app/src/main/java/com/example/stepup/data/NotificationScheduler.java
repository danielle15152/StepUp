package com.example.stepup.data;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.stepup.data.entities.Goal;
import com.example.stepup.data.entities.Reminder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

public class NotificationScheduler {

    private static final String TAG = "NotificationFlow"; // תג לסינון הלוגים

    public static void scheduleNotification(Context context, Goal goal, Reminder reminder) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (goal == null || reminder == null || reminder.days.isEmpty()) {
            Log.w(TAG, "scheduleNotification: Aborting. Goal, reminder, or days are null/empty.");
            return;
        }

        Calendar nextNotificationTime = getNextNotificationTime(reminder);
        if (nextNotificationTime == null) {
            Log.w(TAG, "scheduleNotification: Aborting. Could not calculate next notification time.");
            return;
        }

        // פורמט יפה להצגת תאריכים בלוג
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Log.i(TAG, "Scheduling notification for goal ID " + goal.id + " at: " + sdf.format(nextNotificationTime.getTime()));


        Intent intent = new Intent(context, GoalNotificationReceiver.class);
        intent.putExtra("goal_id", goal.id);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                goal.id, // requestCode ייחודי לכל מטרה
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextNotificationTime.getTimeInMillis(),
                pendingIntent
        );
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
        int nowDayOfWeek = now.get(Calendar.DAY_OF_WEEK) - 1; // 0=ראשון, 1=שני ...
        int nowHour = now.get(Calendar.HOUR_OF_DAY);
        int nowMinute = now.get(Calendar.MINUTE);
        int nowInMinutes = nowHour * 60 + nowMinute;

        int reminderTimeInMinutes = reminder.minuteOfDay;
        Collections.sort(reminder.days);

        // הדפסת מידע ללוג
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Log.d(TAG, "Calculating next notification time. Current time: " + sdf.format(now.getTime()));
        Log.d(TAG, "Reminder days: " + reminder.days.toString() + ", Reminder time (minutes): " + reminderTimeInMinutes);


        // חיפוש היום הבא להתראה, כולל היום
        for (int day : reminder.days) {
            if (day > nowDayOfWeek || (day == nowDayOfWeek && reminderTimeInMinutes > nowInMinutes)) {
                // מצאנו את היום המתאים בשבוע הנוכחי
                return getCalendarForDayAndTime(day, reminderTimeInMinutes);
            }
        }

        // אם לא מצאנו השבוע, ניקח את היום הראשון בשבוע הבא
        if (!reminder.days.isEmpty()) {
            int nextWeekDay = reminder.days.get(0);
            Calendar nextNotification = getCalendarForDayAndTime(nextWeekDay, reminderTimeInMinutes);
            nextNotification.add(Calendar.WEEK_OF_YEAR, 1);
            return nextNotification;
        }

        return null;
    }

    private static Calendar getCalendarForDayAndTime(int dayOfWeek, int minuteOfDay) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek + 1);
        calendar.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60);
        calendar.set(Calendar.MINUTE, minuteOfDay % 60);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // אם השעה המחושבת כבר עברה היום, קובעים אותה ליום המיועד בשבוע הבא
        if(calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
        }
        return calendar;
    }
}
