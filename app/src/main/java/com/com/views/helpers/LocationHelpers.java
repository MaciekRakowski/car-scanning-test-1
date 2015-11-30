package com.com.views.helpers;

import android.app.Activity;
import android.content.Context;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;

import com.google.android.gms.maps.model.LatLng;

import java.util.Locale;

/**
 * Created by Maria on 11/28/2015.
 */
public class LocationHelpers {

    public static LatLng getLattitudeLongitude(Activity activity) {
       // Geocoder geocoder = new Geocoder(activity, Locale.getDefault());
        LocationManager lm = (LocationManager)activity.getSystemService(Context.LOCATION_SERVICE);
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        LatLng latLng = new LatLng(latitude, longitude);

        //double x = latLng.latitude;
        return  latLng;
    }
}
