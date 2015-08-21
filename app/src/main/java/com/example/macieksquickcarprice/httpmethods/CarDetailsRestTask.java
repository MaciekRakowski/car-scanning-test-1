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
			CarDetails carDetails = new CarDetails();
			carDetails.populateCarDetailsFromJSON(result);
			//populateCarDetailsFromJSON(result, carDetails);

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
