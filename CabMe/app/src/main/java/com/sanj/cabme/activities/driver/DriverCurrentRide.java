package com.sanj.cabme.activities.driver;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.sanj.cabme.R;
import com.sanj.cabme.activities.passenger.RateDriver;
import com.sanj.cabme.wrapper.Wrapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static com.sanj.cabme.data.URLs.fetchDriverCurrentRideUrl;
import static com.sanj.cabme.data.URLs.passengerRideActionsUrl;
import static com.sanj.cabme.wrapper.Wrapper.driverNID;

public class DriverCurrentRide extends AppCompatActivity {
    private TextView txtStatus, txtCurrentLocation, txtPhone, txtStartLocation, txtDestinationLocation, txtPickUpTime, txtPassenger, txtPassengerCount;
    private Context mContext;
    private Button btnConfirmPayment;
    private String mOrderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_driver_current_ride);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        txtCurrentLocation = findViewById(R.id.txtCurrentLocation);
        txtPhone = findViewById(R.id.txtPhone);
        txtStartLocation = findViewById(R.id.txtStartLocation);
        txtDestinationLocation = findViewById(R.id.txtDestinationLocation);
        txtPickUpTime = findViewById(R.id.txtPickUpTime);
        txtPassenger = findViewById(R.id.txtPassenger);
        txtPassengerCount = findViewById(R.id.txtPassengerCount);
        txtStatus = findViewById(R.id.txtStatus);
    }

    private void getCurrentRide() {
        AlertDialog dialogWaiting = new Wrapper().waitingDialog("Checking current ride", mContext);
        Runnable loadDataThread = () -> {
            runOnUiThread(dialogWaiting::show);
            HashMap<String, String> params = new HashMap<>();
            params.put("nid", driverNID);

            StringRequest request = new StringRequest(Request.Method.POST, fetchDriverCurrentRideUrl, response -> {
                try {
                    runOnUiThread(dialogWaiting::dismiss);
                    JSONObject responseObject = new JSONObject(response);

                    String responseCode = responseObject.getString("responseCode");
                    if (responseCode.equals("1")) {
                        JSONArray responseArray = responseObject.getJSONArray("responseData");
                        for (int i = 0; i < responseArray.length(); i++) {
                            JSONObject responseArrayObject = responseArray.getJSONObject(i);
                            mOrderId= responseArrayObject.getString("order_id");
                            String startLocation = responseArrayObject.getString("start_location");
                            String destinationLocation = responseArrayObject.getString("destination_location");
                            String pickUpTime = responseArrayObject.getString("pickup_time");
                            String passengerPhone = responseArrayObject.getString("phone");
                            String passengerName = responseArrayObject.getString("name");
                            String passengerCount = responseArrayObject.getString("passenger_count");
                            String status = responseArrayObject.getString("status");
                            runOnUiThread(() -> {
                                txtCurrentLocation.setText(startLocation);
                                txtPickUpTime.setText(pickUpTime);
                                txtStartLocation.setText(startLocation);
                                txtDestinationLocation.setText(destinationLocation);
                                txtPhone.setText(passengerPhone);
                                txtPassenger.setText(passengerName);
                                txtPassengerCount.setText(passengerCount);
                                if (status.equals("000")) {
                                    txtStatus.setText("Departed...");
                                }else if (status.equals("0000")){
                                    txtStatus.setText("PASSENGER CONFIRMED PICKUP...");
                                }
                            });
                        }
                    } else {
                        String message = responseObject.getString("responseMessage");
                        runOnUiThread(() -> new Wrapper().errorToast(message, mContext));
                        finish();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(dialogWaiting::dismiss);
                    runOnUiThread(() -> new Wrapper().messageDialog("Sorry an internal error occurred please try again later\n" + e.getMessage() + response, mContext));
                }

            }, error -> runOnUiThread(() -> {
                new Wrapper().messageDialog("Sorry failed to connect to server please check your internet connection and try again later\n" + error.getMessage(), mContext);
                dialogWaiting.dismiss();
            })) {
                @Override
                protected Map<String, String> getParams() {
                    return params;
                }
            };
            Volley.newRequestQueue(mContext).add(request);
        };
        new Thread(loadDataThread).start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        getCurrentRide();
    }


}