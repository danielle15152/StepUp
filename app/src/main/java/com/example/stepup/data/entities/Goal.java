package com.example.stepup.data.entities;


import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


@Entity()
public class Goal {
    public Goal() {
        // Default constructor for Room
        this.creationDate = Long.parseLong(new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date()));
    }

    @Ignore
    public Goal(String name, String description, boolean active, long categoryId, String notificationType) {
        this.name = name;
        this.description = description;
        this.active = active;
        this.categoryId = categoryId;
        this.notificationType = notificationType;
        this.creationDate = Long.parseLong(new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date()));
    }

    @PrimaryKey(autoGenerate = true)
    public int id;

    /**
     * Name of goal
     */
    public String name;

    /**
     * Description of goal
     */
    public String description;

    /**
     * Is this goal active
     */
    public boolean active;

    /**
     * The category of the goal
     */
    public long categoryId;

    /**
     * The type of notification for the goal (e.g., "GENTLE" or "TOUGH")
     */
    public String notificationType;

    /**
     * The date when the goal was created (e.g., YYYYMMDD).
     */
    public long creationDate;

}
