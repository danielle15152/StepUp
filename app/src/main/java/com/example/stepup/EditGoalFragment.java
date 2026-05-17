package com.example.stepup;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.stepup.data.NotificationScheduler;
import com.example.stepup.data.entities.GoalWithReminder;
import com.example.stepup.data.entities.Reminder;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import com.example.stepup.data.Dao;
import com.example.stepup.data.entities.Goal;
import com.example.stepup.data.entities.Category;

public class EditGoalFragment extends Fragment {

    private static final String ARG_GOAL_ID = "goal_id";
    private static final String TAG = "NotificationFlow"; // תג לסינון הלוגים


    public static EditGoalFragment newInstance(long goalId) {
        EditGoalFragment f = new EditGoalFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_GOAL_ID, goalId);
        f.setArguments(b);
        return f;
    }

    private long goalId;
    private Dao dao;
    private GoalWithReminder currentGoalWithReminder;

    // רכיבי UI
    private EditText etName, etDescription;
    private Spinner spinnerCategory;
    private RadioGroup rgNotificationType;
    private TextView tvTimePicker;
    private ChipGroup cgDays;

    private List<Category> categoriesList;
    private int selectedHour = 8; // ברירת מחדל
    private int selectedMinute = 0; // ברירת מחדל

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_goal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        goalId = requireArguments().getLong(ARG_GOAL_ID);
        dao = ((GoalsActivity) requireActivity()).getDao();

        // קישור רכיבי ה-UI
        etName = view.findViewById(R.id.etName);
        etDescription = view.findViewById(R.id.etDescription);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        rgNotificationType = view.findViewById(R.id.rgNotificationType);
        tvTimePicker = view.findViewById(R.id.tvTimePicker);
        cgDays = view.findViewById(R.id.cgDays);

        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSave = view.findViewById(R.id.btnSave);

        // הגדרת מאזינים
        tvTimePicker.setOnClickListener(v -> showTimePickerDialog());
        btnCancel.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        btnSave.setOnClickListener(v -> saveGoal());

        update_time_text();
        loadCategories();
    }

    private void showTimePickerDialog() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                getContext(),
                (view, hourOfDay, minute) -> {
                    selectedHour = hourOfDay;
                    selectedMinute = minute;
                    update_time_text();
                },
                selectedHour,
                selectedMinute,
                true // 24-hour format
        );
        timePickerDialog.show();
    }

    private void update_time_text() {
        tvTimePicker.setText(String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute));
    }

    private void loadCategories() {
        Executors.newSingleThreadExecutor().execute(() -> {
            categoriesList = dao.getAllCategories();
            List<String> categoryNames = new ArrayList<>();
            for (Category c : categoriesList) {
                categoryNames.add(c.name);
            }

            requireActivity().runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_spinner_item, categoryNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCategory.setAdapter(adapter);

                loadGoalAndReminder();
            });
        });
    }

    private void loadGoalAndReminder() {
        if (goalId == -1) {
            // זו מטרה חדשה, אין מה לטעון
            currentGoalWithReminder = new GoalWithReminder();
            currentGoalWithReminder.goal = new Goal();
            currentGoalWithReminder.reminder = new Reminder();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            currentGoalWithReminder = dao.getGoalWithReminderById(goalId);
            requireActivity().runOnUiThread(() -> {
                if (currentGoalWithReminder != null && currentGoalWithReminder.goal != null) {
                    Goal g = currentGoalWithReminder.goal;
                    etName.setText(g.name);
                    etDescription.setText(g.description);

                    // בחירת קטגוריה
                    for (int i = 0; i < categoriesList.size(); i++) {
                        if (categoriesList.get(i).id == g.categoryId) {
                            spinnerCategory.setSelection(i);
                            break;
                        }
                    }

                    // בחירת סוג התראה
                    rgNotificationType.check("TOUGH".equals(g.notificationType) ? R.id.rbTough : R.id.rbGentle);

                    // הצגת זמן וימים מהתזכורת
                    if (currentGoalWithReminder.reminder != null) {
                        Reminder r = currentGoalWithReminder.reminder;
                        selectedHour = r.minuteOfDay / 60;
                        selectedMinute = r.minuteOfDay % 60;
                        update_time_text();
                        updateSelectedDays(r.days);
                    }
                }
            });
        });
    }

    private void updateSelectedDays(List<Integer> days) {
        for (int i = 0; i < cgDays.getChildCount(); i++) {
            Chip chip = (Chip) cgDays.getChildAt(i);
            // המרה מ-Calendar (1=ראשון) ל-Integer שלנו (0=ראשון)
            int dayIndex = i; // 0 for Sun, 1 for Mon, etc.
            chip.setChecked(days != null && days.contains(dayIndex));
        }
    }

    private List<Integer> getSelectedDays() {
        List<Integer> selected = new ArrayList<>();
        for (int i = 0; i < cgDays.getChildCount(); i++) {
            Chip chip = (Chip) cgDays.getChildAt(i);
            if (chip.isChecked()) {
                selected.add(i); // 0 for Sun, 1 for Mon...
            }
        }
        return selected;
    }


    private void saveGoal() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            etName.setError("Name cannot be empty");
            return;
        }
        String desc = etDescription.getText().toString().trim();
        long categoryId = categoriesList.get(spinnerCategory.getSelectedItemPosition()).id;
        String notificationType = rgNotificationType.getCheckedRadioButtonId() == R.id.rbTough ? "TOUGH" : "GENTLE";
        List<Integer> days = getSelectedDays();

        if (days.isEmpty()) {
            Toast.makeText(getContext(), "Please select at least one day", Toast.LENGTH_SHORT).show();
            return;
        }

        int minuteOfDay = selectedHour * 60 + selectedMinute;

        // עדכון האובייקטים
        currentGoalWithReminder.goal.name = name;
        currentGoalWithReminder.goal.description = desc;
        currentGoalWithReminder.goal.categoryId = categoryId;
        currentGoalWithReminder.goal.notificationType = notificationType;
        currentGoalWithReminder.goal.active = true;

        if (currentGoalWithReminder.reminder == null) {
             currentGoalWithReminder.reminder = new Reminder();
        }
        currentGoalWithReminder.reminder.days = days;
        currentGoalWithReminder.reminder.minuteOfDay = minuteOfDay;


        Executors.newSingleThreadExecutor().execute(() -> {
            if (goalId == -1) {
                // מטרה חדשה - נקבל את ה-ID החדש חזרה
                long newId = dao.insertGoalWithReminder(currentGoalWithReminder.goal, currentGoalWithReminder.reminder);
                currentGoalWithReminder.goal.id = (int)newId; // ונעדכן אותו כאן
            } else {
                // עדכון מטרה קיימת
                dao.updateGoalWithReminder(currentGoalWithReminder.goal, currentGoalWithReminder.reminder);
            }
            
            // תזמון ההתראה בכל מקרה (ליצירה או עדכון)
            Log.d(TAG, "Calling NotificationScheduler from saveGoal for goal ID: " + currentGoalWithReminder.goal.id);
            NotificationScheduler.scheduleNotification(getContext(), currentGoalWithReminder.goal, currentGoalWithReminder.reminder);

            requireActivity().runOnUiThread(() -> {
                requireActivity().getSupportFragmentManager().popBackStack();
                ((GoalsActivity) requireActivity()).loadGoals();
            });
        });
    }
}
