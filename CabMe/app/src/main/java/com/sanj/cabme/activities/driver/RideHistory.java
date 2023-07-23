package com.sanj.cabme.activities.driver;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.sanj.cabme.R;
import com.sanj.cabme.adapters.RideHistoryRecyclerViewAdapter;
import com.sanj.cabme.models.RideHistoryModel;
import com.sanj.cabme.wrapper.Wrapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.sanj.cabme.data.URLs.fetchDriverRideHistoryUrl;
import static com.sanj.cabme.wrapper.Wrapper.driverNID;

@SuppressLint("SetTextI18n")
public class RideHistory extends AppCompatActivity {
    private Context mContext;
    private RecyclerView recyclerView;
    private TextView txtMonth, txtValue;
    private String month, year;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_history);
        mContext = this;
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        recyclerView = findViewById(R.id.recyclerView);
        txtMonth = findViewById(R.id.txtMonth);
        txtValue = findViewById(R.id.txtValue);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onStart() {
        super.onStart();
        Calendar calendar = Calendar.getInstance();
        String[] months = getResources().getStringArray(R.array.months_shrt);
        month = months[calendar.get(Calendar.MONTH)];
        year = String.valueOf(calendar.get(Calendar.YEAR));
        txtMonth.setText(month + " " + calendar.get(Calendar.YEAR));
        loadRideOrders();
    }

    private void loadRideOrders() {
        AlertDialog dialogWaiting = new Wrapper().waitingDialog("Fetching ride history", mContext);
        Runnable fetchRideOrdersThread = () -> {
            runOnUiThread(dialogWaiting::show);
            HashMap<String, String> params = new HashMap<>();
            params.put("nid", driverNID);
            params.put("month", month);
            params.put("year", year);
            StringRequest request = new StringRequest(Request.Method.POST, fetchDriverRideHistoryUrl, response -> {
                try {
                    runOnUiThread(dialogWaiting::dismiss);
                    JSONObject responseObject = new JSONObject(response);

                    String responseCode = responseObject.getString("responseCode");
                    if (responseCode.equals("1")) {
                        List<RideHistoryModel> rideHistoryModelList = new ArrayList<>();
                        JSONArray responseArray = responseObject.getJSONArray("responseData");
                        String total = responseObject.getString("total");

                        for (int i = 0; i < responseArray.length(); i++) {
                            JSONObject responseArrayObject = responseArray.getJSONObject(i);
                            String date, description, departureTime, arrivalTime, passengerPhone, orderId;
                            date = responseArrayObject.getString("date");
                            departureTime = responseArrayObject.getString("departure_time");
                            passengerPhone = responseArrayObject.getString("passenger_phone");
                            arrivalTime = responseArrayObject.getString("arrival_time");
                            orderId = responseArrayObject.getString("order_id");
                            String startLocation = responseArrayObject.getString("start_location");
                            String destinationLocation = responseArrayObject.getString("destination_location");
                            String fare = responseArrayObject.getString("fare");


                            description = "FROM " + startLocation + " TO " + destinationLocation;

                            rideHistoryModelList.add(new RideHistoryModel(date, description, departureTime, arrivalTime, passengerPhone, orderId, fare));
                        }
                        runOnUiThread(() -> {
                            txtValue.setText("Kes. " + total);
                            recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
                            recyclerView.setAdapter(new RideHistoryRecyclerViewAdapter(rideHistoryModelList, mContext));
                        });

                    } else {
                        String responseMessage = responseObject.getString("responseMessage");
                        runOnUiThread(() -> Toast.makeText(mContext, responseMessage, Toast.LENGTH_SHORT).show());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(dialogWaiting::dismiss);
                    runOnUiThread(() -> new Wrapper().errorToast("Sorry an internal error occurred please try again later\n" + e.getMessage() + response, mContext));
                    finish();
                }

            }, error -> runOnUiThread(() -> {
                new Wrapper().errorToast("Sorry failed to connect to server please check your internet connection and try again later\n" + error.getMessage(), mContext);
                dialogWaiting.dismiss();
                finish();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_ride_history_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuPayout) {
            payoutDialog();
        }
        return true;
    }

    @SuppressLint("SetTextI18n")
    private void payoutDialog() {
        @SuppressLint("InflateParams") View root = getLayoutInflater().inflate(R.layout.payout_dialog_view, null);
        Button btnGenerate = root.findViewById(R.id.btnGenerate);
        Spinner monthSpinner = root.findViewById(R.id.monthSpinner);
        TextInputEditText edYear = root.findViewById(R.id.edYear);
        AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setView(root)
                .create();
        dialog.show();
        Calendar calendar = Calendar.getInstance();
        monthSpinner.setSelection(calendar.get(Calendar.MONTH));
        edYear.setText(String.valueOf(calendar.get(Calendar.YEAR)));
        btnGenerate.setOnClickListener(v -> {
            if (!(TextUtils.isEmpty(Objects.requireNonNull(edYear.getText()).toString().trim()))) {
                String[] months = getResources().getStringArray(R.array.months_shrt);
                month = months[(int) monthSpinner.getSelectedItemId()];
                year = String.valueOf(calendar.get(Calendar.YEAR));
                txtMonth.setText(month + " " + edYear.getText().toString().trim());
                dialog.dismiss();
                loadRideOrders();
            } else {
                new Wrapper().errorToast("Year required!", mContext);
            }
        });
    }
}