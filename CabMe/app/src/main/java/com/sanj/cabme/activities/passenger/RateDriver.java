package com.sanj.cabme.activities.passenger;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RatingBar;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.sanj.cabme.R;
import com.sanj.cabme.wrapper.Wrapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.sanj.cabme.data.URLs.ratingDriverUrl;

public class RateDriver extends AppCompatActivity {
    private RatingBar ratingBar;
    private EditText edComment;
    private Context mContext;
    private CheckBox checkboxMusic, checkboxWifi, checkboxDriving, checkboxBehaviour, checkboxCab, checkboxArrival;
    private String mDriverNID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate_driver);
        mContext = this;
        mDriverNID = getIntent().getExtras().getString("nid");
        ratingBar = findViewById(R.id.ratingBar);
        edComment = findViewById(R.id.edComment);
        checkboxMusic = findViewById(R.id.checkboxMusic);
        checkboxWifi = findViewById(R.id.checkboxWifi);
        checkboxDriving = findViewById(R.id.checkboxDriving);
        checkboxBehaviour = findViewById(R.id.checkboxBehaviour);
        checkboxCab = findViewById(R.id.checkboxCab);
        checkboxArrival = findViewById(R.id.checkboxArrival);
        Button btnSubmit = findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(v -> rateDriver());
    }

    private void rateDriver() {
        String passengerResponse = "Response: ";
        if (!edComment.getText().toString().isEmpty()) {
            passengerResponse = "COMMENT: " + edComment.getText().toString().trim() + "::";
        }
        if (checkboxMusic.isChecked()) {
            passengerResponse += " Music,";
        }
        if (checkboxCab.isChecked()) {
            passengerResponse += " Cab quality,";
        }
        if (checkboxBehaviour.isChecked()) {
            passengerResponse += " Behaviour,";
        }
        if (checkboxDriving.isChecked()) {
            passengerResponse += " Driving,";
        }
        if (checkboxWifi.isChecked()) {
            passengerResponse += " WiFi,";
        }
        if (checkboxArrival.isChecked()) {
            passengerResponse += " Arrival time,";
        }
        String ratings = String.valueOf(ratingBar.getRating());

        AlertDialog dialogWaiting = new Wrapper().waitingDialog("Rating driver", mContext);
        String finalPassengerResponse = passengerResponse;
        Runnable rateDriverThread = () -> {
            runOnUiThread(dialogWaiting::show);
            HashMap<String, String> params = new HashMap<>();
            params.put("nid", mDriverNID);
            params.put("response", finalPassengerResponse);
            params.put("ratings", ratings);

            StringRequest request = new StringRequest(Request.Method.POST, ratingDriverUrl, response -> {
                try {
                    runOnUiThread(dialogWaiting::dismiss);
                    JSONObject responseObject = new JSONObject(response);

                    String responseCode = responseObject.getString("responseCode");
                    String responseMessage = responseObject.getString("responseMessage");
                    if (responseCode.equals("1")) {
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
        new Thread(rateDriverThread).start();


    }

}