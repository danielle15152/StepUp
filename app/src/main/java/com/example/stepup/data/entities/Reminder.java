package com.example.stepup.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import java.util.List;

@Entity(
        foreignKeys = @ForeignKey(
                entity = Goal.class,
                parentColumns = "id",
                childColumns = "id",
                onDelete = ForeignKey.CASCADE
        )
)
public class Reminder {

    @PrimaryKey
    public int id; // Will map to goal id

    /**
     * List of days (0-6, Sunday=0) for time-based reminders.
     */
    public List<Integer> days;

    /**
     * The time of day (as minutes since midnight) for time-based reminders.
     * Can be null for location-based reminders.
     */
    public Integer minuteOfDay;

    /**
     * The latitude for location-based reminders.
     * Null if this is a time-based reminder.
     */
    public Double latitude;

    /**
     * The longitude for location-based reminders.
     * Null if this is a time-based reminder.
     */
    public Double longitude;

    /**
     * The timestamp (in millis) of the last time a location-based notification was sent.
     * Used to prevent sending multiple notifications on the same day.
     */
    public long lastLocationNotificationTimestamp;

    /**
     * שם המיקום בפורמט קריא (לדוגמה: "הרצל 12, רמת גן").
     * נקבע אוטומטית ב-MapPickerActivity דרך reverse geocoding.
     * יכול להיות null אם:
     *  - זו תזכורת מבוססת שעה (לא מיקום)
     *  - ה-reverse geocoding נכשל (אין רשת/לא נמצאה כתובת)
     * במקרה כזה ה-UI יציג את הקואורדינטות במקום.
     */
    public String locationName;


    // Room needs a constructor for initialization
    public Reminder() {}
}
