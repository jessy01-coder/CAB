package com.sanj.cabme.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.sanj.cabme.R;
import com.sanj.cabme.models.CabModel;
import com.sanj.cabme.wrapper.Wrapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.sanj.cabme.data.URLs.makeRideOrderUrl;
import static com.sanj.cabme.wrapper.Wrapper.authenticatedUniqueNumber;

public class CabRecyclerViewAdapter extends RecyclerView.Adapter<CabRecyclerViewAdapter.ViewHolder> {

    private final List<CabModel> cabModelList;
    private final Context mContext;
    private final Activity mActivity;
    private final String startLocation;
    private String destinationLocation, pickupTime, passengerCount;

    public CabRecyclerViewAdapter(List<CabModel> cabModelList, Context mContext, Activity mActivity, String startLocation) {
        this.cabModelList = cabModelList;
        this.mContext = mContext;
        this.startLocation = startLocation;
        this.mActivity = mActivity;
    }

    @NonNull
    @Override
    public CabRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.cab_item, parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull CabRecyclerViewAdapter.ViewHolder holder, int position) {
        CabModel model = cabModelList.get(position);

        holder.driverName.setText(model.getName());
        holder.driverPhone.setText(model.getPhone());
        holder.driverLocation.setText(model.getLocation());
        holder.fromRoute.setText(model.getRouteFrom());
        holder.toRoute.setText(model.getRouteTo());
        holder.carPlateNumber.setText(model.getCarPlate());
        holder.carModel.setText(model.getCarModel());
        holder.txtPrice.setText("Kes "+model.getPrice());
        holder.btnOrderRide.setOnClickListener(v -> displayForm(model.getDriverNID()));
    }

    @SuppressLint({"InflateParams", "SetTextI18n"})
    private void displayForm(String mDriverNID) {
        View root = LayoutInflater.from(mContext).inflate(R.layout.order_ride_passenger_item, null);
        TextInputEditText edDestination, edPassengerCount;
        Button btnOrderRide;
        edDestination = root.findViewById(R.id.edDestination);
        btnOrderRide = root.findViewById(R.id.btnOrderRide);
        edPassengerCount = root.findViewById(R.id.edPassengerCount);

        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setView(root);
        dialog = builder.create();
        dialog.show();


        btnOrderRide.setOnClickListener(v -> {
            if (!(TextUtils.isEmpty(Objects.requireNonNull(edDestination.getText()).toString().trim()) || TextUtils.isEmpty(Objects.requireNonNull(edPassengerCount.getText()).toString().trim()))) {
                destinationLocation = edDestination.getText().toString().trim();
                passengerCount = edPassengerCount.getText().toString();
                dialog.dismiss();
                makeRideOrder(mDriverNID);
            } else {
                new Wrapper().errorToast("Incomplete form!!", mContext);
            }
        });

    }

    private void makeRideOrder(String mDriverNID) {
        String month,year;
        Calendar calendar=Calendar.getInstance();
        String[] months = mContext.getResources().getStringArray(R.array.months_shrt);
        month = months[calendar.get(Calendar.MONTH)];
        year = String.valueOf(calendar.get(Calendar.YEAR));
        android.app.AlertDialog dialogWaiting = new Wrapper().waitingDialog("Ordering a ride", mContext);
        Runnable registrationThread = () -> {

            mActivity.runOnUiThread(dialogWaiting::show);

            HashMap<String, String> params = new HashMap<>();
            params.put("start_location", startLocation);
            params.put("nid", mDriverNID);
            params.put("phone", authenticatedUniqueNumber);
            params.put("destination_location", destinationLocation);
            params.put("passenger_count", passengerCount);
            params.put("month", month);
            params.put("year", year);

            StringRequest request = new StringRequest(Request.Method.POST, makeRideOrderUrl, response -> {
                try {
                    JSONObject responseObject = new JSONObject(response);
                    String responseCode = responseObject.getString("responseCode");
                    String responseMessage = responseObject.getString("responseMessage");

                    if (responseCode.equals("1")) {
                        mActivity.runOnUiThread(() -> new Wrapper().successToast(responseMessage, mContext));
                    } else {
                        mActivity.runOnUiThread(() -> new Wrapper().messageDialog(responseMessage, mContext));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    mActivity.runOnUiThread(() -> {
                        dialogWaiting.dismiss();
                        new Wrapper().messageDialog("Sorry an internal error occurred please try again later\n" + e.getMessage() + response, mContext);
                    });
                }
                mActivity.runOnUiThread(dialogWaiting::dismiss);
            }, error -> mActivity.runOnUiThread(() -> {
                dialogWaiting.dismiss();
                new Wrapper().messageDialog("Sorry failed to connect to server please check your internet connection and try again later\n" + error.getMessage(), mContext);
            })) {
                @Override
                protected Map<String, String> getParams() {
                    return params;
                }
            };
            Volley.newRequestQueue(mContext).add(request);

        };
        new Thread(registrationThread).start();
    }

    @Override
    public int getItemCount() {
        return cabModelList.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView driverName, driverPhone, driverLocation, fromRoute, toRoute, carPlateNumber, carModel,txtPrice;
        Button btnOrderRide;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            driverName = itemView.findViewById(R.id.driverName);
            driverPhone = itemView.findViewById(R.id.driverPhone);
            driverLocation = itemView.findViewById(R.id.driverLocation);
            fromRoute = itemView.findViewById(R.id.fromRoute);
            toRoute = itemView.findViewById(R.id.toRoute);
            carPlateNumber = itemView.findViewById(R.id.carPlateNumber);
            carModel = itemView.findViewById(R.id.carModel);
            btnOrderRide = itemView.findViewById(R.id.btnOrderRide);
            txtPrice= itemView.findViewById(R.id.txtPrice);
        }
    }
}
