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

import com.example.stepup.GoalDetailsActivity;
import com.example.stepup.R;
import com.example.stepup.SplashActivity;
import com.example.stepup.data.entities.GoalWithReminder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoalNotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationFlow";

    @Override
    public void onReceive(Context context, Intent intent) {
        long goalId = intent.getLongExtra("goal_id", -1);

        if (goalId == -1) {
            Log.e(TAG, "onReceive: Invalid goal_id received.");
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            GoalWithReminder goalWithReminder = db.dao().getGoalWithReminderById(goalId);

            if (goalWithReminder != null) {
                sendNotification(context, goalWithReminder);
                // קביעת ההתראה הבאה לפי הימים בתזכורת
                NotificationScheduler.scheduleNotification(context, goalWithReminder.goal, goalWithReminder.reminder);
            } else {
                Log.e(TAG, "Could not find GoalWithReminder in database for goal_id: " + goalId);
            }
        });
    }

    private void sendNotification(Context context, GoalWithReminder goalWithReminder) {
        Intent resultIntent = new Intent(context, GoalDetailsActivity.class);
        resultIntent.putExtra(GoalDetailsActivity.EXTRA_GOAL_ID, (long)goalWithReminder.goal.id);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                context,
                goalWithReminder.goal.id,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = goalWithReminder.goal.name;
        String text = getNotificationText(context, goalWithReminder.goal.notificationType);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, SplashActivity.GOAL_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_runner)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true);

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(goalWithReminder.goal.id, builder.build());
        } else {
            Log.e(TAG, "POST_NOTIFICATIONS permission is DENIED. Cannot display notification.");
        }
    }

    private String getNotificationText(Context context, String notificationType) {
        if ("TOUGH".equals(notificationType)) {
            return context.getString(R.string.notification_message_tough);
        } else {
            return context.getString(R.string.notification_message_gentle);
        }
    }
}
