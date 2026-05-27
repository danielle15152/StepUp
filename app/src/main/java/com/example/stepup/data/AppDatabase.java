package com.example.stepup.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.stepup.data.entities.Category;
import com.example.stepup.data.entities.Goal;
import com.example.stepup.data.entities.GoalCompletion;
import com.example.stepup.data.entities.GoalSkip;
import com.example.stepup.data.entities.Reminder;
import com.example.stepup.data.converters.DaysListConverter;

// בסיס הנתונים של האפליקציה. עובד עם Room - ה-ORM של אנדרואיד שעוטף את SQLite.
@Database(
        entities = {Goal.class, Reminder.class, Category.class, GoalCompletion.class, GoalSkip.class},
        version = 10,
        exportSchema = false)
@TypeConverters({Converters.class, DaysListConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract Dao dao();

    // משתמשים ב-Singleton כי פתיחת DB היא פעולה יקרה ויכולה לגרום לבעיות סנכרון
    // אם נפתח את ה-DB ביותר ממקום אחד בו זמנית
    private static volatile AppDatabase INSTANCE;

    // Migration מגרסה 9 ל-10: הוספת עמודת locationName ל-Reminder (שם המקום הקריא,
    // לדוגמה "הרצל 12, רמת גן"). ה-try/catch למקרה שכבר נעשה destructive migration קודם.
    static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            try {
                db.execSQL("ALTER TABLE Reminder ADD COLUMN locationName TEXT");
            } catch (Exception e) {
                // העמודה כבר קיימת
            }
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        // Double-checked locking - בודקים פעמיים את INSTANCE עם synchronized באמצע,
        // כדי שאם שני threads קוראים בו זמנית רק אחד יצור את ה-DB
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "stepup-db")
                            .addMigrations(MIGRATION_9_10)
                            // אם המשתמשת מתקינה גרסה ישנה יותר, נמחק את ה-DB במקום לקרוס
                            .fallbackToDestructiveMigrationOnDowngrade()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
