package com.example.maciek.testapplication2;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.bees4honey.vinscanner.B4HScanner;
import com.example.macieksquickcarprice.models.PriceDetails;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = this.getIntent();
        String vin = intent.getStringExtra("vin");
        if (vin != null) {
            EditText editText = (EditText)this.findViewById(R.id.editTextVin);
            editText.setText(vin);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    public void scanVin(View view) {
        try {
            Intent intent = new Intent(this, Scanner.class);
            startActivity(intent);
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void showDetails(View view) {
        try {
            Intent intent = new Intent(this, ShowCarDetails.class);
            EditText editText = (EditText)this.findViewById(R.id.editTextVin);
            String vin = editText.getText().toString();
            intent.putExtra("vin", vin);
            startActivity(intent);
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
