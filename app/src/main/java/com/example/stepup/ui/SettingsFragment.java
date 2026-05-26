package com.example.stepup.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.stepup.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "StepUpPrefs";
    private static final String NOTIFICATION_SOUND_KEY = "notification_sound_enabled";

    private SwitchMaterial switchNotificationSound;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Find views
        switchNotificationSound = view.findViewById(R.id.switchNotificationSound);

        // Load the saved preference and set the switch state
        loadPreference();

        // Set a listener to save the preference when the switch is toggled
        switchNotificationSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference(isChecked);
        });
    }

    private void loadPreference() {
        // Get the saved value. Default to 'true' (enabled) if not found.
        boolean isSoundEnabled = sharedPreferences.getBoolean(NOTIFICATION_SOUND_KEY, true);
        switchNotificationSound.setChecked(isSoundEnabled);
    }

    private void savePreference(boolean isEnabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(NOTIFICATION_SOUND_KEY, isEnabled);
        editor.apply(); // Use apply() for asynchronous saving
    }
}
