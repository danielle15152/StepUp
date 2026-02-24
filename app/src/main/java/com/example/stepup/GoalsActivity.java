package com.example.stepup;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.example.stepup.data.AppDb;
import com.example.stepup.data.Dao;
import com.example.stepup.data.SeedDataHelper;
import com.example.stepup.data.entities.GoalWithReminder;
import com.example.stepup.ui.GoalsAdapter;

import java.util.List;
import java.util.concurrent.Executors;

public class GoalsActivity extends AppCompatActivity {
    View fragmentContainer;

    private AppDb db;
    private Dao dao;
    private GoalsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_goals);

        View root = findViewById(R.id.root);
        fragmentContainer = findViewById(R.id.fragmentContainer);
        if (root != null) {
            root.setAlpha(0f);
            root.animate().alpha(1f).setDuration(300).start();
        }

        ImageButton settings = findViewById(R.id.btnSettings);
        if (settings != null) {
            settings.setOnClickListener(v -> {
                Intent intent = new Intent(GoalsActivity.this, ActivitySetting.class);
                startActivity(intent);
            });
        }

        RecyclerView rvGoals = findViewById(R.id.rvGoals);
        rvGoals.setLayoutManager(new LinearLayoutManager(this));
getSupportFragmentManager().addOnBackStackChangedListener(()->{
    //fragmentContainer.setVisibility(View.GONE);
    if(getSupportFragmentManager().getFragments().isEmpty())
        fragmentContainer.setVisibility(View.GONE);
    else
        fragmentContainer.setVisibility(View.VISIBLE);
});
        adapter = new GoalsAdapter(new GoalsAdapter.GoalActionsListener() {
            @Override
            public void onEditClicked(GoalWithReminder item) {
                long goalId = item.goal.id;
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, EditGoalFragment.newInstance(goalId))
                        .addToBackStack("edit_goal")
                        .commit();
            }

            @Override
            public void onDeleteClicked(GoalWithReminder item) {
                showDeleteConfirmDialog(item);
            }
        });

        rvGoals.setAdapter(adapter);

        db = Room.databaseBuilder(
                getApplicationContext(),
                AppDb.class,
                "stepup-db"
        ).build();

        dao = db.dao();

        loadGoals();
    }

    public Dao getDao() {
        return dao;
    }

    public void loadGoals() {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (dao.countGoals() == 0) {
                SeedDataHelper.populateWith15Goals(dao);
            }
            List<GoalWithReminder> items = dao.getGoalsWithReminders();
            runOnUiThread(() -> adapter.setItems(items));
        });
    }

    private void showDeleteConfirmDialog(GoalWithReminder item) {
        String goalName = (item.goal != null && item.goal.name != null) ? item.goal.name : "this goal";

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Delete goal?")
                .setMessage("Are you sure you want to delete \"" + goalName + "\"? This cannot be undone.")
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .setPositiveButton("Delete", null) // נצמיד handler אחרי show כדי לשים אייקון/אדום
                .create();

        dialog.setOnShowListener(d -> {
            Button deleteBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (deleteBtn != null) {
                // אדום
                deleteBtn.setTextColor(Color.RED);

                // אייקון פח
                Drawable trashIcon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_delete);
                if (trashIcon != null) {
                    deleteBtn.setCompoundDrawablesWithIntrinsicBounds(trashIcon, null, null, null);
                    deleteBtn.setCompoundDrawablePadding(16);
                }

                // הפעולה בפועל
                deleteBtn.setOnClickListener(v -> {
                    dialog.dismiss();
                    deleteGoal(item);
                });
            }
        });

        dialog.show();
    }

    private void deleteGoal(GoalWithReminder item) {
        long goalId = item.goal.id;

        Executors.newSingleThreadExecutor().execute(() -> {
            dao.deleteGoalWithReminder(goalId);
            List<GoalWithReminder> refreshed = dao.getGoalsWithReminders();
            runOnUiThread(() -> adapter.setItems(refreshed));
        });
    }
}