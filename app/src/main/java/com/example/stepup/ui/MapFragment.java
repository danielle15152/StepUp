package com.example.stepup.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.stepup.GoalDetailsActivity;
import com.example.stepup.R;
import com.example.stepup.data.AppDatabase;
import com.example.stepup.data.Dao;
import com.example.stepup.data.entities.GoalWithReminder;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static final String TAG = "MapFragment";

    private MapView mapView;
    private GoogleMap googleMap;
    private Dao dao;
    private SearchView searchView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        dao = AppDatabase.getDatabase(requireContext()).dao();
        
        searchView = view.findViewById(R.id.searchView);
        setupSearchView();

        return view;
    }
    
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        googleMap.getUiSettings().setZoomControlsEnabled(true);

        googleMap.setOnMarkerClickListener(this);
        loadLocationGoals();
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchLocation(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    // Geocoder עושה קריאת רשת, חייב לרוץ ב-background thread
    private void searchLocation(String locationName) {
        if (locationName == null || locationName.isEmpty()) {
            return;
        }
        if (googleMap == null || getContext() == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            try {
                List<Address> addressList = geocoder.getFromLocationName(locationName, 1);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (addressList != null && !addressList.isEmpty()) {
                        Address address = addressList.get(0);
                        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    } else {
                        Toast.makeText(getContext(), R.string.map_location_not_found,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), R.string.map_search_error,
                                Toast.LENGTH_SHORT).show());
            }
        });
    }


    private void loadLocationGoals() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<GoalWithReminder> locationGoals = dao.getGoalsWithLocationReminders();

            // טוענים את שמות הקטגוריות כאן כדי לא לעשות N+1 שאילתות ב-UI thread
            String[] categoryNames = new String[locationGoals.size()];
            for (int i = 0; i < locationGoals.size(); i++) {
                categoryNames[i] = dao.getCategoryNameById(locationGoals.get(i).goal.categoryId);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (googleMap != null && !locationGoals.isEmpty()) {
                        Context ctx = requireContext();
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        for (int i = 0; i < locationGoals.size(); i++) {
                            GoalWithReminder goal = locationGoals.get(i);
                            if (goal.reminder != null
                                    && goal.reminder.latitude != null
                                    && goal.reminder.longitude != null) {
                                LatLng position = new LatLng(
                                        goal.reminder.latitude,
                                        goal.reminder.longitude);

                                BitmapDescriptor icon = createLabelMarker(
                                        ctx, goal.goal.name, categoryNames[i]);

                                Marker marker = googleMap.addMarker(new MarkerOptions()
                                        .position(position)
                                        .icon(icon)
                                        .anchor(0.5f, 1f)
                                        .title(goal.goal.name));
                                if (marker != null) {
                                    marker.setTag((long) goal.goal.id);
                                }
                                builder.include(position);
                            }
                        }
                        LatLngBounds bounds = builder.build();
                        int padding = 100;
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
                    }
                });
            }
        });
    }

    @DrawableRes
    private static int iconForCategory(String categoryName) {
        if (categoryName == null) return R.drawable.ic_category_health;
        switch (categoryName.toLowerCase(Locale.ROOT)) {
            case "education": return R.drawable.ic_category_education;
            case "sports":    return R.drawable.ic_category_sports;
            case "finance":   return R.drawable.ic_category_finance;
            default:          return R.drawable.ic_category_health;
        }
    }

    @ColorInt
    private static int colorForCategory(String categoryName) {
        if (categoryName == null) return 0xFFF472B6;
        switch (categoryName.toLowerCase(Locale.ROOT)) {
            case "education": return 0xFF6366F1;
            case "sports":    return 0xFF14E0B1;
            case "finance":   return 0xFFFCD34D;
            default:          return 0xFFF472B6;
        }
    }

    // בונה ידנית את ה-Bitmap של ה-marker כי Google Maps לא מקבל View
    private static BitmapDescriptor createLabelMarker(Context ctx, String label,
                                                      String categoryName) {
        if (label == null) label = "מטרה";
        String text = label.length() > 22 ? label.substring(0, 20) + "…" : label;

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF0F1729);
        textPaint.setTextSize(dpToPx(ctx, 13f));
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        float textWidth = textPaint.measureText(text);

        float hPad = dpToPx(ctx, 8f);
        float vPad = dpToPx(ctx, 6f);
        float badgeSize = dpToPx(ctx, 28f);
        float iconInsetPad = dpToPx(ctx, 6f);
        float badgeTextGap = dpToPx(ctx, 8f);

        int width = (int) Math.ceil(hPad + badgeSize + badgeTextGap + textWidth + hPad);
        int height = (int) Math.ceil(Math.max(badgeSize, textPaint.getTextSize()) + 2 * vPad);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pillPaint.setColor(Color.WHITE);
        float strokeWidth = dpToPx(ctx, 1.5f);
        RectF rect = new RectF(
                strokeWidth / 2f,
                strokeWidth / 2f,
                width - strokeWidth / 2f,
                height - strokeWidth / 2f);
        canvas.drawRoundRect(rect, height / 2f, height / 2f, pillPaint);

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(0xFF6366F1);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(strokeWidth);
        canvas.drawRoundRect(rect, height / 2f, height / 2f, strokePaint);

        Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgePaint.setColor(colorForCategory(categoryName));
        float badgeCenterX = hPad + badgeSize / 2f;
        float badgeCenterY = height / 2f;
        canvas.drawCircle(badgeCenterX, badgeCenterY, badgeSize / 2f, badgePaint);

        Drawable categoryIcon = ContextCompat.getDrawable(ctx, iconForCategory(categoryName));
        if (categoryIcon != null) {
            categoryIcon = categoryIcon.mutate();
            categoryIcon.setColorFilter(
                    new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
            int iconLeft = (int) (badgeCenterX - (badgeSize / 2f - iconInsetPad));
            int iconTop = (int) (badgeCenterY - (badgeSize / 2f - iconInsetPad));
            int iconRight = (int) (badgeCenterX + (badgeSize / 2f - iconInsetPad));
            int iconBottom = (int) (badgeCenterY + (badgeSize / 2f - iconInsetPad));
            categoryIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            categoryIcon.draw(canvas);
        }

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = height / 2f - (fm.ascent + fm.descent) / 2f;
        float textX = hPad + badgeSize + badgeTextGap;
        canvas.drawText(text, textX, textY, textPaint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private static float dpToPx(Context ctx, float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                ctx.getResources().getDisplayMetrics());
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        Object tag = marker.getTag();
        if (tag instanceof Long) {
            long goalId = (Long) tag;
            Intent intent = new Intent(getActivity(), GoalDetailsActivity.class);
            intent.putExtra(GoalDetailsActivity.EXTRA_GOAL_ID, goalId);
            startActivity(intent);
        } else {
            Log.e(TAG, "Marker tag is not a Long or is null. Tag: " + tag);
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mapView != null) mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapView != null) mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }
}
