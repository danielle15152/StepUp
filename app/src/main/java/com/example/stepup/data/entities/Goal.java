package com.example.stepup.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity()
public class Goal {
    public Goal() {}

    public Goal(String name, String description, boolean active, String category) {
        this.name = name;
        this.description = description;
        this.active = active;
        this.category = category;
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
    public String category;


}
