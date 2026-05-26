package com.example.stepup.data.model;

import java.util.List;

// Plain Old Java Object (POJO) to hold the results of our complex query.
// Room will map the query results into a list of these objects.
public class GoalProgressStats {
    public int goalId;
    public String goalName;
    public long creationDate; // Added creation date of the goal
    public List<Integer> reminderDays; // Added reminder days for accurate calculation
    public int completionCount;
    public int skipCount;
}
