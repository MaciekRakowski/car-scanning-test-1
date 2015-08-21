package com.example.maciek.testapplication2;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.example.macieksquickcarprice.httpmethods.CarDetailsRestTask;
import com.example.macieksquickcarprice.httpmethods.CarPriceRestTask;
import com.example.macieksquickcarprice.httpmethods.CommonCallback;
import com.example.macieksquickcarprice.models.ApplicationStateSingleton;
import com.example.macieksquickcarprice.models.CarDetails;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;


public class ShowCarDetails extends Activity {

    private CarDetails mCurrentCar = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_car_details);

        Intent intent = this.getIntent();
        String vin = intent.getStringExtra("vin");
        CarDetailsRestTask task = new CarDetailsRestTask();
        task.GetCarDetailsByVin(vin, new CommonCallback<CarDetails>() {
            @Override
            public void execute(CarDetails result) {
                // TODO Auto-generated method stub
                mCurrentCar = result;
                onCarDetailsReady();
            }
        });
    }

    private EditText mTextViewMiles;
    private EditText getTextViewMiles() {
        if (mTextViewMiles != null) {
            return mTextViewMiles;
        }
        EditText milesText = (EditText)this.findViewById(R.id.editTextMiles);
        mTextViewMiles = milesText;
        return mTextViewMiles;
    }

    private TextView mTextViewPrice;
    private TextView getTextViewPrice() {
        if (mTextViewPrice != null) {
            return mTextViewPrice;
        }
        mTextViewPrice = (TextView)this.findViewById(R.id.textViewCarPrice);
        return mTextViewPrice;
    }

    private void onCarDetailsReady() {
        saveCarInHistory();
        TextView textView = (TextView)this.findViewById(R.id.textViewCarDetails);
        textView.setText(String.format("%s\n%s", mCurrentCar.toString(), mCurrentCar.mTrimFullName));

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int carYear = Integer.parseInt(mCurrentCar.mYear);
        Integer miles = 12000 * (currentYear - carYear);
        mCurrentCar.mMiles = miles;
        EditText milesText = getTextViewMiles();
        milesText.setText(miles.toString());

        updatePrice(getCondition());
    }

    private void saveCarInHistory() {
        ApplicationStateSingleton.addCarToHistory(mCurrentCar, this);
    }

    private String getCondition() {
        RadioGroup radioButtonGroup = (RadioGroup)this.findViewById(R.id.radiogroup_condition);
        int radioButtonID = radioButtonGroup.getCheckedRadioButtonId();
        RadioButton radioButton = (RadioButton)radioButtonGroup.findViewById(radioButtonID);
        String condition = radioButton.getText().toString();
        return condition;
    }

    public void updateClick(View view) {
        TextView textViewPrice = getTextViewPrice();
        textViewPrice.setText("Loading...");
        EditText editTextMiles= getTextViewMiles();
        Integer miles = Integer.parseInt(editTextMiles.getText().toString());
        mCurrentCar.mMiles = miles;
        updatePrice(getCondition());
    }

    public void navigateHomeClick(View view) {
        Intent intent = new Intent(this, ActivityMainPageViewer.class);

        startActivity(intent);
    }

    public void onRadioButtonClicked(View view) {
        TextView textViewPrice = getTextViewPrice();
        textViewPrice.setText("Loading...");
        RadioButton radioButton = ((RadioButton) view);
        updatePrice(radioButton.getText().toString());
    }

    private  String getZipcode() {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses.size() > 0) {
                return addresses.get(0).getPostalCode();
            }
        }
        catch(Exception ex) {
            ex.printStackTrace();;

        }
        return "90210";
    }

    private void updatePrice(String condition) {

        CarPriceRestTask carPriceTask = new CarPriceRestTask();
        //Average
        int miles = mCurrentCar.mMiles;

        String zipCode = getZipcode();
        carPriceTask.PopulateCarPrice(mCurrentCar, condition, miles, zipCode, new CommonCallback<CarDetails>() {

            @Override
            public void execute(CarDetails result) {
                // TODO Auto-generated method stub
                ShowCarDetails.this.onCarPriceReady(result);
            }
        });
    }

    private void onCarPriceReady(CarDetails result) {
        //textViewCarPrice
        TextView textViewPrice = getTextViewPrice();
        String display = String.format("Mileage: %d\n\nPrivate Party: %f\nTrade-in: %f\nRetail: %f\n", result.mMiles, result.mPriceDetails.mPrivatePartyValue, result.mPriceDetails.mTradeinValue, result.mPriceDetails.mDealerValue);
        textViewPrice.setText(display);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_show_car_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void appendLocalStorage(String fileName, String contents) {
        FileOutputStream fos;
        try {
            fos = this.openFileOutput(fileName, Context.MODE_APPEND);
            fos.write("\n".getBytes());
            fos.write(contents.getBytes());
            fos.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
