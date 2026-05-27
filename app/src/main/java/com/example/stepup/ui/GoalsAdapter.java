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

/**
 * Adapter של רשימת המטרות.
 *
 * הקסם של העיצוב החדש קורה ב-onBindViewHolder:
 * כל כרטיסייה מקבלת רקע, אייקון ופורמט שמשלבים את שלושת הקריטריונים -
 *   1) שעת התזכורת (או מבוסס-מיקום)  → קובע את הגרדיאנט
 *   2) קטגוריית המטרה               → קובע את האייקון הדקורטיבי
 *   3) notificationType (GENTLE/TOUGH) → קובע את צורת הכרטיסייה
 */
public class GoalsAdapter extends RecyclerView.Adapter<GoalsAdapter.GoalsViewHolder> {

    public interface GoalActionsListener {
        void onItemClicked(GoalWithReminder item);
        void onEditClicked(GoalWithReminder item);
        void onDeleteClicked(GoalWithReminder item);
    }

    private final List<GoalWithReminder> items = new ArrayList<>();
    private final GoalActionsListener listener;
    private final Dao dao;

    // ===== קבועי טקסט בעברית =====
    // ימי השבוע בעברית - 7 ימים. אינדקס 0=ראשון לפי הקונבנציה של המערכת.
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

        // ============================================
        // שכבה 1: צורת הכרטיסייה - זהה ל-GENTLE ול-TOUGH
        // (הבדל בין השניים יבוא רק דרך הצבע - ראי setupIcons למטה).
        // ============================================
        h.cardGoal.setRadius(dpToPx(context, 24));
        h.cardGoal.setStrokeWidth(0);
        h.cardGoal.setCardElevation(dpToPx(context, 4f));

        // ============================================
        // שכבה 2: רקע הכרטיסייה - גרדיאנט דינמי לפי קטגוריה + isTough
        // הצבעים נקבעים בתוך setupIcons (אחרי טעינת שם הקטגוריה מ-DB)
        // הטקסטים תמיד לבנים בזכות ה-scrim שב-XML.
        // ============================================
        boolean isTough = "TOUGH".equalsIgnoreCase(gwr.goal.notificationType);

        // ============================================
        // התוכן: שם המטרה
        // ============================================
        h.tvGoalName.setText(safe(gwr.goal.name));

        // ============================================
        // התוכן: תווית עליונה ("07:30 · GENTLE" או "📍 LOCATION · TOUGH")
        // ============================================
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

        // ============================================
        // התוכן: ימי השבוע + מידע נוסף (שעה/מיקום)
        // ============================================
        if (reminder != null && reminder.days != null && !reminder.days.isEmpty()) {
            h.tvReminderDays.setText(formatDaysHebrew(reminder.days));
            h.tvReminderDays.setVisibility(View.VISIBLE);
        } else {
            h.tvReminderDays.setVisibility(View.GONE);
        }

        // ה-drawables (clock/pin) כבר לבן ב-vector שלהם, לכן אין צורך ב-tint.
        if (isLocationBased) {
            // עדיפות: שם מיקום קריא; fallback: קואורדינטות.
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

        // ============================================
        // האייקונים: קטגוריה (בקדמה, קטן) + דקורציה (ברקע, גדול)
        // אם isTough: אייקון הקטגוריה הקטן מוחלף בברק על רקע כתום
        // (badge TOUGH). הקטגוריה עדיין נראית ברקע כדקורציה.
        // ============================================
        setupIcons(h, gwr.goal.categoryId, isLocationBased, isTough);

        // ============================================
        // Listeners
        // ============================================
        h.cardGoal.setOnClickListener(v -> listener.onItemClicked(gwr));
        h.btnEdit.setOnClickListener(v -> listener.onEditClicked(gwr));
        h.btnDelete.setOnClickListener(v -> listener.onDeleteClicked(gwr));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ====================================================================
    // לוגיקה: בניית הרקע הצבעוני (גרדיאנט)
    // ====================================================================

    /**
     * בונה גרדיאנט דינמי לפי הקטגוריה ומצב התראה.
     *
     * - GENTLE: גוון בהיר עד בינוני של צבע הקטגוריה
     * - TOUGH: גוון כהה עד מאוד כהה של אותו צבע
     *
     * הקטגוריה היא ה"זהות" של הכרטיסייה (ניתן לזהות אותה מבט אחד),
     * וה-TOUGH הוא ה"עוצמה" (כהה יותר).
     */
    static GradientDrawable buildCardGradient(Context ctx, String categoryName,
                                              boolean isTough, float cornerRadiusDp) {
        int[] colors = colorsForCategory(categoryName, isTough);
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, colors);
        gd.setCornerRadius(dpToPx(ctx, cornerRadiusDp));
        return gd;
    }

    /**
     * מחזיר את 2 הצבעים (start, end) של הגרדיאנט עבור קטגוריה ומצב נתון.
     * הסדר: בהיר → כהה (אלכסון מהפינה השמאלית-עליונה לפינה הימנית-תחתונה).
     */
    public static int[] colorsForCategory(String categoryName, boolean isTough) {
        if (categoryName == null) categoryName = "health";
        switch (categoryName.toLowerCase(Locale.ROOT)) {
            case "education":
                // אינדיגו - חינוך/ידע
                return isTough
                        ? new int[]{0xFF4338CA, 0xFF1E1B4B}   // אינדיגו עמוק → כמעט שחור-סגול
                        : new int[]{0xFF818CF8, 0xFF6366F1};  // אינדיגו בהיר → אינדיגו רגיל
            case "sports":
                // מנטה/טורקיז - ספורט/אנרגיה
                return isTough
                        ? new int[]{0xFF0F766E, 0xFF042F2E}   // טורקיז כהה → שחור-ירוק
                        : new int[]{0xFF5EEAD4, 0xFF14B8A6};  // טורקיז בהיר → טורקיז רגיל
            case "finance":
                // זהב/כתום - פיננסים
                return isTough
                        ? new int[]{0xFFB45309, 0xFF451A03}   // כתום-חום עמוק → חום שוקולד
                        : new int[]{0xFFFCD34D, 0xFFF59E0B};  // זהב → כתום-זהב
            default:
                // health (ברירת מחדל) - ורוד
                return isTough
                        ? new int[]{0xFFBE185D, 0xFF500724}   // ורוד עמוק → אדום-יין
                        : new int[]{0xFFF472B6, 0xFFDB2777};  // ורוד בהיר → ורוד עמוק
        }
    }

    // ====================================================================
    // לוגיקה: בחירת האייקונים
    // ====================================================================

    /**
     * מגדיר את הצבע של הכרטיסייה ואת שני האייקונים:
     *  - cardBackground: גרדיאנט דינמי לפי קטגוריה + isTough
     *  - ivCategoryIcon: אייקון הקטגוריה (תמיד - גם ב-TOUGH).
     *  - ivDecoration:    אייקון גדול חצי-שקוף ברקע. הקטגוריה,
     *                     או סיכת מיקום אם זו תזכורת-מיקום.
     *
     * זה ירוץ ב-background thread כי הקטגוריה חיה ב-DB,
     * אבל ה-UI updates מתבצעים ב-main thread דרך post().
     */
    private void setupIcons(GoalsViewHolder h, long categoryId,
                            boolean isLocationBased, boolean isTough) {
        Context ctx = h.itemView.getContext();

        Executors.newSingleThreadExecutor().execute(() -> {
            String categoryName = dao.getCategoryNameById(categoryId);
            int categoryIcon = R.drawable.ic_category_health;
            if (categoryName != null) {
                switch (categoryName.toLowerCase(Locale.ROOT)) {
                    case "education": categoryIcon = R.drawable.ic_category_education; break;
                    case "sports":    categoryIcon = R.drawable.ic_category_sports;    break;
                    case "finance":   categoryIcon = R.drawable.ic_category_finance;   break;
                    // ברירת מחדל: health
                }
            }
            final int finalCategoryIcon = categoryIcon;
            final String finalCategoryName = categoryName;

            // עדכון UI ב-Main thread
            h.ivCategoryIcon.post(() -> {
                // אייקון הקטגוריה - תמיד מציג את הקטגוריה האמיתית
                h.ivCategoryIcon.setImageResource(finalCategoryIcon);
                h.ivCategoryIcon.setBackgroundResource(R.drawable.bg_category_chip);

                // הרקע הצבעוני - גרדיאנט דינמי לפי קטגוריה ו-isTough
                h.cardBackground.setBackground(
                        buildCardGradient(ctx, finalCategoryName, isTough, 24f));

                // אייקון הדקורציה הגדול ברקע
                h.ivDecoration.setImageResource(
                        isLocationBased ? R.drawable.ic_decor_pin : finalCategoryIcon);
            });
        });
    }

    // ====================================================================
    // עזרים: פורמט טקסט
    // ====================================================================

    private static String safe(String s) { return (s == null) ? "" : s; }

    /**
     * ממיר רשימת ימי שבוע (0=ראשון..6=שבת) למחרוזת עברית קצרה.
     */
    private static String formatDaysHebrew(List<Integer> days) {
        if (days == null || days.isEmpty()) return "";
        days.sort(Integer::compareTo);
        // כל הימים = "כל יום"
        if (days.size() == 7) return "כל יום";
        StringBuilder sb = new StringBuilder();
        for (int d : days) {
            if (sb.length() > 0) sb.append(", ");
            if (d >= 0 && d <= 6) sb.append(DAYS_HE[d]);
        }
        return sb.toString();
    }

    /**
     * ממיר מספר דקות מתחילת היום ל-"HH:MM".
     */
    private static String formatMinuteOfDay(Integer minuteOfDay) {
        if (minuteOfDay == null) return "";
        int h = Math.max(0, minuteOfDay) / 60;
        int m = Math.max(0, minuteOfDay) % 60;
        return String.format(Locale.US, "%02d:%02d", h, m);
    }

    // ====================================================================
    // עזרים: מתמטיקה של גרפיקה
    // ====================================================================

    /**
     * המרת dp ל-pixels. צריך כי MaterialCardView מצפה לערכים בפיקסלים.
     */
    private static float dpToPx(Context context, float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics());
    }

    // ====================================================================
    // ViewHolder
    // ====================================================================

    static class GoalsViewHolder extends RecyclerView.ViewHolder {
        // הוספתי את כל הרכיבים מה-XML החדש.
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
