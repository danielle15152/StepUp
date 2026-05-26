package com.example.stepup.ui;

import android.content.Context;
import android.content.res.ColorStateList;
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
        void onItemClicked(GoalWithReminder item); // New action
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

        if (gwr.reminder != null && gwr.reminder.days != null && !gwr.reminder.days.isEmpty()) {
            h.tvReminderDays.setText(formatDays(gwr.reminder.days));
            h.tvReminderDays.setVisibility(View.VISIBLE);

            if (gwr.reminder.latitude != null && gwr.reminder.longitude != null) {
                h.tvReminderSpecifics.setText(String.format(Locale.getDefault(), "Lat: %.2f, Lon: %.2f", gwr.reminder.latitude, gwr.reminder.longitude));
                h.tvReminderSpecifics.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_dialog_map, 0, 0, 0);
            } else if(gwr.reminder.minuteOfDay != null) {
                String timeStr = formatMinuteOfDay(gwr.reminder.minuteOfDay);
                h.tvReminderSpecifics.setText("at " + timeStr);
                h.tvReminderSpecifics.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_my_calendar, 0, 0, 0);
            }
            h.tvReminderSpecifics.setVisibility(View.VISIBLE);
        } else {
            h.tvReminderDays.setVisibility(View.GONE);
            h.tvReminderSpecifics.setVisibility(View.GONE);
        }

        setCategoryIcon(h.ivCategoryIcon, gwr.goal.categoryId);
        
        // Set listeners
        h.itemView.setOnClickListener(v -> listener.onItemClicked(gwr)); // Main item click
        h.btnEdit.setOnClickListener(v -> listener.onEditClicked(gwr));
        h.btnDelete.setOnClickListener(v -> listener.onDeleteClicked(gwr));
    }

    private void setCategoryIcon(ImageView imageView, long categoryId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            String categoryName = dao.getCategoryNameById(categoryId);
            imageView.post(() -> {
                Context context = imageView.getContext();
                int iconResId = R.drawable.ic_category_health;
                int colorResId = R.color.category_health;

                if (categoryName != null) {
                    switch (categoryName.toLowerCase(Locale.ROOT)) {
                        case "education": iconResId = R.drawable.ic_category_education; colorResId = R.color.category_education; break;
                        case "sports": iconResId = R.drawable.ic_category_sports; colorResId = R.color.category_sports; break;
                        case "finance": iconResId = R.drawable.ic_category_finance; colorResId = R.color.category_finance; break;
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
        TextView tvGoalName, tvReminderDays, tvReminderSpecifics;
        ImageButton btnEdit, btnDelete;

        GoalsViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvGoalName = itemView.findViewById(R.id.tvGoalName);
            tvReminderDays = itemView.findViewById(R.id.tvReminderDays);
            tvReminderSpecifics = itemView.findViewById(R.id.tvReminderSpecifics);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    private static String safe(String s) { return (s == null) ? "" : s; }

    private static String formatDays(List<Integer> days) {
        if (days == null || days.isEmpty()) return "No days";
        String[] names = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        StringBuilder sb = new StringBuilder();
        days.sort(Integer::compareTo);
        for (int d : days) {
            if (sb.length() > 0) sb.append(", ");
            sb.append((d >= 0 && d <= 6) ? names[d] : "");
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
