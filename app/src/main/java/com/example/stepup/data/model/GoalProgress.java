package com.example.stepup.data.model;

public class GoalProgress {
    private final String goalName;
    private final int completions;
    private final int totalDays;

    public GoalProgress(String goalName, int completions, int totalDays) {
        this.goalName = goalName;
        this.completions = completions;
        this.totalDays = totalDays;
    }

    public String getGoalName() {
        return goalName;
    }

    public int getCompletions() {
        return completions;
    }

    public int getTotalDays() {
        return totalDays;
    }
}
