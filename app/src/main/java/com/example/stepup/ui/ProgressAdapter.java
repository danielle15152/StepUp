package com.example.stepup.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepup.R;
import com.example.stepup.data.model.GoalProgress;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter עבור רשימת התקדמות המטרות.
 *
 * כל כרטיסייה מציגה: שם המטרה, אחוז ההצלחה, פס התקדמות, וה-"X מתוך Y ימים".
 * צבע פס ההתקדמות נקבע דינמית לפי האחוז - feedback ויזואלי לרמת ההצלחה.
 */
public class ProgressAdapter extends RecyclerView.Adapter<ProgressAdapter.ProgressViewHolder> {

    private final List<GoalProgress> items = new ArrayList<>();

    public void setItems(List<GoalProgress> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProgressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_goal_progress, parent, false);
        return new ProgressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProgressViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * מחזיר את הצבע המתאים לפס ההתקדמות לפי האחוז.
     *   0-29%  → ורוד (רחוק מהיעד, צריך לדחוף)
     *   30-69% → צהוב (באמצע הדרך)
     *   70-99% → אינדיגו (כמעט שם!)
     *   100%   → מנטה (הצלחה מלאה)
     */
    @ColorRes
    private static int colorForPercent(int percent) {
        if (percent >= 100) return R.color.brand_mint;
        if (percent >= 70)  return R.color.brand_indigo;
        if (percent >= 30)  return R.color.brand_sun;
        return R.color.brand_pink;
    }

    static class ProgressViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvGoalName;
        private final TextView tvStats;
        private final TextView tvPercent;
        private final LinearProgressIndicator progressBar;

        public ProgressViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGoalName = itemView.findViewById(R.id.tvGoalName);
            tvStats = itemView.findViewById(R.id.tvStats);
            tvPercent = itemView.findViewById(R.id.tvPercent);
            progressBar = itemView.findViewById(R.id.progressBar);
        }

        public void bind(GoalProgress goalProgress) {
            Context context = itemView.getContext();

            tvGoalName.setText(goalProgress.getGoalName());

            int completions = goalProgress.getCompletions();
            int totalDays = goalProgress.getTotalDays();
            int percent = (totalDays > 0)
                    ? (int) Math.round((double) completions / totalDays * 100)
                    : 0;

            tvPercent.setText(percent + "%");
            // טקסט סטטיסטיקה בעברית: "X מתוך Y ימים", או "🔥 רצף!" עבור 100%
            String statsText;
            if (percent >= 100 && totalDays > 0) {
                statsText = String.format(Locale.getDefault(),
                        "%d מתוך %d ימים · 🔥 רצף!", completions, totalDays);
            } else {
                statsText = String.format(Locale.getDefault(),
                        "%d מתוך %d ימים", completions, totalDays);
            }
            tvStats.setText(statsText);

            // אנימציית התקדמות חלקה (setProgressCompat עם animate=true)
            progressBar.setProgressCompat(percent, true);

            // הגדרת צבע ה-indicator לפי האחוז
            int indicatorColor = ContextCompat.getColor(context, colorForPercent(percent));
            progressBar.setIndicatorColor(indicatorColor);
            // שינוי גם של צבע האחוז כדי לתת לזה דגש
            tvPercent.setTextColor(indicatorColor);
        }
    }
}
