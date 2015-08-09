package com.example.macieksquickcarprice.models;

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
	public Integer mMiles;	
	public PriceDetails mPriceDetails = new PriceDetails();
	public final List<String> mAvailableOptionsIds = new ArrayList<String>();
	public final List<String> mOptionsIds = new ArrayList<String>();

	@Override
	public String toString() {
		return String.format("%s %s %s (%s)", mYear, mMake, mModelName, mTrim);
	}
	
}
