package com.example.stepup.data.converters;

import androidx.room.TypeConverter;

import java.util.ArrayList;
import java.util.List;

public class DaysListConverter {

    @TypeConverter
    public static String fromDaysList(List<Integer> days) {
        if (days == null || days.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < days.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(days.get(i));
        }
        return sb.toString();
    }

    @TypeConverter
    public static List<Integer> toDaysList(String value) {
        List<Integer> out = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) return out;

        String[] parts = value.split(",");
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) out.add(Integer.parseInt(t));
        }
        return out;
    }
}
