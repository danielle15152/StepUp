package com.example.stepup.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepup.R;
import com.google.android.material.chip.ChipGroup;

public class ProgressFragment extends Fragment {

    // ViewModel מכילה את כל הלוגיקה וה-DateRange enum
    private ProgressViewModel viewModel;

    private ProgressAdapter adapter;
    private RecyclerView recyclerView;
    private ChipGroup chipGroupDateRange;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_progress, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ProgressViewModel.class);

        recyclerView = view.findViewById(R.id.rvProgress);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ProgressAdapter();
        recyclerView.setAdapter(adapter);

        // כשה-LiveData מתעדכן, ה-Fragment מציג את הרשימה החדשה
        viewModel.getProgressLiveData().observe(getViewLifecycleOwner(), goalProgressList -> {
            adapter.setItems(goalProgressList);
            recyclerView.scheduleLayoutAnimation();
        });

        chipGroupDateRange = view.findViewById(R.id.chipGroupDateRange);
        chipGroupDateRange.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chip_week) {
                    viewModel.loadProgressForDateRange(ProgressViewModel.DateRange.WEEK);
                } else if (checkedId == R.id.chip_month) {
                    viewModel.loadProgressForDateRange(ProgressViewModel.DateRange.MONTH);
                } else if (checkedId == R.id.chip_quarter) {
                    viewModel.loadProgressForDateRange(ProgressViewModel.DateRange.QUARTER);
                }
            } else {
                chipGroupDateRange.check(R.id.chip_week);
            }
        });

        chipGroupDateRange.check(R.id.chip_week);
        viewModel.loadProgressForDateRange(ProgressViewModel.DateRange.WEEK);
    }
}
