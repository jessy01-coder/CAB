package com.sanj.cabme.activities.passenger;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.sanj.cabme.R;
import com.sanj.cabme.adapters.TransactionStatementRecyclerViewAdapter;
import com.sanj.cabme.models.TransactionStatement;
import com.sanj.cabme.wrapper.Wrapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.sanj.cabme.data.URLs.depositToWalletUrl;
import static com.sanj.cabme.data.URLs.fetchWalletUrl;
import static com.sanj.cabme.data.URLs.filterTransactionStatementUrl;
import static com.sanj.cabme.wrapper.Wrapper.authenticatedUniqueNumber;

@SuppressLint("SetTextI18n")
public class PassengerWallet extends AppCompatActivity {
    private Context mContext;
    private TextView walletBalance;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_wallet);
        mContext=this;
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        walletBalance = findViewById(R.id.walletBalance);
        Button btnDeposit = findViewById(R.id.btnDeposit);
        btnDeposit.setOnClickListener(v -> displayDepositDialog());
        Button btnTransactionStatement = findViewById(R.id.btnTransactionStatement);
        btnTransactionStatement.setOnClickListener(v -> transactionFilterDialog());
        recyclerView = findViewById(R.id.recyclerView);
    }

    @SuppressLint("SetTextI18n")
    private void transactionFilterDialog() {
        @SuppressLint("InflateParams") View root = getLayoutInflater().inflate(R.layout.payout_dialog_view, null);
        Button btnGenerate = root.findViewById(R.id.btnGenerate);
        Spinner monthSpinner = root.findViewById(R.id.monthSpinner);
        TextInputEditText edYear = root.findViewById(R.id.edYear);
        TextView title=root.findViewById(R.id.title);
        AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setView(root)
                .create();
        dialog.show();

        title.setText("Get transactions for ");
        btnGenerate.setText("SUBMIT");
        Calendar calendar = Calendar.getInstance();
        monthSpinner.setSelection(calendar.get(Calendar.MONTH));
        edYear.setText(String.valueOf(calendar.get(Calendar.YEAR)));
        btnGenerate.setOnClickListener(v -> {
            if (!(TextUtils.isEmpty(Objects.requireNonNull(edYear.getText()).toString().trim()))) {
                String[] months = getResources().getStringArray(R.array.months_shrt);
                String month = months[(int) monthSpinner.getSelectedItemId()];
                String year = String.valueOf(calendar.get(Calendar.YEAR));
                getTransactionStatement(month,year);
                dialog.dismiss();
            } else {
                new Wrapper().errorToast("Year required!", mContext);
            }
        });
    }

    private void getTransactionStatement(String month, String year) {
        AlertDialog dialogWaiting = new Wrapper().waitingDialog("Please wait...", mContext);
        Runnable fetchRideOrdersThread = () -> {
            runOnUiThread(dialogWaiting::show);
            HashMap<String, String> params = new HashMap<>();
            params.put("user_id", authenticatedUniqueNumber);
            params.put("month_year", month+" "+year);
            StringRequest request = new StringRequest(Request.Method.POST, filterTransactionStatementUrl, response -> {
                try {
                    runOnUiThread(dialogWaiting::dismiss);
                    JSONObject responseObject = new JSONObject(response);

                    String responseCode = responseObject.getString("responseCode");
                    if (responseCode.equals("1")) {
                        List<TransactionStatement> transactionStatementList = new ArrayList<>();
                        JSONArray responseArray = responseObject.getJSONArray("responseData");
                        for (int i = 0; i < responseArray.length(); i++) {
                            JSONObject responseArrayObject = responseArray.getJSONObject(i);
                            String id, userId, statement, date, amount;
                            id = responseArrayObject.getString("id");
                            userId = responseArrayObject.getString("user_id");
                            statement = responseArrayObject.getString("statement");
                            date = responseArrayObject.getString("date");
                            amount = responseArrayObject.getString("amount");

                            TransactionStatement transactionStatement = new TransactionStatement(id, userId, statement, date, amount);
                            transactionStatementList.add(transactionStatement);

                        }

                        runOnUiThread(() -> {
                            Collections.reverse(transactionStatementList);
                            recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
                            recyclerView.setAdapter(new TransactionStatementRecyclerViewAdapter(transactionStatementList));
                        });
                    } else {
                        String responseMessage = responseObject.getString("responseMessage");
                        runOnUiThread(() -> Toast.makeText(mContext, responseMessage, Toast.LENGTH_SHORT).show());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(dialogWaiting::dismiss);
                    runOnUiThread(() -> new Wrapper().errorToast("Sorry an internal error occurred please try again later\n" + e.getMessage() + response, mContext));
                    finish();
                }

            }, error -> runOnUiThread(() -> {
                new Wrapper().errorToast("Sorry failed to connect to server please check your internet connection and try again later\n" + error.getMessage(), mContext);
                dialogWaiting.dismiss();
                finish();
            })) {
                @Override
                protected Map<String, String> getParams() {
                    return params;
                }
            };
            Volley.newRequestQueue(mContext).add(request);
        };
        new Thread(fetchRideOrdersThread).start();
    }

    private void displayDepositDialog() {
        View root = LayoutInflater.from(mContext).inflate(R.layout.deposit_wallet_item, null);
        EditText edAmount = root.findViewById(R.id.edAmount);
        new androidx.appcompat.app.AlertDialog.Builder(mContext)
                .setView(root)
                .setPositiveButton("SEND", (dialog, which) -> {
                    dialog.dismiss();
                    String amount = edAmount.getText().toString().trim();
                    if (!amount.isEmpty()) {
                        depositToWallet(amount);
                    } else {
                        new Wrapper().errorToast("Enter amount!", mContext);
                    }

                })
                .setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void depositToWallet(String amount) {
        Calendar calendar = Calendar.getInstance();
        String[] months = getResources().getStringArray(R.array.months_shrt);
        String month = months[calendar.get(Calendar.MONTH)];
        String year = String.valueOf(calendar.get(Calendar.YEAR));
        String day = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
        String date=day+" "+month+" "+year;
        AlertDialog dialogWaiting = new Wrapper().waitingDialog("Please wait...", mContext);
        Runnable fetchRideOrdersThread = () -> {

            runOnUiThread(dialogWaiting::show);

            HashMap<String, String> params = new HashMap<>();
            params.put("user_id", authenticatedUniqueNumber);
            params.put("date", date);
            params.put("amount",amount);

            StringRequest request = new StringRequest(Request.Method.POST, depositToWalletUrl, response -> {
                try {
                    runOnUiThread(dialogWaiting::dismiss);
                    JSONObject responseObject = new JSONObject(response);

                    String responseCode = responseObject.getString("responseCode");
                    String responseMessage = responseObject.getString("responseMessage");
                    if (responseCode.equals("1")) {
                        runOnUiThread(() -> new Wrapper().successToast(responseMessage,mContext));
                        onStart();
                    } else {
                        runOnUiThread(() -> new Wrapper().errorToast(responseMessage,mContext));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(dialogWaiting::dismiss);
                    runOnUiThread(() -> new Wrapper().errorToast("Sorry an internal error occurred please try again later\n" + e.getMessage() + response, mContext));
                    finish();
                }

            }, error -> runOnUiThread(() -> {
                new Wrapper().errorToast("Sorry failed to connect to server please check your internet connection and try again later\n" + error.getMessage(), mContext);
                dialogWaiting.dismiss();
                finish();
            })) {
                @Override
                protected Map<String, String> getParams() {
                    return params;
                }
            };
            Volley.newRequestQueue(mContext).add(request);
        };
        new Thread(fetchRideOrdersThread).start();

    }


    @Override
    protected void onStart() {
        super.onStart();
        loadWallet();
    }

    private void loadWallet() {
        AlertDialog dialogWaiting = new Wrapper().waitingDialog("Please wait...", mContext);
        Runnable fetchWalletThread = () -> {
            runOnUiThread(dialogWaiting::show);
            HashMap<String, String> params = new HashMap<>();
            params.put("user_id", authenticatedUniqueNumber);
            StringRequest request = new StringRequest(Request.Method.POST, fetchWalletUrl, response -> {
                try {

                    JSONObject responseObject = new JSONObject(response);

                    String responseCode = responseObject.getString("responseCode");
                    String balance = responseObject.getString("balance");
                    runOnUiThread(() -> {
                        walletBalance.setText("Kes. "+balance);
                        dialogWaiting.dismiss();
                    });
                    if (responseCode.equals("1")) {
                        List<TransactionStatement> transactionStatementList = new ArrayList<>();
                        JSONArray responseArray = responseObject.getJSONArray("responseData");
                        for (int i = 0; i < responseArray.length(); i++) {
                            JSONObject responseArrayObject = responseArray.getJSONObject(i);
                            String id, userId, statement, date, amount;
                            id = responseArrayObject.getString("id");
                            userId = responseArrayObject.getString("user_id");
                            statement = responseArrayObject.getString("statement");
                            date = responseArrayObject.getString("date");
                            amount = responseArrayObject.getString("amount");

                            TransactionStatement transactionStatement = new TransactionStatement(id, userId, statement, date, amount);
                            transactionStatementList.add(transactionStatement);

                        }

                        runOnUiThread(() -> {
                            Collections.reverse(transactionStatementList);
                            recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
                            recyclerView.setAdapter(new TransactionStatementRecyclerViewAdapter(transactionStatementList));
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(dialogWaiting::dismiss);
                    runOnUiThread(() -> new Wrapper().errorToast("Sorry an internal error occurred please try again later\n" + e.getMessage() + response, mContext));
                    finish();
                }

            }, error -> runOnUiThread(() -> {
                new Wrapper().errorToast("Sorry failed to connect to server please check your internet connection and try again later\n" + error.getMessage(), mContext);
                dialogWaiting.dismiss();
                finish();
            })) {
                @Override
                protected Map<String, String> getParams() {
                    return params;
                }
            };
            Volley.newRequestQueue(mContext).add(request);
        };
        new Thread(fetchWalletThread).start();
    }
}