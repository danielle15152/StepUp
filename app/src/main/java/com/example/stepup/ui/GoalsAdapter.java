package com.example.stepup.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.stepup.R;
import com.example.stepup.data.Dao;
import com.example.stepup.data.entities.GoalWithReminder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class GoalsAdapter extends RecyclerView.Adapter<GoalsAdapter.GoalsViewHolder> {

    public interface GoalActionsListener {
        void onEditClicked(GoalWithReminder item);
        void onDeleteClicked(GoalWithReminder item);
    }

    private final List<GoalWithReminder> items = new ArrayList<>();
    private final GoalActionsListener listener;
    private final Dao dao;

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

        h.tvGoalName.setText(safe(gwr.goal.name));

        // Format reminder details
        if (gwr.reminder != null && gwr.reminder.days != null && !gwr.reminder.days.isEmpty()) {
            String daysStr = formatDays(gwr.reminder.days);
            String timeStr = formatMinuteOfDay(gwr.reminder.minuteOfDay);
            h.tvReminderDetails.setText(String.format("%s at %s", daysStr, timeStr));
            h.tvReminderDetails.setVisibility(View.VISIBLE);
        } else {
            h.tvReminderDetails.setVisibility(View.GONE);
        }

        // Set category icon and color
        setCategoryIcon(h.ivCategoryIcon, gwr.goal.categoryId);


        h.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClicked(gwr);
        });

        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClicked(gwr);
        });
    }

    private void setCategoryIcon(ImageView imageView, long categoryId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            String categoryName = dao.getCategoryNameById(categoryId);
            imageView.post(() -> {
                Context context = imageView.getContext();
                int iconResId = R.drawable.ic_category_health; // Default icon
                int colorResId = R.color.category_health; // Default color

                if (categoryName != null) {
                    switch (categoryName.toLowerCase(Locale.ROOT)) {
                        case "education":
                            iconResId = R.drawable.ic_category_education;
                            colorResId = R.color.category_education;
                            break;
                        case "sports":
                            iconResId = R.drawable.ic_category_sports;
                            colorResId = R.color.category_sports;
                            break;
                        case "finance":
                            iconResId = R.drawable.ic_category_finance;
                            colorResId = R.color.category_finance;
                            break;
                    }
                }
                imageView.setImageResource(iconResId);
                imageView.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, colorResId)));
            });
        });
    }


    @Override
    public int getItemCount() {
        return items.size();
    }

    static class GoalsViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCategoryIcon;
        TextView tvGoalName, tvReminderDetails;
        ImageButton btnEdit, btnDelete;

        GoalsViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvGoalName = itemView.findViewById(R.id.tvGoalName);
            tvReminderDetails = itemView.findViewById(R.id.tvReminderDetails);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    private static String safe(String s) { return (s == null) ? "" : s; }

    private static String formatDays(List<Integer> days) {
        if (days == null || days.isEmpty()) return "No days";
        String[] names = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < days.size(); i++) {
            int d = days.get(i);
            String label = (d >= 0 && d <= 6) ? names[d] : "";
            if (!label.isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(label);
            }
        }
        return sb.toString();
    }

    private static String formatMinuteOfDay(int minuteOfDay) {
        int h = Math.max(0, minuteOfDay) / 60;
        int m = Math.max(0, minuteOfDay) % 60;
        return String.format(Locale.US, "%02d:%02d", h, m);
    }
}
