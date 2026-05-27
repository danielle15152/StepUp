package com.example.stepup.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.stepup.R;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "StepUpPrefs";
    private static final String NOTIFICATION_SOUND_KEY = "notification_sound_enabled";
    private static final String THEME_MODE_KEY = "theme_mode";

    private static final int THEME_LIGHT = AppCompatDelegate.MODE_NIGHT_NO;
    private static final int THEME_DARK = AppCompatDelegate.MODE_NIGHT_YES;
    private static final int THEME_AUTO = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

    private MaterialSwitch switchNotificationSound;
    private ChipGroup chipGroupTheme;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        switchNotificationSound = view.findViewById(R.id.switchNotificationSound);
        chipGroupTheme = view.findViewById(R.id.chipGroupTheme);

        loadCurrentValues();

        switchNotificationSound.setOnCheckedChangeListener(
                (buttonView, isChecked) -> saveNotificationSound(isChecked));

        chipGroupTheme.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            int themeMode;
            if (checkedId == R.id.chip_theme_light)      themeMode = THEME_LIGHT;
            else if (checkedId == R.id.chip_theme_dark)  themeMode = THEME_DARK;
            else                                          themeMode = THEME_AUTO;

            saveThemeMode(themeMode);
            AppCompatDelegate.setDefaultNightMode(themeMode);
        });
    }

    private void loadCurrentValues() {
        boolean soundEnabled = sharedPreferences.getBoolean(NOTIFICATION_SOUND_KEY, true);
        switchNotificationSound.setChecked(soundEnabled);

        int themeMode = sharedPreferences.getInt(THEME_MODE_KEY, THEME_AUTO);
        int chipToCheck;
        if (themeMode == THEME_LIGHT)      chipToCheck = R.id.chip_theme_light;
        else if (themeMode == THEME_DARK)  chipToCheck = R.id.chip_theme_dark;
        else                                chipToCheck = R.id.chip_theme_auto;
        chipGroupTheme.check(chipToCheck);
    }

    private void saveNotificationSound(boolean isEnabled) {
        sharedPreferences.edit().putBoolean(NOTIFICATION_SOUND_KEY, isEnabled).apply();
    }

    private void saveThemeMode(int themeMode) {
        sharedPreferences.edit().putInt(THEME_MODE_KEY, themeMode).apply();
    }
}
