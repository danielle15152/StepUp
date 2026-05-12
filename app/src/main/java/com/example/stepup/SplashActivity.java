package com.example.stepup;
//המסך פתיחה של האפליקציה
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SplashActivity extends AppCompatActivity {

    public static final String GOAL_NOTIFICATION_CHANNEL_ID = "goal_notifications";
    private static final long SPLASH_DURATION_MS = 1000; // adjust as needed

    // מנגנון מודרני לבקשת הרשאות. הוא מטפל בהצגת הבקשה ובקבלת התשובה מהמשתמש
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // לא עושים כלום עם התשובה כרגע, אבל בעתיד אפשר להציג הודעה אם המשתמש סירב
                proceedToApp();
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // יצירת ערוץ התראות ובקשת הרשאה מהמשתמש
        createNotificationChannel();
        askForNotificationPermission();
    }

    // פונקציה שממשיכה לפתיחת האפליקציה אחרי שהטיפול בהרשאות הסתיים
    private void proceedToApp() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, GoalsActivity.class);
            startActivity(intent);

            // Cross-fade transition
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

            // Prevent returning to splash on back press
            finish();
        }, SPLASH_DURATION_MS);
    }

    // פונקציה שיוצרת את ערוץ ההתראות של האפליקציה
    private void createNotificationChannel() {
        // ערוצי התראות נדרשים רק בגרסאות אנדרואיד 8 (API 26) ומעלה
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name); // צריך להוסיף את הטקסט לקבצי התרגום
            String description = getString(R.string.channel_description); // צריך להוסיף את הטקסט לקבצי התרגום
            int importance = NotificationManager.IMPORTANCE_HIGH; // חשיבות גבוהה כדי שההתראה תקפוץ
            NotificationChannel channel = new NotificationChannel(GOAL_NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // רישום הערוץ במערכת. אחרי שקוראים לפונקציה הזו אי אפשר לשנות את הגדרות הערוץ
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // פונקציה שבודקת ומבקשת הרשאת התראות
    private void askForNotificationPermission() {
        // הרשאה זו נדרשת רק בגרסאות אנדרואיד 13 (API 33) ומעלה
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // בודקים אם ההרשאה כבר ניתנה
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // אם כן, פשוט ממשיכים לאפליקציה
                proceedToApp();
            } else {
                // אם לא, מקפיצים את בקשת ההרשאה
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            // בגרסאות ישנות יותר, אין צורך לבקש הרשאה, אז ממשיכים ישר
            proceedToApp();
        }
    }
}