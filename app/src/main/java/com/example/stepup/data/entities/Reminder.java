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
    public int id; // מזהה זהה למזהה המטרה

    public List<Integer> days;

    public Integer minuteOfDay;

    public Double latitude;

    public Double longitude;

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

    public Reminder() {}
}
