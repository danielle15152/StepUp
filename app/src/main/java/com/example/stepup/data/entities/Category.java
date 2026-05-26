package com.example.stepup.data.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class Category {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;
    public boolean isDefault;

    public Category() {}

    @Ignore
    public Category(String name, boolean isDefault) {
        this.name = name;
        this.isDefault = isDefault;
    }
}
