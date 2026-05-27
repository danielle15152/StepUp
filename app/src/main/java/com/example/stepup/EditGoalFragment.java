package com.example.stepup;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.stepup.data.AppDatabase;
import com.example.stepup.data.Dao;
import com.example.stepup.data.GeofenceHelper;
import com.example.stepup.data.NotificationScheduler;
import com.example.stepup.data.entities.Category;
import com.example.stepup.data.entities.Goal;
import com.example.stepup.data.entities.GoalWithReminder;
import com.example.stepup.data.entities.Reminder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * טופס יצירה/עריכה של מטרה.
 *
 * שינויים מהגרסה הישנה:
 *  - Spinner קטגוריה הוחלף ב-ChipGroup דינמי (chips נוצרים מהקטגוריות ב-DB)
 *  - RadioGroup של סגנון התראה הוחלף ב-2 MaterialCardView קליקאביליות
 *    (cardGentle/cardTough) עם אייקון, כותרת ותיאור
 *  - RadioGroup של סוג תזכורת הוחלף ב-2 MaterialCardView קליקאביליות
 *    (cardTime/cardLocation)
 *  - EditText הוחלפו ב-TextInputEditText (לכן זה Material3)
 *  - Button הוחלפו ב-MaterialButton
 *  - הטופס מסתיר את ה-Toolbar של GoalsActivity ב-onResume
 */
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
    private ChipGroup cgCategory;
    private MaterialCardView cardGentle, cardTough;
    private ChipGroup cgDays;
    private MaterialCardView cardTime, cardLocation;
    private TextView tvTimePicker;
    private MaterialButton btnSelectLocation;
    private TextView tvSelectedLocation;
    private LinearLayout timeReminderLayout, locationReminderLayout;

    // מצב הבחירה הנוכחי (true=Tough, false=Gentle ; true=Time, false=Location)
    private boolean isTough = false;
    private boolean isTimeReminder = true;

    private List<Category> categoriesList = new ArrayList<>();
    // נשמר את ה-id של הקטגוריה הנבחרת כדי לשרוד recreate של chips
    private Long selectedCategoryId = null;

    private Integer selectedHour = 8;
    private Integer selectedMinute = 0;
    private Double selectedLatitude = null;
    private Double selectedLongitude = null;
    // שם מיקום קריא (לדוגמה "הרצל 12, רמת גן"). יכול להיות null אם
    // ה-reverse geocoding נכשל.
    private String selectedLocationName = null;

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
                        // השם נקבע ב-MapPickerActivity דרך reverse geocoding.
                        // יכול להיות null אם ה-geocoding נכשל.
                        selectedLocationName = data.getStringExtra(MapPickerActivity.EXTRA_LOCATION_NAME);
                        updateLocationText();
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
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

    /**
     * מסתירים את ה-Toolbar של GoalsActivity כשנכנסים למסך עריכה,
     * כי יש לטופס header משלו (✕ + "מטרה חדשה" + שמירה).
     * בלי זה היה לנו שני "headers" אחד מעל השני.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) actionBar.hide();
        }
    }

    /**
     * מחזירים את ה-Toolbar כשמתנתקים מהמסך (מעבר חזרה ל-Home).
     */
    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) actionBar.show();
        }
    }

    private void initializeViews(View view) {
        etName = view.findViewById(R.id.etName);
        etDescription = view.findViewById(R.id.etDescription);

        cgCategory = view.findViewById(R.id.cgCategory);

        cardGentle = view.findViewById(R.id.cardGentle);
        cardTough = view.findViewById(R.id.cardTough);

        cgDays = view.findViewById(R.id.cgDays);

        cardTime = view.findViewById(R.id.cardTime);
        cardLocation = view.findViewById(R.id.cardLocation);

        timeReminderLayout = view.findViewById(R.id.time_reminder_layout);
        tvTimePicker = view.findViewById(R.id.tvTimePicker);

        locationReminderLayout = view.findViewById(R.id.location_reminder_layout);
        btnSelectLocation = view.findViewById(R.id.btnSelectLocation);
        tvSelectedLocation = view.findViewById(R.id.tvSelectedLocation);
    }

    private void setupListeners() {
        View root = requireView();

        // כפתורי שמירה וביטול - הן בראש (X + Save Top) והן בתחתית
        ImageButton btnCancel = root.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        root.findViewById(R.id.btnSave).setOnClickListener(v -> saveGoal());
        root.findViewById(R.id.btnSaveTop).setOnClickListener(v -> saveGoal());

        tvTimePicker.setOnClickListener(v -> showTimePickerDialog());
        btnSelectLocation.setOnClickListener(v -> selectLocation());

        // לחיצה על כרטיסיית סגנון התראה (Gentle/Tough)
        cardGentle.setOnClickListener(v -> selectNotificationStyle(false));
        cardTough.setOnClickListener(v -> selectNotificationStyle(true));

        // לחיצה על כרטיסיית סוג תזכורת (Time/Location)
        cardTime.setOnClickListener(v -> selectReminderType(true));
        cardLocation.setOnClickListener(v -> selectReminderType(false));
    }

    /**
     * מטפל בבחירה של סגנון התראה - מסמן את הכרטיסייה הנבחרת
     * עם stroke אינדיגו עבה ורקע אינדיגו עדין, ומנקה את האחרת.
     */
    private void selectNotificationStyle(boolean tough) {
        isTough = tough;
        updateChoiceCard(cardGentle, !tough);
        updateChoiceCard(cardTough, tough);
    }

    /**
     * מטפל בבחירה של סוג תזכורת - גם מחליף את התצוגה בין שעה למיקום.
     */
    private void selectReminderType(boolean time) {
        isTimeReminder = time;
        updateChoiceCard(cardTime, time);
        updateChoiceCard(cardLocation, !time);
        // עדכון התצוגה והניקוי של הערכים הלא רלוונטיים
        if (time) {
            timeReminderLayout.setVisibility(View.VISIBLE);
            locationReminderLayout.setVisibility(View.GONE);
            selectedLatitude = null;
            selectedLongitude = null;
            updateLocationText();
        } else {
            timeReminderLayout.setVisibility(View.GONE);
            locationReminderLayout.setVisibility(View.VISIBLE);
            // לא מאפסים את השעה כדי שלא לאבד את הבחירה אם המשתמש מחליף הלוך וחזור
        }
    }

    /**
     * משנה את ה-stroke/רקע של כרטיסיית בחירה כדי לסמן selected/unselected.
     * נבחר: stroke אינדיגו 2dp + רקע אינדיגו 12% שקיפות.
     * לא נבחר: stroke ברירת מחדל + רקע surface.
     */
    private void updateChoiceCard(MaterialCardView card, boolean selected) {
        if (selected) {
            card.setStrokeColor(getResources().getColor(R.color.brand_indigo, null));
            card.setStrokeWidth((int) (2 * getResources().getDisplayMetrics().density));
            card.setCardBackgroundColor(0x1F6366F1); // אינדיגו עם 12% שקיפות
        } else {
            // החזרת ערכי ברירת המחדל
            int outlineColor = resolveThemeColor(com.google.android.material.R.attr.colorOutline);
            card.setStrokeColor(outlineColor);
            card.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
            int surfaceColor = resolveThemeColor(com.google.android.material.R.attr.colorSurface);
            card.setCardBackgroundColor(surfaceColor);
        }
    }

    /**
     * עזר: מקבל theme attribute (כמו colorSurface) ומחזיר את הצבע המתאים
     * לפי התמה הנוכחית (light/dark).
     */
    private int resolveThemeColor(int attrId) {
        android.util.TypedValue tv = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(attrId, tv, true);
        return tv.data;
    }

    private void selectLocation() {
        Intent intent = new Intent(getActivity(), MapPickerActivity.class);
        mapPickerLauncher.launch(intent);
    }

    private void updateLocationText() {
        if (selectedLatitude == null || selectedLongitude == null) {
            tvSelectedLocation.setText(R.string.edit_goal_no_location_selected);
            return;
        }
        // עדיפות לשם המיקום הקריא, ו-fallback לקואורדינטות
        if (selectedLocationName != null && !selectedLocationName.isEmpty()) {
            tvSelectedLocation.setText("📍 " + selectedLocationName);
        } else {
            tvSelectedLocation.setText(String.format(Locale.getDefault(),
                    "📍 %.4f, %.4f", selectedLatitude, selectedLongitude));
        }
    }

    private void showTimePickerDialog() {
        final int hour = selectedHour != null ? selectedHour : 8;
        final int minute = selectedMinute != null ? selectedMinute : 0;

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                getContext(),
                (view, hourOfDay, min) -> {
                    selectedHour = hourOfDay;
                    selectedMinute = min;
                    updateTimeText();
                },
                hour, minute, true);
        timePickerDialog.show();
    }

    private void updateTimeText() {
        if (selectedHour != null && selectedMinute != null) {
            tvTimePicker.setText(String.format(Locale.getDefault(),
                    "%02d:%02d", selectedHour, selectedMinute));
        } else {
            tvTimePicker.setText(R.string.edit_goal_pick_time);
        }
    }

    /**
     * טוען את הקטגוריות מ-DB ובונה chips דינמיים ב-cgCategory.
     */
    private void loadCategories() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Category> loaded = dao.getAllCategories();
            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    categoriesList = loaded != null ? loaded : new ArrayList<>();
                    buildCategoryChips();
                    loadGoalAndReminder();
                });
            }
        });
    }

    /**
     * בונה chip לכל קטגוריה ב-cgCategory. שומר על ה-id של הקטגוריה
     * בתור tag של ה-Chip כדי לקרוא אותו בשמירה.
     */
    private void buildCategoryChips() {
        cgCategory.removeAllViews();
        for (Category category : categoriesList) {
            Chip chip = new Chip(requireContext());
            chip.setText(translateCategoryToHebrew(category.name));
            chip.setCheckable(true);
            chip.setTag(category.id);   // שמירת ה-id כדי לאחזר אותו ב-saveGoal
            cgCategory.addView(chip);
        }
        // בחירה דיפולטית של הראשונה אם אין בחירה קודמת
        if (cgCategory.getChildCount() > 0 && cgCategory.getCheckedChipId() == View.NO_ID) {
            ((Chip) cgCategory.getChildAt(0)).setChecked(true);
            if (!categoriesList.isEmpty()) selectedCategoryId = categoriesList.get(0).id;
        }
    }

    /**
     * תרגום שם קטגוריה לעברית (לתצוגה ב-chip).
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

    private void loadGoalAndReminder() {
        if (goalId == -1) {
            // מצב יצירת מטרה חדשה - ברירת מחדל: עדין + לפי שעה
            currentGoalWithReminder = new GoalWithReminder();
            currentGoalWithReminder.goal = new Goal();
            currentGoalWithReminder.reminder = new Reminder();
            selectNotificationStyle(false);  // Gentle
            selectReminderType(true);        // Time
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

        // סימון הקטגוריה הקיימת
        selectedCategoryId = g.categoryId;
        markCategoryChip(g.categoryId);

        // סימון סגנון התראה לפי הערך השמור
        selectNotificationStyle("TOUGH".equalsIgnoreCase(g.notificationType));

        // טיפול ב-reminder
        if (currentGoalWithReminder.reminder != null) {
            Reminder r = currentGoalWithReminder.reminder;
            updateSelectedDays(r.days);

            if (r.latitude != null && r.longitude != null) {
                selectedLatitude = r.latitude;
                selectedLongitude = r.longitude;
                selectedLocationName = r.locationName;  // טעינת השם השמור
                selectReminderType(false);  // Location
            } else if (r.minuteOfDay != null) {
                selectedHour = r.minuteOfDay / 60;
                selectedMinute = r.minuteOfDay % 60;
                selectReminderType(true);   // Time
            } else {
                selectReminderType(true);   // ברירת מחדל
            }
            updateLocationText();
            updateTimeText();
        } else {
            selectReminderType(true);
        }
    }

    /**
     * סימון ה-chip של הקטגוריה לפי ה-id (שנשמר ב-tag).
     */
    private void markCategoryChip(long categoryId) {
        for (int i = 0; i < cgCategory.getChildCount(); i++) {
            Chip chip = (Chip) cgCategory.getChildAt(i);
            Object tag = chip.getTag();
            if (tag instanceof Long && ((Long) tag) == categoryId) {
                chip.setChecked(true);
                return;
            }
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

    /**
     * שולף את ה-id של הקטגוריה הנבחרת מתוך ה-chip שמסומן.
     */
    private long getSelectedCategoryId() {
        int checkedId = cgCategory.getCheckedChipId();
        if (checkedId == View.NO_ID) return -1;
        Chip checked = cgCategory.findViewById(checkedId);
        if (checked == null) return -1;
        Object tag = checked.getTag();
        return (tag instanceof Long) ? (Long) tag : -1;
    }

    private void saveGoal() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            etName.setError("יש להזין שם למטרה");
            return;
        }

        long categoryId = getSelectedCategoryId();
        if (categoryId < 0) {
            Toast.makeText(getContext(), "יש לבחור קטגוריה", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Integer> days = getSelectedDays();
        if (days.isEmpty()) {
            Toast.makeText(getContext(), "יש לבחור לפחות יום אחד לתזכורת", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isTimeReminder && (selectedHour == null || selectedMinute == null)) {
            Toast.makeText(getContext(), "יש לבחור שעה לתזכורת", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isTimeReminder && (selectedLatitude == null || selectedLongitude == null)) {
            Toast.makeText(getContext(), "יש לבחור מיקום לתזכורת", Toast.LENGTH_SHORT).show();
            return;
        }

        currentGoalWithReminder.goal.name = name;
        currentGoalWithReminder.goal.description = etDescription.getText().toString().trim();
        currentGoalWithReminder.goal.categoryId = categoryId;
        currentGoalWithReminder.goal.notificationType = isTough ? "TOUGH" : "GENTLE";
        currentGoalWithReminder.goal.active = true;

        if (currentGoalWithReminder.reminder == null) currentGoalWithReminder.reminder = new Reminder();

        currentGoalWithReminder.reminder.days = days;
        currentGoalWithReminder.reminder.minuteOfDay = isTimeReminder
                ? (selectedHour * 60 + selectedMinute) : 0;
        currentGoalWithReminder.reminder.latitude = isTimeReminder ? null : selectedLatitude;
        currentGoalWithReminder.reminder.longitude = isTimeReminder ? null : selectedLongitude;
        // שם המיקום נשמר רק לתזכורת מבוססת מיקום (יכול להיות null)
        currentGoalWithReminder.reminder.locationName = isTimeReminder ? null : selectedLocationName;

        Executors.newSingleThreadExecutor().execute(() -> {
            if (goalId == -1) {
                long newId = dao.insertGoalWithReminder(
                        currentGoalWithReminder.goal, currentGoalWithReminder.reminder);
                currentGoalWithReminder.goal.id = (int) newId;
            } else {
                geofenceHelper.removeGeofence(String.valueOf(goalId));
                dao.updateGoalWithReminder(
                        currentGoalWithReminder.goal, currentGoalWithReminder.reminder);
            }

            if (isTimeReminder) {
                NotificationScheduler.scheduleNotification(getContext(),
                        currentGoalWithReminder.goal, currentGoalWithReminder.reminder);
            } else {
                NotificationScheduler.cancelNotification(getContext(),
                        currentGoalWithReminder.goal);
            }

            if (!isTimeReminder) {
                geofenceHelper.addGeofence(currentGoalWithReminder.goal,
                        currentGoalWithReminder.reminder);
            }

            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> getParentFragmentManager().popBackStack());
            }
        });
    }
}
