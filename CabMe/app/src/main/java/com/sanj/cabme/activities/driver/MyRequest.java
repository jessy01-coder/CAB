package com.sanj.cabme.activities.driver;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.sanj.cabme.R;
import com.sanj.cabme.adapters.RideOrderRecyclerViewAdapter;
import com.sanj.cabme.models.RideOrderModel;
import com.sanj.cabme.wrapper.Wrapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sanj.cabme.data.URLs.fetchRideOrdersUrl;
import static com.sanj.cabme.wrapper.Wrapper.driverNID;

public class MyRequest extends AppCompatActivity {
    private Context mContext;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_request);
        mContext = this;
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        recyclerView = findViewById(R.id.recyclerView);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadRideOrders();
    }

    private void loadRideOrders() {
        AlertDialog dialogWaiting = new Wrapper().waitingDialog("Fetching your ride orders", mContext);
        Runnable fetchRideOrdersThread = () -> {
            runOnUiThread(dialogWaiting::show);
            HashMap<String, String> params = new HashMap<>();
            params.put("nid", driverNID);

            StringRequest request = new StringRequest(Request.Method.POST, fetchRideOrdersUrl, response -> {
                try {
                    runOnUiThread(dialogWaiting::dismiss);
                    JSONObject responseObject = new JSONObject(response);

                    String responseCode = responseObject.getString("responseCode");
                    if (responseCode.equals("1")) {
                        List<RideOrderModel> rideOrderModelList = new ArrayList<>();
                        JSONArray responseArray = responseObject.getJSONArray("responseData");
                        for (int i = 0; i < responseArray.length(); i++) {
                            JSONObject responseArrayObject = responseArray.getJSONObject(i);
                            String name, phone, startLocation, pickupTime, destinationLocation, orderId;
                            name = responseArrayObject.getString("name");
                            phone = responseArrayObject.getString("passenger_phone");
                            startLocation = responseArrayObject.getString("start_location");
                            pickupTime = responseArrayObject.getString("pickup_time");
                            destinationLocation = responseArrayObject.getString("destination_location");
                            orderId = responseArrayObject.getString("order_id");


                            rideOrderModelList.add(new RideOrderModel(name, phone, startLocation, pickupTime, destinationLocation, orderId));
                        }
                        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
                        recyclerView.setAdapter(new RideOrderRecyclerViewAdapter(rideOrderModelList, mContext, this));
                    } else {
                        String responseMessage = responseObject.getString("responseMessage");
                        runOnUiThread(() -> Toast.makeText(mContext, responseMessage, Toast.LENGTH_SHORT).show());
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
        new Thread(fetchRideOrdersThread).start();
    }
}