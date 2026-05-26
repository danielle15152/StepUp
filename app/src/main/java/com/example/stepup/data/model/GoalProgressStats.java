package com.example.stepup.data.model;

// Plain Old Java Object (POJO) to hold the results of our complex query.
// Room will map the query results into a list of these objects.
public class GoalProgressStats {
    public int goalId;
    public String goalName;
    public int completionCount;
    public int skipCount;
}
