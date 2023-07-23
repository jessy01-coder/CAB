package com.sanj.cabme.activities.driver;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.sanj.cabme.R;
import com.sanj.cabme.wrapper.Wrapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.sanj.cabme.data.URLs.driverRegistrationUrl;

public class DriverSignUp extends AppCompatActivity {
    private CardView cardPersonalDetails, cardCarDetails, cardLocationDetails, cardFinishUp;
    private AutoCompleteTextView edModel;
    private TextInputEditText edFirstName, edSecondName, edIdNo, edEmail, edPhone, edCounty, edCarPlate, edColor, edFromRoute, edToRoute, edPassword, edConfirmPassword;
    private Context mContext;
    private String carModel;
    private List<String> autoCompleteHints;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_sign_up);
        cardPersonalDetails = findViewById(R.id.cardPersonalDetails);
        cardCarDetails = findViewById(R.id.cardCarDetails);
        cardLocationDetails = findViewById(R.id.cardLocationDetails);
        cardFinishUp = findViewById(R.id.cardFinishUp);
        Button toNextPersonal = findViewById(R.id.toNextPersonal);
        Button toNextCar = findViewById(R.id.toNextCar);
        Button toPreviousCar = findViewById(R.id.toPreviousCar);
        Button toNextLocation = findViewById(R.id.toNextLocation);
        Button toPreviousLocation = findViewById(R.id.toPreviousLocation);

        mContext = this;
        edFirstName = findViewById(R.id.edFirstName);
        edSecondName = findViewById(R.id.edSecondName);
        edIdNo = findViewById(R.id.edIdNo);
        edEmail = findViewById(R.id.edEmail);
        edPhone = findViewById(R.id.edPhone);
        edCounty = findViewById(R.id.edCounty);
        Button btnSignUp = findViewById(R.id.btnSignUp);
        edCarPlate = findViewById(R.id.edCarPlate);
        edColor = findViewById(R.id.edColor);
        edFromRoute = findViewById(R.id.edFromRoute);
        edToRoute = findViewById(R.id.edToRoute);
        edPassword = findViewById(R.id.edPassword);
        edConfirmPassword = findViewById(R.id.edConfirmPassword);
        edModel = findViewById(R.id.edModel);

        btnSignUp.setOnClickListener(v -> register());
        cardPersonalDetails.setVisibility(View.VISIBLE);
        toNextPersonal.setOnClickListener(v -> {

            if (!(TextUtils.isEmpty(Objects.requireNonNull(edFirstName.getText()).toString().trim()) || TextUtils.isEmpty(Objects.requireNonNull(edSecondName.getText()).toString().trim())
                    || TextUtils.isEmpty(Objects.requireNonNull(edIdNo.getText()).toString().trim()) || TextUtils.isEmpty(Objects.requireNonNull(edPhone.getText()).toString().trim())
                    || TextUtils.isEmpty(Objects.requireNonNull(edEmail.getText()).toString().trim()))) {
                if (edEmail.getText().toString().contains(".") && edEmail.getText().toString().contains("@")) {
                    cardCarDetails.setVisibility(View.VISIBLE);
                    cardPersonalDetails.setVisibility(View.GONE);
                } else {
                    new Wrapper().messageDialog("Invalid Email, accepted format is abc@xyz.com", mContext);
                }
            } else {
                new Wrapper().errorToast("Incomplete form!!", mContext);
            }

        });
        toNextCar.setOnClickListener(v -> {
            carModel = Objects.requireNonNull(edModel.getText()).toString().trim();
            if (!(TextUtils.isEmpty(Objects.requireNonNull(edCarPlate.getText()).toString().trim()) || TextUtils.isEmpty(Objects.requireNonNull(edColor.getText()).toString().trim())
                    || TextUtils.isEmpty(carModel))) {
                if (autoCompleteHints.contains(carModel)) {
                    cardLocationDetails.setVisibility(View.VISIBLE);
                    cardCarDetails.setVisibility(View.GONE);
                } else {
                    new Wrapper().messageDialog("The car model provided is invalid. Please make another selection from the options provided list", mContext);
                }
            } else {
                new Wrapper().errorToast("Incomplete form!!", mContext);
            }

        });
        toPreviousCar.setOnClickListener(v -> {
            cardCarDetails.setVisibility(View.GONE);
            cardPersonalDetails.setVisibility(View.VISIBLE);
        });
        toNextLocation.setOnClickListener(v -> {
            if (!(TextUtils.isEmpty(Objects.requireNonNull(edFromRoute.getText()).toString().trim()) || TextUtils.isEmpty(Objects.requireNonNull(edToRoute.getText()).toString().trim())
                    || TextUtils.isEmpty(Objects.requireNonNull(edCounty.getText()).toString().trim()))) {
                cardFinishUp.setVisibility(View.VISIBLE);
                cardLocationDetails.setVisibility(View.GONE);
            } else {
                new Wrapper().errorToast("Incomplete form!!", mContext);
            }

        });
        toPreviousLocation.setOnClickListener(v -> {
            cardLocationDetails.setVisibility(View.GONE);
            cardCarDetails.setVisibility(View.VISIBLE);
        });


    }

    @Override
    protected void onStart() {
        super.onStart();
        setAutoCompleteHints();
    }

    private void setAutoCompleteHints() {
        autoCompleteHints = Arrays.asList("Toyota Axio", "Toyota Fielder", "Toyota Probox", "Toyota Succeed", "Toyota Corolla",
                "Toyota Allion", "Toyota Auris", "Toyota Premio", "Honda Airwave", "Honda Fit", "Honda Insight", "Honda Shuttle",
                "Nissan Bluebird", "Nissan Cube", "Nissan Juke", "Nissan Note", "Nissan Tilda", "Nissan Sunny", "Mazda Demio",
                "Mazda Axela", "Mazda CX5", "Mazda Atenza", "Mercedes S class", "Mercedes C class", "Mercedes E class", "Mercedes G class");

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, autoCompleteHints);
        edModel.setAdapter(arrayAdapter);
    }

    private void register() {
        AlertDialog dialogWaiting = new Wrapper().waitingDialog("Creating driver account", mContext);
        Runnable registrationThread = () -> {

            if (!(TextUtils.isEmpty(Objects.requireNonNull(edPassword.getText()).toString().trim()) || TextUtils.isEmpty(Objects.requireNonNull(edConfirmPassword.getText()).toString().trim()))) {
                if (edPassword.getText().toString().trim().equals(edConfirmPassword.getText().toString().trim())) {

                    runOnUiThread(dialogWaiting::show);
                    String name, nid, email, county, phone, password, routeFrom, routeTo, carPlate, carColor;
                    name = Objects.requireNonNull(edFirstName.getText()).toString().trim() + " " + Objects.requireNonNull(edSecondName.getText()).toString().trim();
                    email = Objects.requireNonNull(edEmail.getText()).toString().trim();
                    nid = Objects.requireNonNull(edIdNo.getText()).toString().trim();
                    county = Objects.requireNonNull(edCounty.getText()).toString().trim();
                    phone = Objects.requireNonNull(edPhone.getText()).toString().trim();
                    password = Objects.requireNonNull(edPassword.getText()).toString().trim();
                    routeFrom = Objects.requireNonNull(edFromRoute.getText()).toString().trim();
                    routeTo = Objects.requireNonNull(edToRoute.getText()).toString().trim();
                    carPlate = Objects.requireNonNull(edCarPlate.getText()).toString().trim();
                    carColor = Objects.requireNonNull(edColor.getText()).toString().trim();


                    HashMap<String, String> params = new HashMap<>();
                    params.put("name", name);
                    params.put("nid", nid);
                    params.put("phone", phone);
                    params.put("email", email);
                    params.put("county", county);
                    params.put("password", password);
                    params.put("route_from", routeFrom);
                    params.put("route_to", routeTo);
                    params.put("plate", carPlate);
                    params.put("model", carModel);
                    params.put("color", carColor);

                    StringRequest request = new StringRequest(Request.Method.POST, driverRegistrationUrl, response -> {
                        try {
                            JSONObject responseObject = new JSONObject(response);
                            String responseCode = responseObject.getString("responseCode");
                            String responseMessage = responseObject.getString("responseMessage");

                            if (responseCode.equals("1")) {
                                runOnUiThread(() -> new Wrapper().successToast(responseMessage, mContext));
                                finish();
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
                        runOnUiThread(dialogWaiting::dismiss);
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
                    runOnUiThread(() -> new Wrapper().errorToast("Passwords do not match", mContext));
                }
            } else {
                runOnUiThread(() -> new Wrapper().errorToast("Incomplete registration form!", mContext));
            }
        };
        new Thread(registrationThread).start();
    }
}