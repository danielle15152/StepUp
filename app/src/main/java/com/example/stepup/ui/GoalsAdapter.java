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

public class GoalsAdapter extends RecyclerView.Adapter<GoalsAdapter.GoalsViewHolder> {

    public interface GoalActionsListener {
        void onEditClicked(GoalWithReminder item);
        void onDeleteClicked(GoalWithReminder item);
    }

    private final List<GoalWithReminder> items = new ArrayList<>();
    private final GoalActionsListener listener;

    public GoalsAdapter(GoalActionsListener listener) {
        this.listener = listener;
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

        h.tvGoalName.setText(safe(gwr.goal.name));
        h.tvDescription.setText(safe(gwr.goal.description));
        h.tvCategory.setText(safe(gwr.goal.category));

        if (gwr.goal.active) {
            h.tvActive.setText("ACTIVE");
            h.tvActive.setAlpha(1f);
        } else {
            h.tvActive.setText("PAUSED");
            h.tvActive.setAlpha(0.6f);
        }

        if (gwr.reminder == null) {
            h.tvReminder.setText("No reminder");
            h.tvReminder.setAlpha(0.7f);
        } else {
            String daysStr = formatDays(gwr.reminder.days);
            String timeStr = formatMinuteOfDay(gwr.reminder.minuteOfDay);
            h.tvReminder.setText(daysStr + " @ " + timeStr);
            h.tvReminder.setAlpha(1f);
        }

        h.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClicked(gwr);
        });

        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClicked(gwr);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class GoalsViewHolder extends RecyclerView.ViewHolder {
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

    private static String formatDays(List<Integer> days) {
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

    private static String formatMinuteOfDay(int minuteOfDay) {
        int h = Math.max(0, minuteOfDay) / 60;
        int m = Math.max(0, minuteOfDay) % 60;
        return String.format(Locale.US, "%02d:%02d", h, m);
    }
}
