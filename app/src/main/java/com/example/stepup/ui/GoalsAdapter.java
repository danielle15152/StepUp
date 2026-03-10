package com.example.stepup.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepup.R;
import com.example.stepup.data.entities.GoalWithReminder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GoalsAdapter extends RecyclerView.Adapter<GoalsAdapter.GoalsViewHolder> {//אדפטר- לוקח את הקוד ומעצב אותו ויזואלית על המסך

    public interface GoalActionsListener {//ממשק עם הפעולות שלוחצים על עריכה ומחיקה
        void onEditClicked(GoalWithReminder item);
        void onDeleteClicked(GoalWithReminder item);
    }

    private final List<GoalWithReminder> items = new ArrayList<>();
    private final GoalActionsListener listener;

    public GoalsAdapter(GoalActionsListener listener) {//מעבירים מאזין לאדפטק שידע למי לדווח כאשר קיימת לחיצה
        this.listener = listener;
    }

    public void setItems(List<GoalWithReminder> newItems) {//פעולה שמעדכנת את הרשימה ומורה למסך לצייר אותו מחדש
        items.clear();//לנקות את הישן
        if (newItems != null) items.addAll(newItems);//הוספת נתונים חדשים
        notifyDataSetChanged();//לרענן! המסך השתנה!
    }

    @NonNull
    @Override
    public GoalsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {//יצירה של פריט נוסף ברשימה
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_goal, parent, false);// לוקחים את עיצוב הפריט מהxml של שאר הפריטים הרשימה כדי לעצב אותו אותו הדבר
        return new GoalsViewHolder(v);//אובייקט שניתן לעבוד איתו
    }

    @Override
    public void onBindViewHolder(@NonNull GoalsViewHolder h, int position) {//מילוי נתונים באובייקט החדש שברשימה
        GoalWithReminder gwr = items.get(position);// שליפת הנתון מהיעד הנוכחי

        h.tvGoalName.setText(safe(gwr.goal.name));//לוקחת מהאדפטר את הפרטים למלא
        h.tvDescription.setText(safe(gwr.goal.description));//סייפ זו פעולה ששומרת שלא יקרוס אם יש טקסט ריק
        h.tvCategory.setText(safe(gwr.goal.category));

        if (gwr.goal.active) {
            h.tvActive.setText("ACTIVE");//אם המטרה פעילה
            h.tvActive.setAlpha(1f);
        } else {
            h.tvActive.setText("PAUSED");//אם המטרה לא פעילה
            h.tvActive.setAlpha(0.6f);//הכיתוב נעשה שקוף
        }

        if (gwr.reminder == null) {
            h.tvReminder.setText("No reminder");//אם אין תזכורות למטרה
            h.tvReminder.setAlpha(0.7f);
        } else {//אם יש תזכורות
            String daysStr = formatDays(gwr.reminder.days);//פעולה שהופכת את התאריך לקריא
            String timeStr = formatMinuteOfDay(gwr.reminder.minuteOfDay);//פעולה שהופכת אתהדקות לקריאות
            h.tvReminder.setText(daysStr + " @ " + timeStr);//הטקסט הקריא
            h.tvReminder.setAlpha(1f);
        }

        h.btnEdit.setOnClickListener(v -> {//האדפטר מעדכן את המאזין שלחצו על כפתור העריכה
            if (listener != null) listener.onEditClicked(gwr);
        });

        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClicked(gwr);//כנל לכפתור של מחיקה
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class GoalsViewHolder extends RecyclerView.ViewHolder {//מחלקה שמחזיקה את כל הview במשתנים במקום לחפש כל פעם בxml
        TextView tvGoalName, tvDescription, tvCategory, tvActive, tvReminder;
        TextView btnEdit, btnDelete;

        GoalsViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGoalName = itemView.findViewById(R.id.tvGoalName);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvActive = itemView.findViewById(R.id.tvActive);
            tvReminder = itemView.findViewById(R.id.tvReminder);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    private static String safe(String s) { return (s == null) ? "" : s; }

    private static String formatDays(List<Integer> days) {//פעולת עזר שמחליפה בין מספר ליום
        if (days == null || days.isEmpty()) return "No days";
        String[] names = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < days.size(); i++) {
            int d = days.get(i);
            String label = (d >= 0 && d <= 6) ? names[d] : ("Day " + d);
            if (i > 0) sb.append(", ");
            sb.append(label);
        }
        return sb.toString();
    }

    private static String formatMinuteOfDay(int minuteOfDay) {//פעולת עזר לספירת הדקות
        int h = Math.max(0, minuteOfDay) / 60;
        int m = Math.max(0, minuteOfDay) % 60;
        return String.format(Locale.US, "%02d:%02d", h, m);
    }
}
