package com.example.stepup.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(tableName = "goal_skips",
        primaryKeys = {"goalId", "skipDate"},
        foreignKeys = @ForeignKey(entity = Goal.class,
                                  parentColumns = "id",
                                  childColumns = "goalId",
                                  onDelete = ForeignKey.CASCADE),
        indices = {@Index("goalId")})
public class GoalSkip {

    public int goalId;

    /**
     * The date of the skip, stored as a long representing the day (e.g., YYYYMMDD).
     */
    public long skipDate;

    public GoalSkip(int goalId, long skipDate) {
        this.goalId = goalId;
        this.skipDate = skipDate;
    }
}
