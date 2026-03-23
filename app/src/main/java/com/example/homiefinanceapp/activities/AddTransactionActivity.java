package com.example.homiefinanceapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.example.homiefinanceapp.api.ApiService;
import com.example.homiefinanceapp.api.RetrofitClient;
import com.example.homiefinanceapp.databinding.ActivityAddTransactionBinding;
import com.example.homiefinanceapp.models.ApiResponse;
import com.example.homiefinanceapp.models.Category;
import com.example.homiefinanceapp.models.Group;
import com.example.homiefinanceapp.models.TransactionCreateRequest;
import com.example.homiefinanceapp.models.TransactionCreateResponse;
import com.example.homiefinanceapp.models.Wallet;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddTransactionActivity extends AppCompatActivity {
    private ActivityAddTransactionBinding binding;

    private String token;

    private final List<Category> allCategories = new ArrayList<>();
    private final List<Wallet> allWallets = new ArrayList<>();
    private final List<Group> allGroups = new ArrayList<>();

    private String selectedCategoryId;
    private String selectedWalletId;
    private String selectedGroupId = null;

    private String getSelectedType() {
        // Radio mapping theo type backend
        return binding.rbExpense.isChecked() ? "EXPENSE" : "INCOME";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddTransactionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        token = getSharedPreferences("HomiePrefs", MODE_PRIVATE).getString("token", "");

        // Xử lý nút Back
        binding.btnBack.setOnClickListener(v -> finish());

        // Khi đổi loại (Chi/Thu) => reset category đã chọn
        binding.rgTransactionType.setOnCheckedChangeListener((group, checkedId) -> {
            selectedCategoryId = null;
            binding.tvSelectedCategory.setText("Chọn danh mục");
        });

        // Dropdown chọn category
        binding.layoutSelectCategory.setOnClickListener(v -> showCategoryDropdown());

        // Dropdown chọn wallet
        binding.layoutSelectWallet.setOnClickListener(v -> showWalletDropdown());

        // Dropdown chọn group (optional)
        binding.layoutSelectGroup.setOnClickListener(v -> showGroupDropdown());

        // Default date = today
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        binding.etDate.setText(today);
        binding.tvSelectedGroup.setText("Để trống (cá nhân)");

        // Nhấn Lưu Giao Dịch
        binding.btnSaveTransaction.setOnClickListener(v -> {
            submitTransaction();
        });
    }

    private void loadCategories() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getCategories("Bearer " + token).enqueue(new retrofit2.Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse<List<Category>>> call, retrofit2.Response<ApiResponse<List<Category>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    allCategories.clear();
                    allCategories.addAll(response.body().getData());
                } else {
                    Toast.makeText(AddTransactionActivity.this, "Không lấy được danh mục!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse<List<Category>>> call, Throwable t) {
                Log.e("API_ERROR", "loadCategories: " + t.getMessage());
                Toast.makeText(AddTransactionActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadWallets() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getWallets("Bearer " + token).enqueue(new retrofit2.Callback<ApiResponse<List<Wallet>>>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse<List<Wallet>>> call, retrofit2.Response<ApiResponse<List<Wallet>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    allWallets.clear();
                    allWallets.addAll(response.body().getData());
                } else {
                    Toast.makeText(AddTransactionActivity.this, "Không lấy được ví!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse<List<Wallet>>> call, Throwable t) {
                Log.e("API_ERROR", "loadWallets: " + t.getMessage());
                Toast.makeText(AddTransactionActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadGroups() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getMyGroups("Bearer " + token).enqueue(new retrofit2.Callback<ApiResponse<List<Group>>>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse<List<Group>>> call, retrofit2.Response<ApiResponse<List<Group>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    allGroups.clear();
                    allGroups.addAll(response.body().getData());
                } else {
                    Toast.makeText(AddTransactionActivity.this, "Không lấy được nhóm!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse<List<Group>>> call, Throwable t) {
                Log.e("API_ERROR", "loadGroups: " + t.getMessage());
                Toast.makeText(AddTransactionActivity.this, "Lỗi kết nối server!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCategoryDropdown() {
        String type = getSelectedType();
        List<Category> filtered = new ArrayList<>();
        for (Category c : allCategories) {
            if (c != null && c.getType() != null && c.getType().equalsIgnoreCase(type)) {
                filtered.add(c);
            }
        }

        if (filtered.isEmpty()) {
            Toast.makeText(this, "Chưa có danh mục cho loại: " + type, Toast.LENGTH_SHORT).show();
            return;
        }

        CharSequence[] items = new CharSequence[filtered.size()];
        for (int i = 0; i < filtered.size(); i++) {
            items[i] = filtered.get(i).getName();
        }

        new AlertDialog.Builder(this)
                .setTitle("Chọn danh mục (" + type + ")")
                .setItems(items, (dialog, which) -> {
                    Category selected = filtered.get(which);
                    selectedCategoryId = selected.getId();
                    binding.tvSelectedCategory.setText(selected.getName());
                })
                .show();
    }

    private void showWalletDropdown() {
        if (allWallets.isEmpty()) {
            Toast.makeText(this, "Chưa có ví. Vui lòng quay lại tạo ví trước.", Toast.LENGTH_SHORT).show();
            return;
        }

        CharSequence[] items = new CharSequence[allWallets.size()];
        for (int i = 0; i < allWallets.size(); i++) {
            items[i] = allWallets.get(i).getName();
        }

        new AlertDialog.Builder(this)
                .setTitle("Chọn ví")
                .setItems(items, (dialog, which) -> {
                    Wallet selected = allWallets.get(which);
                    selectedWalletId = selected.getId();
                    binding.tvSelectedWallet.setText(selected.getName());
                })
                .show();
    }

    private void showGroupDropdown() {
        // Nếu chưa load nhóm xong, vẫn cho chọn "không có"
        CharSequence[] items = new CharSequence[allGroups.size() + 1];
        items[0] = "Để trống (cá nhân)";
        for (int i = 0; i < allGroups.size(); i++) {
            items[i + 1] = allGroups.get(i).getName();
        }

        new AlertDialog.Builder(this)
                .setTitle("Chọn nhóm (optional)")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        selectedGroupId = null;
                        binding.tvSelectedGroup.setText("Để trống (cá nhân)");
                        return;
                    }
                    Group g = allGroups.get(which - 1);
                    selectedGroupId = g.getId();
                    binding.tvSelectedGroup.setText(g.getName());
                })
                .show();
    }

    private void submitTransaction() {
        String amountStr = binding.etAmount.getText().toString().trim();
        String note = binding.etNote.getText().toString().trim();
        String dateStr = binding.etDate.getText().toString().trim();

        if (amountStr.isEmpty()) {
                Toast.makeText(this, "Homie chưa nhập số tiền kìa!", Toast.LENGTH_SHORT).show();
                return;
            }

        if (selectedCategoryId == null || selectedCategoryId.trim().isEmpty()) {
            Toast.makeText(this, "Chưa chọn danh mục!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedWalletId == null || selectedWalletId.trim().isEmpty()) {
            Toast.makeText(this, "Chưa chọn ví!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (token == null || token.trim().isEmpty()) {
            Toast.makeText(this, "Chưa có token đăng nhập. Vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dateStr.isEmpty() || !dateStr.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            Toast.makeText(this, "Ngày không hợp lệ! Định dạng yyyy-MM-dd", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Double amount = Double.parseDouble(amountStr);
            String date = dateStr;

            TransactionCreateRequest request = new TransactionCreateRequest(amount, note, date);

            ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
            String groupIdQuery = (selectedGroupId == null || selectedGroupId.trim().isEmpty()) ? null : selectedGroupId;
            apiService.createTransaction(
                            "Bearer " + token,
                            selectedWalletId,
                            selectedCategoryId,
                            groupIdQuery, // groupId optional (null => Retrofit bỏ query)
                            request
                    )
                    .enqueue(new retrofit2.Callback<ApiResponse<TransactionCreateResponse>>() {
                        @Override
                        public void onResponse(retrofit2.Call<ApiResponse<TransactionCreateResponse>> call, retrofit2.Response<ApiResponse<TransactionCreateResponse>> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                                TransactionCreateResponse data = response.body().getData();
                                String id = data.getId() != null ? data.getId() : "";
                                Toast.makeText(AddTransactionActivity.this, "Đã tạo giao dịch thành công! ID: " + id, Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                String toastMsg = "Tạo giao dịch thất bại!";
                                try {
                                    if (response.errorBody() != null) {
                                        String err = response.errorBody().string();
                                        JSONObject obj = new JSONObject(err);
                                        String beMsg = obj.optString("message", "");
                                        if (beMsg != null && !beMsg.trim().isEmpty()) {
                                            toastMsg = beMsg;
                                        }
                                    }
                                } catch (Exception ignored) {}
                                Toast.makeText(AddTransactionActivity.this, toastMsg, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(retrofit2.Call<ApiResponse<TransactionCreateResponse>> call, Throwable t) {
                            Log.e("API_ERROR", "createTransaction: " + t.getMessage(), t);
                            Toast.makeText(AddTransactionActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
                        }
                    });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số tiền không hợp lệ!", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        loadCategories();
        loadWallets();
        loadGroups();
    }
}