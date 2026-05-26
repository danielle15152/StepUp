package com.example.stepup;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.stepup.ui.HomeFragment;
import com.example.stepup.ui.MapFragment;
import com.example.stepup.ui.ProgressFragment;
import com.example.stepup.ui.SettingsFragment;
import com.google.android.material.navigation.NavigationView;

public class GoalsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "GoalsActivity";
    private DrawerLayout drawerLayout;

    // Multiple permission launcher
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                Log.d(TAG, "Permission result received");
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: START");
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: super.onCreate finished");

        try {
            setContentView(R.layout.activity_goals);
            Log.d(TAG, "onCreate: setContentView finished");
        } catch (Exception e) {
            Log.e(TAG, "CRASH during setContentView", e);
            // If the app crashes here, this log will show the exception.
        }

        requestRequiredPermissions();
        Log.d(TAG, "onCreate: requestRequiredPermissions finished");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Log.d(TAG, "onCreate: Toolbar setup finished");

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        Log.d(TAG, "onCreate: NavigationView setup finished");

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        Log.d(TAG, "onCreate: ActionBarDrawerToggle setup finished");

        // Load the default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.nav_host_fragment, new HomeFragment()).commit();
            navigationView.setCheckedItem(R.id.nav_home);
            Log.d(TAG, "onCreate: Default fragment loaded");
        }

        Log.d(TAG, "onCreate: END");
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // ... (rest of the code is the same)
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            selectedFragment = new HomeFragment();
        } else if (itemId == R.id.nav_progress) {
            selectedFragment = new ProgressFragment();
        } else if (itemId == R.id.nav_map) {
            selectedFragment = new MapFragment();
        } else if (itemId == R.id.nav_settings) {
            selectedFragment = new SettingsFragment();
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.nav_host_fragment, selectedFragment).commit();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void requestRequiredPermissions() {
        Log.d(TAG, "requestRequiredPermissions: START");
        String[] requiredPermissions = {
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions = new String[]{ Manifest.permission.ACCESS_FINE_LOCATION };
        }

        requestPermissionLauncher.launch(requiredPermissions);
        Log.d(TAG, "requestRequiredPermissions: END");
    }
}
