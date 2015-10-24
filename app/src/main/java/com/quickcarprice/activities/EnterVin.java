package com.quickcarprice.activities;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;

import com.example.maciek.testapplication2.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link EnterVin.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link EnterVin#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EnterVin extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_enter_vin, container, false);
        Button scanVinButton = (Button)view.findViewById(R.id.buttonScanVin);
        scanVinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanVin(v);
            }
        });

        Button showDetailsButton = (Button)view.findViewById(R.id.buttonShowCarDetails);
        showDetailsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDetails(v);
            }
        });

        EditText editText = (EditText)view.findViewById(R.id.editTextVin);
        editText.setText(ActivityMainPageViewer.getVin());

        return view;
    }

    public void scanVin(View view) {
        try {
            Vibrator v = (Vibrator) this.getActivity().getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (v.hasVibrator()) {
                v.vibrate(75);
            }
//            Intent intent = new Intent(view.getContext(), Scanner.class);
//            startActivity(intent);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("b4hvinscan://scan?caller_name=Show Details&callback_url=my.special.scheme%3A%2F%2Fscan%3Fscript%3Dmaciek%26param%3DB4HVINCODE"));
            startActivity(intent);
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void showDetails(View view) {
        try {
            Intent intent = new Intent(view.getContext(), ShowCarDetails.class);

            EditText editText = (EditText)this.getView().findViewById(R.id.editTextVin);
            String vin = editText.getText().toString();
            intent.putExtra("vin", vin);
            ActivityMainPageViewer.setVin(vin);
            startActivity(intent);
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
