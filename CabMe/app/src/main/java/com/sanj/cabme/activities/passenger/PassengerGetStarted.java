package com.sanj.cabme.activities.passenger;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

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

import static com.sanj.cabme.data.URLs.fetchPassengerDataUrl;
import static com.sanj.cabme.wrapper.Wrapper.authenticatedUniqueNumber;

public class PassengerGetStarted extends AppCompatActivity {
    private TextInputEditText edPhone;
    private Context mContext;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_get_started);
        mContext = this;
        edPhone = findViewById(R.id.passengerPhone);
        Button btnNext = findViewById(R.id.btnNext);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyPhone();
            }
        });
        SharedPreferences sharedPreferences = getSharedPreferences("cabme", MODE_PRIVATE);
        editor = sharedPreferences.edit();

    }

    private void verifyPhone() {
        AlertDialog dialogWaiting = new Wrapper().waitingDialog("Checking passenger account", mContext);
        Runnable registrationThread = () -> {
            if (!(TextUtils.isEmpty(Objects.requireNonNull(edPhone.getText()).toString().trim()))) {

                runOnUiThread(dialogWaiting::show);
                String phone;
                phone = edPhone.getText().toString().trim();

                HashMap<String, String> params = new HashMap<>();
                params.put("phone", phone);

                StringRequest request = new StringRequest(Request.Method.POST, fetchPassengerDataUrl, response -> {
                    try {
                        JSONObject responseObject = new JSONObject(response);
                        String responseCode = responseObject.getString("responseCode");
                        authenticatedUniqueNumber = phone;
                        if (responseCode.equals("1")) {
                            editor.putString("phone", phone);
                            editor.putBoolean("passenger", true);
                            editor.putBoolean("haveAccount", true);
                            editor.apply();
                            startActivity(new Intent(mContext, PassengerMainActivity.class));
                        } else {

                            startActivity(new Intent(mContext, PassengerSignUp.class));
                        }
                        finish();

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
                runOnUiThread(() -> new Wrapper().errorToast("Incomplete registration form!", mContext));
            }
        };
        new Thread(registrationThread).start();
    }
}