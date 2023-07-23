package com.sanj.cabme.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.sanj.cabme.R;
import com.sanj.cabme.activities.driver.DriverSignIn;
import com.sanj.cabme.activities.passenger.PassengerGetStarted;

public class Category extends AppCompatActivity {
    private RadioGroup categoryRadioGroup;
    private Button btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);
        categoryRadioGroup = findViewById(R.id.categoryRadioGroup);
        btnNext = findViewById(R.id.btnNext);
        RadioButton radioPassenger = findViewById(R.id.radioPassenger);
        radioPassenger.setChecked(true);

        btnNext.setOnClickListener(v -> {
            int selectedCategory = categoryRadioGroup.getCheckedRadioButtonId();
            RadioButton radioButton = categoryRadioGroup.findViewById(selectedCategory);
            if (radioButton.getText().toString().equals("Driver")) {
                startActivity(new Intent(Category.this, DriverSignIn.class));
            } else {
                startActivity(new Intent(Category.this, PassengerGetStarted.class));
            }
            finish();
        });
    }
}