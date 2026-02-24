// File: SeedDataHelper.java
package com.example.stepup.data;

import com.example.stepup.data.entities.Goal;
import com.example.stepup.data.entities.Reminder;

import java.util.Arrays;
import java.util.List;

public final class SeedDataHelper {

    private SeedDataHelper() {}

    /**
     * Populates the DB with 15 Goals using Dao.insertGoalWithReminder(goal, reminder).
     * NOTE: This should be called from a background thread (Room doesn't allow DB on main thread by default).
     */
    public static void populateWith15Goals(Dao dao) {

        // 1
        dao.insertGoalWithReminder(
                new Goal("Wake up at 06:30", "Start the day early", true, "Health"),
                new Reminder(days(0, 1, 2, 3, 4, 5, 6), minute(6, 30), null)
        );

        // 2
        dao.insertGoalWithReminder(
                new Goal("Drink water", "2 liters a day", true, "Health"),
                new Reminder(days(0, 2, 4, 6), minute(10, 0), null)
        );

        // 3
        dao.insertGoalWithReminder(
                new Goal("Workout", "Strength or cardio session", true, "Fitness"),
                new Reminder(days(1, 3, 5), minute(18, 0), null)
        );

        // 4
        dao.insertGoalWithReminder(
                new Goal("Read 20 pages", "Any book counts", true, "Learning"),
                new Reminder(days(0, 1, 2, 3, 4, 5, 6), minute(21, 0), null)
        );

        // 5
        dao.insertGoalWithReminder(
                new Goal("No sugar today", "Avoid candy & soft drinks", true, "Discipline"),
                new Reminder(days(0, 1, 2, 3, 4, 5, 6), minute(9, 0), null)
        );

        // 6
        dao.insertGoalWithReminder(
                new Goal("Study / coding", "Practice coding for 45 minutes", true, "Learning"),
                new Reminder(days(0, 1, 2, 3, 4), minute(19, 30), null)
        );

        // 7
        dao.insertGoalWithReminder(
                new Goal("Meditate", "10 minutes breathing", true, "Mind"),
                new Reminder(days(0, 1, 2, 3, 4, 5, 6), minute(7, 15), null)
        );

        // 8
        dao.insertGoalWithReminder(
                new Goal("Stretch", "5 minutes stretching", true, "Fitness"),
                new Reminder(days(0, 2, 4), minute(8, 0), null)
        );

        // 9
        dao.insertGoalWithReminder(
                new Goal("Walk 8,000 steps", "Daily steps goal", true, "Health"),
                new Reminder(days(0, 1, 2, 3, 4, 5, 6), minute(16, 0), null)
        );

        // 10
        dao.insertGoalWithReminder(
                new Goal("Journal", "Write 3 lines about the day", true, "Mind"),
                new Reminder(days(0, 1, 2, 3, 4, 5, 6), minute(22, 15), null)
        );

        // 11
        dao.insertGoalWithReminder(
                new Goal("Save money", "Put aside some money weekly", true, "Finance"),
                new Reminder(days(5), minute(12, 0), null) // Friday at 12:00
        );

        // 12
        dao.insertGoalWithReminder(
                new Goal("Clean room", "Quick 10-minute clean", true, "Habits"),
                new Reminder(days(6), minute(11, 0), null) // Saturday at 11:00
        );

        // 13
        dao.insertGoalWithReminder(
                new Goal("Healthy lunch", "Protein + veggies", true, "Nutrition"),
                new Reminder(days(0, 1, 2, 3, 4), minute(13, 0), null)
        );

        // 14
        dao.insertGoalWithReminder(
                new Goal("Limit screen time", "No scrolling after 23:00", true, "Discipline"),
                new Reminder(days(0, 1, 2, 3, 4, 5, 6), minute(23, 0), null)
        );

        // 15
        dao.insertGoalWithReminder(
                new Goal("Call family", "Check in with someone you care about", true, "Social"),
                new Reminder(days(0, 3), minute(20, 0), null) // Sunday + Wednesday 20:00
        );
    }

    // --------- Small builders ---------

    /** Sunday=0 ... Saturday=6 */
    private static List<Integer> days(Integer... d) {
        return Arrays.asList(d);
    }

    /** minutes since midnight; e.g. 06:30 -> 390 */
    private static int minute(int hour24, int minute) {
        return (hour24 * 60) + minute;
    }
}
