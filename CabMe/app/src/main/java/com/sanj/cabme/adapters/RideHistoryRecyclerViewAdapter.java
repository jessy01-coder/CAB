package com.sanj.cabme.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sanj.cabme.R;
import com.sanj.cabme.models.RideHistoryModel;

import java.util.List;

public class RideHistoryRecyclerViewAdapter extends RecyclerView.Adapter<RideHistoryRecyclerViewAdapter.ViewHolder> {
    private final List<RideHistoryModel> rideHistoryModelList;
    private final Context mContext;

    public RideHistoryRecyclerViewAdapter(List<RideHistoryModel> rideHistoryModelList, Context mContext) {
        this.rideHistoryModelList = rideHistoryModelList;
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.my_ride_history_item, parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RideHistoryRecyclerViewAdapter.ViewHolder holder, int position) {
        RideHistoryModel model = rideHistoryModelList.get(position);
        if (model.getDate().equals("0")){
            holder.txtDate.setText("N/A");
        }else{
            holder.txtDate.setText(model.getDate());
        }
        String arrivalTime=model.getArrivalTime().equals("0")? "N/A":model.getArrivalTime();
        holder.txtDescription.setText(model.getDescription());
        holder.txtDuration.setText(model.getDepartureTime().equals("0")? "N/A":model.getDepartureTime()+"-"+arrivalTime);
        holder.txtAmount.setText(model.getFare());
    }

    @Override
    public int getItemCount() {
        return rideHistoryModelList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtDate, txtDescription, txtDuration, txtAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtDescription = itemView.findViewById(R.id.txtDescription);
            txtDuration = itemView.findViewById(R.id.txtDuration);
            txtAmount = itemView.findViewById(R.id.txtAmount);
        }
    }
}
