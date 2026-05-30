package com.example.stepup.data;

import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.stepup.data.entities.Category;
import com.example.stepup.data.entities.Goal;
import com.example.stepup.data.entities.GoalCompletion;
import com.example.stepup.data.entities.GoalSkip;
import com.example.stepup.data.entities.GoalWithReminder;
import com.example.stepup.data.entities.Reminder;
import com.example.stepup.data.model.GoalProgressStats;

import java.util.List;

@androidx.room.Dao
public abstract class Dao {

    // שאילתות מטרות ותזכורות
    @Transaction
    @Query("SELECT * FROM Goal")
    public abstract List<GoalWithReminder> getGoalsWithReminders();

    @Transaction
    @Query("SELECT * FROM Goal g JOIN Reminder r ON g.id = r.id WHERE r.latitude IS NOT NULL AND r.longitude IS NOT NULL")
    public abstract List<GoalWithReminder> getGoalsWithLocationReminders();

    @Query("SELECT * FROM Goal")
    public abstract List<Goal> getAllGoals();

    @Transaction
    @Query("SELECT * FROM Goal WHERE id = :goalId")
    public abstract GoalWithReminder getGoalWithReminderById(long goalId);

    @Query("SELECT * FROM Goal WHERE id = :goalId LIMIT 1")
    public abstract Goal getGoalById(long goalId);

    @Transaction
    public void deleteGoalWithReminder(long goalId) {
        deleteReminderByGoalId(goalId);
        deleteGoalById(goalId);
    }

    @Query("DELETE FROM Reminder WHERE id = :goalId")
    protected abstract void deleteReminderByGoalId(long goalId);

    @Query("DELETE FROM Goal WHERE id = :goalId")
    protected abstract void deleteGoalById(long goalId);

    @Insert
    protected abstract long insertGoal(Goal goal);

    @Insert
    protected abstract void insertReminder(Reminder reminder);

    @Transaction
    public long insertGoalWithReminder(Goal goal, Reminder reminder) {
        long goalId = insertGoal(goal);
        reminder.id = (int) goalId;
        insertReminder(reminder);
        return goalId;
    }

    @Update
    public abstract void updateGoal(Goal goal);

    @Update
    public abstract void updateReminder(Reminder reminder);

    // INSERT OR REPLACE – משמש ב-updateGoalWithReminder כדי לטפל במקרה
    // שה-Reminder נמחק מהDB מסיבה כלשהי. @Update היה מחזיר 0 בשקט בלי לזרוק
    // exception, כך שה-@Transaction לא היה יכול להגן. REPLACE מבטיח שה-Reminder
    // תמיד יקים/יתעדכן בתוך אותה טרנזקציה.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract void upsertReminder(Reminder reminder);

    @Transaction
    public void updateGoalWithReminder(Goal goal, Reminder reminder) {
        updateGoal(goal);
        upsertReminder(reminder);   // upsert ולא update – בטוח גם אם Reminder חסר
    }

    // שאילתות קטגוריות
    @Insert
    public abstract long insertCategory(Category category);

    @Query("SELECT * FROM categories")
    public abstract List<Category> getAllCategories();

    @Query("SELECT name FROM categories WHERE id = :catId")
    public abstract String getCategoryNameById(long catId);

    // שאילתות סטטוס מטרה
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertCompletion(GoalCompletion completion);

    @Query("DELETE FROM goal_completions WHERE goalId = :goalId AND completionDate = :date")
    public abstract void deleteCompletion(int goalId, long date);

    @Query("SELECT * FROM goal_completions WHERE goalId = :goalId AND completionDate = :date")
    public abstract GoalCompletion getCompletion(int goalId, long date);

    @Query("SELECT * FROM goal_completions WHERE completionDate = :date")
    public abstract List<GoalCompletion> getCompletionsForDate(long date);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertSkip(GoalSkip skip);

    @Query("DELETE FROM goal_skips WHERE goalId = :goalId AND skipDate = :date")
    public abstract void deleteSkip(int goalId, long date);



    @Query("SELECT * FROM goal_skips WHERE goalId = :goalId AND skipDate = :date")
    public abstract GoalSkip getSkip(int goalId, long date);

    // שאילתת מעקב התקדמות
    @Query("SELECT g.id as goalId, g.name as goalName, g.creationDate as creationDate, r.days as reminderDays, " +
           "(SELECT COUNT(*) FROM goal_completions WHERE goalId = g.id AND completionDate BETWEEN :startDate AND :endDate) as completionCount, " +
           "(SELECT COUNT(*) FROM goal_skips WHERE goalId = g.id AND skipDate BETWEEN :startDate AND :endDate) as skipCount " +
           "FROM Goal g LEFT JOIN Reminder r ON g.id = r.id")
    public abstract List<GoalProgressStats> getProgressStatsInRange(long startDate, long endDate);
}
