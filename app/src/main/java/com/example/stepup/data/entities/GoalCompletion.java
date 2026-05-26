package com.example.stepup.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(tableName = "goal_completions",
        primaryKeys = {"goalId", "completionDate"},
        foreignKeys = @ForeignKey(entity = Goal.class,
                                  parentColumns = "id",
                                  childColumns = "goalId",
                                  onDelete = ForeignKey.CASCADE),
        indices = {@Index("goalId")})
public class GoalCompletion {

    public int goalId;

    /**
     * The date of completion, stored as a long representing the day (e.g., YYYYMMDD).
     */
    public long completionDate;

    public GoalCompletion(int goalId, long completionDate) {
        this.goalId = goalId;
        this.completionDate = completionDate;
    }
}
