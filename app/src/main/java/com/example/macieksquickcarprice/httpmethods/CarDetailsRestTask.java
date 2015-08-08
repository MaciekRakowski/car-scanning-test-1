package com.example.macieksquickcarprice.httpmethods;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.macieksquickcarprice.models.CarDetails;

public class CarDetailsRestTask extends PerformRestTask {
	
	private CommonCallback<CarDetails> mHandler;
	private String mVin;
	
	public void GetCarDetailsByVin(String vin, CommonCallback<CarDetails> callback) {
		mHandler = callback;
		mVin = vin;
		this.execute(vin);
	}
  
	@Override  
	protected void onPostExecute(String result) {
		try {
			JSONObject fullObject = new JSONObject(result);
			JSONArray  years = fullObject.getJSONArray("years");			
			JSONObject json = years.getJSONObject(0);
			String year = json.get("year").toString();
			JSONArray styles = json.getJSONArray("styles");
			json = styles.getJSONObject(0);
			String id = json.get("id").toString();
			String trim = json.getString("trim");
			String trimName = json.getString("name");
			String make = fullObject.getJSONObject("make").getString("niceName");
			String model = fullObject.getJSONObject("model").getString("niceName");

			
			CarDetails carDetails = new CarDetails();
			
			//get options
			JSONArray options = fullObject.getJSONArray("options");
			for (int index = 0; index < options.length(); index++) {
				JSONObject optionJson = options.getJSONObject(index);
				JSONArray subOptions = optionJson.getJSONArray("options");
				for (int subOptionIndex = 0; subOptionIndex < subOptions.length(); subOptionIndex++) {
					JSONObject actualOption = subOptions.optJSONObject(subOptionIndex);
					String optionId = actualOption.getString("id");
					carDetails.mOptionsIds.add(optionId);
				}
			}
			
			carDetails.mModelId = id;
			carDetails.mVin = mVin;
			carDetails.mYear = year;
			carDetails.mMake = make;
			carDetails.mModelName = model;
			carDetails.mTrim = trim;
			carDetails.mTrimFullName = trimName;

			mHandler.execute(carDetails);
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	@Override
	protected ApiMethods getMethod() {
		// TODO Auto-generated method stub
		return ApiMethods.GET_CAR_BY_VIN;
	}

}
