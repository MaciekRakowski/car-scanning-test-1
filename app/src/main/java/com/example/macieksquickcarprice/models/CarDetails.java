package com.example.macieksquickcarprice.models;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CarDetails {
	public String mMake;
	public String mModelName;
	public String mVin;
	public String mModelId;
	public String mYear;
	public String mTrim = "";
	public String mTrimFullName = "";
	public String mFullJSON;
	public Integer mMiles;
	public PriceDetails mPriceDetails = new PriceDetails();
	public final List<String> mAvailableOptionsIds = new ArrayList<String>();
	public final List<String> mOptionsIds = new ArrayList<String>();

	public CarDetails(String vin) {
		mVin = vin;
	}

	@Override
	public String toString() {
		return String.format("%s %s %s (%s)", mYear, mMake, mModelName, mTrim);
	}

	public void populateCarDetailsFromJSON(String jsonResult) throws JSONException {
		mFullJSON = jsonResult;
		JSONObject fullObject = new JSONObject(jsonResult);
		JSONArray years = fullObject.getJSONArray("years");
		JSONObject json = years.getJSONObject(0);
		String year = json.get("year").toString();
		JSONArray styles = json.getJSONArray("styles");
		json = styles.getJSONObject(0);
		String id = json.get("id").toString();
		String trim = json.getString("trim");
		String trimName = json.getString("name");
		String make = fullObject.getJSONObject("make").getString("niceName");
		String model = fullObject.getJSONObject("model").getString("niceName");
		//String vin = fullObject.getString("vin");

		//get options
		JSONArray options = fullObject.getJSONArray("options");
		for (int index = 0; index < options.length(); index++) {
			JSONObject optionJson = options.getJSONObject(index);
			JSONArray subOptions = optionJson.getJSONArray("options");
			for (int subOptionIndex = 0; subOptionIndex < subOptions.length(); subOptionIndex++) {
				JSONObject actualOption = subOptions.optJSONObject(subOptionIndex);
				String optionId = actualOption.getString("id");
				this.mAvailableOptionsIds.add(optionId);
			}
		}

		this.mModelId = id;
		//this.mVin = vin;
		this.mYear = year;
		this.mMake = make;
		this.mModelName = model;
		this.mTrim = trim;
		this.mTrimFullName = trimName;
	}

	public Boolean tryPopulateCarDetailsFromJSON(String jsonResult) {
		try {
			populateCarDetailsFromJSON(jsonResult);
			return true;
		}
		catch (JSONException ex) {
			ex.printStackTrace();
			return false;
		}
	}
}

