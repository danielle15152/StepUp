package com.example.stepup.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.stepup.data.GoalRepository;
import com.example.stepup.data.model.GoalProgress;
import com.example.stepup.data.model.GoalProgressStats;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * ProgressViewModel אחראית על לוגיקת חישוב ההתקדמות.
 *
 * הלוגיקה עברה לכאן מ-ProgressFragment –
 * Fragment אמור רק להציג נתונים, לא לחשב אותם.
 * זהו עיקרון MVVM: Model-View-ViewModel.
 *
 * הטיפוס DateRange הועבר לכאן כי הוא שייך ללוגיקה, לא לתצוגה.
 */
public class ProgressViewModel extends AndroidViewModel {

    public enum DateRange {
        WEEK, MONTH, QUARTER
    }

    private final GoalRepository repository;
    private final MutableLiveData<List<GoalProgress>> progressLiveData = new MutableLiveData<>();

    public ProgressViewModel(@NonNull Application application) {
        super(application);
        repository = GoalRepository.getInstance(application);
    }

    public LiveData<List<GoalProgress>> getProgressLiveData() {
        return progressLiveData;
    }

    public void loadProgressForDateRange(DateRange range) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Calendar startCalendar = Calendar.getInstance();
            Calendar queryEndCalendar = Calendar.getInstance();
            Calendar calculationEndCalendar;

            queryEndCalendar.set(Calendar.HOUR_OF_DAY, 23);
            queryEndCalendar.set(Calendar.MINUTE, 59);
            queryEndCalendar.set(Calendar.SECOND, 59);
            queryEndCalendar.set(Calendar.MILLISECOND, 999);

            switch (range) {
                case WEEK:
                    startCalendar.set(Calendar.DAY_OF_WEEK, startCalendar.getFirstDayOfWeek());
                    calculationEndCalendar = (Calendar) startCalendar.clone();
                    calculationEndCalendar.add(Calendar.DAY_OF_YEAR, 6);
                    break;
                case MONTH:
                    startCalendar.set(Calendar.DAY_OF_MONTH, 1);
                    calculationEndCalendar = (Calendar) startCalendar.clone();
                    calculationEndCalendar.set(Calendar.DAY_OF_MONTH,
                            calculationEndCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                    break;
                case QUARTER:
                    startCalendar.add(Calendar.MONTH, -2);
                    startCalendar.set(Calendar.DAY_OF_MONTH, 1);
                    calculationEndCalendar = Calendar.getInstance();
                    calculationEndCalendar.set(Calendar.DAY_OF_MONTH,
                            calculationEndCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                    break;
                default:
                    calculationEndCalendar = (Calendar) queryEndCalendar.clone();
                    break;
            }

            startCalendar.set(Calendar.HOUR_OF_DAY, 0);
            startCalendar.set(Calendar.MINUTE, 0);
            startCalendar.set(Calendar.SECOND, 0);
            startCalendar.set(Calendar.MILLISECOND, 0);

            calculationEndCalendar.set(Calendar.HOUR_OF_DAY, 23);
            calculationEndCalendar.set(Calendar.MINUTE, 59);
            calculationEndCalendar.set(Calendar.SECOND, 59);
            calculationEndCalendar.set(Calendar.MILLISECOND, 999);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            long startDateLong = Long.parseLong(sdf.format(startCalendar.getTime()));
            long queryEndDateLong = Long.parseLong(sdf.format(queryEndCalendar.getTime()));

            List<GoalProgressStats> statsList = repository.getProgressStatsInRange(startDateLong, queryEndDateLong);
            List<GoalProgress> goalProgressList = new ArrayList<>();

            for (GoalProgressStats stats : statsList) {
                int totalPotentialDays = 0;

                Calendar actualRangeStartForGoal = (Calendar) startCalendar.clone();
                try {
                    Calendar goalCreationCalendar = Calendar.getInstance();
                    goalCreationCalendar.setTime(sdf.parse(String.valueOf(stats.creationDate)));
                    goalCreationCalendar.set(Calendar.HOUR_OF_DAY, 0);
                    goalCreationCalendar.set(Calendar.MINUTE, 0);
                    goalCreationCalendar.set(Calendar.SECOND, 0);
                    goalCreationCalendar.set(Calendar.MILLISECOND, 0);

                    if (actualRangeStartForGoal.before(goalCreationCalendar)) {
                        actualRangeStartForGoal = goalCreationCalendar;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                Calendar currentDay = (Calendar) actualRangeStartForGoal.clone();
                while (!currentDay.after(calculationEndCalendar)) {
                    int dayOfWeek = currentDay.get(Calendar.DAY_OF_WEEK) - 1;
                    if (stats.reminderDays == null || stats.reminderDays.isEmpty()
                            || stats.reminderDays.contains(dayOfWeek)) {
                        totalPotentialDays++;
                    }
                    currentDay.add(Calendar.DAY_OF_YEAR, 1);
                }

                goalProgressList.add(new GoalProgress(stats.goalName, stats.completionCount, totalPotentialDays));
            }

            progressLiveData.postValue(goalProgressList);
        });
    }
}
