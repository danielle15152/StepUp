package com.example.stepup.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
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

        // Enable zoom controls
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

    /**
     * חיפוש מיקום לפי שם.
     * Geocoder עושה קריאת רשת, לכן הוא חייב לרוץ ב-background thread
     * (אחרת חסום ה-UI ואף ייזרק DEADLINE_EXCEEDED).
     */
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
                        Toast.makeText(getContext(), "המיקום לא נמצא",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(),
                                "שגיאה בחיפוש המיקום - בדקי את החיבור לאינטרנט",
                                Toast.LENGTH_SHORT).show());
            }
        });
    }


    private void loadLocationGoals() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<GoalWithReminder> locationGoals = dao.getGoalsWithLocationReminders();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (googleMap != null && !locationGoals.isEmpty()) {
                        Context ctx = requireContext();
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        for (GoalWithReminder goal : locationGoals) {
                            if (goal.reminder != null
                                    && goal.reminder.latitude != null
                                    && goal.reminder.longitude != null) {
                                LatLng position = new LatLng(
                                        goal.reminder.latitude,
                                        goal.reminder.longitude);

                                // Icon מותאם: pill לבן עם נקודה אדומה + שם המטרה.
                                // anchor=0.5f,1f מצמיד את התחתית של ה-icon לנקודה במפה.
                                BitmapDescriptor icon = createLabelMarker(ctx, goal.goal.name);

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

    /**
     * בונה Bitmap שמשמש כ-icon למרקר על המפה.
     *
     * המבנה (משמאל לימין):
     *   [נקודה אדומה] [שם המטרה]
     * הכל בתוך "pill" (מלבן עם פינות מעוגלות) לבן עם מסגרת אינדיגו.
     *
     * אני בונה את זה ב-Canvas בקוד במקום בקובץ XML, כי Google Maps
     * דורש BitmapDescriptor (תמונת רסטר) ולא ניתן להעביר View ישירות
     * ל-marker. בכל פעם שמוסיפים marker חדש, אנחנו מציירים תמונה חדשה.
     */
    private static BitmapDescriptor createLabelMarker(Context ctx, String label) {
        if (label == null) label = "מטרה";
        // לחיתוך שמות ארוכים מדי כדי שה-pill לא ייצא מהמסך
        String text = label.length() > 22 ? label.substring(0, 20) + "…" : label;

        // ===== מדידת הטקסט =====
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF0F1729);  // ink (טקסט כהה)
        textPaint.setTextSize(dpToPx(ctx, 13f));
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        float textWidth = textPaint.measureText(text);

        // ===== מימדים =====
        float hPad = dpToPx(ctx, 12f);    // ריווח אופקי בקצוות
        float vPad = dpToPx(ctx, 8f);     // ריווח אנכי
        float dotRadius = dpToPx(ctx, 4.5f);  // רדיוס הנקודה האדומה
        float dotTextGap = dpToPx(ctx, 8f);   // רווח בין הנקודה לטקסט

        // הרוחב הכולל: padding + נקודה + רווח + טקסט + padding
        int width = (int) Math.ceil(hPad + dotRadius * 2 + dotTextGap + textWidth + hPad);
        int height = (int) Math.ceil(textPaint.getTextSize() + 2 * vPad);

        // ===== Canvas והציור =====
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // ה-pill הלבן עצמו
        Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pillPaint.setColor(Color.WHITE);
        float strokeWidth = dpToPx(ctx, 1.5f);
        RectF rect = new RectF(
                strokeWidth / 2f,
                strokeWidth / 2f,
                width - strokeWidth / 2f,
                height - strokeWidth / 2f);
        canvas.drawRoundRect(rect, height / 2f, height / 2f, pillPaint);

        // ה-stroke בצבע אינדיגו
        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(0xFF6366F1);  // indigo
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(strokeWidth);
        canvas.drawRoundRect(rect, height / 2f, height / 2f, strokePaint);

        // הנקודה האדומה הקטנה משמאל
        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(0xFFEF4444);  // status_error - אדום
        float dotX = hPad + dotRadius;
        float dotY = height / 2f;
        canvas.drawCircle(dotX, dotY, dotRadius, dotPaint);

        // טקסט שם המטרה - מיושר אנכית למרכז ה-pill
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = height / 2f - (fm.ascent + fm.descent) / 2f;
        float textX = dotX + dotRadius + dotTextGap;
        canvas.drawText(text, textX, textY, textPaint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    /**
     * המרת dp ל-pixels. צריך כי כל המדידות בציור נעשות בפיקסלים,
     * אבל אנחנו רוצים שהמראה יהיה זהה בכל צפיפות מסך.
     */
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
        return true; // Return true to indicate we have handled the event
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
