package com.example.stepup.data;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

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

    private static final String TAG = "NotificationFlow"; // תג לסינון הלוגים

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "GoalNotificationReceiver onReceive triggered!");

        int goalId = intent.getIntExtra("goal_id", -1);
        Log.d(TAG, "Received goal_id: " + goalId);

        if (goalId == -1) {
            Log.e(TAG, "onReceive: Invalid goal_id received.");
            return;
        }

        // גישה למסד הנתונים צריכה להיות ברקע
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            GoalWithReminder goalWithReminder = db.dao().getGoalWithReminderById(goalId);

            if (goalWithReminder != null) {
                Log.i(TAG, "Found goal '" + goalWithReminder.goal.name + "' in database. Proceeding to send notification.");
                sendNotification(context, goalWithReminder);

                // תזמון מחדש של ההתראה למועד הבא
                Log.d(TAG, "Rescheduling notification for goal ID " + goalId);
                NotificationScheduler.scheduleNotification(context, goalWithReminder.goal, goalWithReminder.reminder);
            } else {
                Log.e(TAG, "Could not find GoalWithReminder in database for goal_id: " + goalId);
            }
        });
    }

    private void sendNotification(Context context, GoalWithReminder goalWithReminder) {
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
                .setSmallIcon(R.drawable.ic_runner)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true);

        // בודקים אם יש הרשאה לפני שמציגים את ההתראה
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "POST_NOTIFICATIONS permission is GRANTED. Displaying notification.");
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(goalWithReminder.goal.id, builder.build());
        } else {
            Log.e(TAG, "POST_NOTIFICATIONS permission is DENIED. Cannot display notification.");
        }
    }

    private String getNotificationText(String notificationType) {
        if ("TOUGH".equals(notificationType)) {
            return "No excuses. It's time to work on your goal!";
        } else {
            return "A small step today leads to a big success tomorrow. You can do it!";
        }
    }
}
