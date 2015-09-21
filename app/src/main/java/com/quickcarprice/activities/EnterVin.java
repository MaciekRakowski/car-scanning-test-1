package com.quickcarprice.activities;

import android.os.Bundle;
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
            Intent intent = new Intent(view.getContext(), Scanner.class);
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
