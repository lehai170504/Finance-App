package com.example.homiefinanceapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Color;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homiefinanceapp.R;
import com.example.homiefinanceapp.activities.ReceiptPreviewActivity;
import com.example.homiefinanceapp.models.TransactionListItem;
import com.bumptech.glide.Glide;

import java.text.DecimalFormat;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
    private final List<TransactionListItem> transactions = new ArrayList<>();
    private final OnTransactionLongPressListener longPressListener;

    public interface OnTransactionLongPressListener {
        void onAddReceipt(TransactionListItem item);
    }

    public TransactionAdapter(OnTransactionLongPressListener longPressListener) {
        this.longPressListener = longPressListener;
    }

    public TransactionAdapter() {
        this.longPressListener = null;
    }

    public void setTransactions(List<TransactionListItem> list) {
        transactions.clear();
        if (list != null) transactions.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        TransactionListItem item = transactions.get(position);
        String type = item.getCategoryType() != null ? item.getCategoryType() : "";
        boolean isExpense = type.equalsIgnoreCase("EXPENSE");
        boolean isIncome = type.equalsIgnoreCase("INCOME");

        int badgeBg;
        int badgeText;
        int amountText;
        int stroke;
        if (isExpense) {
            badgeBg = Color.parseColor("#FFF1F1");
            badgeText = Color.parseColor("#B71C1C");
            amountText = Color.parseColor("#B71C1C");
            stroke = Color.parseColor("#FFCDD2");
        } else if (isIncome) {
            badgeBg = Color.parseColor("#E8F5E9");
            badgeText = Color.parseColor("#1B5E20");
            amountText = Color.parseColor("#1B5E20");
            stroke = Color.parseColor("#C8E6C9");
        } else {
            badgeBg = Color.parseColor("#F1F1F1");
            badgeText = Color.parseColor("#616161");
            amountText = Color.parseColor("#1A1A1A");
            stroke = Color.parseColor("#E6E6E6");
        }

        holder.tvTypeBadge.setText(isExpense ? "CHI" : (isIncome ? "THU" : "GD"));
        holder.tvTypeBadge.setBackgroundColor(badgeBg);
        holder.tvTypeBadge.setTextColor(badgeText);
        holder.cardRoot.setStrokeColor(stroke);

        String note = item.getNote() != null ? item.getNote() : "";
        holder.tvNote.setText(note);

        holder.tvCategoryName.setText(item.getCategoryName() != null ? item.getCategoryName() : "");
        String receiptUrl = item.getReceiptUrl() != null ? item.getReceiptUrl().trim() : "";
        if (!receiptUrl.isEmpty()) {
            holder.tvReceipt.setText("Hoa don: Nhan vao de xem chi tiet");
            holder.tvReceipt.setTextColor(Color.parseColor("#1565C0"));
            holder.ivReceipt.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(receiptUrl)
                    .centerCrop()
                    .into(holder.ivReceipt);
            View.OnClickListener openPreview = v -> v.getContext().startActivity(
                    ReceiptPreviewActivity.newIntent(v.getContext(), receiptUrl)
            );
            holder.tvReceipt.setOnClickListener(openPreview);
            holder.ivReceipt.setOnClickListener(openPreview);
        } else {
            holder.tvReceipt.setText("Hoa don: Chua co");
            holder.tvReceipt.setTextColor(Color.parseColor("#9E9E9E"));
            holder.tvReceipt.setOnClickListener(null);
            holder.ivReceipt.setOnClickListener(null);
            holder.ivReceipt.setVisibility(View.GONE);
        }
        holder.tvDate.setText(item.getDate() != null ? item.getDate() : "");
        holder.tvWallet.setText(item.getWalletName() != null ? item.getWalletName() : "");

        Double amount = item.getAmount();
        String formattedAmount = formatAmount(amount);
        String sign = isExpense ? "- " : (isIncome ? "+ " : "");
        holder.tvAmount.setText(sign + formattedAmount);
        holder.tvAmount.setTextColor(amountText);

        holder.itemView.setOnLongClickListener(v -> {
            if (longPressListener != null) {
                longPressListener.onAddReceipt(item);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        com.google.android.material.card.MaterialCardView cardRoot;
        TextView tvTypeBadge;
        TextView tvAmount;
        TextView tvNote;
        TextView tvCategoryName;
        TextView tvReceipt;
        ImageView ivReceipt;
        TextView tvDate;
        TextView tvWallet;

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardTransaction);
            tvTypeBadge = itemView.findViewById(R.id.tvTransactionTypeBadge);
            tvAmount = itemView.findViewById(R.id.tvTransactionAmount);
            tvNote = itemView.findViewById(R.id.tvTransactionNote);
            tvCategoryName = itemView.findViewById(R.id.tvTransactionCategoryName);
            tvReceipt = itemView.findViewById(R.id.tvTransactionReceipt);
            ivReceipt = itemView.findViewById(R.id.ivTransactionReceipt);
            tvDate = itemView.findViewById(R.id.tvTransactionDate);
            tvWallet = itemView.findViewById(R.id.tvTransactionWallet);
        }
    }

    private String formatAmount(Double amount) {
        if (amount == null) return "0";
        long whole = Math.round(amount);
        if (Math.abs(amount - whole) < 0.000001) {
            return String.valueOf(whole);
        }
        DecimalFormat df = new DecimalFormat("#,###.##");
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(amount);
    }
}

