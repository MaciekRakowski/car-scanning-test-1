package com.quickcarprice.activities;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.example.maciek.testapplication2.R;
import com.example.macieksquickcarprice.models.ApplicationStateSingleton;
import com.example.macieksquickcarprice.models.CarDetails;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.Map;

public class CarHistoryMap extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_history_map);
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        //mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
        Map<String, LatLng> allLocations = ApplicationStateSingleton.getAllScanLocations(this);
        HashMap<String, CarDetails> cars = ApplicationStateSingleton.getAllCarsInHistoryHash();
        final HashMap<Marker, CarDetails> markerToCar = new HashMap<Marker, CarDetails>();
        LatLng firstLocation = null;
        for (String vin : allLocations.keySet()) {
            LatLng latLng  = allLocations.get(vin);
            CarDetails carDetails = cars.get(vin);
            if (carDetails == null) {
                continue;
            }
            String notes = ApplicationStateSingleton.getNotesForVehicle(vin, this);
            Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title(carDetails.toString()).snippet(notes));
            markerToCar.put(marker, carDetails);
            if (firstLocation == null) {
                firstLocation = latLng;
            }
        }
        if (firstLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(firstLocation));
        }

        GoogleMap.OnMarkerClickListener listener = new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                CarDetails carDetails = markerToCar.get(marker);
                System.out.println(carDetails);

                return false;
            }
        };
        mMap.setOnMarkerClickListener(listener);

//        LatLng sydney = new LatLng(-34, 151);
//
//        //final LayoutInflater inflater = (LayoutInflater)this.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney").snippet("sydney.").snippet("Click on the marker again to view the car"));
//        mMap.addMarker(new MarkerOptions().position(new LatLng(-32, 150)).title("Another Marker").snippet("Yet another marker\nClick on the marker again to view the car."));
//
//        GoogleMap.OnMarkerClickListener listener = new GoogleMap.OnMarkerClickListener() {
//            @Override
//            public boolean onMarkerClick(Marker marker) {
//                String snippet = marker.getSnippet();
//                System.out.println(snippet);
//
//                return false;
//            }
//        };
//
//        mMap.setOnMarkerClickListener(listener);
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }
}
