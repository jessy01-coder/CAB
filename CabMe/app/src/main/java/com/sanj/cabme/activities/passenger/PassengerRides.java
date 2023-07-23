package com.sanj.cabme.activities.passenger;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.sanj.cabme.R;
import com.sanj.cabme.wrapper.Wrapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.sanj.cabme.data.URLs.fetchCurrentPassengerRideUrl;
import static com.sanj.cabme.data.URLs.passengerRideActionsUrl;
import static com.sanj.cabme.wrapper.Wrapper.authenticatedUniqueNumber;

public class PassengerRides extends AppCompatActivity {
    private Context mContext;
    private TextView driverName, driverPhone,
            passengerLocation, passengerDestinationLocation,
            passengerPickUpTime, txtStatus;
    private Button btnAction;
    private String mOrderId, mDriverNID, feedback, fare,mTodayDate;
    private Menu mMenu;
    private Calendar calendar;
    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_passenger_rides);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        setSupportActionBar(toolbar);

        driverName = findViewById(R.id.driverName);
        driverPhone = findViewById(R.id.driverPhone);
        passengerLocation = findViewById(R.id.passengerLocation);
        passengerDestinationLocation = findViewById(R.id.passengerDestinationLocation);
        passengerPickUpTime = findViewById(R.id.passengerPickUpTime);
        txtStatus = findViewById(R.id.txtStatus);
        btnAction = findViewById(R.id.btnAction);
        btnAction.setOnClickListener(v -> {
            feedback = btnAction.getText().toString();
            if (feedback.equals("CONFIRM PICKUP")) {
                feedback = "CONFIRM";
            }
            passengerRideActions();

        });
        calendar = Calendar.getInstance();
        String[] months=getResources().getStringArray(R.array.months_shrt);
        String month=months[calendar.get(Calendar.MONTH)];
        mTodayDate=calendar.get(Calendar.DAY_OF_MONTH)+" "+month+" "+calendar.get(Calendar.YEAR);

        sharedPreferences = getSharedPreferences("cabme", MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    private void passengerRideActions() {
        int minute = calendar.get(Calendar.MINUTE);
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        String minStr = minute < 10 ? "0" + minute : String.valueOf(minute);
        String hourOfDayStr = hourOfDay < 10 ? "0" + hourOfDay : String.valueOf(hourOfDay);
        String mCurrentTime = hourOfDayStr + minStr + "hrs";

        AlertDialog dialogWaiting = new Wrapper().waitingDialog("Sending feedback", mContext);
        Runnable sendFeedbackThread = () -> {
            runOnUiThread(dialogWaiting::show);
            HashMap<String, String> params = new HashMap<>();
            params.put("id", mOrderId);
            params.put("feedback", feedback);
            params.put("time", mCurrentTime);
            params.put("user_id", authenticatedUniqueNumber);
            params.put("fare", fare);
            params.put("date", mTodayDate);


            StringRequest request = new StringRequest(Request.Method.POST, passengerRideActionsUrl, response -> {
                try {
                    runOnUiThread(dialogWaiting::dismiss);
                    JSONObject responseObject = new JSONObject(response);

                    String responseCode = responseObject.getString("responseCode");
                    String responseMessage = responseObject.getString("responseMessage");

                    if (responseCode.equals("1")) {
                        if (feedback.equals("ARRIVAL")) {
                            editor.putBoolean("isReminderSet",false);
                            editor.apply();
                            Intent intent = new Intent(mContext, RateDriver.class);
                            intent.putExtra("nid", mDriverNID);
                            startActivity(intent);
                        }
                        finish();
                        runOnUiThread(() -> new Wrapper().successToast(responseMessage, mContext));
                    } else {
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
        new Thread(sendFeedbackThread).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.passenger_rides_menu, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuCancel) {
            new androidx.appcompat.app.AlertDialog.Builder(mContext)
                    .setTitle("CABME")
                    .setMessage("Are you sure you want to cancel the ride order?")
                    .setPositiveButton("CANCEL RIDE", (dialog, which) -> {
                        dialog.dismiss();
                        cancelRideOrder();
                    })
                    .setNegativeButton("ABORT", (dialog, which) -> dialog.dismiss())
                    .create()
                    .show();
        }
        return true;
    }

    private void cancelRideOrder() {
        AlertDialog dialogWaiting = new Wrapper().waitingDialog("Cancelling ride order", mContext);
        Runnable loadDataThread = () -> {
            runOnUiThread(dialogWaiting::show);
            HashMap<String, String> params = new HashMap<>();
            params.put("id", mOrderId);
            params.put("feedback", "CANCEL");
            params.put("time", "0");
            params.put("user_id", authenticatedUniqueNumber);
            params.put("fare", fare);
            params.put("date", mTodayDate);

            StringRequest request = new StringRequest(Request.Method.POST, passengerRideActionsUrl, response -> {
                try {
                    runOnUiThread(dialogWaiting::dismiss);
                    JSONObject responseObject = new JSONObject(response);
                    String responseCode = responseObject.getString("responseCode");
                    String responseMessage = responseObject.getString("responseMessage");
                    if (responseCode.equals("1")) {
                        editor.putBoolean("isReminderSet",false);
                        editor.apply();
                        runOnUiThread(() -> new Wrapper().successToast(responseMessage, mContext));
                        finish();
                    } else {
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
    protected void onStart() {
        super.onStart();
        loadData();
    }

    private void loadData() {
        AlertDialog dialogWaiting = new Wrapper().waitingDialog("Fetching your ride details", mContext);
        Runnable loadDataThread = () -> {
            runOnUiThread(dialogWaiting::show);
            HashMap<String, String> params = new HashMap<>();
            params.put("phone", authenticatedUniqueNumber);

            StringRequest request = new StringRequest(Request.Method.POST, fetchCurrentPassengerRideUrl, response -> {
                try {
                    runOnUiThread(dialogWaiting::dismiss);
                    JSONObject responseObject = new JSONObject(response);

                    String responseCode = responseObject.getString("responseCode");
                    if (responseCode.equals("1")) {
                        JSONArray responseArray = responseObject.getJSONArray("responseData");
                        String name = null, phone = null, startLocation = null, destinationLocation = null, date = null, pickupTime = null, status = null;
                        for (int i = 0; i < responseArray.length(); i++) {
                            JSONObject responseArrayObject = responseArray.getJSONObject(i);
                            name = responseArrayObject.getString("name");
                            phone = responseArrayObject.getString("phone");
                            startLocation = responseArrayObject.getString("start_location");
                            destinationLocation = responseArrayObject.getString("destination_location");
                            pickupTime = responseArrayObject.getString("pickup_time");
                            mOrderId = responseArrayObject.getString("order_id");
                            status = responseArrayObject.getString("status");
                            mDriverNID = responseArrayObject.getString("nid");
                            fare = responseArrayObject.getString("fare");
                            date = responseArrayObject.getString("date");
                        }
                        String finalName = name;
                        String finalPhone = phone;
                        String finalStartLocation = startLocation;
                        String finalDestinationLocation = destinationLocation;
                        String finalPickupTime = pickupTime;
                        String finalStatus = status;
                        String finalDate = date;
                        runOnUiThread(() -> {
                            driverName.setText(finalName);
                            driverPhone.setText(finalPhone);
                            passengerLocation.setText(finalStartLocation);
                            passengerDestinationLocation.setText(finalDestinationLocation);
                            if (finalPickupTime.equals("0")) {
                                passengerPickUpTime.setText("N/A");
                            } else {
                                passengerPickUpTime.setText(finalDate+"," + finalPickupTime);
                            }
                            switch (finalStatus) {
                                case "0000":
                                    mMenu.clear();
                                    txtStatus.setText("PICKUP CONFIRMED...");
                                    btnAction.setText("DEPARTURE");
                                    break;
                                case "00":
                                    txtStatus.setText("ACCEPTED...");
                                    btnAction.setText("CONFIRM PICKUP");
                                    break;
                                case "000":
                                    mMenu.clear();
                                    txtStatus.setText("DEPARTED...");
                                    btnAction.setText("ARRIVAL");
                                    break;
                                default:
                                    txtStatus.setText("PENDING...");
                                    btnAction.setEnabled(false);
                                    break;
                            }
                            if (!finalPickupTime.equals("0") && !finalDate.equals("0") && !sharedPreferences.getBoolean("isReminderSet",false)) {
                                String reminderHr = finalPickupTime.charAt(0) + "" + finalPickupTime.charAt(1);
                                int reminderMin = Integer.parseInt(finalPickupTime.charAt(2) + "" + finalPickupTime.charAt(3)) - 3;
                                String reminderTime = reminderHr + ":" + reminderMin;
                                String reminderDate = finalDate + " " + reminderTime;
                                String description = "This is a reminder for your ride to " + finalDestinationLocation + " scheduled pick up by the driver at " + finalPickupTime;
                                new Wrapper().setVaccinesReminder(mContext, reminderDate, description);
                                editor.putBoolean("isReminderSet",true);
                                editor.apply();
                            }
                        });
                    } else {
                        editor.putBoolean("isReminderSet",false);
                        editor.apply();
                        String responseMessage = responseObject.getString("responseMessage");
                        runOnUiThread(() -> new Wrapper().errorToast(responseMessage, mContext));
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
}