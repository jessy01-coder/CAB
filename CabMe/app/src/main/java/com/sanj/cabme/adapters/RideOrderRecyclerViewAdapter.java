package com.sanj.cabme.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.sanj.cabme.R;
import com.sanj.cabme.models.RideOrderModel;
import com.sanj.cabme.wrapper.Wrapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.sanj.cabme.data.URLs.sendOrderFeedbackUrl;
import static com.sanj.cabme.wrapper.Wrapper.driverNID;

public class RideOrderRecyclerViewAdapter extends RecyclerView.Adapter<RideOrderRecyclerViewAdapter.ViewHolder> {

    private final List<RideOrderModel> rideOrderModelList;
    private final Context mContext;
    private final Activity mActivity;
    private String reminderTime, reminderDate;

    public RideOrderRecyclerViewAdapter(List<RideOrderModel> rideOrderModelList, Context mContext, Activity mActivity) {
        this.rideOrderModelList = rideOrderModelList;
        this.mContext = mContext;
        this.mActivity = mActivity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.my_request_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RideOrderRecyclerViewAdapter.ViewHolder holder, int position) {
        RideOrderModel model = rideOrderModelList.get(position);
        String startLocation = model.getStartLocation();
        String destinationLocation = model.getDestinationLocation();

        String description = "FROM " + startLocation + " TO " + destinationLocation;
        holder.txtTime.setText(model.getPickupTime().equals("0") ? "N/A" : model.getPickupTime());
        holder.txtPhone.setText(model.getPhone());
        holder.txtDescription.setText(description);
        holder.btnDecline.setOnClickListener(v -> sendFeedback("decline", "0", "0", model.getOrderId(), "0"));
        holder.btnAccept.setOnClickListener(v -> displayForm(destinationLocation, model.getOrderId()));
    }

    private void sendFeedback(String feedback, String pickupTime, String fare, String mOrderId, String date) {
        AlertDialog dialogWaiting = new Wrapper().waitingDialog("Sending feedback", mContext);
        Runnable sendFeedbackThread = () -> {
            mActivity.runOnUiThread(dialogWaiting::show);
            HashMap<String, String> params = new HashMap<>();
            params.put("id", mOrderId);
            params.put("feedback", feedback);
            params.put("nid", driverNID);
            params.put("pickup_time", pickupTime);
            params.put("fare", fare);
            params.put("date", date);

            StringRequest request = new StringRequest(Request.Method.POST, sendOrderFeedbackUrl, response -> {
                try {
                    mActivity.runOnUiThread(dialogWaiting::dismiss);
                    JSONObject responseObject = new JSONObject(response);

                    String responseCode = responseObject.getString("responseCode");
                    String responseMessage = responseObject.getString("responseMessage");

                    if (responseCode.equals("1")) {
                        mActivity.finish();
                        mActivity.runOnUiThread(() -> new Wrapper().successToast(responseMessage, mContext));
                    } else {
                        mActivity.runOnUiThread(() -> new Wrapper().messageDialog(responseMessage, mContext));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    mActivity.runOnUiThread(dialogWaiting::dismiss);
                    mActivity.runOnUiThread(() -> new Wrapper().messageDialog("Sorry an internal error occurred please try again later\n" + e.getMessage() + response, mContext));
                }

            }, error -> mActivity.runOnUiThread(() -> {
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
        new Thread(sendFeedbackThread).start();
    }

    @Override
    public int getItemCount() {
        return rideOrderModelList.size();
    }

    @SuppressLint("SetTextI18n")
    private void displayForm(String destination, String mOrderId) {
        View root = LayoutInflater.from(mContext).inflate(R.layout.driver_pick_up_time, null);
        TextView txtPickupTime, txtPickupDate, btnSubmit;
        Button btnTimePicker, btnDatePicker;
        TextInputEditText edPrice;
        txtPickupTime = root.findViewById(R.id.txtPickupTime);
        btnTimePicker = root.findViewById(R.id.btnTimePicker);
        txtPickupDate = root.findViewById(R.id.txtPickupDate);
        btnDatePicker = root.findViewById(R.id.btnDatePicker);
        btnSubmit = root.findViewById(R.id.btnSubmit);
        edPrice = root.findViewById(R.id.edPrice);

        androidx.appcompat.app.AlertDialog dialog;
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(mContext);
        builder.setView(root);
        dialog = builder.create();
        dialog.show();
        final Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int min = calendar.get(Calendar.MINUTE);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int mon = calendar.get(Calendar.MONTH);
        int yr = calendar.get(Calendar.YEAR);
        btnTimePicker.setOnClickListener(v -> new TimePickerDialog(mContext, (view, hourOfDay, minute) -> {
            String minStr = minute < 10 ? "0" + minute : String.valueOf(minute);
            String hourOfDayStr = hourOfDay < 10 ? "0" + hourOfDay : String.valueOf(hourOfDay);
            txtPickupTime.setText(hourOfDayStr + minStr + "hrs");
            String reminderMin = (minute - 3) < 10 ? "0" + (minute - 3) : String.valueOf((minute - 3));
            reminderTime = hourOfDayStr + ":" + reminderMin;
        }, hour, min, true).show());
        btnDatePicker.setOnClickListener(v -> new DatePickerDialog(mContext, (view, year, month, dayOfMonth) -> {
            reminderDate = dayOfMonth + "-" + (month + 1) + "-" + year;
            txtPickupDate.setText(reminderDate);
        }, yr, mon, day).show());

        btnSubmit.setOnClickListener(v -> {
            if (!txtPickupTime.getText().toString().isEmpty() || !txtPickupDate.getText().toString().isEmpty() || !Objects.requireNonNull(edPrice.getText()).toString().isEmpty()) {
                dialog.dismiss();
                String remDate = reminderDate + " " + reminderTime;
                String description = "This is a reminder for your ride to " + destination + " scheduled to pick up the passenger at " + txtPickupTime.getText().toString();
                new Wrapper().setVaccinesReminder(mContext, remDate, description);
                sendFeedback("accept", txtPickupTime.getText().toString(), Objects.requireNonNull(edPrice.getText()).toString().trim(), mOrderId, reminderDate);
            } else {
                new Wrapper().errorToast("All fields required!!", mContext);
            }
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTime, txtPhone, txtDescription, btnAccept, btnDecline;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtPhone = itemView.findViewById(R.id.txtPhone);
            txtDescription = itemView.findViewById(R.id.txtDescription);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }
    }
}
