package com.example.stepup;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.stepup.data.AppDatabase;
import com.example.stepup.data.Dao;
import com.example.stepup.data.entities.GoalCompletion;
import com.example.stepup.data.entities.GoalSkip;
import com.example.stepup.data.entities.GoalWithReminder;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoalDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_GOAL_ID = "extra_goal_id";

    private enum GoalStatus { INITIAL, COMPLETED, SKIPPED }

    private Dao dao;
    private GoalWithReminder goalWithReminder;
    private GoalStatus currentStatus;
    private BottomSheetBehavior<FrameLayout> bottomSheetBehavior;

    // Views
    private FrameLayout bottomSheet;
    private ImageView ivBackgroundGraphic, ivCategoryIcon;
    private TextView tvCategoryName, tvGoalName, tvGoalDescription, tvReminderDays, tvReminderSpecifics;
    private LinearLayout initialActionLayout, completedLayout, skippedLayout;
    private Button btnSkip, btnComplete, btnUndoComplete, btnUndoSkip;
    private View mainContent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_details);

        initializeViews();
        setupBottomSheet();

        dao = AppDatabase.getDatabase(this).dao();
        long goalId = getIntent().getLongExtra(EXTRA_GOAL_ID, -1);
        loadGoalDetails(goalId);
    }

    private void initializeViews() {
        bottomSheet = findViewById(R.id.bottom_sheet);
        ivBackgroundGraphic = findViewById(R.id.ivBackgroundGraphic);
        ivCategoryIcon = findViewById(R.id.ivCategoryIcon);
        tvCategoryName = findViewById(R.id.tvCategoryName);
        tvGoalName = findViewById(R.id.tvGoalName);
        tvGoalDescription = findViewById(R.id.tvGoalDescription);
        tvReminderDays = findViewById(R.id.tvReminderDays);
        tvReminderSpecifics = findViewById(R.id.tvReminderSpecifics);
        initialActionLayout = findViewById(R.id.initial_action_layout);
        completedLayout = findViewById(R.id.completed_layout);
        skippedLayout = findViewById(R.id.skipped_layout);
        btnSkip = findViewById(R.id.btnSkip);
        btnComplete = findViewById(R.id.btnComplete);
        btnUndoComplete = findViewById(R.id.btnUndoComplete);
        btnUndoSkip = findViewById(R.id.btnUndoSkip);
        mainContent = findViewById(R.id.main_content);
    }

    private void setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    finish();
                }
            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });
        mainContent.setOnClickListener(v -> bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN));
    }

    private void loadGoalDetails(long goalId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            goalWithReminder = dao.getGoalWithReminderById(goalId);
            if (goalWithReminder != null) {
                long today = getTodayAsLong();
                boolean isCompleted = dao.getCompletion(goalWithReminder.goal.id, today) != null;
                boolean isSkipped = dao.getSkip(goalWithReminder.goal.id, today) != null;
                if (isCompleted) {
                    currentStatus = GoalStatus.COMPLETED;
                } else if (isSkipped) {
                    currentStatus = GoalStatus.SKIPPED;
                } else {
                    currentStatus = GoalStatus.INITIAL;
                }
            }
            runOnUiThread(this::populateUI);
        });
    }

    private void populateUI() {
        if (goalWithReminder == null) {
            finish();
            return;
        }

        boolean isTough = "TOUGH".equals(goalWithReminder.goal.notificationType);
        bottomSheet.setBackgroundResource(isTough ? R.drawable.bottom_sheet_background_tough : R.drawable.bottom_sheet_background_gentle);
        ivBackgroundGraphic.setImageResource(isTough ? R.drawable.ic_tough_background : R.drawable.ic_gentle_background);

        tvGoalName.setText(goalWithReminder.goal.name);
        tvGoalDescription.setText(goalWithReminder.goal.description);
        setCategoryInfo(goalWithReminder.goal.categoryId);

        if (goalWithReminder.reminder != null && goalWithReminder.reminder.days != null) {
            tvReminderDays.setText(formatDays(goalWithReminder.reminder.days));
            if (goalWithReminder.reminder.latitude != null) {
                tvReminderSpecifics.setText(String.format(Locale.getDefault(), "Lat: %.2f, Lon: %.2f", goalWithReminder.reminder.latitude, goalWithReminder.reminder.longitude));
            } else if (goalWithReminder.reminder.minuteOfDay != null) {
                tvReminderSpecifics.setText("at " + formatMinuteOfDay(goalWithReminder.reminder.minuteOfDay));
            }
        }
        
        updateActionArea();
        
        btnComplete.setOnClickListener(v -> setStatus(GoalStatus.COMPLETED));
        btnSkip.setOnClickListener(v -> setStatus(GoalStatus.SKIPPED));
        btnUndoComplete.setOnClickListener(v -> setStatus(GoalStatus.INITIAL));
        btnUndoSkip.setOnClickListener(v -> setStatus(GoalStatus.INITIAL));
    }

    private void updateActionArea() {
        initialActionLayout.setVisibility(currentStatus == GoalStatus.INITIAL ? View.VISIBLE : View.GONE);
        completedLayout.setVisibility(currentStatus == GoalStatus.COMPLETED ? View.VISIBLE : View.GONE);
        skippedLayout.setVisibility(currentStatus == GoalStatus.SKIPPED ? View.VISIBLE : View.GONE);
    }

    private void setStatus(GoalStatus newStatus) {
        currentStatus = newStatus;
        updateActionArea();
        
        Executors.newSingleThreadExecutor().execute(() -> {
            long today = getTodayAsLong();
            // First, clear any existing status for the day
            dao.deleteCompletion(goalWithReminder.goal.id, today);
            dao.deleteSkip(goalWithReminder.goal.id, today);
            
            // Then, set the new status
            if (newStatus == GoalStatus.COMPLETED) {
                dao.insertCompletion(new GoalCompletion(goalWithReminder.goal.id, today));
            } else if (newStatus == GoalStatus.SKIPPED) {
                dao.insertSkip(new GoalSkip(goalWithReminder.goal.id, today));
            }
        });
    }
    
    private void setCategoryInfo(long categoryId) {
        Executors.newSingleThreadExecutor().execute(()-> {
            String categoryName = dao.getCategoryNameById(categoryId);
            runOnUiThread(() -> {
                tvCategoryName.setText(categoryName);
                
                int iconResId = R.drawable.ic_category_health;
                int colorResId = R.color.category_health;

                if (categoryName != null) {
                    switch (categoryName.toLowerCase(Locale.ROOT)) {
                        case "education": iconResId = R.drawable.ic_category_education; colorResId = R.color.category_education; break;
                        case "sports": iconResId = R.drawable.ic_category_sports; colorResId = R.color.category_sports; break;
                        case "finance": iconResId = R.drawable.ic_category_finance; colorResId = R.color.category_finance; break;
                    }
                }
                ivCategoryIcon.setImageResource(iconResId);
                ivCategoryIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, colorResId)));
            });
        });
    }

    @Override
    public void finish() {
        super.finish();
    }
    
    private long getTodayAsLong() {
        return Long.parseLong(new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date()));
    }

    private String formatDays(java.util.List<Integer> days) {
        if (days == null || days.isEmpty()) return "No repeating days";
        String[] names = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        StringBuilder sb = new StringBuilder();
        days.sort(Integer::compareTo);
        for (int d : days) {
            if (sb.length() > 0) sb.append(", ");
            sb.append((d >= 0 && d <= 6) ? names[d] : "");
        }
        return sb.toString();
    }

    private String formatMinuteOfDay(Integer minuteOfDay) {
        if (minuteOfDay == null) return "";
        int h = Math.max(0, minuteOfDay) / 60;
        int m = Math.max(0, minuteOfDay) % 60;
        return String.format(Locale.US, "%02d:%02d", h, m);
    }
}
