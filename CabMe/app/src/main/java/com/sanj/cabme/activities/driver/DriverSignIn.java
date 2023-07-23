package com.sanj.cabme.activities.driver;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.android.material.textfield.TextInputEditText;
import com.sanj.cabme.R;
import com.sanj.cabme.activities.About;
import com.sanj.cabme.wrapper.Wrapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.sanj.cabme.data.URLs.driverAuthenticationUrl;
import static com.sanj.cabme.wrapper.Wrapper.authenticatedUniqueNumber;

public class DriverSignIn extends AppCompatActivity {
    private Context mContext;
    private TextInputEditText edEmail, edPassword;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_sign_in);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mContext = this;
        TextView txtToSignUp = findViewById(R.id.txtToSignUp);
        edEmail = findViewById(R.id.edEmail);
        edPassword = findViewById(R.id.edPassword);
        Button btnSignIn = findViewById(R.id.btnSignIn);
        btnSignIn.setOnClickListener(v -> authentication());
        txtToSignUp.setOnClickListener(v -> startActivity(new Intent(mContext, DriverSignUp.class)));
        SharedPreferences sharedPreferences = getSharedPreferences("cabme", MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.driver_login_menu, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuAbout:
                startActivity(new Intent(mContext, About.class));
                break;
            case R.id.menuForgotPassword:
                Intent intent = new Intent(mContext, ResetPassword.class);
                intent.putExtra("auth", false);
                startActivity(intent);
                break;
        }
        return true;
    }

    private void authentication() {
        AlertDialog dialogWaiting = new Wrapper().waitingDialog("Authenticating driver", mContext);
        Runnable registrationThread = () -> {
            if (!(TextUtils.isEmpty(Objects.requireNonNull(edEmail.getText()).toString().trim()) || TextUtils.isEmpty(Objects.requireNonNull(edPassword.getText()).toString().trim()))) {
                if (edEmail.getText().toString().contains(".") && edEmail.getText().toString().contains("@")) {
                    runOnUiThread(dialogWaiting::show);
                    String email, password;
                    email = Objects.requireNonNull(edEmail.getText()).toString().trim();
                    password = Objects.requireNonNull(edPassword.getText()).toString().trim();

                    HashMap<String, String> params = new HashMap<>();
                    params.put("password", password);
                    params.put("email", email);

                    StringRequest request = new StringRequest(Request.Method.POST, driverAuthenticationUrl, response -> {
                        try {
                            JSONObject responseObject = new JSONObject(response);
                            String responseCode = responseObject.getString("responseCode");
                            String responseMessage = responseObject.getString("responseMessage");

                            if (responseCode.equals("1")) {
                                authenticatedUniqueNumber = email;
                                editor.putBoolean("haveAccount", true);
                                editor.apply();
                                runOnUiThread(() -> new Wrapper().successToast(responseMessage, mContext));
                                startActivity(new Intent(mContext, DriverMainActivity.class));
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
                    runOnUiThread(()->new Wrapper().messageDialog("Invalid Email, accepted format is abc@xyz.com", mContext));
                }
            } else {
                runOnUiThread(() -> new Wrapper().errorToast("Incomplete registration form!", mContext));
            }
        };
        new Thread(registrationThread).start();
    }
}