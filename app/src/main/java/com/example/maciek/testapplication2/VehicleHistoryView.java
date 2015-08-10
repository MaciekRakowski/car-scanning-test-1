package com.example.maciek.testapplication2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.macieksquickcarprice.models.ApplicationStateSingleton;
import com.example.macieksquickcarprice.models.CarDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link VehicleHistoryView.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link VehicleHistoryView#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VehicleHistoryView extends Fragment {

    CarHistoryAdapter mCarHistoryAdapter;// = new CarHistoryAdapter();
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_vehicle_history_view, container, false);
        ListView listView = (ListView)view.findViewById(R.id.listview);
        ArrayList<CarDetails> carDetails = new ArrayList<>();

        for (CarDetails carDetail : ApplicationStateSingleton.getAllCarsInHistory()) {
            carDetails.add(carDetail);
        }


        mCarHistoryAdapter = new CarHistoryAdapter(view.getContext(), carDetails, this);
        listView.setAdapter(mCarHistoryAdapter);
        return view;
    }
}

class CarHistoryAdapter extends ArrayAdapter<CarDetails> {

    private final Context context;
    private final List<CarDetails> values;
    private final VehicleHistoryView mMainHistoryView;

    public CarHistoryAdapter(Context context, List<CarDetails> values, VehicleHistoryView mainHistoryView) {
        super(context, -1, values);
        this.context = context;
        this.values = values;
        this.mMainHistoryView = mainHistoryView;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView != null) {
            return convertView;
        }
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.fragment_history, parent, false);
        final CarDetails carDetail = this.values.get(position);
        Button b = (Button)view.findViewById(R.id.buttonShowCarDetails);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), ShowCarDetails.class);
                intent.putExtra("vin", carDetail.mVin);
                mMainHistoryView.startActivity(intent);
            }
        });

        TextView tv = (TextView)view.findViewById(R.id.carDetailsText);
        tv.setText(carDetail.toString());
        return view;
    }
}
