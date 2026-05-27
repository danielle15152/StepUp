package com.example.stepup;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.stepup.data.AppDatabase;
import com.example.stepup.data.Dao;
import com.example.stepup.data.entities.GoalCompletion;
import com.example.stepup.data.entities.GoalSkip;
import com.example.stepup.data.entities.GoalWithReminder;
import com.example.stepup.data.entities.Reminder;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * מציג את פרטי המטרה ב-bottom sheet, ומאפשר לסמן השלמה/דילוג.
 *
 * הגרדיאנט של ה-sheet נבחר דינמית לפי שעת התזכורת (או מיקום) -
 * זהה ללוגיקה ב-GoalsAdapter כך שיש המשכיות ויזואלית כשלוחצים על
 * כרטיסיה ועוברים ל-sheet.
 */
public class GoalDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_GOAL_ID = "extra_goal_id";

    private enum GoalStatus {
        INITIAL,         // היום פעיל, מחכים לסטטוס
        COMPLETED,       // המשתמש סימן ✓
        SKIPPED,         // המשתמש בחר לדלג
        INACTIVE_TODAY   // היום לא בימי התזכורת - בלי כפתורי פעולה
    }

    // ימי השבוע בעברית (אינדקס 0=ראשון)
    private static final String[] DAYS_HE_FULL = {"ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת"};

    private Dao dao;
    private GoalWithReminder goalWithReminder;
    private GoalStatus currentStatus;
    private BottomSheetBehavior<FrameLayout> bottomSheetBehavior;

    // Views
    private FrameLayout bottomSheet;
    private ImageView ivBackgroundGraphic, ivCategoryIcon;
    private TextView tvCategoryName, tvGoalName, tvGoalDescription,
            tvReminderDays, tvReminderSpecifics;
    private LinearLayout initialActionLayout, completedLayout, skippedLayout, inactiveLayout;
    private MaterialButton btnSkip, btnComplete, btnUndoComplete, btnUndoSkip;
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
        inactiveLayout = findViewById(R.id.inactive_layout);
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
        // לחיצה מחוץ ל-sheet סוגרת אותו
        mainContent.setOnClickListener(
                v -> bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN));
    }

    private void loadGoalDetails(long goalId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            goalWithReminder = dao.getGoalWithReminderById(goalId);
            if (goalWithReminder != null) {
                // קודם בודקים אם המטרה פעילה היום (לפי ימי השבוע ב-reminder).
                // אם לא - אין טעם להציג כפתורי "דלגי/עשיתי", הם לא רלוונטיים.
                if (!isGoalActiveToday(goalWithReminder)) {
                    currentStatus = GoalStatus.INACTIVE_TODAY;
                } else {
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
            }
            runOnUiThread(this::populateUI);
        });
    }

    /**
     * בודק אם המטרה פעילה היום על פי ימי השבוע ב-reminder.
     *
     * Calendar.DAY_OF_WEEK מחזיר 1=ראשון..7=שבת, אבל ב-Reminder.days
     * אנחנו משתמשים ב-0=ראשון..6=שבת (כדי להתאים לאינדקסים של
     * cgDays.getChildAt(i)). אז מחסירים 1 כדי להמיר.
     */
    private static boolean isGoalActiveToday(GoalWithReminder gwr) {
        if (gwr.reminder == null
                || gwr.reminder.days == null
                || gwr.reminder.days.isEmpty()) {
            Log.d("GoalDetails",
                  "isGoalActiveToday: no days defined - treating as active");
            return true;
        }
        int todayIndex = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
        boolean active = gwr.reminder.days.contains(todayIndex);
        Log.d("GoalDetails",
              "isGoalActiveToday: todayIndex=" + todayIndex
                      + " days=" + gwr.reminder.days
                      + " active=" + active);
        return active;
    }

    private void populateUI() {
        if (goalWithReminder == null) {
            finish();
            return;
        }

        Reminder reminder = goalWithReminder.reminder;

        // הרקע של ה-sheet ייבנה דינמית ב-setCategoryInfo (כי הצבע
        // תלוי בקטגוריה שנטענת ברקע מ-DB).

        // טקסטים
        tvGoalName.setText(safe(goalWithReminder.goal.name));
        // הסתרה של תיאור אם הוא ריק כדי שלא ייווצר רווח מיותר
        if (goalWithReminder.goal.description != null
                && !goalWithReminder.goal.description.trim().isEmpty()) {
            tvGoalDescription.setText(goalWithReminder.goal.description);
            tvGoalDescription.setVisibility(View.VISIBLE);
        } else {
            tvGoalDescription.setVisibility(View.GONE);
        }
        setCategoryInfo(goalWithReminder.goal.categoryId, reminder);

        // ימים + מידע על שעה/מיקום - שניהם עם אייקון מתאים ליד הטקסט
        if (reminder != null && reminder.days != null && !reminder.days.isEmpty()) {
            tvReminderDays.setText(formatDaysFull(reminder.days));
            tvReminderDays.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_calendar_small, 0, 0, 0);
            tvReminderDays.setVisibility(View.VISIBLE);
        } else {
            tvReminderDays.setVisibility(View.GONE);
        }

        if (reminder != null && reminder.latitude != null && reminder.longitude != null) {
            // עדיפות: שם מיקום קריא; fallback: קואורדינטות.
            String locText;
            if (reminder.locationName != null && !reminder.locationName.isEmpty()) {
                locText = reminder.locationName;
            } else {
                locText = String.format(Locale.getDefault(),
                        "%.4f, %.4f", reminder.latitude, reminder.longitude);
            }
            tvReminderSpecifics.setText(locText);
            tvReminderSpecifics.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_pin_small, 0, 0, 0);
            tvReminderSpecifics.setVisibility(View.VISIBLE);
        } else if (reminder != null && reminder.minuteOfDay != null) {
            tvReminderSpecifics.setText(
                    "בכל יום ב-" + formatMinuteOfDay(reminder.minuteOfDay));
            tvReminderSpecifics.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_clock_small, 0, 0, 0);
            tvReminderSpecifics.setVisibility(View.VISIBLE);
        } else {
            tvReminderSpecifics.setVisibility(View.GONE);
        }

        updateActionArea();

        btnComplete.setOnClickListener(v -> setStatus(GoalStatus.COMPLETED));
        btnSkip.setOnClickListener(v -> setStatus(GoalStatus.SKIPPED));
        btnUndoComplete.setOnClickListener(v -> setStatus(GoalStatus.INITIAL));
        btnUndoSkip.setOnClickListener(v -> setStatus(GoalStatus.INITIAL));
    }

    /**
     * בונה GradientDrawable של ה-sheet עם פינות עליונות בלבד (28dp).
     * הצבעים זהים ללוגיקה ב-GoalsAdapter (קטגוריה + isTough),
     * אבל הפינות שונות כי ה-sheet "צמוד" לתחתית המסך.
     */
    private static GradientDrawable buildSheetGradient(Context ctx,
                                                       String categoryName,
                                                       boolean isTough) {
        int[] colors = com.example.stepup.ui.GoalsAdapter
                .colorsForCategory(categoryName, isTough);
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, colors);
        float topR = dpToPx(ctx, 28f);
        gd.setCornerRadii(new float[]{
                topR, topR,   // top-left
                topR, topR,   // top-right
                0, 0,         // bottom-right
                0, 0          // bottom-left
        });
        return gd;
    }

    private static float dpToPx(Context ctx, float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                ctx.getResources().getDisplayMetrics());
    }

    private void updateActionArea() {
        initialActionLayout.setVisibility(
                currentStatus == GoalStatus.INITIAL ? View.VISIBLE : View.GONE);
        completedLayout.setVisibility(
                currentStatus == GoalStatus.COMPLETED ? View.VISIBLE : View.GONE);
        skippedLayout.setVisibility(
                currentStatus == GoalStatus.SKIPPED ? View.VISIBLE : View.GONE);
        inactiveLayout.setVisibility(
                currentStatus == GoalStatus.INACTIVE_TODAY ? View.VISIBLE : View.GONE);
    }

    private void setStatus(GoalStatus newStatus) {
        currentStatus = newStatus;
        updateActionArea();

        Executors.newSingleThreadExecutor().execute(() -> {
            long today = getTodayAsLong();
            // ניקוי סטטוס קודם ליום זה
            dao.deleteCompletion(goalWithReminder.goal.id, today);
            dao.deleteSkip(goalWithReminder.goal.id, today);

            // הוספת הסטטוס החדש
            if (newStatus == GoalStatus.COMPLETED) {
                dao.insertCompletion(new GoalCompletion(goalWithReminder.goal.id, today));
            } else if (newStatus == GoalStatus.SKIPPED) {
                dao.insertSkip(new GoalSkip(goalWithReminder.goal.id, today));
            }
        });
    }

    /**
     * טוען את שם הקטגוריה, ובונה את הרקע הגרדיאנט הדינמי של ה-sheet.
     *
     * הצבע נקבע לפי הקטגוריה (בריאות=ורוד, ספורט=מנטה, וכו'), והגוון
     * תלוי ב-isTough (כהה משמעותית). אייקון הקטגוריה אותו דבר ב-TOUGH
     * וב-GENTLE - כדי שתמיד יהיה ניתן לזהות את סוג המטרה במבט.
     *
     * אם זו תזכורת-מיקום: ה-decoration הגדול ברקע הוא סיכת מפה.
     */
    private void setCategoryInfo(long categoryId, Reminder reminder) {
        boolean isLocationBased = reminder != null
                && reminder.latitude != null
                && reminder.longitude != null;
        boolean isTough = "TOUGH".equalsIgnoreCase(goalWithReminder.goal.notificationType);
        String styleBadge = isTough ? "תקיפה" : "עדינה";

        Executors.newSingleThreadExecutor().execute(() -> {
            String categoryName = dao.getCategoryNameById(categoryId);
            String displayName = translateCategoryToHebrew(categoryName);
            int iconResId = iconForCategory(categoryName);

            runOnUiThread(() -> {
                tvCategoryName.setText(displayName + " · " + styleBadge);
                // אייקון הקטגוריה - תמיד הקטגוריה האמיתית, כדי שניתן יהיה
                // לזהות את סוג המטרה במבט ראשון.
                ivCategoryIcon.setImageResource(iconResId);
                ivCategoryIcon.setBackgroundResource(R.drawable.bg_category_chip);
                // ה-decoration הגדול ברקע
                ivBackgroundGraphic.setImageResource(
                        isLocationBased ? R.drawable.ic_decor_pin : iconResId);
                // הרקע של ה-sheet - גרדיאנט דינמי לפי קטגוריה + isTough.
                // ב-TOUGH הצבע כהה משמעותית, ה-scrim הקיים מבטיח קריאות.
                bottomSheet.setBackground(buildSheetGradient(this, categoryName, isTough));
            });
        });
    }

    /**
     * ממיר את שם הקטגוריה (באנגלית, כפי שמופיע ב-DB) לעברית להצגה.
     */
    private static String translateCategoryToHebrew(String englishName) {
        if (englishName == null) return "";
        switch (englishName.toLowerCase(Locale.ROOT)) {
            case "health":    return "בריאות";
            case "education": return "חינוך";
            case "sports":    return "ספורט";
            case "finance":   return "פיננסים";
            default:          return englishName;
        }
    }

    @DrawableRes
    private static int iconForCategory(String categoryName) {
        if (categoryName == null) return R.drawable.ic_category_health;
        switch (categoryName.toLowerCase(Locale.ROOT)) {
            case "education": return R.drawable.ic_category_education;
            case "sports":    return R.drawable.ic_category_sports;
            case "finance":   return R.drawable.ic_category_finance;
            default:          return R.drawable.ic_category_health;
        }
    }

    private long getTodayAsLong() {
        return Long.parseLong(new SimpleDateFormat("yyyyMMdd",
                Locale.getDefault()).format(new Date()));
    }

    private static String safe(String s) { return s == null ? "" : s; }

    /**
     * "ראשון, שני, שלישי..." (שמות מלאים, בעברית).
     * אם 7 ימים → "כל יום".
     */
    private static String formatDaysFull(List<Integer> days) {
        if (days == null || days.isEmpty()) return "";
        days.sort(Integer::compareTo);
        if (days.size() == 7) return "כל יום";
        StringBuilder sb = new StringBuilder();
        for (int d : days) {
            if (sb.length() > 0) sb.append(", ");
            if (d >= 0 && d <= 6) sb.append(DAYS_HE_FULL[d]);
        }
        return sb.toString();
    }

    private static String formatMinuteOfDay(Integer minuteOfDay) {
        if (minuteOfDay == null) return "";
        int h = Math.max(0, minuteOfDay) / 60;
        int m = Math.max(0, minuteOfDay) % 60;
        return String.format(Locale.US, "%02d:%02d", h, m);
    }
}
