package com.example.stepup;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Select a Location");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        Button confirmButton = findViewById(R.id.confirm_location_button);
        confirmButton.setOnClickListener(v -> {
            if (selectedLocation != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_LATITUDE, selectedLocation.latitude);
                resultIntent.putExtra(EXTRA_LONGITUDE, selectedLocation.longitude);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Please select a location on the map.", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Try to move to the user's current location
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
            // Create a new marker
            selectedLocationMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
        } else {
            // Move the existing marker
            selectedLocationMarker.setPosition(latLng);
        }
        // Animate camera to the new position
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
    }
}
