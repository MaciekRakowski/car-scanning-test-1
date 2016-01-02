package com.quickcarprice.activities;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.com.views.helpers.EmptyTextPlaceholderHelper;
import com.com.views.helpers.RealPathUtil;
import com.example.maciek.testapplication2.R;
import com.example.macieksquickcarprice.httpmethods.CarDetailsRestTask;
import com.example.macieksquickcarprice.httpmethods.CarPriceRestTask;
import com.example.macieksquickcarprice.httpmethods.CommonCallback;
import com.example.macieksquickcarprice.models.ApplicationStateSingleton;
import com.example.macieksquickcarprice.models.CarDetails;
import com.google.android.gms.maps.model.LatLng;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;


public class ShowCarDetails extends LocationActivity {

    private CarDetails mCurrentCar = null;
    private boolean mIgnoreLocation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_car_details);

        Intent intent = this.getIntent();
        String vin = intent.getStringExtra("vin");

        if (vin == null) {
            //comes from bees4honey app.
            vin = intent.getData().getQueryParameter("param");
            ActivityMainPageViewer.setVin(vin);
        }
        initializeIgnoreLocationMember(intent, vin);
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

        if (vin != null) {
            String notes = ApplicationStateSingleton.getNotesForVehicle(vin, this);
            editTextNotes.setText(notes);
            setPicture(vin);
        }
    }

    private void initializeIgnoreLocationMember(Intent intent, String vin) {
        mIgnoreLocation = intent.getBooleanExtra("ignore-location", false);
        if (!mIgnoreLocation) {
            LatLng loc = ApplicationStateSingleton.getVehicleScanLocation(vin, this);
            mIgnoreLocation = loc != null;
        }
    }

    @Override
    protected void onLocationChanged() {
        if (mIgnoreLocation || mCurrentCar == null || mCurrentCar.mVin == null) {
            return;
        }
        ApplicationStateSingleton.addVehicleScanLocation(mCurrentCar.mVin, new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), this );
        mIgnoreLocation = true;
    }

    private void setPicture(String vin) {
        Set<String> files = ApplicationStateSingleton.getPicturesForCar(this, vin);
        if (files.size() == 0)
            return;

        Object [] filesArray = files.toArray();
        String myJpgPath = filesArray[0].toString();
        //Drawable drawable = Drawable.createFromPath(filesArray[0].toString());
        ImageView jpgView = (ImageView)this.findViewById(R.id.imageView);
        //imageView.setImageDrawable(drawable);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        //options.inScaled = true;
        Bitmap bm = BitmapFactory.decodeFile(myJpgPath, options);
        jpgView.setImageBitmap(bm);

        //imageView.setImageURI(Uri.fromFile(new File(filesArray[0].toString())));
    }

    private void setPicture(Uri uri) {

        ImageView jpgView = (ImageView)this.findViewById(R.id.imageView);
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inSampleSize = 2;
//        Bitmap bm = BitmapFactory.decodeFile(uri.getPath(), options);
//        jpgView.setImageBitmap(bm);

        jpgView.setImageURI(uri);
        //imageView.setImageURI(Uri.fromFile(new File(filesArray[0].toString())));
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

        TextView textViewMSRP = (TextView)this.findViewById(R.id.textViewMSRP);
        textViewMSRP.setText(formatPrice(mCurrentCar.mBaseMSRP));

        updatePrice(getCondition());

//        String notes = ApplicationStateSingleton.getNotesForVehicle(mCurrentCar.mVin, this);
//        EditText editTextNotes = (EditText)this.findViewById(R.id.editTextNotes);
//        editTextNotes.setText(notes);
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

    public void takePhoto(View view) {
        try {
//            Intent intent = new Intent(this, CameraActivity.class);
//            startActivity(intent);

//            Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//            startActivityForResult(takePicture, 0);//zero can be replaced with any action code
//
            Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickPhoto , 1);//one can be replaced with any action code
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch(requestCode) {
            case 0:
                if(resultCode == RESULT_OK){
                    Bundle extras = imageReturnedIntent.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    ImageView imageView = (ImageView)this.findViewById(R.id.imageView);
                    imageView.setImageBitmap(imageBitmap);
                }

                break;
            case 1:
                if(resultCode == RESULT_OK && ActivityMainPageViewer.getVin() != null && ActivityMainPageViewer.getVin() != ""){
                    Uri uri = imageReturnedIntent.getData();
                    InputStream iStream = null;
                    byte [] data = null;
                    try {
                        iStream = getContentResolver().openInputStream(uri);
                        data = getBytes(iStream);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    File pictureFile = CameraActivity.getOutputMediaFile(CameraActivity.MEDIA_TYPE_IMAGE, ActivityMainPageViewer.getVin());
                    if (pictureFile == null){
                        return;
                    }

                    try {
                        FileOutputStream fos = new FileOutputStream(pictureFile);
                        fos.write(data);
                        fos.close();

                    } catch (FileNotFoundException e) {

                    } catch (IOException e) {
                    }
//                    Uri selectedImage = imageReturnedIntent.getData();
//                    setPicture(selectedImage);
                    //String realPath = new File(imageReturnedIntent.getData().getPath()).getAbsolutePath();
                    // SDK < API11
//                    if (Build.VERSION.SDK_INT < 11)
//                        realPath = RealPathUtil.getRealPathFromURI_BelowAPI11(this, imageReturnedIntent.getData());
//
//                        // SDK >= 11 && SDK < 19
//                    else if (Build.VERSION.SDK_INT < 19)
//                        realPath = RealPathUtil.getRealPathFromURI_API11to18(this, imageReturnedIntent.getData());
//
//                        // SDK > 19 (Android 4.4)
//                    else
//                        realPath = RealPathUtil.getRealPathFromURI_API19(this, imageReturnedIntent.getData());
                    ImageView jpgView = (ImageView)this.findViewById(R.id.imageView);
                    //imageView.setImageDrawable(drawable);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2;
                    //options.inScaled = true;
                    Bitmap bm = BitmapFactory.decodeFile(pictureFile.getAbsolutePath(), options);
                    jpgView.setImageBitmap(bm);
                    ApplicationStateSingleton.addPictureForCar(this, ActivityMainPageViewer.getVin(), pictureFile.getAbsolutePath());
                    //jpgView.setImageURI(uri);
                }
                break;
        }
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
            List<Address> addresses = geocoder.getFromLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1);
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
