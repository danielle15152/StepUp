package com.example.stepup.data;

import android.content.Context;

import com.example.stepup.data.entities.Category;
import com.example.stepup.data.entities.GoalWithReminder;
import com.example.stepup.data.model.GoalProgressStats;

import java.util.List;

/**
 * GoalRepository היא שכבת הגישה לנתונים (Data Layer).
 *
 * מטרתה: לרכז את כל הפעולות על מסד הנתונים במקום אחד,
 * כך שה-ViewModel לא צריך לדעת אם הנתונים מגיעים מה-DB, מהרשת, או ממקום אחר.
 *
 * זהו עיקרון "הפרדת אחריויות" (Separation of Concerns) –
 * כל מחלקה אחראית על דבר אחד בלבד.
 *
 * הכיתה ממומשת כ-Singleton – מופע יחיד לאורך חיי האפליקציה.
 */
public class GoalRepository {

    private final Dao dao;
    private static GoalRepository instance;

    private GoalRepository(Context context) {
        // ApplicationContext מונע זליגת זיכרון (memory leak) של Activity
        dao = AppDatabase.getDatabase(context.getApplicationContext()).dao();
    }

    public static synchronized GoalRepository getInstance(Context context) {
        if (instance == null) {
            instance = new GoalRepository(context);
        }
        return instance;
    }

    public List<GoalWithReminder> getGoalsWithReminders() {
        return dao.getGoalsWithReminders();
    }

    public void deleteGoalWithReminder(long goalId) {
        dao.deleteGoalWithReminder(goalId);
    }

    /**
     * מוודאת שקטגוריות ברירת המחדל קיימות ב-DB.
     * נקראת בפעם הראשונה שהאפליקציה עולה.
     */
    public void ensureDefaultCategories() {
        if (dao.getAllCategories().isEmpty()) {
            dao.insertCategory(new Category("Health", true));
            dao.insertCategory(new Category("Education", true));
            dao.insertCategory(new Category("Sports", true));
            dao.insertCategory(new Category("Finance", true));
        }
    }

    public List<GoalProgressStats> getProgressStatsInRange(long startDate, long endDate) {
        return dao.getProgressStatsInRange(startDate, endDate);
    }
}
