package com.example.stepup.ui;

import android.os.Bundle;
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
                // Default to week if nothing is selected (prevents empty state)
                chipGroupDateRange.check(R.id.chip_week);
            }
        });

        // Set initial chip state and load data
        chipGroupDateRange.check(R.id.chip_week); // Ensure the chip is checked visually
        loadProgressForDateRange(DateRange.WEEK); // Explicitly load data for the default range
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
            Calendar endCalendar = Calendar.getInstance();

            // Set end date to end of today
            endCalendar.set(Calendar.HOUR_OF_DAY, 23);
            endCalendar.set(Calendar.MINUTE, 59);
            endCalendar.set(Calendar.SECOND, 59);
            endCalendar.set(Calendar.MILLISECOND, 999);

            switch (range) {
                case WEEK:
                    // Start of this week (Monday if week starts on Monday, etc.)
                    startCalendar.set(Calendar.DAY_OF_WEEK, startCalendar.getFirstDayOfWeek());
                    break;
                case MONTH:
                    // Start of this month
                    startCalendar.set(Calendar.DAY_OF_MONTH, 1);
                    break;
                case QUARTER:
                    // Start of 3 months ago
                    startCalendar.add(Calendar.MONTH, -2); // Go back 2 full months to get to the start of the 3-month period
                    startCalendar.set(Calendar.DAY_OF_MONTH, 1);
                    break;
            }

            // Set start date to beginning of the day
            startCalendar.set(Calendar.HOUR_OF_DAY, 0);
            startCalendar.set(Calendar.MINUTE, 0);
            startCalendar.set(Calendar.SECOND, 0);
            startCalendar.set(Calendar.MILLISECOND, 0);

            long startDateLong = Long.parseLong(new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(startCalendar.getTime()));
            long endDateLong = Long.parseLong(new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(endCalendar.getTime()));
            
            List<GoalProgressStats> statsList = dao.getProgressStatsInRange(startDateLong, endDateLong);
            List<GoalProgress> goalProgressList = new ArrayList<>();

            for (GoalProgressStats stats : statsList) {
                int totalPotentialDays = 0;
                Calendar currentDay = (Calendar) startCalendar.clone();
                
                // Iterate through each day in the range
                while (!currentDay.after(endCalendar)) {
                    long currentDayLong = Long.parseLong(new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(currentDay.getTime()));

                    // Check if the goal existed on this day
                    if (currentDayLong >= stats.creationDate) {
                        // Check if the goal is set for this day of the week
                        // Calendar.DAY_OF_WEEK returns SUNDAY=1, MONDAY=2, ... SATURDAY=7
                        // Our reminderDays are 0-6 (Sun-Sat)
                        int dayOfWeek = currentDay.get(Calendar.DAY_OF_WEEK) - 1; // Adjust to 0-6 range
                        if (stats.reminderDays == null || stats.reminderDays.isEmpty() || stats.reminderDays.contains(dayOfWeek)) {
                            totalPotentialDays++;
                        }
                    }
                    currentDay.add(Calendar.DAY_OF_YEAR, 1);
                }

                goalProgressList.add(new GoalProgress(stats.goalName, stats.completionCount, totalPotentialDays));
            }

            requireActivity().runOnUiThread(() -> {
                adapter.setItems(goalProgressList);
            });
        });
    }

    private enum DateRange {
        WEEK, MONTH, QUARTER
    }
}
