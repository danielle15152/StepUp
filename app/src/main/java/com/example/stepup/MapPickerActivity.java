package com.example.stepup;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng selectedLocation;
    private Marker selectedLocationMarker;
    private FusedLocationProviderClient fusedLocationClient;

    public static final String EXTRA_LATITUDE = "extra_latitude";
    public static final String EXTRA_LONGITUDE = "extra_longitude";
    public static final String EXTRA_LOCATION_NAME = "extra_location_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // הכותרת מוגדרת ב-XML דרך app:title
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // כפתור החזרה בסרגל הכלים
        toolbar.setNavigationOnClickListener(v -> finish());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        MaterialButton confirmButton = findViewById(R.id.confirm_location_button);
        confirmButton.setOnClickListener(v -> {
            if (selectedLocation == null) {
                Toast.makeText(this, "יש לבחור מיקום על המפה", Toast.LENGTH_SHORT).show();
                return;
            }
            // עושים reverse geocoding ב-background, ואז מסיימים ומחזירים תוצאה.
            confirmButton.setEnabled(false); // למנוע לחיצות כפולות
            confirmAndReverseGeocode(selectedLocation.latitude, selectedLocation.longitude);
        });

        // SearchView לחיפוש מיקום לפי שם (זהה ל-MapFragment הראשי)
        SearchView searchView = findViewById(R.id.searchView);
        setupSearchView(searchView);
    }

    /**
     * מגדיר את ה-SearchView לחיפוש מיקום לפי טקסט.
     * משתמש ב-Geocoder להמרת שם המיקום לקואורדינטות.
     */
    private void setupSearchView(SearchView searchView) {
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
     * מחפש מיקום לפי שם ומעביר את המצלמה אליו.
     *
     * חשוב: Geocoder.getFromLocationName() היא קריאה רשתית סינכרונית
     * שיכולה לקחת כמה שניות. אסור להריץ אותה ב-Main Thread כי זה
     * חוסם את ה-UI ועלול לזרוק DEADLINE_EXCEEDED.
     *
     * הפתרון: ריצה ב-Executor (background thread), והעברת התוצאה
     * חזרה ל-Main Thread דרך runOnUiThread.
     */
    private void searchLocation(String locationName) {
        if (locationName == null || locationName.isEmpty()) return;
        if (mMap == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addressList = geocoder.getFromLocationName(locationName, 1);
                runOnUiThread(() -> {
                    if (addressList != null && !addressList.isEmpty()) {
                        Address address = addressList.get(0);
                        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                        // עדכון ה-marker אוטומטית - חוסך מהמשתמש להקליק שוב
                        updateMarker(latLng);
                    } else {
                        Toast.makeText(this, "המיקום לא נמצא", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "שגיאה בחיפוש המיקום - בדקי את החיבור לאינטרנט",
                                Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * עושה reverse geocoding (קואורדינטות → שם מיקום) ב-background thread,
     * ואז מחזיר את התוצאה ב-Intent ומסיים את ה-Activity.
     *
     * אם ה-geocoding נכשל (אין רשת וכו'), אנחנו עדיין מחזירים את
     * הקואורדינטות עם locationName=null. ה-UI ידע להתמודד עם זה
     * (יציג את הקואורדינטות במקום השם).
     */
    private void confirmAndReverseGeocode(double lat, double lng) {
        Executors.newSingleThreadExecutor().execute(() -> {
            String locationName = null;
            try {
                Geocoder geocoder = new Geocoder(this, new Locale("he", "IL"));
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    locationName = formatAddress(addresses.get(0));
                }
            } catch (IOException e) {
                e.printStackTrace();
                // נמשיכים בלי שם - השם יישאר null וה-UI יציג קואורדינטות
            }

            final String finalLocationName = locationName;
            runOnUiThread(() -> {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_LATITUDE, lat);
                resultIntent.putExtra(EXTRA_LONGITUDE, lng);
                resultIntent.putExtra(EXTRA_LOCATION_NAME, finalLocationName);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            });
        });
    }

    /**
     * מקבל Address ומחזיר string קצר וקריא.
     * עדיפויות:
     *  1. רחוב + מספר + עיר ("הרצל 12, רמת גן")
     *  2. רחוב + עיר ("ככר רבין, תל אביב")
     *  3. שכונה + עיר
     *  4. featureName (POI כמו מסעדה/פארק)
     *  5. addressLine(0) - שורה מלאה מ-Geocoder כ-fallback
     */
    private static String formatAddress(Address addr) {
        if (addr == null) return null;
        StringBuilder sb = new StringBuilder();

        // קומבינציה של רחוב/feature + מספר
        String street = addr.getThoroughfare();
        String streetNumber = addr.getSubThoroughfare();
        String feature = addr.getFeatureName();
        String subLocality = addr.getSubLocality();

        if (street != null && !street.isEmpty()) {
            sb.append(street);
            if (streetNumber != null && !streetNumber.isEmpty()) {
                sb.append(" ").append(streetNumber);
            }
        } else if (feature != null && !feature.isEmpty()
                && !feature.equals(streetNumber)) {
            // featureName שונה ממספר בית (לעיתים geocoder מחזיר את המספר כ-feature)
            sb.append(feature);
        } else if (subLocality != null && !subLocality.isEmpty()) {
            sb.append(subLocality);
        }

        // הוספת שם העיר (אם יש)
        String city = addr.getLocality();
        if (city != null && !city.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city);
        }

        // fallback סופי - שורה מלאה
        if (sb.length() == 0) {
            String fullLine = addr.getAddressLine(0);
            if (fullLine != null) sb.append(fullLine);
        }

        return sb.length() > 0 ? sb.toString() : null;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // הפעלת כפתורי הזום (+/-) של Google Maps בפינה הימנית-תחתונה.
        // זהה ל-MapFragment הראשי.
        mMap.getUiSettings().setZoomControlsEnabled(true);
        // המחווה של pinch-to-zoom גם מופעלת ע"י Google Maps כברירת מחדל,
        // אבל נוודא ליתר ביטחון.
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        // הפעלת כפתור "המיקום שלי" אם יש הרשאה
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // נסה לעבור למיקום הנוכחי של המשתמש
        moveToCurrentLocation();

        mMap.setOnMapClickListener(this::updateMarker);
    }

    private void moveToCurrentLocation() {
        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true); // Show the blue dot and the "center me" button
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            // Got a last known location. Use it.
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                            // Pre-select this location for the user
                            updateMarker(currentLocation);
                        } else {
                            // Last location is null, fall back to default
                            moveToDefaultLocation();
                        }
                    });
        } else {
            // No permission, fall back to default
            moveToDefaultLocation();
        }
    }

    private void moveToDefaultLocation() {
        // Default location (e.g., Tel Aviv)
        LatLng defaultLocation = new LatLng(32.0853, 34.7818);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10));
    }
    
    private void updateMarker(LatLng latLng) {
        selectedLocation = latLng;
        if (selectedLocationMarker == null) {
            // יצירת marker חדש
            selectedLocationMarker = mMap.addMarker(
                    new MarkerOptions().position(latLng).title("המיקום שנבחר"));
        } else {
            // הזזת ה-marker הקיים
            selectedLocationMarker.setPosition(latLng);
        }
        // אנימציה למיקום החדש
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
    }
}
