package com.example.stepup;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.stepup.data.AppDatabase;
import com.example.stepup.data.Dao;
import com.example.stepup.data.GeofenceHelper;
import com.example.stepup.data.NotificationScheduler;
import com.example.stepup.data.entities.Category;
import com.example.stepup.data.entities.Goal;
import com.example.stepup.data.entities.GoalWithReminder;
import com.example.stepup.data.entities.Reminder;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class EditGoalFragment extends Fragment {

    private static final String ARG_GOAL_ID = "goal_id";

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
    private GeofenceHelper geofenceHelper;

    // UI Components
    private EditText etName, etDescription;
    private Spinner spinnerCategory;
    private RadioGroup rgNotificationType, rgReminderType;
    private RadioButton rbTime, rbLocation;
    private TextView tvTimePicker;
    private ChipGroup cgDays;
    private Button btnSelectLocation;
    private TextView tvSelectedLocation;
    private LinearLayout timeReminderLayout, locationReminderLayout;

    private List<Category> categoriesList;
    private Integer selectedHour = 8;
    private Integer selectedMinute = 0;
    private Double selectedLatitude = null;
    private Double selectedLongitude = null;

    private ActivityResultLauncher<Intent> mapPickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        geofenceHelper = new GeofenceHelper(requireContext());
        mapPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        selectedLatitude = data.getDoubleExtra(MapPickerActivity.EXTRA_LATITUDE, 0);
                        selectedLongitude = data.getDoubleExtra(MapPickerActivity.EXTRA_LONGITUDE, 0);
                        updateLocationText();
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_goal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        goalId = requireArguments().getLong(ARG_GOAL_ID);
        dao = AppDatabase.getDatabase(requireContext()).dao();
        initializeViews(view);
        setupListeners();
        updateTimeText();
        loadCategories();
    }

    private void initializeViews(View view) {
        etName = view.findViewById(R.id.etName);
        etDescription = view.findViewById(R.id.etDescription);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        rgNotificationType = view.findViewById(R.id.rgNotificationType);
        cgDays = view.findViewById(R.id.cgDays);
        
        rgReminderType = view.findViewById(R.id.rgReminderType);
        rbTime = view.findViewById(R.id.rbTime);
        rbLocation = view.findViewById(R.id.rbLocation);
        
        timeReminderLayout = view.findViewById(R.id.time_reminder_layout);
        tvTimePicker = view.findViewById(R.id.tvTimePicker);
        
        locationReminderLayout = view.findViewById(R.id.location_reminder_layout);
        btnSelectLocation = view.findViewById(R.id.btnSelectLocation);
        tvSelectedLocation = view.findViewById(R.id.tvSelectedLocation);
    }

    private void setupListeners() {
        getView().findViewById(R.id.btnCancel).setOnClickListener(v -> getParentFragmentManager().popBackStack());
        getView().findViewById(R.id.btnSave).setOnClickListener(v -> saveGoal());
        tvTimePicker.setOnClickListener(v -> showTimePickerDialog());
        btnSelectLocation.setOnClickListener(v -> selectLocation());
        
        rgReminderType.setOnCheckedChangeListener((group, checkedId) -> {
            updateReminderTypeVisibility(checkedId);
            // Clear the data of the unselected type
            if (checkedId == R.id.rbTime) {
                selectedLatitude = null;
                selectedLongitude = null;
                updateLocationText();
            } else {
                selectedHour = null;
                selectedMinute = null;
                updateTimeText();
            }
        });
    }
    
    private void updateReminderTypeVisibility(int checkedId) {
        if (checkedId == R.id.rbTime) {
            timeReminderLayout.setVisibility(View.VISIBLE);
            locationReminderLayout.setVisibility(View.GONE);
        } else {
            timeReminderLayout.setVisibility(View.GONE);
            locationReminderLayout.setVisibility(View.VISIBLE);
        }
    }

    private void selectLocation() {
        Intent intent = new Intent(getActivity(), MapPickerActivity.class);
        mapPickerLauncher.launch(intent);
    }
    
    private void updateLocationText() {
        if (selectedLatitude != null && selectedLongitude != null) {
            tvSelectedLocation.setText(String.format(Locale.getDefault(), "Lat: %.4f, Lon: %.4f", selectedLatitude, selectedLongitude));
        } else {
            tvSelectedLocation.setText("No location selected");
        }
    }

    private void showTimePickerDialog() {
        final int hour = selectedHour != null ? selectedHour : 8;
        final int minute = selectedMinute != null ? selectedMinute : 0;
        
        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view, hourOfDay, min) -> {
            selectedHour = hourOfDay;
            selectedMinute = min;
            updateTimeText();
        }, hour, minute, true);
        timePickerDialog.show();
    }

    private void updateTimeText() {
        if (selectedHour != null && selectedMinute != null) {
            tvTimePicker.setText(String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute));
        } else {
            tvTimePicker.setText("Select a time");
        }
    }

    private void loadCategories() {
        Executors.newSingleThreadExecutor().execute(() -> {
            categoriesList = dao.getAllCategories();
            List<String> categoryNames = new ArrayList<>();
            for (Category c : categoriesList) categoryNames.add(c.name);

            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, categoryNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerCategory.setAdapter(adapter);
                    loadGoalAndReminder();
                });
            }
        });
    }

    private void loadGoalAndReminder() {
        if (goalId == -1) {
            currentGoalWithReminder = new GoalWithReminder();
            currentGoalWithReminder.goal = new Goal();
            currentGoalWithReminder.reminder = new Reminder();
            rbTime.setChecked(true); // Default to time reminder for new goals
            updateReminderTypeVisibility(R.id.rbTime);
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            currentGoalWithReminder = dao.getGoalWithReminderById(goalId);
            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (currentGoalWithReminder != null) populateUI();
                });
            }
        });
    }
    
    private void populateUI() {
        Goal g = currentGoalWithReminder.goal;
        etName.setText(g.name);
        etDescription.setText(g.description);
        for (int i = 0; i < categoriesList.size(); i++) {
            if (categoriesList.get(i).id == g.categoryId) {
                spinnerCategory.setSelection(i);
                break;
            }
        }
        rgNotificationType.check("TOUGH".equals(g.notificationType) ? R.id.rbTough : R.id.rbGentle);

        if (currentGoalWithReminder.reminder != null) {
            Reminder r = currentGoalWithReminder.reminder;
            updateSelectedDays(r.days);
            
            if (r.latitude != null && r.longitude != null) {
                selectedLatitude = r.latitude;
                selectedLongitude = r.longitude;
                rbLocation.setChecked(true);
                updateReminderTypeVisibility(R.id.rbLocation);
            } else {
                selectedHour = r.minuteOfDay / 60;
                selectedMinute = r.minuteOfDay % 60;
                rbTime.setChecked(true);
                updateReminderTypeVisibility(R.id.rbTime);
            }
            updateLocationText();
            updateTimeText();
        }
    }

    private void updateSelectedDays(List<Integer> days) {
        for (int i = 0; i < cgDays.getChildCount(); i++) {
            Chip chip = (Chip) cgDays.getChildAt(i);
            chip.setChecked(days != null && days.contains(i));
        }
    }

    private List<Integer> getSelectedDays() {
        List<Integer> selected = new ArrayList<>();
        for (int i = 0; i < cgDays.getChildCount(); i++) {
            if (((Chip) cgDays.getChildAt(i)).isChecked()) selected.add(i);
        }
        return selected;
    }

    private void saveGoal() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            etName.setError("Name cannot be empty");
            return;
        }

        List<Integer> days = getSelectedDays();
        if (days.isEmpty()) {
            Toast.makeText(getContext(), "Please select at least one day for the reminder", Toast.LENGTH_SHORT).show();
            return;
        }
        
        boolean isTimeReminder = rbTime.isChecked();
        
        if(isTimeReminder && (selectedHour == null || selectedMinute == null)) {
            Toast.makeText(getContext(), "Please select a time for the reminder", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if(!isTimeReminder && (selectedLatitude == null || selectedLongitude == null)) {
            Toast.makeText(getContext(), "Please select a location for the reminder", Toast.LENGTH_SHORT).show();
            return;
        }
        
        currentGoalWithReminder.goal.name = name;
        currentGoalWithReminder.goal.description = etDescription.getText().toString().trim();
        currentGoalWithReminder.goal.categoryId = categoriesList.get(spinnerCategory.getSelectedItemPosition()).id;
        currentGoalWithReminder.goal.notificationType = rgNotificationType.getCheckedRadioButtonId() == R.id.rbTough ? "TOUGH" : "GENTLE";
        currentGoalWithReminder.goal.active = true;

        if (currentGoalWithReminder.reminder == null) currentGoalWithReminder.reminder = new Reminder();
        
        currentGoalWithReminder.reminder.days = days;
        currentGoalWithReminder.reminder.minuteOfDay = isTimeReminder ? (selectedHour * 60 + selectedMinute) : 0;
        currentGoalWithReminder.reminder.latitude = isTimeReminder ? null : selectedLatitude;
        currentGoalWithReminder.reminder.longitude = isTimeReminder ? null : selectedLongitude;

        Executors.newSingleThreadExecutor().execute(() -> {
            if (goalId == -1) {
                long newId = dao.insertGoalWithReminder(currentGoalWithReminder.goal, currentGoalWithReminder.reminder);
                currentGoalWithReminder.goal.id = (int) newId;
            } else {
                geofenceHelper.removeGeofence(String.valueOf(goalId));
                dao.updateGoalWithReminder(currentGoalWithReminder.goal, currentGoalWithReminder.reminder);
            }
            
            if (isTimeReminder) {
                NotificationScheduler.scheduleNotification(getContext(), currentGoalWithReminder.goal, currentGoalWithReminder.reminder);
            } else {
                NotificationScheduler.cancelNotification(getContext(), currentGoalWithReminder.goal);
            }
            
            if (!isTimeReminder) {
                geofenceHelper.addGeofence(currentGoalWithReminder.goal, currentGoalWithReminder.reminder);
            }
            
            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> getParentFragmentManager().popBackStack());
            }
        });
    }
}
