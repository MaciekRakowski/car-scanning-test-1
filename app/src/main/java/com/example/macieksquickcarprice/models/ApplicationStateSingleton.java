package com.example.macieksquickcarprice.models;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Maria on 8/9/2015.
 */
public class ApplicationStateSingleton {
    private static final HashMap<String, CarDetails> mCarHistory = new HashMap<String, CarDetails>();
    private static String historyKey = "VehicleHistory";
    private static String vinMileageKey = "VinMileage";
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
