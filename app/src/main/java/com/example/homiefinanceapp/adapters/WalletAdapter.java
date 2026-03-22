package com.example.homiefinanceapp.adapters;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private final OnWalletClickListener listener; // 💡 1. Thêm cái Listener

    // 💡 2. Interface để giao tiếp với Activity
    public interface OnWalletClickListener {
        void onEditClick(Wallet wallet);
        void onDeleteClick(Wallet wallet);
    }

    // Constructor yêu cầu phải truyền Listener vào
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

        try {
            holder.layoutWalletBg.setBackgroundColor(Color.parseColor(wallet.getColor()));
        } catch (Exception e) {
            holder.layoutWalletBg.setBackgroundColor(Color.parseColor("#333333"));
        }

        // 💡 3. Bắt sự kiện ẤN GIỮ (Long Click) vào cái ví
        holder.itemView.setOnLongClickListener(v -> {
            CharSequence[] options = new CharSequence[]{"✏️ Chỉnh sửa ví", "🗑 Xóa ví"};
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Tùy chọn: " + wallet.getName())
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            listener.onEditClick(wallet); // Gọi hàm Sửa
                        } else {
                            listener.onDeleteClick(wallet); // Gọi hàm Xóa
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
        LinearLayout layoutWalletBg;

        public WalletViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWalletName = itemView.findViewById(R.id.tvWalletName);
            tvWalletBalance = itemView.findViewById(R.id.tvWalletBalance);
            layoutWalletBg = itemView.findViewById(R.id.layoutWalletBg);
        }
    }
}