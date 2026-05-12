package com.example.stepup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.stepup.data.NotificationScheduler;
import com.example.stepup.data.entities.Reminder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

import com.example.stepup.data.Dao;
import com.example.stepup.data.entities.Goal;
import com.example.stepup.data.entities.Category;

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

    private EditText etName, etDescription;
    private Spinner spinnerCategory;
    private RadioGroup rgNotificationType; // הוספנו את ה-RadioGroup
    private List<Category> categoriesList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_goal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        goalId = requireArguments().getLong(ARG_GOAL_ID);

        etName = view.findViewById(R.id.etName);
        etDescription = view.findViewById(R.id.etDescription);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        rgNotificationType = view.findViewById(R.id.rgNotificationType); // מצאנו את ה-RadioGroup

        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSave = view.findViewById(R.id.btnSave);

        dao = ((GoalsActivity) requireActivity()).getDao();

        loadCategories();

        btnCancel.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        btnSave.setOnClickListener(v -> saveGoal());
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

                loadGoal();
            });
        });
    }

    private void loadGoal() {
        if (goalId == -1) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            Goal g = dao.getGoalById(goalId);
            requireActivity().runOnUiThread(() -> {
                if (g != null) {
                    etName.setText(g.name);
                    etDescription.setText(g.description);

                    // בחירת הקטגוריה הנכונה
                    if (categoriesList != null) {
                        for (int i = 0; i < categoriesList.size(); i++) {
                            if (categoriesList.get(i).id == g.categoryId) {
                                spinnerCategory.setSelection(i);
                                break;
                            }
                        }
                    }

                    // בחירת סוג ההתראה הנכון
                    if ("TOUGH".equals(g.notificationType)) {
                        rgNotificationType.check(R.id.rbTough);
                    } else {
                        rgNotificationType.check(R.id.rbGentle);
                    }
                }
            });
        });
    }

    private void saveGoal() {
        String name = etName.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Name cannot be empty");
            return;
        }

        int selectedPosition = spinnerCategory.getSelectedItemPosition();
        if (selectedPosition == Spinner.INVALID_POSITION || categoriesList == null || categoriesList.isEmpty()) {
            Toast.makeText(getContext(), "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }
        long categoryId = categoriesList.get(selectedPosition).id;

        // קביעת סוג ההתראה לפי הבחירה
        String notificationType = "GENTLE";
        if (rgNotificationType.getCheckedRadioButtonId() == R.id.rbTough) {
            notificationType = "TOUGH";
        }

        // שמירת המידע ברקע
        final String finalNotificationType = notificationType;
        Executors.newSingleThreadExecutor().execute(() -> {
            if (goalId == -1) {
                // יצירת מטרה חדשה - תיקנו את הקריאה והוספנו את סוג ההתראה
                Goal newGoal = new Goal(name, desc, true, categoryId, finalNotificationType);
                long newGoalId = dao.insertGoal(newGoal);
                newGoal.id = (int)newGoalId; // נעדכן את ה-ID באובייקט

                // ניצור תזכורת לדוגמה (כל יום ב-8 בבוקר) ונתזמן אותה
                Reminder reminder = new Reminder(Collections.singletonList(Calendar.SUNDAY), 8 * 60, null);
                dao.insertReminder(reminder); // צריך לוודא שה-ID של התזכורת מתעדכן
                
                NotificationScheduler.scheduleNotification(getContext(), newGoal, reminder);

            } else {
                // עדכון מטרה קיימת
                Goal g = dao.getGoalById(goalId);
                if (g != null) {
                    g.name = name;
                    g.description = desc;
                    g.categoryId = categoryId;
                    g.notificationType = finalNotificationType;
                    dao.updateGoal(g);

                    // כאן צריך לשלוף את התזכורת הקיימת ולתזמן מחדש
                    //כרגע אין לנו דרך לשלוף תזכורת בודדת אז נשאיר את זה לעתיד
                }
            }

            // חזרה למסך הראשי ורענון הרשימה
            requireActivity().runOnUiThread(() -> {
                requireActivity().getSupportFragmentManager().popBackStack();
                ((GoalsActivity) requireActivity()).loadGoals();
            });
        });
    }
}