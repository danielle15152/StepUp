package com.example.stepup.data;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.example.stepup.GoalsActivity;
import com.example.stepup.R;
import com.example.stepup.SplashActivity;
import com.example.stepup.data.entities.GoalWithReminder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoalNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int goalId = intent.getIntExtra("goal_id", -1);
        if (goalId == -1) {
            return;
        }

        // גישה למסד הנתונים צריכה להיות ברקע
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            GoalWithReminder goalWithReminder = db.dao().getGoalWithReminderById(goalId);

            if (goalWithReminder != null) {
                sendNotification(context, goalWithReminder);

                // תזמון מחדש של ההתראה למועד הבא
                NotificationScheduler.scheduleNotification(context, goalWithReminder.goal, goalWithReminder.reminder);
            }
        });
    }

    private void sendNotification(Context context, GoalWithReminder goalWithReminder) {
        // Intent שיופעל כשהמשתמש ילחץ על ההתראה - יפתח את האפליקציה
        Intent resultIntent = new Intent(context, GoalsActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                context,
                goalWithReminder.goal.id,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = goalWithReminder.goal.name;
        String text = getNotificationText(goalWithReminder.goal.notificationType);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, SplashActivity.GOAL_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_runner) // צריך לוודא שהאייקון קיים
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true); // ההתראה תיעלם כשהמשתמש ילחץ עליה

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // בודקים אם יש הרשאה לפני שמציגים את ההתראה
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(goalWithReminder.goal.id, builder.build());
        }
    }

    // פונקציה שבוחרת את הטקסט לפי סוג המוטיבציה
    private String getNotificationText(String notificationType) {
        if ("TOUGH".equals(notificationType)) {
            return "No excuses. It's time to work on your goal!";
        } else {
            // ברירת המחדל היא סגנון עדין
            return "A small step today leads to a big success tomorrow. You can do it!";
        }
    }
}
