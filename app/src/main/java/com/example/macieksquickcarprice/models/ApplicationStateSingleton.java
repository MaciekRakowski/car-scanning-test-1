package com.example.macieksquickcarprice.models;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

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
    private static boolean mCarsLoaded = false;

    public static Collection<CarDetails> getAllCarsInHistory() {
        return mCarHistory.values();
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

            CarDetails carDetails = new CarDetails();
            if (!carDetails.tryPopulateCarDetailsFromJSON(jsonStr)) {
                continue;
            }

            mCarHistory.put(carDetails.mVin, carDetails);
        }
        mCarsLoaded = true;
    }
}
