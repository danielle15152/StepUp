package com.example.stepup.data;

import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.stepup.data.entities.Goal;
import com.example.stepup.data.entities.GoalWithReminder;
import com.example.stepup.data.entities.Reminder;
import com.example.stepup.data.entities.Category;
import java.util.List;

@androidx.room.Dao
public abstract class Dao {

    @Query("SELECT COUNT(*) FROM Goal")
    public abstract int countGoals();
    @Transaction
    @Query("SELECT * FROM Goal")
    public abstract List<GoalWithReminder> getGoalsWithReminders();

    @Transaction
    @Query("SELECT * FROM Goal WHERE id = :goalId")
    public abstract GoalWithReminder getGoalWithReminderById(long goalId);

    @Query("DELETE FROM Reminder WHERE id = :goalId")
    public abstract void deleteReminderByGoalId(long goalId);

    @Query("DELETE FROM Goal WHERE id = :goalId")
    public abstract void deleteGoalById(long goalId);

    @androidx.room.Transaction
    public void deleteGoalWithReminder(long goalId) {
        deleteReminderByGoalId(goalId);
        deleteGoalById(goalId);
    }
    @Insert
    public abstract long insertGoal(Goal goal);

    @Insert
    public abstract void insertReminder(Reminder reminder);

    @Transaction
    public void insertGoalWithReminder(Goal goal, Reminder reminder) {

        // 1️⃣ Insert Goal first
        long goalId = insertGoal(goal);

        // 2️⃣ Set foreign key
        reminder.id = (int) goalId;

        // 3️⃣ Insert Reminder
        insertReminder(reminder);
    }
    @Query("SELECT * FROM Goal WHERE id = :goalId LIMIT 1")
    public abstract Goal getGoalById(long goalId);

    @Update
    public abstract void updateGoal(Goal goal);

    @Update
    public abstract void updateReminder(Reminder reminder);

    @Transaction
    public void updateGoalWithReminder(Goal goal, Reminder reminder) {
        updateGoal(goal);
        updateReminder(reminder);
    }


    @Insert
    public abstract long insertCategory(Category category);

    @Query("SELECT * FROM categories")
    public abstract List<Category> getAllCategories();

    @Query("SELECT * FROM categories WHERE isDefault = 1")
    public abstract List<Category> getDefaultCategories();

    @Query("SELECT name FROM categories WHERE id = :catId")
    public abstract String getCategoryNameById(long catId);
}
