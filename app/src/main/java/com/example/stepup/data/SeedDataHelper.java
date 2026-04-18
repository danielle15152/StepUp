package com.example.stepup.data;

import com.example.stepup.data.entities.Goal;
import com.example.stepup.data.entities.Reminder;
import java.util.Arrays;
import java.util.List;

public final class SeedDataHelper {

    private SeedDataHelper() {}

    /**
     * הפונקציה הזו כרגע ריקה כדי למנוע שגיאות קומפילציה
     * לאחר ששינינו את מבנה הקטגוריות ב-Goal.
     */
    public static void populateWith15Goals(Dao dao) {
        // לא עושים כלום - המטרות הישנות לא תואמות למבנה החדש
    }

    private static List<Integer> days(Integer... d) {
        return Arrays.asList(d);
    }

    private static int minute(int hour24, int minute) {
        return (hour24 * 60) + minute;
    }
}