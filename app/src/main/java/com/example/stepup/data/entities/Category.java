package com.example.stepup.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class Category {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;
    public boolean isDefault;

    public Category() {}

    public Category(String name, boolean isDefault) {
        this.name = name;
        this.isDefault = isDefault;
    }
}
