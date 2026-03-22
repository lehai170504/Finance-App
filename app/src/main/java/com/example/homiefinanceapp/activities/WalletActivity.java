package com.example.homiefinanceapp.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.homiefinanceapp.adapters.WalletAdapter;
import com.example.homiefinanceapp.api.ApiService;
import com.example.homiefinanceapp.api.RetrofitClient;
import com.example.homiefinanceapp.databinding.ActivityWalletBinding;
import com.example.homiefinanceapp.models.ApiResponse;
import com.example.homiefinanceapp.models.Wallet;
import com.example.homiefinanceapp.models.WalletRequest;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletActivity extends AppCompatActivity {
    private ActivityWalletBinding binding;
    private WalletAdapter walletAdapter;
    private String token;

    private List<Wallet> currentWallets = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWalletBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Lấy token từ bộ nhớ đệm
        SharedPreferences prefs = getSharedPreferences("HomiePrefs", MODE_PRIVATE);
        token = prefs.getString("token", "");

        // Khởi tạo RecyclerView
        setupRecyclerView();

        // Xử lý nút Back
        binding.btnBack.setOnClickListener(v -> finish());

        // Xử lý nút Thêm Ví
        binding.btnAddWallet.setOnClickListener(v -> {
            showAddWalletDialog();
        });

        // Xử lý nút Chuyển Tiền
        binding.btnTransferMoney.setOnClickListener(v -> {
            showTransferDialog();
        });

        // GỌI API LẤY DỮ LIỆU DANH SÁCH VÍ
        fetchWallets();
    }

    private void setupRecyclerView() {
        walletAdapter = new WalletAdapter(new WalletAdapter.OnWalletClickListener() {
            @Override
            public void onEditClick(Wallet wallet) {
                showEditWalletDialog(wallet);
            }

            @Override
            public void onDeleteClick(Wallet wallet) {
                new AlertDialog.Builder(WalletActivity.this)
                        .setTitle("Xóa ví")
                        .setMessage("Homie có chắc muốn xóa ví '" + wallet.getName() + "' không?")
                        .setPositiveButton("Xóa", (dialog, which) -> deleteWalletApi(wallet.getId()))
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });

        binding.rvWallets.setLayoutManager(new LinearLayoutManager(this));
        binding.rvWallets.setAdapter(walletAdapter);
    }

    // ==========================================================
    // DIALOG THÊM VÍ MỚI
    // ==========================================================
    private void showAddWalletDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText etName = new EditText(this);
        etName.setHint("Tên ví (Ví dụ: MoMo, Tiền mặt)");
        layout.addView(etName);

        EditText etBalance = new EditText(this);
        etBalance.setHint("Số dư ban đầu (VNĐ)");
        etBalance.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etBalance);

        EditText etColor = new EditText(this);
        etColor.setHint("Mã màu Hex (Ví dụ: #E91E63)");
        etColor.setText("#AE2070");
        layout.addView(etColor);

        new AlertDialog.Builder(this)
                .setTitle("Thêm Ví Mới")
                .setView(layout)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String balanceStr = etBalance.getText().toString().trim();
                    String color = etColor.getText().toString().trim();

                    if (name.isEmpty() || balanceStr.isEmpty() || color.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        Double balance = Double.parseDouble(balanceStr);
                        createNewWallet(name, balance, color);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số dư không hợp lệ!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ==========================================================
    // DIALOG SỬA VÍ
    // ==========================================================
    private void showEditWalletDialog(Wallet wallet) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText etName = new EditText(this);
        etName.setText(wallet.getName());
        layout.addView(etName);

        EditText etBalance = new EditText(this);
        etBalance.setText(String.valueOf(wallet.getBalance()));
        etBalance.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etBalance);

        EditText etColor = new EditText(this);
        etColor.setText(wallet.getColor());
        layout.addView(etColor);

        new AlertDialog.Builder(this)
                .setTitle("Chỉnh sửa Ví")
                .setView(layout)
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String balanceStr = etBalance.getText().toString().trim();
                    String color = etColor.getText().toString().trim();

                    if (name.isEmpty() || balanceStr.isEmpty() || color.isEmpty()) {
                        Toast.makeText(this, "Không được để trống nha homie!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        Double balance = Double.parseDouble(balanceStr);
                        updateWalletApi(wallet.getId(), name, balance, color);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số dư không hợp lệ!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ==========================================================
    // DIALOG CHUYỂN TIỀN
    // ==========================================================
    private void showTransferDialog() {
        if (currentWallets == null || currentWallets.size() < 2) {
            Toast.makeText(this, "Phải có ít nhất 2 ví mới chuyển tiền được homie ơi!", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        List<String> walletNames = new ArrayList<>();
        for (Wallet w : currentWallets) {
            walletNames.add(w.getName() + " (" + w.getBalance() + "đ)");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, walletNames);

        android.widget.TextView tvFrom = new android.widget.TextView(this);
        tvFrom.setText("Từ ví:");
        layout.addView(tvFrom);
        Spinner spFrom = new Spinner(this);
        spFrom.setAdapter(adapter);
        layout.addView(spFrom);

        android.widget.TextView tvTo = new android.widget.TextView(this);
        tvTo.setText("\nĐến ví:");
        layout.addView(tvTo);
        Spinner spTo = new Spinner(this);
        spTo.setAdapter(adapter);
        spTo.setSelection(1);
        layout.addView(spTo);

        android.widget.TextView tvAmount = new android.widget.TextView(this);
        tvAmount.setText("\nSố tiền cần chuyển:");
        layout.addView(tvAmount);
        EditText etAmount = new EditText(this);
        etAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etAmount);

        new AlertDialog.Builder(this)
                .setTitle("Chuyển tiền nội bộ")
                .setView(layout)
                .setPositiveButton("Chuyển ngay", (dialog, which) -> {
                    int fromIndex = spFrom.getSelectedItemPosition();
                    int toIndex = spTo.getSelectedItemPosition();
                    String amountStr = etAmount.getText().toString();

                    if (fromIndex == toIndex) {
                        Toast.makeText(this, "Trùng ví rồi, chọn ví khác đi!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (amountStr.isEmpty()) {
                        Toast.makeText(this, "Nhập số tiền vô!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String fromId = currentWallets.get(fromIndex).getId();
                    String toId = currentWallets.get(toIndex).getId();
                    try {
                        Double amount = Double.parseDouble(amountStr);
                        transferMoneyApi(fromId, toId, amount);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số tiền không hợp lệ!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ==========================================================
    // CÁC HÀM GỌI API
    // ==========================================================

    private void fetchWallets() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getWallets("Bearer " + token).enqueue(new Callback<ApiResponse<List<Wallet>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Wallet>>> call, Response<ApiResponse<List<Wallet>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentWallets = response.body().getData(); // Lưu trữ data nội bộ

                    if (currentWallets != null && !currentWallets.isEmpty()) {
                        walletAdapter.setWallets(currentWallets);
                    } else {
                        Toast.makeText(WalletActivity.this, "Homie chưa có ví nào. Thêm ngay nhé!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(WalletActivity.this, "Lỗi lấy dữ liệu ví!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Wallet>>> call, Throwable t) {
                Log.e("API_ERROR", "Lỗi mạng: " + t.getMessage());
                Toast.makeText(WalletActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createNewWallet(String name, Double balance, String color) {
        WalletRequest request = new WalletRequest(name, balance, color);
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

        apiService.createWallet("Bearer " + token, request).enqueue(new Callback<ApiResponse<Wallet>>() {
            @Override
            public void onResponse(Call<ApiResponse<Wallet>> call, Response<ApiResponse<Wallet>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(WalletActivity.this, "Tạo ví thành công! 🎉", Toast.LENGTH_SHORT).show();
                    fetchWallets();
                } else {
                    Toast.makeText(WalletActivity.this, "Lỗi tạo ví!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Wallet>> call, Throwable t) {
                Toast.makeText(WalletActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateWalletApi(String walletId, String name, Double balance, String color) {
        WalletRequest request = new WalletRequest(name, balance, color);
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

        apiService.updateWallet("Bearer " + token, walletId, request).enqueue(new Callback<ApiResponse<Wallet>>() {
            @Override
            public void onResponse(Call<ApiResponse<Wallet>> call, Response<ApiResponse<Wallet>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(WalletActivity.this, "Đã cập nhật ví! ✌️", Toast.LENGTH_SHORT).show();
                    fetchWallets();
                } else {
                    Toast.makeText(WalletActivity.this, "Lỗi cập nhật ví!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Wallet>> call, Throwable t) {
                Toast.makeText(WalletActivity.this, "Lỗi mạng!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void deleteWalletApi(String walletId) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.deleteWallet("Bearer " + token, walletId).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(WalletActivity.this, "Đã xóa ví!", Toast.LENGTH_SHORT).show();
                    fetchWallets();
                } else {
                    Toast.makeText(WalletActivity.this, "Không thể xóa! Ví vẫn còn tiền phải không homie?", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Toast.makeText(WalletActivity.this, "Lỗi mạng!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void transferMoneyApi(String fromId, String toId, Double amount) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.transferMoney("Bearer " + token, fromId, toId, amount).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(WalletActivity.this, "Chuyển tiền cái vèo! 💸", Toast.LENGTH_SHORT).show();
                    fetchWallets(); // Load lại tiền
                } else {
                    Toast.makeText(WalletActivity.this, "Lỗi rồi! Kiểm tra xem ví nguồn đủ tiền không nhé?", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Toast.makeText(WalletActivity.this, "Lỗi mạng!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}