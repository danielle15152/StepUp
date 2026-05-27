package com.example.stepup.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepup.EditGoalFragment;
import com.example.stepup.GoalDetailsActivity;
import com.example.stepup.R;
import com.example.stepup.data.AppDatabase;
import com.example.stepup.data.Dao;
import com.example.stepup.data.GeofenceHelper;
import com.example.stepup.data.entities.GoalWithReminder;

import java.util.List;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private Dao dao;
    private GoalsAdapter adapter;
    private RecyclerView rvGoals;
    private LinearLayout emptyStateLayout;
    private GeofenceHelper geofenceHelper;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppDatabase db = AppDatabase.getDatabase(requireContext().getApplicationContext());
        dao = db.dao();
        geofenceHelper = new GeofenceHelper(requireContext());

        rvGoals = view.findViewById(R.id.rvGoals);
        emptyStateLayout = view.findViewById(R.id.empty_state_layout);
        rvGoals.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new GoalsAdapter(new GoalsAdapter.GoalActionsListener() {
            @Override
            public void onItemClicked(GoalWithReminder item) {
                Intent intent = new Intent(requireActivity(), GoalDetailsActivity.class);
                intent.putExtra(GoalDetailsActivity.EXTRA_GOAL_ID, (long)item.goal.id);
                startActivity(intent);
            }

            @Override
            public void onEditClicked(GoalWithReminder item) {
                long goalId = item.goal.id;
                // אנימציה: ה-Edit form נכנס ב-fade, מה שעוזב נעלם ב-fade.
                // בחזרה (back press) - אותו הדבר אבל הפוך.
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(
                                R.anim.fade_in, R.anim.fade_out,
                                R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.nav_host_fragment, EditGoalFragment.newInstance(goalId))
                        .addToBackStack("edit_goal")
                        .commit();
            }

            @Override
            public void onDeleteClicked(GoalWithReminder item) {
                showDeleteConfirmDialog(item);
            }
        }, dao);

        rvGoals.setAdapter(adapter);

        View fab = view.findViewById(R.id.fabAddGoal);
        fab.setOnClickListener(v -> {
            // אנימציה זהה למעבר ל-Edit Goal
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                            R.anim.fade_in, R.anim.fade_out,
                            R.anim.fade_in, R.anim.fade_out)
                    .replace(R.id.nav_host_fragment, EditGoalFragment.newInstance(-1))
                    .addToBackStack("add_goal")
                    .commit();
        });

        // אנימציית כניסה ל-FAB - מתחיל בגודל 0 ושקוף, גדל לחיים
        // עם overshoot (חורג מעט מעבר ל-1 ואז חוזר). יוצר תחושת
        // "קפיצה" קלה במקום הופעה משעממת.
        fab.setScaleX(0f);
        fab.setScaleY(0f);
        fab.setAlpha(0f);
        fab.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setStartDelay(200)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator(1.4f))
                .start();

        Executors.newSingleThreadExecutor().execute(() -> {
            if (dao.getAllCategories().isEmpty()) {
                dao.insertCategory(new com.example.stepup.data.entities.Category("Health", true));
                dao.insertCategory(new com.example.stepup.data.entities.Category("Education", true));
                dao.insertCategory(new com.example.stepup.data.entities.Category("Sports", true));
                dao.insertCategory(new com.example.stepup.data.entities.Category("Finance", true));
            }
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        loadGoals();
    }

    public void loadGoals() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<GoalWithReminder> items = dao.getGoalsWithReminders();
            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    adapter.setItems(items);
                    updateVisibility(items.isEmpty());
                    // הפעלת אנימציית fall-down של כל הפריטים מחדש,
                    // אחרי שהרשימה נטענה
                    rvGoals.scheduleLayoutAnimation();
                });
            }
        });
    }

    private void updateVisibility(boolean isEmpty) {
        if (isEmpty) {
            rvGoals.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            rvGoals.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }


    private void showDeleteConfirmDialog(GoalWithReminder item) {
        String goalName = (item.goal != null && item.goal.name != null) ? item.goal.name : "this goal";

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Delete goal?")
                .setMessage("Are you sure you want to delete \"" + goalName + "\"? This cannot be undone.")
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .setPositiveButton("Delete", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button deleteBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (deleteBtn != null) {
                deleteBtn.setTextColor(Color.RED);
                Drawable trashIcon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete);
                if (trashIcon != null) {
                    deleteBtn.setCompoundDrawablesWithIntrinsicBounds(trashIcon, null, null, null);
                    deleteBtn.setCompoundDrawablePadding(16);
                }
                deleteBtn.setOnClickListener(v -> {
                    dialog.dismiss();
                    deleteGoal(item);
                });
            }
        });

        dialog.show();
    }

    private void deleteGoal(GoalWithReminder item) {
        String goalId = String.valueOf(item.goal.id);

        Executors.newSingleThreadExecutor().execute(() -> {
            dao.deleteGoalWithReminder(item.goal.id);
            geofenceHelper.removeGeofence(goalId);
            loadGoals();
        });
    }
}
