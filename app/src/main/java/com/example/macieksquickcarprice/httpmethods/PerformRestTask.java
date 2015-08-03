package com.example.macieksquickcarprice.httpmethods;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;


public abstract class PerformRestTask extends AsyncTask<String, Void, String> {

	static final String API_KEY = "kqgxzvjdbfhe8evjjjhffjk4";
	static final HashMap<ApiMethods, String> sMethods = new HashMap<ApiMethods, String>();
	static {
		sMethods.put(ApiMethods.GET_ALL_CARS, "https://api.edmunds.com/api/vehicle/v2/makes?state=used&year=%s&view=basic&fmt=json&api_key=" + API_KEY);
		sMethods.put(ApiMethods.GET_CAR_BY_VIN, "https://api.edmunds.com/api/vehicle/v2/vins/%S?fmt=json&api_key=" + API_KEY);
		//styleid, condition, mileage, zip, options, 
		sMethods.put(ApiMethods.GET_CAR_PRICE, "https://api.edmunds.com/v1/api/tmv/tmvservice/calculateusedtmv?styleid=%s&condition=%s&mileage=%s&zip=%s&%sfmt=json&api_key=" + API_KEY);
		//https://api.edmunds.com/v1/api/tmv/tmvservice/calculateusedtmv?styleid=101395591&condition=Clean&mileage=100000&zip=90404&optionid=200381492&fmt=json&api_key=kqgxzvjdbfhe8evjjjhffjk4
		
	}

	protected abstract ApiMethods getMethod();
	
	
    @Override
    protected String doInBackground(String... args) {
    	ApiMethods method = getMethod();
    	String url = sMethods.get(method);
    	switch (method) {
	    	case GET_ALL_CARS:
	    		url = String.format(url, args[0]);
	    		break;
	    	case GET_CAR_BY_VIN:
	    		url = String.format(url, args[0]);
	    		break;
	    	case GET_CAR_PRICE:
	    		url = String.format(url, args[0], args[1], args[2], args[3], args[4]);
	    		break;
			default:
				throw new RuntimeException("no method found.");
    	}
    	String result = DoHttpGet(url);
    	return result;
    }

    
    private String DoHttpGet(String url) {
    	
    	try {
	        HttpClient httpclient = new DefaultHttpClient();
	        HttpGet httpGet = new HttpGet(url);
	        HttpResponse response = httpclient.execute(httpGet);
	        StatusLine statusLine = response.getStatusLine();
	        if(statusLine.getStatusCode() == HttpStatus.SC_OK){
	            ByteArrayOutputStream out = new ByteArrayOutputStream();
	            response.getEntity().writeTo(out);
	            out.close();
	            String responseString = out.toString();
	            System.out.println(responseString);
	            return responseString;
	        } else{
	            //Closes the connection.
	            response.getEntity().getContent().close();
	            return "error";
	        }
    	} catch (Exception ex) {
    		String message = ex.getMessage();
    		System.out.println(message);
    		ex.printStackTrace();
    		return "error";
    	}
    }
}
