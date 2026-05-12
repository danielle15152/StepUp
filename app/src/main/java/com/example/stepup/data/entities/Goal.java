package com.example.stepup.data.entities;


import androidx.room.Entity;
import androidx.room.PrimaryKey;



@Entity()
public class Goal {
    public Goal() {}

    public Goal(String name, String description, boolean active,  long categoryId, String notificationType) {
        this.name = name;
        this.description = description;
        this.active = active;
        this.categoryId = categoryId;
        this.notificationType = notificationType;
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


}