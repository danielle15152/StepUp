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

@Database(
        entities = {Goal.class, Reminder.class, Category.class, GoalCompletion.class, GoalSkip.class},
        version = 10,
        exportSchema = false)
@TypeConverters({Converters.class, DaysListConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract Dao dao();

    private static volatile AppDatabase INSTANCE;

    /**
     * Migration ידנית מגרסת DB 9 ל-10.
     *
     * השינוי היחיד: הוספת עמודת locationName ל-Reminder.
     * זה שם המיקום הקריא (לדוגמה "הרצל 12, רמת גן"), שנקבע
     * דרך reverse geocoding ב-MapPickerActivity.
     *
     * ALTER TABLE עם ADD COLUMN שומר על כל הנתונים הקיימים -
     * רק העמודה החדשה תהיה NULL במטרות שנוצרו לפני העדכון.
     *
     * עטיפת try/catch מטפלת במקרה שהעמודה כבר קיימת (אם פעם הרצנו
     * destructive migration לפני שכתבנו את ה-Migration הזה).
     */
    static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            try {
                db.execSQL("ALTER TABLE Reminder ADD COLUMN locationName TEXT");
            } catch (Exception e) {
                // העמודה כבר קיימת - בסדר, ממשיכים.
            }
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "stepup-db")
                            // ה-Migration המסודר. רץ פעם אחת בלבד -
                            // כשעוברים מגרסת DB 9 ל-10.
                            .addMigrations(MIGRATION_9_10)
                            // safety net: אם משהו לא צפוי קורה (downgrade,
                            // mismatch schema לא ידוע), נעדיף destructive
                            // על קריסה. בייצור עדיף בלי, אבל לפרוייקט בית-ספרי
                            // עדיף שזה לא יקרוס.
                            .fallbackToDestructiveMigrationOnDowngrade()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
