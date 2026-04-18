package com.example.stepup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
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

        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSave = view.findViewById(R.id.btnSave);

        dao = ((GoalsActivity) requireActivity()).getDao();

        // קודם טוענים קטגוריות, ורק אז את המטרה (בתוך loadCategories)
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

                // עכשיו כשיש קטגוריות ב-Spinner, אפשר לטעון את נתוני המטרה
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

                    // בחירת הקטגוריה הנכונה ב-Spinner לפי ה-ID
                    if (categoriesList != null) {
                        for (int i = 0; i < categoriesList.size(); i++) {
                            if (categoriesList.get(i).id == g.categoryId) {
                                spinnerCategory.setSelection(i);
                                break;
                            }
                        }
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

        // מוודאים שנבחרה קטגוריה
        int selectedPosition = spinnerCategory.getSelectedItemPosition();
        if (selectedPosition == Spinner.INVALID_POSITION || categoriesList == null || categoriesList.isEmpty()) {
            return;
        }
        long categoryId = categoriesList.get(selectedPosition).id;

        Executors.newSingleThreadExecutor().execute(() -> {
            if (goalId == -1) {
                Goal newGoal = new Goal(name, desc, true, categoryId);
                dao.insertGoal(newGoal);
            } else {
                Goal g = dao.getGoalById(goalId);
                if (g != null) {
                    g.name = name;
                    g.description = desc;
                    g.categoryId = categoryId;
                    dao.updateGoal(g);
                }
            }

            requireActivity().runOnUiThread(() -> {
                requireActivity().getSupportFragmentManager().popBackStack();
                ((GoalsActivity) requireActivity()).loadGoals();
            });
        });
    }
}