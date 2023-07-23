package com.sanj.cabme.activities.passenger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.sanj.cabme.R;
import com.sanj.cabme.activities.Category;
import com.sanj.cabme.adapters.CabRecyclerViewAdapter;
import com.sanj.cabme.models.CabModel;
import com.sanj.cabme.wrapper.Wrapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.sanj.cabme.data.URLs.fetchPassengerDataUrl;
import static com.sanj.cabme.data.URLs.filterDriverUrl;
import static com.sanj.cabme.data.URLs.getDriverInPassengerRadiusUrl;
import static com.sanj.cabme.wrapper.Wrapper.authenticatedUniqueNumber;
import static com.sanj.cabme.wrapper.Wrapper.isDeleted;

public class PassengerMainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private Context mContext;
    private Activity mActivity;
    private TextView initProfile;
    private String mLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mContext = this;
        mActivity=this;
        recyclerView = findViewById(R.id.recyclerView);
        initProfile = findViewById(R.id.profile_init);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.passenger_home_menu, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuRide:
                startActivity(new Intent(mContext, PassengerRides.class));
                break;
            case R.id.menuProfile:
                startActivity(new Intent(mContext, PassengerProfile.class));
                break;
            case R.id.menuSignOut:
                signOut();
                break;
            case R.id.menuFilter:
                filterForm();
                break;
            case R.id.meuWallet:
                startActivity(new Intent(mContext, PassengerWallet.class));
                break;
        }
        return true;
    }

    private void filterForm() {
        @SuppressLint("InflateParams") View root = LayoutInflater.from(mContext).inflate(R.layout.filter_form_dialog, null);
        Spinner searchBySpinner = root.findViewById(R.id.searchBySpinner);
        TextInputEditText edSearchField = root.findViewById(R.id.edSearchField);
        Button btnSearch = root.findViewById(R.id.btnSearch);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(mContext)
                .setView(root)
                .create();
        dialog.show();

        btnSearch.setOnClickListener(v -> {
            String[] searchKeys = getResources().getStringArray(R.array.search_by);
            String key = getSearchKey(searchKeys[(int) searchBySpinner.getSelectedItemId()]);
            if (!(TextUtils.isEmpty(Objects.requireNonNull(edSearchField.getText()).toString()) || key.equals("Search driver by"))) {
               dialog.dismiss();
                String value = edSearchField.getText().toString();
                AlertDialog dialogWaiting = new Wrapper().waitingDialog("Filtering drivers", mContext);
                Runnable loadDataThread = () -> {
                    runOnUiThread(dialogWaiting::show);
                    HashMap<String, String> params = new HashMap<>();
                    params.put("key", key);
                    params.put("value", value);

                    StringRequest request = new StringRequest(Request.Method.POST, filterDriverUrl, response -> {
                        try {
                            runOnUiThread(dialogWaiting::dismiss);
                            JSONObject responseObject = new JSONObject(response);

                            String responseCode = responseObject.getString("responseCode");
                            if (responseCode.equals("1")) {
                                List<CabModel> cabModelList = new ArrayList<>();
                                JSONArray responseArray = responseObject.getJSONArray("responseData");
                                for (int i = 0; i < responseArray.length(); i++) {
                                    JSONObject responseArrayObject = responseArray.getJSONObject(i);
                                    String name, phone, routeFrom, routeTo, carPlate, carModel, mDriverNID,price;
                                    name = responseArrayObject.getString("name");
                                    mLocation = responseArrayObject.getString("county") + " county";
                                    phone = responseArrayObject.getString("phone");
                                    routeFrom = responseArrayObject.getString("route_from");
                                    routeTo = responseArrayObject.getString("route_to");
                                    carPlate = responseArrayObject.getString("plate");
                                    carModel = responseArrayObject.getString("model");
                                    mDriverNID = responseArrayObject.getString("nid");
                                    price= responseArrayObject.getString("route_price");


                                    cabModelList.add(new CabModel(name, phone, mLocation, routeFrom, routeTo, carPlate, carModel, mDriverNID,price));
                                }
                                recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
                                recyclerView.setAdapter(new CabRecyclerViewAdapter(cabModelList, mContext, mActivity, mLocation));
                            } else {
                                String responseMessage = responseObject.getString("responseMessage");
                                runOnUiThread(() -> Toast.makeText(mContext, responseMessage, Toast.LENGTH_SHORT).show());
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

            } else {
                new Wrapper().errorToast("All fields required", mContext);
            }
        });

    }

    private String getSearchKey(String input) {
        switch (input) {
            case "Name":
                return "name";
            case "Phone":
                return "phone";
            case "County":
                return "county";
            case "Constituency":
                return "constituency";
            case "Ward":
                return "Ward";
            case "Street":
                return "street";
            default:
                return "";
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!isDeleted) {
            loadData();
        } else {
            signOut();
        }
    }

    private void signOut() {
        SharedPreferences sharedPreferences = getSharedPreferences("cabme", MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
        startActivity(new Intent(mContext, Category.class));
        finish();
    }

    private void loadData() {
        AlertDialog dialogWaiting = new Wrapper().waitingDialog("Fetching your location data", mContext);
        Runnable loadDataThread = () -> {
            runOnUiThread(dialogWaiting::show);
            HashMap<String, String> params = new HashMap<>();
            params.put("phone", authenticatedUniqueNumber);

            StringRequest request = new StringRequest(Request.Method.POST, fetchPassengerDataUrl, response -> {
                try {
                    runOnUiThread(dialogWaiting::dismiss);
                    JSONObject responseObject = new JSONObject(response);

                    String responseCode = responseObject.getString("responseCode");
                    if (responseCode.equals("1")) {
                        String county = null,name = null;
                        JSONArray responseArray = responseObject.getJSONArray("responseData");
                        for (int i = 0; i < responseArray.length(); i++) {
                            JSONObject responseArrayObject = responseArray.getJSONObject(i);
                            name = responseArrayObject.getString("name");
                            county = responseArrayObject.getString("county");
                        }
                        String finalName = name;
                        runOnUiThread(() -> initProfile.setText(String.valueOf(finalName.charAt(0))));
                        loadDrivers(county);
                    } else {
                        String responseMessage = responseObject.getString("responseMessage");
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

    private void loadDrivers(String county) {
        AlertDialog dialogWaiting = new Wrapper().waitingDialog("Fetching rides in your area", mContext);
        Runnable loadDataThread = () -> {
            runOnUiThread(dialogWaiting::show);
            HashMap<String, String> params = new HashMap<>();
            params.put("county", county);

            StringRequest request = new StringRequest(Request.Method.POST, getDriverInPassengerRadiusUrl, response -> {
                try {
                    runOnUiThread(dialogWaiting::dismiss);
                    JSONObject responseObject = new JSONObject(response);

                    String responseCode = responseObject.getString("responseCode");
                    if (responseCode.equals("1")) {
                        List<CabModel> cabModelList = new ArrayList<>();
                        JSONArray responseArray = responseObject.getJSONArray("responseData");
                        for (int i = 0; i < responseArray.length(); i++) {
                            JSONObject responseArrayObject = responseArray.getJSONObject(i);
                            String name, phone, routeFrom, routeTo, carPlate, carModel, mDriverNID,price;
                            name = responseArrayObject.getString("name");
                            mLocation = responseArrayObject.getString("county") + " county";
                            phone = responseArrayObject.getString("phone");
                            routeFrom = responseArrayObject.getString("route_from");
                            routeTo = responseArrayObject.getString("route_to");
                            carPlate = responseArrayObject.getString("plate");
                            carModel = responseArrayObject.getString("model");
                            mDriverNID = responseArrayObject.getString("nid");
                            price= responseArrayObject.getString("route_price");

                            cabModelList.add(new CabModel(name, phone, mLocation, routeFrom, routeTo, carPlate, carModel, mDriverNID,price));
                        }
                        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
                        recyclerView.setAdapter(new CabRecyclerViewAdapter(cabModelList, mContext, this, mLocation));
                    } else {
                        String responseMessage = responseObject.getString("responseMessage");
                        runOnUiThread(() -> Toast.makeText(mContext, responseMessage, Toast.LENGTH_SHORT).show());
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