package com.example.stepup.data.entities;

import androidx.room.Embedded;
import androidx.room.Relation;

public class GoalWithReminder {
    @Embedded
    public Goal goal;

    @Relation(
            parentColumn = "id",
            entityColumn = "id"
    )
    public Reminder reminder;
}
