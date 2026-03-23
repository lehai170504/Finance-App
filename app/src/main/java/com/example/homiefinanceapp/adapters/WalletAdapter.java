package com.example.homiefinanceapp.adapters;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homiefinanceapp.R;
import com.example.homiefinanceapp.models.Wallet;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WalletAdapter extends RecyclerView.Adapter<WalletAdapter.WalletViewHolder> {
    private List<Wallet> walletList = new ArrayList<>();
    private final OnWalletClickListener listener;

    public interface OnWalletClickListener {
        void onEditClick(Wallet wallet);
        void onDeleteClick(Wallet wallet);
    }

    public WalletAdapter(OnWalletClickListener listener) {
        this.listener = listener;
    }

    public void setWallets(List<Wallet> wallets) {
        this.walletList = wallets;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WalletViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallet, parent, false);
        return new WalletViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WalletViewHolder holder, int position) {
        Wallet wallet = walletList.get(position);

        holder.tvWalletName.setText(wallet.getName());

        NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
        holder.tvWalletBalance.setText(format.format(wallet.getBalance()) + " đ");

        // Gán icon thông minh dựa trên tên ví
        String nameLower = wallet.getName().toLowerCase();
        if (nameLower.contains("momo")) {
            holder.ivWalletIcon.setImageResource(android.R.drawable.ic_menu_save); // Có thể thay bằng icon MoMo thật sau này
        } else if (nameLower.contains("tiền mặt") || nameLower.contains("cash")) {
            holder.ivWalletIcon.setImageResource(android.R.drawable.ic_menu_view);
        } else if (nameLower.contains("ngân hàng") || nameLower.contains("bank") || nameLower.contains("vcb") || nameLower.contains("bidv")) {
            holder.ivWalletIcon.setImageResource(android.R.drawable.ic_menu_agenda);
        } else {
            holder.ivWalletIcon.setImageResource(android.R.drawable.ic_menu_myplaces);
        }

        try {
            holder.layoutWalletBg.setBackgroundColor(Color.parseColor(wallet.getColor()));
        } catch (Exception e) {
            holder.layoutWalletBg.setBackgroundColor(Color.parseColor("#333333"));
        }

        holder.itemView.setOnLongClickListener(v -> {
            CharSequence[] options = new CharSequence[]{"✏️ Chỉnh sửa ví", "🗑 Xóa ví"};
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Tùy chọn: " + wallet.getName())
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            listener.onEditClick(wallet);
                        } else {
                            listener.onDeleteClick(wallet);
                        }
                    })
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return walletList != null ? walletList.size() : 0;
    }

    static class WalletViewHolder extends RecyclerView.ViewHolder {
        TextView tvWalletName, tvWalletBalance;
        ImageView ivWalletIcon;
        LinearLayout layoutWalletBg;

        public WalletViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWalletName = itemView.findViewById(R.id.tvWalletName);
            tvWalletBalance = itemView.findViewById(R.id.tvWalletBalance);
            ivWalletIcon = itemView.findViewById(R.id.ivWalletIcon);
            layoutWalletBg = itemView.findViewById(R.id.layoutWalletBg);
        }
    }
}