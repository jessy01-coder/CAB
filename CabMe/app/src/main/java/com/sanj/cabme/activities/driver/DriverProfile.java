package com.sanj.cabme.activities.driver;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.sanj.cabme.R;
import com.sanj.cabme.wrapper.Wrapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.sanj.cabme.data.URLs.deleteAccountUrl;
import static com.sanj.cabme.data.URLs.fetchDriverProfileUrl;
import static com.sanj.cabme.data.URLs.updateAccountDetailsUrl;
import static com.sanj.cabme.data.URLs.updateCarDetailsUrl;
import static com.sanj.cabme.data.URLs.updateLocationDetailsUrl;
import static com.sanj.cabme.wrapper.Wrapper.driverNID;
import static com.sanj.cabme.wrapper.Wrapper.isDeleted;

public class DriverProfile extends AppCompatActivity {
    private TextView profile_init, txtPhone, txtEmail, txtPendingValue, txtCompleteValue;
    private RatingBar ratingBar;
    private Context mContext;
    private float ratingValue;
    private List<String> autoCompleteHints;
    private String price, model, plate, color, county, route_to, route_from, name, phone, email, availableCount, completeCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_profile);
        mContext = this;
        profile_init = findViewById(R.id.profile_init);
        txtPhone = findViewById(R.id.txtPhone);
        txtEmail = findViewById(R.id.txtEmail);
        txtPendingValue = findViewById(R.id.txtPendingValue);
        txtCompleteValue = findViewById(R.id.txtCompleteValue);
        TextView txtChangePassword = findViewById(R.id.txtChangePassword);
        TextView txtDeleteAccount = findViewById(R.id.txtDeleteAccount);
        TextView txtViewLocation = findViewById(R.id.txtViewLocation);
        TextView txtCarDetails = findViewById(R.id.txtCarDetails);
        ratingBar = findViewById(R.id.ratingBar);
        ratingBar.setEnabled(false);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        txtViewLocation.setOnClickListener(v -> displayLocationForm());
        txtCarDetails.setOnClickListener(v -> displayCarForm());
        txtChangePassword.setOnClickListener(v -> toResetPassword());
        txtDeleteAccount.setOnClickListener(v -> displayQuizDeleteAccount());
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
            params.put("nid", driverNID);

            StringRequest request = new StringRequest(Request.Method.POST, fetchDriverProfileUrl, response -> {
                try {
                    runOnUiThread(dialogWaiting::dismiss);
                    JSONObject responseObject = new JSONObject(response);

                    String responseCode = responseObject.getString("responseCode");
                    availableCount = responseObject.getString("availableCount");
                    completeCount = responseObject.getString("completeCount");
                    ratingValue = Float.parseFloat(responseObject.getString("ratings"));

                    if (responseCode.equals("1")) {
                        JSONArray responseArray = responseObject.getJSONArray("responseData");
                        for (int i = 0; i < responseArray.length(); i++) {
                            JSONObject responseArrayObject = responseArray.getJSONObject(i);
                            model = responseArrayObject.getString("model");
                            plate = responseArrayObject.getString("plate");
                            color = responseArrayObject.getString("color");
                            county = responseArrayObject.getString("county");
                            route_to = responseArrayObject.getString("route_to");
                            route_from = responseArrayObject.getString("route_from");
                            name = responseArrayObject.getString("name");
                            phone = responseArrayObject.getString("phone");
                            email = responseArrayObject.getString("email");
                            price = responseArrayObject.getString("route_price");

                            runOnUiThread(() -> {
                                profile_init.setText(String.valueOf(name.charAt(0)));
                                txtPhone.setText(phone);
                                txtEmail.setText(email);
                                txtPendingValue.setText(availableCount);
                                txtCompleteValue.setText(completeCount);
                                ratingBar.setRating(ratingValue);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.driver_profile_menu, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuEdit) {
            displayAccountEditingForm();
        }
        return true;
    }

    private void displayAccountEditingForm() {
        String[] names = name.split(" ");
        @SuppressLint("InflateParams") View root = LayoutInflater.from(mContext).inflate(R.layout.driver_edit_account_details_form, null);
        TextInputEditText edFirstName, edSecondName, edIdNo, edEmail, edPhone;
        Button btnSubmit;

        edFirstName = root.findViewById(R.id.edFirstName);
        edSecondName = root.findViewById(R.id.edSecondName);
        edIdNo = root.findViewById(R.id.edIdNo);
        edEmail = root.findViewById(R.id.edEmail);
        edPhone = root.findViewById(R.id.edPhone);
        btnSubmit = root.findViewById(R.id.btnSubmit);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(mContext)
                .setView(root)
                .create();
        dialog.show();

        edFirstName.setText(names[0]);
        edSecondName.setText(names[1]);
        edIdNo.setText(driverNID);
        edEmail.setText(email);
        edPhone.setText(phone);
        btnSubmit.setOnClickListener(v -> {
            if (!(TextUtils.isEmpty(Objects.requireNonNull(edFirstName.getText()).toString().trim()) || TextUtils.isEmpty(Objects.requireNonNull(edSecondName.getText()).toString().trim())
                    || TextUtils.isEmpty(Objects.requireNonNull(edIdNo.getText()).toString().trim()) || TextUtils.isEmpty(Objects.requireNonNull(edPhone.getText()).toString().trim())
                    || TextUtils.isEmpty(Objects.requireNonNull(edEmail.getText()).toString().trim()))) {
                if (edEmail.getText().toString().contains(".") && edEmail.getText().toString().contains("@")) {
                    name = Objects.requireNonNull(edFirstName.getText()).toString().trim() + " " + Objects.requireNonNull(edSecondName.getText()).toString().trim();
                    email = Objects.requireNonNull(edEmail.getText()).toString().trim();
                    phone = Objects.requireNonNull(edPhone.getText()).toString().trim();
                    dialog.dismiss();
                    updateAccountDetails();
                } else {
                    new Wrapper().messageDialog("Invalid Email, accepted format is abc@xyz.com", mContext);
                }
            } else {
                new Wrapper().errorToast("Incomplete form!!", mContext);
            }
        });
    }

    private void updateAccountDetails() {
        AlertDialog dialogWaiting = new Wrapper().waitingDialog("Updating account details", mContext);
        Runnable updateAccountDetailsThread = () -> {
            runOnUiThread(dialogWaiting::show);
            HashMap<String, String> params = new HashMap<>();
            params.put("nid", driverNID);
            params.put("name", name);
            params.put("phone", phone);
            params.put("email", email);

            StringRequest request = new StringRequest(Request.Method.POST, updateAccountDetailsUrl, response -> {
                try {
                    runOnUiThread(dialogWaiting::dismiss);
                    JSONObject responseObject = new JSONObject(response);

                    String responseCode = responseObject.getString("responseCode");
                    String responseMessage = responseObject.getString("responseMessage");

                    if (responseCode.equals("1")) {
                        onStart();
                        runOnUiThread(() -> new Wrapper().successToast(responseMessage, mContext));
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
        new Thread(updateAccountDetailsThread).start();
    }

    private void displayCarForm() {

        @SuppressLint("InflateParams") View root = LayoutInflater.from(mContext).inflate(R.layout.driver_edit_car_form, null);
        TextInputEditText edCarPlate, edColor;
        AutoCompleteTextView edModel;
        Button btnSubmit;

        edCarPlate = root.findViewById(R.id.edCarPlate);
        edColor = root.findViewById(R.id.edColor);
        edModel = root.findViewById(R.id.edModel);
        btnSubmit = root.findViewById(R.id.btnSubmit);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(mContext)
                .setView(root)
                .create();
        dialog.show();

        autoCompleteHints = Arrays.asList("Toyota Axio", "Toyota Fielder", "Toyota Probox", "Toyota Succeed", "Toyota Corolla",
                "Toyota Allion", "Toyota Auris", "Toyota Premio", "Honda Airwave", "Honda Fit", "Honda Insight", "Honda Shuttle",
                "Nissan Bluebird", "Nissan Cube", "Nissan Juke", "Nissan Note", "Nissan Tilda", "Nissan Sunny", "Mazda Demio",
                "Mazda Axela", "Mazda CX5", "Mazda Atenza", "Mercedes S class", "Mercedes C class", "Mercedes E class", "Mercedes G class");

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, autoCompleteHints);
        edModel.setAdapter(arrayAdapter);

        edCarPlate.setText(plate);
        edColor.setText(color);
        edModel.setText(model);


        btnSubmit.setOnClickListener(v -> {
            model = Objects.requireNonNull(edModel.getText()).toString().trim();
            if (!(TextUtils.isEmpty(Objects.requireNonNull(edCarPlate.getText()).toString().trim()) || TextUtils.isEmpty(Objects.requireNonNull(edColor.getText()).toString().trim())
                    || TextUtils.isEmpty(model))) {
                if (autoCompleteHints.contains(model)) {
                    dialog.dismiss();
                    plate = edCarPlate.getText().toString().trim();
                    color = edColor.getText().toString().trim();
                    updateCarDetails();
                } else {
                    new Wrapper().messageDialog("The car model provided is invalid. Please make another selection from the options provided list", mContext);
                }

            } else {
                new Wrapper().errorToast("Incomplete form!!", mContext);
            }
        });

    }


    private void updateCarDetails() {
        AlertDialog dialogWaiting = new Wrapper().waitingDialog("Updating account details", mContext);
        Runnable updateCarDetailsThread = () -> {
            runOnUiThread(dialogWaiting::show);
            HashMap<String, String> params = new HashMap<>();
            params.put("nid", driverNID);
            params.put("plate", plate);
            params.put("model", model);
            params.put("color", color);

            StringRequest request = new StringRequest(Request.Method.POST, updateCarDetailsUrl, response -> {
                try {
                    runOnUiThread(dialogWaiting::dismiss);
                    JSONObject responseObject = new JSONObject(response);

                    String responseCode = responseObject.getString("responseCode");
                    String responseMessage = responseObject.getString("responseMessage");

                    if (responseCode.equals("1")) {
                        runOnUiThread(() -> new Wrapper().successToast(responseMessage, mContext));
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
        new Thread(updateCarDetailsThread).start();
    }

    private void displayLocationForm() {
        @SuppressLint("InflateParams") View root = LayoutInflater.from(mContext).inflate(R.layout.driver_edit_location_form, null);
        TextInputEditText edCounty, edFromRoute, edToRoute, edPrice;
        Button btnSubmit;

        edCounty = root.findViewById(R.id.edCounty);
        edFromRoute = root.findViewById(R.id.edFromRoute);
        edToRoute = root.findViewById(R.id.edToRoute);
        edPrice = root.findViewById(R.id.edPrice);
        btnSubmit = root.findViewById(R.id.btnSubmit);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(mContext)
                .setView(root)
                .create();
        dialog.show();

        edCounty.setText(county);
        edFromRoute.setText(route_from);
        edToRoute.setText(route_to);
        edPrice.setText(price);
        btnSubmit.setOnClickListener(v -> {
            if (!(TextUtils.isEmpty(Objects.requireNonNull(edFromRoute.getText()).toString().trim()) || TextUtils.isEmpty(Objects.requireNonNull(edToRoute.getText()).toString().trim())
                    || TextUtils.isEmpty(Objects.requireNonNull(edCounty.getText()).toString().trim())|| TextUtils.isEmpty(Objects.requireNonNull(edPrice.getText()).toString().trim()))) {
                dialog.dismiss();
                county = edCounty.getText().toString().trim();
                route_from = edFromRoute.getText().toString().trim();
                route_to = edToRoute.getText().toString().trim();
                price = edPrice.getText().toString().trim();
                updateLocationDetails();
            } else {
                new Wrapper().errorToast("Incomplete form!!", mContext);
            }
        });
    }

    private void updateLocationDetails() {
        AlertDialog dialogWaiting = new Wrapper().waitingDialog("Updating account details", mContext);
        Runnable updateLocationDetailsThread = () -> {
            runOnUiThread(dialogWaiting::show);
            HashMap<String, String> params = new HashMap<>();
            params.put("nid", driverNID);
            params.put("county", county);
            params.put("route_from", route_from);
            params.put("route_to", route_to);
            params.put("route_price", price);

            StringRequest request = new StringRequest(Request.Method.POST, updateLocationDetailsUrl, response -> {
                try {
                    runOnUiThread(dialogWaiting::dismiss);
                    JSONObject responseObject = new JSONObject(response);

                    String responseCode = responseObject.getString("responseCode");
                    String responseMessage = responseObject.getString("responseMessage");

                    if (responseCode.equals("1")) {
                        runOnUiThread(() -> new Wrapper().successToast(responseMessage, mContext));
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
        new Thread(updateLocationDetailsThread).start();
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
            params.put("key", driverNID);
            params.put("type", "D1");

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

    private void toResetPassword() {
        Intent intent = new Intent(mContext, ResetPassword.class);
        intent.putExtra("auth", true);
        startActivity(intent);
    }
}