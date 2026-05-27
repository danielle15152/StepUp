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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        MaterialButton confirmButton = findViewById(R.id.confirm_location_button);
        confirmButton.setOnClickListener(v -> {
            if (selectedLocation == null) {
                Toast.makeText(this, R.string.map_select_on_map, Toast.LENGTH_SHORT).show();
                return;
            }
            confirmButton.setEnabled(false);
            confirmAndReverseGeocode(selectedLocation.latitude, selectedLocation.longitude);
        });

        SearchView searchView = findViewById(R.id.searchView);
        setupSearchView(searchView);
    }

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

    // Geocoder עושה קריאת רשת, חייב לרוץ ב-background thread
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
                        updateMarker(latLng);
                    } else {
                        Toast.makeText(this, R.string.map_location_not_found, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, R.string.map_search_error, Toast.LENGTH_SHORT).show());
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

    // אם ה-geocoding נכשל מחזיר locationName=null וה-UI מציג קואורדינטות
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

    // בונה כתובת קצרה: רחוב+מספר+עיר, או fallback ל-feature/subLocality
    private static String formatAddress(Address addr) {
        if (addr == null) return null;
        StringBuilder sb = new StringBuilder();

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
            sb.append(feature);
        } else if (subLocality != null && !subLocality.isEmpty()) {
            sb.append(subLocality);
        }

        String city = addr.getLocality();
        if (city != null && !city.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city);
        }

        if (sb.length() == 0) {
            String fullLine = addr.getAddressLine(0);
            if (fullLine != null) sb.append(fullLine);
        }

        return sb.length() > 0 ? sb.toString() : null;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        moveToCurrentLocation();

        mMap.setOnMapClickListener(this::updateMarker);
    }

    private void moveToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                            updateMarker(currentLocation);
                        } else {
                            moveToDefaultLocation();
                        }
                    });
        } else {
            moveToDefaultLocation();
        }
    }

    private void moveToDefaultLocation() {
        // ברירת מחדל - תל אביב
        LatLng defaultLocation = new LatLng(32.0853, 34.7818);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10));
    }

    private void updateMarker(LatLng latLng) {
        selectedLocation = latLng;
        if (selectedLocationMarker == null) {
            selectedLocationMarker = mMap.addMarker(
                    new MarkerOptions().position(latLng)
                            .title(getString(R.string.map_selected_location_title)));
        } else {
            selectedLocationMarker.setPosition(latLng);
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
    }
}
