package com.example.stepup.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepup.R;
import com.example.stepup.data.Dao;
import com.example.stepup.data.entities.GoalWithReminder;
import com.example.stepup.data.entities.Reminder;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

// ה-Adapter שמזין את ה-RecyclerView של רשימת המטרות במסך הבית.
// כל כרטיסייה מקבלת צבע לפי הקטגוריה ועוצמה לפי TOUGH/GENTLE.
public class GoalsAdapter extends RecyclerView.Adapter<GoalsAdapter.GoalsViewHolder> {

    public interface GoalActionsListener {
        void onItemClicked(GoalWithReminder item);
        void onEditClicked(GoalWithReminder item);
        void onDeleteClicked(GoalWithReminder item);
    }

    private final List<GoalWithReminder> items = new ArrayList<>();
    private final GoalActionsListener listener;
    private final Dao dao;

    // Executor יחיד משותף לכל הכרטיסיות.
    // במקום ליצור thread חדש לכל כרטיסייה (שגרם לעומס),
    // כולן מחכות בתור אחד ורצות אחת-אחת.
    private final java.util.concurrent.ExecutorService iconExecutor =
            Executors.newSingleThreadExecutor();

    // ימי השבוע בעברית. אינדקס 0=ראשון
    private static final String[] DAYS_HE = {"א׳", "ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ש׳"};

    public GoalsAdapter(GoalActionsListener listener, Dao dao) {
        this.listener = listener;
        this.dao = dao;
    }

    public void setItems(List<GoalWithReminder> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GoalsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_goal, parent, false);
        return new GoalsViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull GoalsViewHolder h, int position) {
        GoalWithReminder gwr = items.get(position);
        Context context = h.itemView.getContext();
        Reminder reminder = gwr.reminder;
        boolean isLocationBased = reminder != null
                && reminder.latitude != null
                && reminder.longitude != null;

        // צורת הכרטיסייה זהה ב-GENTLE וב-TOUGH. רק הצבע משתנה ביניהם.
        h.cardGoal.setRadius(dpToPx(context, 24));
        h.cardGoal.setStrokeWidth(0);
        h.cardGoal.setCardElevation(dpToPx(context, 4f));

        boolean isTough = "TOUGH".equalsIgnoreCase(gwr.goal.notificationType);

        h.tvGoalName.setText(safe(gwr.goal.name));

        // התווית העליונה משלבת שעה (או "LOCATION") עם סגנון ההתראה
        String styleLabel = isTough ? "TOUGH" : "GENTLE";
        String topLabel;
        if (isLocationBased) {
            topLabel = "📍 LOCATION · " + styleLabel;
        } else if (reminder != null && reminder.minuteOfDay != null) {
            topLabel = formatMinuteOfDay(reminder.minuteOfDay) + " · " + styleLabel;
        } else {
            topLabel = styleLabel;
        }
        h.tvLabel.setText(topLabel);

        if (reminder != null && reminder.days != null && !reminder.days.isEmpty()) {
            h.tvReminderDays.setText(formatDaysHebrew(reminder.days));
            h.tvReminderDays.setVisibility(View.VISIBLE);
        } else {
            h.tvReminderDays.setVisibility(View.GONE);
        }

        if (isLocationBased) {
            // מעדיפים להציג שם מקום (אם נשמר), אחרת קואורדינטות
            String locText;
            if (reminder.locationName != null && !reminder.locationName.isEmpty()) {
                locText = reminder.locationName;
            } else {
                locText = String.format(Locale.getDefault(),
                        "%.4f, %.4f", reminder.latitude, reminder.longitude);
            }
            h.tvReminderSpecifics.setText(locText);
            h.tvReminderSpecifics.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_pin_small, 0, 0, 0);
            h.tvReminderSpecifics.setVisibility(View.VISIBLE);
        } else if (reminder != null && reminder.minuteOfDay != null) {
            h.tvReminderSpecifics.setText(formatMinuteOfDay(reminder.minuteOfDay));
            h.tvReminderSpecifics.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_clock_small, 0, 0, 0);
            h.tvReminderSpecifics.setVisibility(View.VISIBLE);
        } else {
            h.tvReminderSpecifics.setVisibility(View.GONE);
        }

        // טעינת הקטגוריה מ-DB וצביעת הכרטיסייה. רץ ב-background
        setupIcons(h, gwr.goal.categoryId, isLocationBased, isTough);

        h.cardGoal.setOnClickListener(v -> listener.onItemClicked(gwr));
        h.btnEdit.setOnClickListener(v -> listener.onEditClicked(gwr));
        h.btnDelete.setOnClickListener(v -> listener.onDeleteClicked(gwr));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // בונה גרדיאנט אלכסוני לכרטיסייה. הצבעים נבחרים לפי הקטגוריה,
    // ו-TOUGH מקבל גרסה כהה משמעותית של אותה משפחת צבעים.
    static GradientDrawable buildCardGradient(Context ctx, String categoryName,
                                              boolean isTough, float cornerRadiusDp) {
        int[] colors = colorsForCategory(categoryName, isTough);
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, colors);
        gd.setCornerRadius(dpToPx(ctx, cornerRadiusDp));
        return gd;
    }

    // מחזיר זוג צבעים (start, end) של הגרדיאנט לכל קומבינציה של קטגוריה ו-isTough
    public static int[] colorsForCategory(String categoryName, boolean isTough) {
        if (categoryName == null) categoryName = "health";
        switch (categoryName.toLowerCase(Locale.ROOT)) {
            case "education":
                return isTough
                        ? new int[]{0xFF4338CA, 0xFF1E1B4B}   // אינדיגו עמוק
                        : new int[]{0xFF818CF8, 0xFF6366F1};  // אינדיגו רגיל
            case "sports":
                return isTough
                        ? new int[]{0xFF0F766E, 0xFF042F2E}   // טורקיז כהה
                        : new int[]{0xFF5EEAD4, 0xFF14B8A6};  // מנטה
            case "finance":
                return isTough
                        ? new int[]{0xFFB45309, 0xFF451A03}   // חום-שוקולד
                        : new int[]{0xFFFCD34D, 0xFFF59E0B};  // זהב
            default:
                return isTough
                        ? new int[]{0xFFBE185D, 0xFF500724}   // אדום-יין
                        : new int[]{0xFFF472B6, 0xFFDB2777};  // ורוד
        }
    }

    // טוען את שם הקטגוריה מ-DB ואז מצייר את הצבעים והאייקונים.
    // הקריאה ל-DB ב-Executor כי אסורה גישה ל-DB מה-Main Thread.
    private void setupIcons(GoalsViewHolder h, long categoryId,
                            boolean isLocationBased, boolean isTough) {
        Context ctx = h.itemView.getContext();

        iconExecutor.execute(() -> {
            String categoryName = dao.getCategoryNameById(categoryId);
            int categoryIcon = R.drawable.ic_category_health;
            if (categoryName != null) {
                switch (categoryName.toLowerCase(Locale.ROOT)) {
                    case "education": categoryIcon = R.drawable.ic_category_education; break;
                    case "sports":    categoryIcon = R.drawable.ic_category_sports;    break;
                    case "finance":   categoryIcon = R.drawable.ic_category_finance;   break;
                }
            }
            final int finalCategoryIcon = categoryIcon;
            final String finalCategoryName = categoryName;

            h.ivCategoryIcon.post(() -> {
                h.ivCategoryIcon.setImageResource(finalCategoryIcon);
                h.ivCategoryIcon.setBackgroundResource(R.drawable.bg_category_chip);

                h.cardBackground.setBackground(
                        buildCardGradient(ctx, finalCategoryName, isTough, 24f));

                // אם זו מטרת מיקום - האייקון הגדול ברקע הוא סיכה.
                // אחרת - אייקון הקטגוריה (גם הוא מופיע בקדמה, אבל ברקע הוא חצי-שקוף).
                h.ivDecoration.setImageResource(
                        isLocationBased ? R.drawable.ic_decor_pin : finalCategoryIcon);
            });
        });
    }

    private static String safe(String s) { return (s == null) ? "" : s; }

    // מקבל רשימת מספרים (0=ראשון..6=שבת) ומחזיר מחרוזת כמו "ב׳, ד׳, ו׳".
    // אם כל הימים נבחרו - מציג "כל יום" במקום הרשימה המלאה.
    private static String formatDaysHebrew(List<Integer> days) {
        if (days == null || days.isEmpty()) return "";
        days.sort(Integer::compareTo);
        if (days.size() == 7) return "כל יום";
        StringBuilder sb = new StringBuilder();
        for (int d : days) {
            if (sb.length() > 0) sb.append(", ");
            if (d >= 0 && d <= 6) sb.append(DAYS_HE[d]);
        }
        return sb.toString();
    }

    // מספר דקות מתחילת היום (480) -> "08:00"
    private static String formatMinuteOfDay(Integer minuteOfDay) {
        if (minuteOfDay == null) return "";
        int h = Math.max(0, minuteOfDay) / 60;
        int m = Math.max(0, minuteOfDay) % 60;
        return String.format(Locale.US, "%02d:%02d", h, m);
    }

    // המרת dp לפיקסלים - צריך כי כל המידות בקוד מחושבות בפיקסלים פיזיים
    private static float dpToPx(Context context, float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics());
    }

    static class GoalsViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardGoal;
        final FrameLayout cardBackground;
        final ImageView ivDecoration;
        final ImageView ivCategoryIcon;
        final TextView tvLabel;
        final TextView tvGoalName;
        final TextView tvReminderDays;
        final TextView tvReminderSpecifics;
        final ImageButton btnEdit;
        final ImageButton btnDelete;

        GoalsViewHolder(@NonNull View itemView) {
            super(itemView);
            cardGoal = (MaterialCardView) itemView;
            cardBackground = itemView.findViewById(R.id.cardBackground);
            ivDecoration = itemView.findViewById(R.id.ivDecoration);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvLabel = itemView.findViewById(R.id.tvLabel);
            tvGoalName = itemView.findViewById(R.id.tvGoalName);
            tvReminderDays = itemView.findViewById(R.id.tvReminderDays);
            tvReminderSpecifics = itemView.findViewById(R.id.tvReminderSpecifics);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
