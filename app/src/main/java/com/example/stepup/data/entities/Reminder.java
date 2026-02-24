package com.example.stepup.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import java.util.Date;
import java.util.List;

@Entity(
        foreignKeys = @ForeignKey(
                entity = Goal.class,
                parentColumns = "id",
                childColumns = "id",
                onDelete = ForeignKey.CASCADE
        )
)
public class Reminder {

    public Reminder() {}

    public Reminder(List<Integer> days, int minuteOfDay, Date lastReminder) {
        this.days = days;
        this.minuteOfDay = minuteOfDay;
        this.lastReminder = lastReminder;
    }

    @PrimaryKey()
    public int id; //will map to goal id

    /**
     * List of days (0 based, sunday =0) that the reminder should fire on
     */
    public List<Integer> days;

    /**
     * The time of day (as minutes since midnight) the reminder should fire
     */
    public int minuteOfDay;

    /**
     * The last time this reminder fired
     */
    public Date lastReminder;

}
