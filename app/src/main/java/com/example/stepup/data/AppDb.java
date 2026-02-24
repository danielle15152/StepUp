package com.example.stepup.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.stepup.data.converters.DateConverter;
import com.example.stepup.data.converters.DaysListConverter;
import com.example.stepup.data.entities.Goal;
import com.example.stepup.data.entities.Reminder;

@Database(entities = {Goal.class, Reminder.class}, version = 1)
@TypeConverters({DaysListConverter.class, DateConverter.class})
public abstract class AppDb extends RoomDatabase {
    public abstract Dao dao();
}
