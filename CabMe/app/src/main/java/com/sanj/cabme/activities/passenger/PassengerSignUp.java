package com.sanj.cabme.activities.passenger;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.sanj.cabme.R;
import com.sanj.cabme.wrapper.Wrapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.sanj.cabme.data.URLs.passengerRegistrationUrl;
import static com.sanj.cabme.wrapper.Wrapper.authenticatedUniqueNumber;

public class PassengerSignUp extends AppCompatActivity {
    private TextInputEditText edFirstName, edSecondName, edIdNo, edEmail, edCounty;
    private Button btnSignUp;
    private Context mContext;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_sign_up);
        mContext = this;
        edFirstName = findViewById(R.id.edFirstName);
        edSecondName = findViewById(R.id.edSecondName);
        edIdNo = findViewById(R.id.edIdNo);
        edEmail = findViewById(R.id.edEmail);
        edCounty = findViewById(R.id.edCounty);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnSignUp.setOnClickListener(v -> register());

        SharedPreferences sharedPreferences = getSharedPreferences("cabme", MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    private void register() {
        AlertDialog dialogWaiting = new Wrapper().waitingDialog("Creating passenger account", mContext);
        Runnable registrationThread = () -> {
            if (!(TextUtils.isEmpty(Objects.requireNonNull(edFirstName.getText()).toString().trim()) || TextUtils.isEmpty(Objects.requireNonNull(edSecondName.getText()).toString().trim())
                    || TextUtils.isEmpty(Objects.requireNonNull(edIdNo.getText()).toString().trim())|| TextUtils.isEmpty(Objects.requireNonNull(edCounty.getText()).toString().trim())
                    || TextUtils.isEmpty(Objects.requireNonNull(edEmail.getText()).toString().trim()))) {
                if (edEmail.getText().toString().contains(".") && edEmail.getText().toString().contains("@")) {

                    runOnUiThread(dialogWaiting::show);
                    String name, nid, email, county;
                    name = Objects.requireNonNull(edFirstName.getText()).toString().trim() + " " + Objects.requireNonNull(edSecondName.getText()).toString().trim();
                    email = Objects.requireNonNull(edEmail.getText()).toString().trim();
                    nid = Objects.requireNonNull(edIdNo.getText()).toString().trim();
                    county = Objects.requireNonNull(edCounty.getText()).toString().trim();

                    HashMap<String, String> params = new HashMap<>();
                    params.put("name", name);
                    params.put("nid", nid);
                    params.put("email", email);
                    params.put("county", county);
                    params.put("phone", authenticatedUniqueNumber);

                    StringRequest request = new StringRequest(Request.Method.POST, passengerRegistrationUrl, response -> {
                        try {
                            JSONObject responseObject = new JSONObject(response);
                            String responseCode = responseObject.getString("responseCode");
                            String responseMessage = responseObject.getString("responseMessage");

                            if (responseCode.equals("1")) {
                                editor.putString("phone", authenticatedUniqueNumber);
                                editor.putBoolean("passenger", true);
                                editor.putBoolean("haveAccount", true);
                                editor.apply();
                                runOnUiThread(() -> new Wrapper().successToast(responseMessage, mContext));
                                startActivity(new Intent(mContext, PassengerMainActivity.class));
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
                    runOnUiThread(()->new Wrapper().messageDialog("Invalid Email, accepted format is abc@xyz.com", mContext));                }
            } else {
                runOnUiThread(() -> new Wrapper().errorToast("Incomplete registration form!", mContext));
            }
        };
        new Thread(registrationThread).start();
    }
}