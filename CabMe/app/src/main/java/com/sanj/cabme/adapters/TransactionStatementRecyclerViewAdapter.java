package com.sanj.cabme.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sanj.cabme.R;
import com.sanj.cabme.models.TransactionStatement;

import java.util.List;

public class TransactionStatementRecyclerViewAdapter extends RecyclerView.Adapter<TransactionStatementRecyclerViewAdapter.ViewHolder> {
    private final List<TransactionStatement> transactionStatementList;

    public TransactionStatementRecyclerViewAdapter(List<TransactionStatement> transactionStatementList) {
        this.transactionStatementList = transactionStatementList;
    }

    @NonNull
    @Override
    public TransactionStatementRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.transaction_item,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionStatementRecyclerViewAdapter.ViewHolder holder, int position) {
        TransactionStatement statement=transactionStatementList.get(position);
        holder.txtDate.setText(statement.getDate());
        holder.txtStatement.setText(statement.getStatement());
        holder.txtAmount.setText("Kes. "+statement.getAmount());
    }

    @Override
    public int getItemCount() {
        return transactionStatementList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtStatement,txtAmount,txtDate;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtAmount=itemView.findViewById(R.id.txtAmount);
            txtStatement=itemView.findViewById(R.id.txtStatement);
            txtDate=itemView.findViewById(R.id.txtDate);
        }
    }
}
