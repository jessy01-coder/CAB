package com.sanj.cabme.activities.passenger;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.sanj.cabme.R;
import com.sanj.cabme.wrapper.Wrapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.sanj.cabme.data.URLs.deleteAccountUrl;
import static com.sanj.cabme.data.URLs.fetchPassengerDataUrl;
import static com.sanj.cabme.data.URLs.updatePassengerAccountDetailsUrl;
import static com.sanj.cabme.wrapper.Wrapper.authenticatedUniqueNumber;
import static com.sanj.cabme.wrapper.Wrapper.isDeleted;

public class PassengerProfile extends AppCompatActivity {
    private Context mContext;
    private TextInputEditText edFirstName, edSecondName, edIdNo, edEmail, edCounty, edPhone;
    private TextView profile_init, greetings;
    private String name, nid, email, county, phone;
    private SharedPreferences.Editor editor;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_profile);
        mContext = this;
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        setSupportActionBar(toolbar);
        edFirstName = findViewById(R.id.edFirstName);
        edSecondName = findViewById(R.id.edSecondName);
        edIdNo = findViewById(R.id.edIdNo);
        edEmail = findViewById(R.id.edEmail);
        edCounty = findViewById(R.id.edCounty);
        edPhone = findViewById(R.id.edPhone);
        greetings = findViewById(R.id.greetings);
        profile_init = findViewById(R.id.profile_init);
        SharedPreferences sharedPreferences = getSharedPreferences("cabme", MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.passenger_update_menu, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuSave:
                updateAccountDetails();
                break;
            case R.id.menuDelete:
                displayQuizDeleteAccount();
                break;
        }
        return true;
    }

    private void updateAccountDetails() {
        AlertDialog dialogWaiting = new Wrapper().waitingDialog("Updating passenger account", mContext);
        Runnable registrationThread = () -> {
            if (!(TextUtils.isEmpty(Objects.requireNonNull(edFirstName.getText()).toString().trim()) || TextUtils.isEmpty(Objects.requireNonNull(edSecondName.getText()).toString().trim())
                    || TextUtils.isEmpty(Objects.requireNonNull(edIdNo.getText()).toString().trim()) || TextUtils.isEmpty(Objects.requireNonNull(edCounty.getText()).toString().trim())
                    || TextUtils.isEmpty(Objects.requireNonNull(edEmail.getText()).toString().trim()) || TextUtils.isEmpty(Objects.requireNonNull(edPhone.getText()).toString().trim()))) {
                if (edEmail.getText().toString().contains(".") && edEmail.getText().toString().contains("@")) {
                    runOnUiThread(dialogWaiting::show);
                    name = Objects.requireNonNull(edFirstName.getText()).toString().trim() + " " + Objects.requireNonNull(edSecondName.getText()).toString().trim();
                    email = Objects.requireNonNull(edEmail.getText()).toString().trim();
                    nid = Objects.requireNonNull(edIdNo.getText()).toString().trim();
                    county = Objects.requireNonNull(edCounty.getText()).toString().trim();
                    phone = Objects.requireNonNull(edPhone.getText()).toString().trim();

                    HashMap<String, String> params = new HashMap<>();
                    params.put("name", name);
                    params.put("nid", nid);
                    params.put("email", email);
                    params.put("county", county);
                    params.put("phone", phone);

                    StringRequest request = new StringRequest(Request.Method.POST, updatePassengerAccountDetailsUrl, response -> {
                        try {
                            runOnUiThread(dialogWaiting::dismiss);
                            JSONObject responseObject = new JSONObject(response);
                            String responseCode = responseObject.getString("responseCode");
                            String responseMessage = responseObject.getString("responseMessage");

                            if (responseCode.equals("1")) {
                                editor.putString("phone", phone);
                                editor.apply();
                                runOnUiThread(() -> new Wrapper().successToast(responseMessage, mContext));
                            } else {
                                runOnUiThread(() -> new Wrapper().messageDialog(responseMessage, mContext));
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            runOnUiThread(() -> {
                                dialogWaiting.dismiss();
                                new Wrapper().messageDialog("Sorry an internal error occurred please try again later\n" + e.getMessage() + response, mContext);
                            });
                        }
                    }, error -> runOnUiThread(() -> {
                        dialogWaiting.dismiss();
                        new Wrapper().messageDialog("Sorry failed to connect to server please check your internet connection and try again later\n" + error.getMessage(), mContext);
                    })) {
                        @Override
                        protected Map<String, String> getParams() {
                            return params;
                        }
                    };
                    Volley.newRequestQueue(mContext).add(request);
                } else {
                    runOnUiThread(()->new Wrapper().messageDialog("Invalid Email, accepted format is abc@xyz.com", mContext));                }
            } else {
                runOnUiThread(() -> new Wrapper().errorToast("Incomplete update form!", mContext));
            }
        };
        new Thread(registrationThread).start();
    }

    private void displayQuizDeleteAccount() {
        new androidx.appcompat.app.AlertDialog.Builder(mContext)
                .setTitle("CABME")
                .setMessage("Do you want delete account?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    dialog.dismiss();
                    deleteAccount();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void deleteAccount() {
        AlertDialog dialogWaiting = new Wrapper().waitingDialog("Deleting account", mContext);
        Runnable deleteAccountThread = () -> {
            runOnUiThread(dialogWaiting::show);
            HashMap<String, String> params = new HashMap<>();
            params.put("key", authenticatedUniqueNumber);
            params.put("type", "P1");

            StringRequest request = new StringRequest(Request.Method.POST, deleteAccountUrl, response -> {
                try {
                    runOnUiThread(dialogWaiting::dismiss);
                    JSONObject responseObject = new JSONObject(response);

                    String responseCode = responseObject.getString("responseCode");
                    String responseMessage = responseObject.getString("responseMessage");

                    if (responseCode.equals("1")) {
                        isDeleted = true;
                        runOnUiThread(() -> new Wrapper().successToast(responseMessage, mContext));
                        finish();
                    } else {
                        runOnUiThread(() -> new Wrapper().errorToast(responseMessage, mContext));
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
        new Thread(deleteAccountThread).start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        fetchDriverProfile();
    }

    private void fetchDriverProfile() {
        AlertDialog dialogWaiting = new Wrapper().waitingDialog("Fetching profile data", mContext);
        Runnable fetchDriverProfileThread = () -> {
            runOnUiThread(dialogWaiting::show);
            HashMap<String, String> params = new HashMap<>();
            params.put("phone", authenticatedUniqueNumber);

            StringRequest request = new StringRequest(Request.Method.POST, fetchPassengerDataUrl, response -> {
                try {
                    runOnUiThread(dialogWaiting::dismiss);
                    JSONObject responseObject = new JSONObject(response);

                    String responseCode = responseObject.getString("responseCode");

                    if (responseCode.equals("1")) {
                        JSONArray responseArray = responseObject.getJSONArray("responseData");
                        for (int i = 0; i < responseArray.length(); i++) {
                            JSONObject responseArrayObject = responseArray.getJSONObject(i);
                            name = responseArrayObject.getString("name");
                            nid = responseArrayObject.getString("nid");
                            email = responseArrayObject.getString("email");
                            county = responseArrayObject.getString("county");
                            phone = responseArrayObject.getString("phone");

                            runOnUiThread(() -> {
                                String[] names = name.split(" ");
                                profile_init.setText(String.valueOf(name.charAt(0)));
                                greetings.setText(getGreetings() + "\n\t" + name);
                                edFirstName.setText(names[0]);
                                edSecondName.setText(names[1]);
                                edIdNo.setText(nid);
                                edEmail.setText(email);
                                edCounty.setText(county);
                                edPhone.setText(phone);
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
        new Thread(fetchDriverProfileThread).start();
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

}