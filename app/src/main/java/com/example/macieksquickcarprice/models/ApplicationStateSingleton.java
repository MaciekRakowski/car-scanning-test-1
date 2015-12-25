package com.example.macieksquickcarprice.models;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Maria on 8/9/2015.
 */
public class ApplicationStateSingleton {
    private static final HashMap<String, CarDetails> mCarHistory = new HashMap<String, CarDetails>();
    private static String historyKey = "VehicleHistory";
    private static String vinMileageKey = "VinMileage";
    private static String vehicleNotesKey = "vehicleNotesKey";
    private static String vehiclePicturesKey = "vehiclePictures";
    private static String vehicleScanLocationKey = "vehicleScanLocation";
    private static boolean mCarsLoaded = false;

    public static Collection<CarDetails> getAllCarsInHistory() {
        return mCarHistory.values();
    }

    public static HashMap<String, CarDetails> getAllCarsInHistoryHash() {
        return mCarHistory;
    }

    public static void addVehicleScanLocation(String vin, LatLng latLng, Activity activity) {
        SharedPreferences sharedPref = activity.getSharedPreferences(vehicleScanLocationKey, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(vin, String.format("%f,%f", latLng.latitude, latLng.longitude));
        editor.commit();
    }

    public  static void removeScanLocation(String vin, Activity activity) {
        SharedPreferences sharedPref = activity.getSharedPreferences(vehicleScanLocationKey, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        SharedPreferences.Editor remove = editor.remove(vin);
        remove.commit();
        editor.commit();
    }

    public static LatLng getVehicleScanLocation(String vin, Activity activity) {
        SharedPreferences sharedPref = activity.getSharedPreferences(vehicleScanLocationKey, Context.MODE_PRIVATE);

        String latLngStr = sharedPref.getString(vin, "");
        String [] latLngStrArray = latLngStr.split(",");
        if (latLngStrArray.length != 2) {
            return null;
        }
        try {
            double latitude = Double.parseDouble(latLngStrArray[0]);
            double longitude = Double.parseDouble(latLngStrArray[1]);
            return new LatLng(latitude, longitude);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static LatLng getLatLngFromString(String latLngStr) {
        try {
            String [] latLongStrs = latLngStr.split(",");
            return new LatLng(Double.parseDouble(latLongStrs[0]), Double.parseDouble(latLongStrs[1]));

        }
        catch (Exception ex) {
            ex.printStackTrace();;
            return null;
        }
    }

    public static Map<String, LatLng> getAllScanLocations(Activity activity) {
        SharedPreferences sharedPref = activity.getSharedPreferences(vehicleScanLocationKey, Context.MODE_PRIVATE);
        Map<String, ?> latLongAsString = sharedPref.getAll();
        Map<String, LatLng> latLngMap = new HashMap<String, LatLng>();

        for (String key : latLongAsString.keySet()) {
            String result = String.valueOf(latLongAsString.get(key));
            LatLng latLng = getLatLngFromString(result);
            if (latLng == null)
                continue;;
            latLngMap.put(key, latLng);
        }
        return latLngMap;
    }

    public static void addCarToHistory(CarDetails carDetails, Activity activity) {

        mCarHistory.put(carDetails.mVin, carDetails);
        SharedPreferences sharedPref = activity.getSharedPreferences(historyKey, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(carDetails.mVin, carDetails.mFullJSON);
        editor.commit();
    }

    public static void addVinAndMileage(String vin, int miles, Activity activity) {
        SharedPreferences sharedPref = activity.getSharedPreferences(vinMileageKey, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(vin, miles);
        editor.commit();
    }

    public static int getMilesForVin(String vin, Activity activity) {
        SharedPreferences sharedPref = activity.getSharedPreferences(vinMileageKey, Context.MODE_PRIVATE);
        int miles = sharedPref.getInt(vin, 0);
        return miles;
    }

    public static void addNotesForVehicle(String vin, String notes, Activity activity) {
        SharedPreferences sharedPref = activity.getSharedPreferences(vehicleNotesKey, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(vin, notes);
        editor.commit();
    }

    public static  String getNotesForVehicle(String vin, Activity activity) {
        SharedPreferences sharedPref = activity.getSharedPreferences(vehicleNotesKey, Context.MODE_PRIVATE);
        String notes = sharedPref.getString(vin, "");
        return notes;
    }

    public static void removeCar(String vin, Activity activity) {
        SharedPreferences sharedPref = activity.getSharedPreferences(historyKey, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        SharedPreferences.Editor remove = editor.remove(vin);
        editor.commit();
        remove.commit();

        mCarHistory.remove(vin);

        removeScanLocation(vin, activity);
    }

    public static void loadCarsFromHistoryIfNotLoaded(Activity activity) {
        if (mCarsLoaded) {
            return;
        }
        loadCarsFromHistory(activity);
    }

    private static String getVehiclePictureKey(String vin) {
        return String.format("%s-%s-single", "pictures", vin);
    }

    public static Set<String> getPicturesForCar(Activity activity, String vin) {
        String key = getVehiclePictureKey(vin);
        SharedPreferences sharedPref = activity.getSharedPreferences(vehiclePicturesKey, Context.MODE_PRIVATE);
        Set<String> values = new HashSet<String>();
        String picture = sharedPref.getString(key, "");
        values.add(picture);
        //Set<String> values = sharedPref.getStringSet(key, new HashSet<String>());

        return values;
    }

    public static void addPictureForCar(Activity activity, String vin, String name) {
        String key = getVehiclePictureKey(vin);
        SharedPreferences sharedPref = activity.getSharedPreferences(vehiclePicturesKey, Context.MODE_PRIVATE);
        //Set<String> values = new HashSet<String>();
        //Set<String> values = sharedPref.getStringSet(key, new HashSet<String>());
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, name);
        //values.add(name);
        //SharedPreferences.Editor addCarEditor = editor.putStringSet(key, values);

        editor.commit();
        //addCarEditor.commit();

    }

    public static void clearPhotosForCar(Activity activity, String vin) {
        String key = getVehiclePictureKey(vin);
        SharedPreferences sharedPref = activity.getSharedPreferences(vehiclePicturesKey, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        SharedPreferences.Editor putEdit = editor.putStringSet(key, new HashSet<String>());

        editor.commit();
        putEdit.commit();
    }

    public static void loadCarsFromHistory(Activity activity) {
        SharedPreferences sharedPref = activity.getSharedPreferences(historyKey, Context.MODE_PRIVATE);
        Map<String, ?> allCars = sharedPref.getAll();

        for (String vin : allCars.keySet()) {
            String jsonStr = sharedPref.getString(vin, "");
            if (jsonStr.equals("")) {
                continue;
            }

            CarDetails carDetails = new CarDetails(vin);
            if (!carDetails.tryPopulateCarDetailsFromJSON(jsonStr)) {
                continue;
            }

            mCarHistory.put(carDetails.mVin, carDetails);
        }
        mCarsLoaded = true;
    }
}
