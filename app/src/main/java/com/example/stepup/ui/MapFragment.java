package com.example.stepup.ui;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
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

    private void searchLocation(String locationName) {
        if (locationName == null || locationName.isEmpty()) {
            return;
        }

        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocationName(locationName, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            } else {
                Toast.makeText(getContext(), "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error searching for location", Toast.LENGTH_SHORT).show();
        }
    }


    private void loadLocationGoals() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<GoalWithReminder> locationGoals = dao.getGoalsWithLocationReminders();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (googleMap != null && !locationGoals.isEmpty()) {
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        for (GoalWithReminder goal : locationGoals) {
                            if (goal.reminder != null && goal.reminder.latitude != null && goal.reminder.longitude != null) {
                                LatLng position = new LatLng(goal.reminder.latitude, goal.reminder.longitude);
                                Marker marker = googleMap.addMarker(new MarkerOptions()
                                        .position(position)
                                        .title(goal.goal.name));
                                if (marker != null) {
                                    marker.setTag((long) goal.goal.id);
                                }
                                builder.include(position);
                            }
                        }
                        LatLngBounds bounds = builder.build();
                        int padding = 100; // offset from edges of the map in pixels
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
                    }
                });
            }
        });
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
