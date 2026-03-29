package com.example.stepup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.concurrent.Executors;

import com.example.stepup.data.Dao;
import com.example.stepup.data.entities.Goal;

public class EditGoalFragment extends Fragment {

    private static final String ARG_GOAL_ID = "goal_id";

    // שורה 23 בערך
    public static EditGoalFragment newInstance(long goalId) {
        EditGoalFragment f = new EditGoalFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_GOAL_ID, goalId); // אם זה -1, זה אומר "חדש"
        f.setArguments(b);
        return f;
    }

    private long goalId;
    private Dao dao;

    private EditText etName, etDescription, etCategory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,//חיבור הקוד לקובץ העיצוב
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_goal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        goalId = requireArguments().getLong(ARG_GOAL_ID);

        etName = view.findViewById(R.id.etName);
        etDescription = view.findViewById(R.id.etDescription);
        etCategory = view.findViewById(R.id.etCategory);

        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSave = view.findViewById(R.id.btnSave);

        // מקבלים את DAO מה-Activity שמארח
        dao = ((GoalsActivity) requireActivity()).getDao();

        loadGoal();

        btnCancel.setOnClickListener(v ->//כפתור הביטול
                requireActivity().getSupportFragmentManager().popBackStack()//סוגר את הפרגמנט ומחזיר אותנו לרשימה בלי לשנות כלום
        );

        btnSave.setOnClickListener(v ->//שמירת השינויים
                saveGoal()
        );
    }

    private void loadGoal() {
        // לוגיקה חכמה: אם ה-ID הוא -1, זה אומר שאנחנו מוסיפים מטרה חדשה.
        // אין מה לטעון מהמסד, פשוט נשאיר את השדות ריקים.
        if (goalId == -1) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            Goal g = dao.getGoalById(goalId);
            requireActivity().runOnUiThread(() -> {
                if (g != null) {
                    etName.setText(g.name);
                    etDescription.setText(g.description);
                    etCategory.setText(g.category);
                }
            });
        });
    }

    private void saveGoal() {
        String name = etName.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String cat = etCategory.getText().toString().trim();

        // לוגיקה עסקית בסיסית: מניעת שמירה של שם ריק
        if (name.isEmpty()) {
            etName.setError("Name cannot be empty");
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            if (goalId == -1) {
                // --- לוגיקת הוספה (Create) ---
                Goal newGoal = new Goal();
                newGoal.name = name;
                newGoal.description = desc;
                newGoal.category = cat;
                newGoal.active = true; // מטרה חדשה מתחילה כפעילה

                dao.insertGoal(newGoal); // וודאי שב-Dao.java יש פונקציית @Insert
            } else {
                // --- לוגיקת עריכה (Update) ---
                Goal g = dao.getGoalById(goalId);
                if (g != null) {
                    g.name = name;
                    g.description = desc;
                    g.category = cat;
                    dao.updateGoal(g);
                }
            }

            requireActivity().runOnUiThread(() -> {
                requireActivity().getSupportFragmentManager().popBackStack();
                ((GoalsActivity) requireActivity()).loadGoals(); // רענון הרשימה במסך הראשי
            });
        });
    }
    }



