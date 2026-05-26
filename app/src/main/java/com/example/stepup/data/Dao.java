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

    // Goal and Reminder Queries
    @Transaction
    @Query("SELECT * FROM Goal")
    public abstract List<GoalWithReminder> getGoalsWithReminders();
    
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

    @Transaction
    public void updateGoalWithReminder(Goal goal, Reminder reminder) {
        updateGoal(goal);
        updateReminder(reminder);
    }

    // Category Queries
    @Insert
    public abstract long insertCategory(Category category);

    @Query("SELECT * FROM categories")
    public abstract List<Category> getAllCategories();
    
    @Query("SELECT name FROM categories WHERE id = :catId")
    public abstract String getCategoryNameById(long catId);
    
    // Goal Status Queries
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

    // Progress Tracking Query
    @Query("SELECT g.id as goalId, g.name as goalName, " +
           "(SELECT COUNT(*) FROM goal_completions WHERE goalId = g.id AND completionDate BETWEEN :startDate AND :endDate) as completionCount, " +
           "(SELECT COUNT(*) FROM goal_skips WHERE goalId = g.id AND skipDate BETWEEN :startDate AND :endDate) as skipCount " +
           "FROM Goal g")
    public abstract List<GoalProgressStats> getProgressStatsInRange(long startDate, long endDate);
}
