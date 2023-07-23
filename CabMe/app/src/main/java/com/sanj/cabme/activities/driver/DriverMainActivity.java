package com.sanj.cabme.activities.driver;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.sanj.cabme.R;
import com.sanj.cabme.activities.About;
import com.sanj.cabme.activities.SplashActivity;
import com.sanj.cabme.wrapper.Wrapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static com.sanj.cabme.data.URLs.fetchDriverCurrentRideUrl;
import static com.sanj.cabme.data.URLs.fetchDriverDataUrl;
import static com.sanj.cabme.wrapper.Wrapper.authenticatedUniqueNumber;
import static com.sanj.cabme.wrapper.Wrapper.driverNID;
import static com.sanj.cabme.wrapper.Wrapper.isDeleted;

public class DriverMainActivity extends AppCompatActivity {
    private Context mContext;
    private String surname;
    private TextView txtSurname, txtDate, txtPickUpTime, txtFrom, txtTo, txtNoCurrentRide, btnView, midBullet, txtPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_driver_main);
        txtSurname = findViewById(R.id.txtSurname);
        txtDate = findViewById(R.id.txtDate);
        txtPickUpTime = findViewById(R.id.txtPickUpTime);
        txtFrom = findViewById(R.id.txtFrom);
        txtTo = findViewById(R.id.txtTo);
        txtNoCurrentRide = findViewById(R.id.txtNoCurrentRide);
        txtPhone = findViewById(R.id.txtPhone);
        midBullet = findViewById(R.id.midBullet);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        TextView txtMyRequest = findViewById(R.id.txtMyRequest);
        txtMyRequest.setOnClickListener(v -> startActivity(new Intent(mContext, MyRequest.class)));
        TextView txtRideHistory = findViewById(R.id.txtRideHistory);
        txtRideHistory.setOnClickListener(v -> startActivity(new Intent(mContext, RideHistory.class)));
        TextView txtAccountSettings = findViewById(R.id.txtAccountSettings);
        txtAccountSettings.setOnClickListener(v -> startActivity(new Intent(mContext, DriverProfile.class)));
        btnView = findViewById(R.id.btnView);
        btnView.setOnClickListener(v -> startActivity(new Intent(mContext, DriverCurrentRide.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!isDeleted) {
            loadData();
            Calendar calendar = Calendar.getInstance();
            String[] months = getResources().getStringArray(R.array.months);
            String[] days = getResources().getStringArray(R.array.days);

            String day = days[calendar.get(Calendar.DAY_OF_WEEK) - 1];
            String month = months[calendar.get(Calendar.MONTH)];
            String currentDate = day + ", " + calendar.get(Calendar.DAY_OF_MONTH) + " " + month + " " + calendar.get(Calendar.YEAR);
            txtDate.setText(currentDate);
        } else {
            signOut();
        }

    }

    private void loadData() {
        AlertDialog dialogWaiting = new Wrapper().waitingDialog("Fetching your data", mContext);
        Runnable loadDataThread = () -> {
            runOnUiThread(dialogWaiting::show);
            HashMap<String, String> params = new HashMap<>();
            params.put("email", authenticatedUniqueNumber);

            StringRequest request = new StringRequest(Request.Method.POST, fetchDriverDataUrl, response -> {
                try {
                    runOnUiThread(dialogWaiting::dismiss);
                    JSONObject responseObject = new JSONObject(response);

                    String responseCode = responseObject.getString("responseCode");
                    if (responseCode.equals("1")) {
                        JSONArray responseArray = responseObject.getJSONArray("responseData");
                        for (int i = 0; i < responseArray.length(); i++) {
                            JSONObject responseArrayObject = responseArray.getJSONObject(i);
                            driverNID = responseArrayObject.getString("nid");
                            surname = responseArrayObject.getString("name").split(" ")[0];
                            runOnUiThread(() -> txtSurname.setText(String.format("%s%s", getGreetings(), surname)));
                            getCurrentRide();
                        }
                    } else {
                        String responseMessage = responseObject.getString("responseMessage");
                        runOnUiThread(() -> new Wrapper().messageDialog(responseMessage, mContext));
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.driver_home_menu, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuSignOut:
                signOut();
                break;
            case R.id.menuAbout:
                startActivity(new Intent(mContext, About.class));
                break;
            case R.id.menuRateApp:
                rateApp();
                break;
        }
        return true;
    }

    private void signOut() {
        getSharedPreferences("cabme", MODE_PRIVATE).edit().clear().apply();
        startActivity(new Intent(mContext, SplashActivity.class));
        finish();
    }

    private void rateApp() {
        @SuppressLint("InflateParams") View root = getLayoutInflater().inflate(R.layout.rate_app_dialog_view, null);
        TextView txtAskMeLater, txtNoSorry, txtConfirm;
        txtAskMeLater = root.findViewById(R.id.txtAskMeLater);
        txtNoSorry = root.findViewById(R.id.txtNoSorry);
        txtConfirm = root.findViewById(R.id.txtConfirm);
        AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setView(root)
                .create();
        dialog.show();
        txtAskMeLater.setOnClickListener(v -> {
            dialog.dismiss();
            new Wrapper().errorToast("Ask you later", mContext);
        });
        txtNoSorry.setOnClickListener(v -> {
            dialog.dismiss();
            new Wrapper().errorToast("Will try other time", mContext);
        });
        txtConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            new Wrapper().successToast("Thank you", mContext);
        });
    }

    private String getGreetings() {
        Calendar calendar = Calendar.getInstance();
        int hoursOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        if (hoursOfDay >= 12 && hoursOfDay <= 16) {
            return "Good Afternoon ";
        } else if (hoursOfDay >= 17 || hoursOfDay <= 3) {
            return "Good Evening ";
        } else {
            return "Good Morning ";
        }
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
                        txtNoCurrentRide.setVisibility(View.GONE);
                        midBullet.setVisibility(View.VISIBLE);
                        btnView.setVisibility(View.VISIBLE);
                        JSONArray responseArray = responseObject.getJSONArray("responseData");
                        for (int i = 0; i < responseArray.length(); i++) {
                            JSONObject responseArrayObject = responseArray.getJSONObject(i);

                            String startLocation = responseArrayObject.getString("start_location");
                            String destinationLocation = responseArrayObject.getString("destination_location");
                            String pickUpTime = responseArrayObject.getString("pickup_time");
                            String passengerPhone = responseArrayObject.getString("phone");
                            String date= responseArrayObject.getString("date");


                            runOnUiThread(() -> {
                                txtPickUpTime.setText(String.format("%s, %s",date, pickUpTime));
                                txtFrom.setText(startLocation);
                                txtTo.setText(destinationLocation);
                                txtPhone.setText(passengerPhone);
                            });
                        }
                    } else {
                        txtPickUpTime.setText("");
                        txtFrom.setText("");
                        txtTo.setText("");
                        txtPhone.setText("");
                        txtNoCurrentRide.setVisibility(View.VISIBLE);
                        midBullet.setVisibility(View.GONE);
                        btnView.setVisibility(View.GONE);
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
}