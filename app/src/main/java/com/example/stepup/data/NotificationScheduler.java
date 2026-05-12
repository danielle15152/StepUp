package com.example.stepup.data;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.example.stepup.data.entities.Goal;
import com.example.stepup.data.entities.Reminder;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class NotificationScheduler {

    public static void scheduleNotification(Context context, Goal goal, Reminder reminder) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, GoalNotificationReceiver.class);
        intent.putExtra("goal_id", goal.id);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                goal.id, // משתמשים ב-ID של המטרה כקוד בקשה ייחודי
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long nextTriggerTime = calculateNextTriggerTime(reminder.days, reminder.minuteOfDay);

        // קובעים התראה מדויקת שתפעל גם כשהמכשיר במצב שינה
        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTriggerTime,
                pendingIntent
        );
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
        }
    }

    // פונקציית עזר לחישוב הזמן הבא שבו ההתראה צריכה לפעול
    private static long calculateNextTriggerTime(List<Integer> days, int minuteOfDay) {
        Calendar now = Calendar.getInstance();
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60);
        next.set(Calendar.MINUTE, minuteOfDay % 60);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        // ממיר את ימי התזכורת (0=ראשון) לימי לוח השנה (1=ראשון)
        Collections.sort(days);
        int today = now.get(Calendar.DAY_OF_WEEK) - 1; // 0=ראשון

        for (int day : days) {
            if (day > today || (day == today && next.after(now))) {
                // מצאנו את היום הבא בשבוע הנוכחי
                next.add(Calendar.DAY_OF_YEAR, day - today);
                return next.getTimeInMillis();
            }
        }

        // אם לא מצאנו יום בשבוע הנוכחי, קובעים ליום הראשון בשבוע הבא
        int firstDay = days.get(0);
        next.add(Calendar.DAY_OF_YEAR, (7 - today + firstDay) % 7);
        return next.getTimeInMillis();
    }
}