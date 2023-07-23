package com.sanj.cabme.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.sanj.cabme.R;
import com.sanj.cabme.activities.driver.DriverSignIn;
import com.sanj.cabme.activities.passenger.PassengerMainActivity;

import static com.sanj.cabme.wrapper.Wrapper.authenticatedUniqueNumber;
import static java.lang.Thread.sleep;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Runnable runnable = () -> {
            try {
                sleep(3000);
                SharedPreferences sharedPreferences = getSharedPreferences("cabme", MODE_PRIVATE);
                if (sharedPreferences.getBoolean("haveAccount", false)) {
                    if (sharedPreferences.getBoolean("passenger", false)) {
                        authenticatedUniqueNumber = sharedPreferences.getString("phone", "");
                        startActivity(new Intent(SplashActivity.this, PassengerMainActivity.class));
                    } else {
                        startActivity(new Intent(SplashActivity.this, DriverSignIn.class));
                    }
                } else {
                    startActivity(new Intent(SplashActivity.this, Category.class));
                }
                finish();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        new Thread(runnable).start();
    }
}