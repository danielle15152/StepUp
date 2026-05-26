package com.example.stepup.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepup.R;
import com.example.stepup.data.model.GoalProgress;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProgressAdapter extends RecyclerView.Adapter<ProgressAdapter.ProgressViewHolder> {

    private final List<GoalProgress> items = new ArrayList<>();

    public void setItems(List<GoalProgress> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProgressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_goal_progress, parent, false);
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

    static class ProgressViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvGoalName;
        private final TextView tvStats;
        private final ProgressBar progressBar;

        public ProgressViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGoalName = itemView.findViewById(R.id.tvGoalName);
            tvStats = itemView.findViewById(R.id.tvStats);
            progressBar = itemView.findViewById(R.id.progressBar);
        }

        public void bind(GoalProgress goalProgress) {
            tvGoalName.setText(goalProgress.getGoalName());

            int completions = goalProgress.getCompletions();
            int totalDays = goalProgress.getTotalDays();
            int percentage = (totalDays > 0) ? (int) ((double) completions / totalDays * 100) : 0;
            
            String statsText = String.format(Locale.getDefault(), "Completed %d of %d days (%d%%)",
                    completions, totalDays, percentage);
            
            tvStats.setText(statsText);
            progressBar.setProgress(percentage);
        }
    }
}
