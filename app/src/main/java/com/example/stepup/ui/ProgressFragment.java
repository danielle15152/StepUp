package com.example.stepup.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepup.R;
import com.example.stepup.GoalsActivity; // Added import for GoalsActivity
import com.example.stepup.data.AppDatabase;
import com.example.stepup.data.Dao;
import com.example.stepup.data.model.GoalProgress;
import com.example.stepup.data.model.GoalProgressStats;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ProgressFragment extends Fragment {

    private static final String TAG = "ProgressFragment";

    private Dao dao;
    private ProgressAdapter adapter;
    private RecyclerView recyclerView;
    private ChipGroup chipGroupDateRange;
    private MaterialToolbar toolbar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_progress, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dao = AppDatabase.getDatabase(requireContext()).dao();

        toolbar = view.findViewById(R.id.toolbar);
        setupToolbar();

        recyclerView = view.findViewById(R.id.rvProgress);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ProgressAdapter();
        recyclerView.setAdapter(adapter);

        chipGroupDateRange = view.findViewById(R.id.chipGroupDateRange);
        chipGroupDateRange.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chip_week) {
                    loadProgressForDateRange(DateRange.WEEK);
                } else if (checkedId == R.id.chip_month) {
                    loadProgressForDateRange(DateRange.MONTH);
                } else if (checkedId == R.id.chip_quarter) {
                    loadProgressForDateRange(DateRange.QUARTER);
                }
            } else {
                chipGroupDateRange.check(R.id.chip_week);
            }
        });

        chipGroupDateRange.check(R.id.chip_week);
        loadProgressForDateRange(DateRange.WEEK);
    }

    private void setupToolbar() {
        if (getActivity() instanceof GoalsActivity) {
            ((GoalsActivity) getActivity()).setSupportActionBar(toolbar);
            ((GoalsActivity) getActivity()).getSupportActionBar().setTitle("Progress Report");
        }
    }

    private void loadProgressForDateRange(DateRange range) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
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
                    calculationEndCalendar.set(Calendar.DAY_OF_MONTH, calculationEndCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                    break;
                case QUARTER:
                    startCalendar.add(Calendar.MONTH, -2);
                    startCalendar.set(Calendar.DAY_OF_MONTH, 1);
                    calculationEndCalendar = Calendar.getInstance();
                    calculationEndCalendar.set(Calendar.DAY_OF_MONTH, calculationEndCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
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

            long startDateLong = Long.parseLong(new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(startCalendar.getTime()));
            long queryEndDateLong = Long.parseLong(new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(queryEndCalendar.getTime()));
            
            List<GoalProgressStats> statsList = dao.getProgressStatsInRange(startDateLong, queryEndDateLong);
            List<GoalProgress> goalProgressList = new ArrayList<>();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

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
                    if (stats.reminderDays == null || stats.reminderDays.isEmpty() || stats.reminderDays.contains(dayOfWeek)) {
                        totalPotentialDays++;
                    }
                    currentDay.add(Calendar.DAY_OF_YEAR, 1);
                }

                goalProgressList.add(new GoalProgress(stats.goalName, stats.completionCount, totalPotentialDays));
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    adapter.setItems(goalProgressList);
                });
            }
        });
    }

    private enum DateRange {
        WEEK, MONTH, QUARTER
    }
}
