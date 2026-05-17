package com.example.stepup.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.stepup.data.entities.Category;
import com.example.stepup.data.entities.Goal;
import com.example.stepup.data.entities.Reminder;

@Database(entities = {Goal.class, Reminder.class, Category.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract Dao dao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "stepup-db")
                            .fallbackToDestructiveMigration() // הוספנו את זה כדי למנוע קריסות בעדכוני גרסה
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
