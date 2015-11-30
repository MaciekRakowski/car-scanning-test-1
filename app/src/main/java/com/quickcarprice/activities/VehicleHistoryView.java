package com.quickcarprice.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.maciek.testapplication2.R;
import com.example.macieksquickcarprice.models.ApplicationStateSingleton;
import com.example.macieksquickcarprice.models.CarDetails;

import java.util.ArrayList;
import java.util.List;


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
        TextView textViewShowMap = (TextView)view.findViewById(R.id.textViewShowMap);
        textViewShowMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), CarHistoryMap.class );
                view.getContext().startActivity(intent);
            }
        });
        return view;
    }

    public void onMapHistoryClick(View view) {

    }

    @Override
    public void onResume() {
        View view = this.getView();
        ListView listView = (ListView)view.findViewById(R.id.listview);
        ArrayList<CarDetails> carDetails = getCarDetailsList();

        mCarHistoryAdapter = new CarHistoryAdapter(view.getContext(), carDetails, this);
        listView.setAdapter(mCarHistoryAdapter);
        if (ActivityMainPageViewer.getListViewState() != null) {
            listView.onRestoreInstanceState(ActivityMainPageViewer.getListViewState());
        }

        super.onResume();
    }

    static ArrayList<CarDetails> getCarDetailsList() {
        ArrayList<CarDetails> carDetails = new ArrayList<>();
        for (CarDetails carDetail : ApplicationStateSingleton.getAllCarsInHistory()) {
            carDetails.add(carDetail);
        }
        return carDetails;
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
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.fragment_history, parent, false);
        final CarDetails carDetail = this.values.get(position);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), ShowCarDetails.class);
                intent.putExtra("vin", carDetail.mVin);
                intent.putExtra("ignore-location", true);
                ActivityMainPageViewer.setVin(carDetail.mVin);
                mMainHistoryView.startActivity(intent);
            }
        });

        Button buttonRemoveCar = (Button)view.findViewById(R.id.buttonRemoveFromHistory);
        buttonRemoveCar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast toast = Toast.makeText(CarHistoryAdapter.this.getContext(),//getApplicationContext(),
                        String.format("%s %s %s removed!", carDetail.mYear, carDetail.mMake, carDetail.mModelName),
                        Toast.LENGTH_SHORT);

                toast.show();

                ApplicationStateSingleton.removeCar(carDetail.mVin, mMainHistoryView.getActivity());
                values.remove(position);
                CarHistoryAdapter.this.notifyDataSetChanged();
            }
        });

        TextView tv = (TextView)view.findViewById(R.id.carDetailsText);
        tv.setText(carDetail.toString());

        TextView modelDetailsTextView = (TextView)view.findViewById(R.id.modelDetailsText);
        modelDetailsTextView.setText(carDetail.mTrimFullName);
        return view;
    }
}
