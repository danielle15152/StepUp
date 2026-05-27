package com.example.stepup;
// המסך פתיחה של האפליקציה
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

public class SplashActivity extends AppCompatActivity {

    public static final String GOAL_NOTIFICATION_CHANNEL_ID = "goal_notifications";
    private static final long SPLASH_DURATION_MS = 1000;

    // מנגנון לבקשת הרשאה מהמשתמשת - מוצג כדיאלוג מערכת והתוצאה חוזרת ב-callback
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                proceedToApp();
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // טוענים את ערכת הנושא לפני setContentView כדי שהמסך יופיע ישר במצב הנכון
        applyStoredThemeMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        createNotificationChannel();
        askForNotificationPermission();
    }

    // טעינת ערכת הנושא שהמשתמשת בחרה בהגדרות, או "לפי המכשיר" אם עוד לא בחרה
    private void applyStoredThemeMode() {
        SharedPreferences prefs = getSharedPreferences("StepUpPrefs", Context.MODE_PRIVATE);
        int themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    // מעבר למסך הראשי אחרי שניה - נותן זמן ל-splash להופיע
    private void proceedToApp() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, GoalsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            // finish כדי שלא נוכל לחזור ל-splash בלחיצת back
            finish();
        }, SPLASH_DURATION_MS);
    }

    // יוצרים ערוץ התראות פעם אחת בהפעלה הראשונה - אחר כך לא ניתן לשנות אותו
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(GOAL_NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // בודקים אם המשתמשת אישרה הצגת התראות. אם לא - מציגים דיאלוג בקשה
    private void askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                proceedToApp();
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            proceedToApp();
        }
    }
}
