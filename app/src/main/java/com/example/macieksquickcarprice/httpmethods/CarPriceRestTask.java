package com.example.macieksquickcarprice.httpmethods;

import org.json.JSONException;
import org.json.JSONObject;

import com.example.macieksquickcarprice.models.CarDetails;

public class CarPriceRestTask extends PerformRestTask {
	
	private CommonCallback<CarDetails> mHandler;
	private CarDetails mCarDetails;
	
	public void PopulateCarPrice(CarDetails carDetails, String condition, Integer mileage, String zip, CommonCallback<CarDetails> callback) {
		mHandler = callback;
		mCarDetails = carDetails;
		this.execute(carDetails.mModelId, condition, mileage.toString(), zip, getOptionsString(carDetails));
	}

	private String getOptionsString(CarDetails carDetails) {
		// TODO Auto-generated method stub
		if (carDetails.mAvailableOptionsIds.size() == 0) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (String option : carDetails.mOptionsIds) {
			builder.append("optionid=" + option + "&");			
		}
		return builder.toString();
	}
	
	@Override  
	protected void onPostExecute(String result) {
		try {
			JSONObject fullResult = new JSONObject(result);
			JSONObject totalWithOptionsJson = fullResult.getJSONObject("tmv").getJSONObject("totalWithOptions");
			
			mCarDetails.mPriceDetails.mPrivatePartyValue = Float.parseFloat(totalWithOptionsJson.get("usedPrivateParty").toString());
			mCarDetails.mPriceDetails.mTradeinValue = Float.parseFloat(totalWithOptionsJson.get("usedTradeIn").toString());
			mCarDetails.mPriceDetails.mDealerValue = Float.parseFloat(totalWithOptionsJson.get("usedTmvRetail").toString());
			
			mHandler.execute(mCarDetails);
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	protected ApiMethods getMethod() {
		// TODO Auto-generated method stub
		return ApiMethods.GET_CAR_PRICE;
	}

}
