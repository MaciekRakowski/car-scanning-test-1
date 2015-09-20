package com.example.maciek.testapplication2;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;

import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.com.views.helpers.EmptyTextPlaceholderHelper;
import com.example.macieksquickcarprice.httpmethods.CarDetailsRestTask;
import com.example.macieksquickcarprice.httpmethods.CarPriceRestTask;
import com.example.macieksquickcarprice.httpmethods.CommonCallback;
import com.example.macieksquickcarprice.models.ApplicationStateSingleton;
import com.example.macieksquickcarprice.models.CarDetails;

import android.app.Activity;
import android.content.Intent;
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

        final EditText editTextNotes = (EditText)this.findViewById(R.id.editTextNotes);
        editTextNotes.addTextChangedListener(new EmptyTextPlaceholderHelper(editTextNotes,
                this.findViewById(R.id.addNotesPlaceHolder)));
//        editTextNotes.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//                String notes = editTextNotes.getText().toString();
//                View view = ShowCarDetails.this.findViewById(R.id.addNotesPlaceHolder);
//                view.setVisibility(notes.length() == 0 ? View.VISIBLE : View.GONE);
//            }
//        });
    }

    @Override
    protected void onPause() {
        final EditText editTextNotes = (EditText)this.findViewById(R.id.editTextNotes);
        String notes = editTextNotes.getText().toString();
        if (notes.length() > 0 && mCurrentCar != null && mCurrentCar.mVin != null && mCurrentCar.mVin.length() > 0) {
            ApplicationStateSingleton.addNotesForVehicle(mCurrentCar.mVin, notes, this);
        }
        super.onPause();
    }

    private EditText mTextViewMiles;
    private EditText getEditTextMiles() {
        if (mTextViewMiles != null) {
            return mTextViewMiles;
        }
        EditText milesText = (EditText)this.findViewById(R.id.editTextMiles);
        mTextViewMiles = milesText;
        return mTextViewMiles;
    }

    private void onCarDetailsReady() {
        saveCarInHistory();
        TextView textView = (TextView)this.findViewById(R.id.textViewCarDetails);
        textView.setText(String.format("%s\n%s", mCurrentCar.toString(), mCurrentCar.mTrimFullName));

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int carYear = Integer.parseInt(mCurrentCar.mYear);
        Integer miles = ApplicationStateSingleton.getMilesForVin(mCurrentCar.mVin, this);
        if (miles <= 0) {
            miles = 12000 * (currentYear - carYear);
        }
        mCurrentCar.mMiles = miles;
        EditText milesText = getEditTextMiles();
        milesText.setText(miles.toString());

        TextView textViewMiles = (TextView)this.findViewById(R.id.textViewMiles);
        textViewMiles.setText(formatMiles(miles));

        updatePrice(getCondition());

        String notes = ApplicationStateSingleton.getNotesForVehicle(mCurrentCar.mVin, this);
        EditText editTextNotes = (EditText)this.findViewById(R.id.editTextNotes);
        editTextNotes.setText(notes);
    }

    private void saveCarInHistory() {
        ApplicationStateSingleton.addCarToHistory(mCurrentCar, this);
    }

    private String getCondition() {
        RadioGroup radioButtonGroup = (RadioGroup)this.findViewById(R.id.radiogroup_condition);
        int radioButtonID = radioButtonGroup.getCheckedRadioButtonId();
        RadioButton radioButton = (RadioButton)radioButtonGroup.findViewById(radioButtonID);
        String condition = radioButton.getTag().toString();
        return condition;
    }

    public void updateClick(View view) {
        //TextView textViewPrice = getTextViewPrice();
        TextView textViewPrivateParty = (TextView)this.findViewById(R.id.textViewPrivateParty);
        textViewPrivateParty.setText("Loading...");
        TextView textViewTradeIn = (TextView)this.findViewById(R.id.textViewTradeIn);
        textViewTradeIn.setText("Loading...");
        TextView textViewRetail = (TextView)this.findViewById(R.id.textViewRetail);
        textViewRetail.setText("Loading...");


        EditText editTextMiles= getEditTextMiles();
        Integer miles = Integer.parseInt(editTextMiles.getText().toString());
        ApplicationStateSingleton.addVinAndMileage(mCurrentCar.mVin, miles, this);
        mCurrentCar.mMiles = miles;

        mCurrentCar.mMiles = miles;
        EditText milesText = getEditTextMiles();
        milesText.setText(miles.toString());

        TextView textViewMiles = (TextView)this.findViewById(R.id.textViewMiles);
        textViewMiles.setText(formatMiles(miles));

        updatePrice(getCondition());
    }

    public void navigateHomeClick(View view) {
        Intent intent = new Intent(this, ActivityMainPageViewer.class);
        startActivity(intent);
    }

    public void onRadioButtonClicked(View view) {
        TextView textViewPrivateParty = (TextView)this.findViewById(R.id.textViewPrivateParty);
        textViewPrivateParty.setText("Loading...");
        TextView textViewTradeIn = (TextView)this.findViewById(R.id.textViewTradeIn);
        textViewTradeIn.setText("Loading...");
        TextView textViewRetail = (TextView)this.findViewById(R.id.textViewRetail);
        textViewRetail.setText("Loading...");

        RadioButton radioButton = ((RadioButton) view);
        updatePrice(radioButton.getTag().toString());
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
                ShowCarDetails.this.onCarPriceReady(result);
            }
        });
    }

    public static String formatPrice(float price) {
        DecimalFormat formatter = new DecimalFormat("###,###,###.00");
        return "$" + formatter.format(price);
    }

    private static String formatMiles(Integer miles) {
        DecimalFormat formatter = new DecimalFormat("###,###,###");
        return "Mileage: " + formatter.format(miles);
    }

    private void onCarPriceReady(CarDetails result) {
        TextView textViewPrivateParty = (TextView)this.findViewById(R.id.textViewPrivateParty);
        textViewPrivateParty.setText(formatPrice(result.mPriceDetails.mPrivatePartyValue));
        TextView textViewTradeIn = (TextView)this.findViewById(R.id.textViewTradeIn);
        textViewTradeIn.setText(formatPrice(result.mPriceDetails.mTradeinValue));
        TextView textViewRetail = (TextView)this.findViewById(R.id.textViewRetail);
        textViewRetail.setText(formatPrice(result.mPriceDetails.mDealerValue));


        //textViewCarPrice
//        TextView textViewPrice = getTextViewPrice();
//        String display = String.format("Mileage: %s\n\nPrivate Party: $%s\nTrade-in: $%s\nRetail: $%s\n",
//                formatMiles(result.mMiles),
//                formatPrice(result.mPriceDetails.mPrivatePartyValue),
//                formatPrice(result.mPriceDetails.mTradeinValue),
//                formatPrice(result.mPriceDetails.mDealerValue));
//        textViewPrice.setText(display);
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

}
