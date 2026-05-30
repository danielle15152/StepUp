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

    public String name;

    public String description;

    public boolean active;

    public long categoryId;

    public String notificationType;

    public long creationDate;

}
