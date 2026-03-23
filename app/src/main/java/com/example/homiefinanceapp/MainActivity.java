package com.example.homiefinanceapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.homiefinanceapp.activities.AddTransactionActivity;
import com.example.homiefinanceapp.activities.CategoryActivity;
import com.example.homiefinanceapp.activities.GroupActivity;
import com.example.homiefinanceapp.activities.LoginActivity;
import com.example.homiefinanceapp.activities.ProfileActivity;
import com.example.homiefinanceapp.activities.TrashTransactionsActivity;
import com.example.homiefinanceapp.activities.WalletActivity; // 💡 Nhớ import cái này
import com.example.homiefinanceapp.api.ApiService;
import com.example.homiefinanceapp.api.RetrofitClient;
import com.example.homiefinanceapp.databinding.ActivityMainBinding;
import com.example.homiefinanceapp.models.ApiResponse;
import com.example.homiefinanceapp.models.UserResponse;

import java.text.NumberFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. 🛡 KIỂM TRA ĐĂNG NHẬP TRƯỚC
        if (!isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // 2. CHỈ KHỞI TẠO KHI ĐÃ LOGGED IN
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupDashboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null && binding.bottomNav.getSelectedItemId() != R.id.nav_home) {
            binding.bottomNav.setSelectedItemId(R.id.nav_home);
        }
        fetchUserInfo();
        fetchTotalBalance();
        fetchTotalIncome();
        fetchTotalExpense();
    }

    private void setupDashboard() {
        updateUserHeader();

        // 💡 BẤM VÀO SỐ DƯ -> MỞ MÀN HÌNH QUẢN LÝ VÍ
        binding.tvBalance.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, WalletActivity.class));
        });

        // Gán sự kiện mở Profile
        binding.layoutUserHeader.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });

        // 1. FAB: Thêm giao dịch
        binding.btnAddTransaction.setOnClickListener(v -> {
            startActivity(new Intent(this, AddTransactionActivity.class));
        });

        // 2. Avatar: Click vào để sang Profile cho nhanh
        binding.ivUserAvatar.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        // 3. Bottom Navigation: Điều hướng
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                // Đang ở home rồi nên không làm gì hoặc refresh data
                fetchUserInfo();
                return true;
            } else if (id == R.id.nav_category) {
                startActivity(new Intent(this, CategoryActivity.class));
                return true;
            } else if (id == R.id.nav_group) {
                startActivity(new Intent(this, GroupActivity.class));
                return true;
            } else if (id == R.id.nav_trash) {
                startActivity(new Intent(this, TrashTransactionsActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        // Đặt mục mặc định là Home
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
    }

    private void updateUserHeader() {
        SharedPreferences prefs = getSharedPreferences("HomiePrefs", MODE_PRIVATE);
        String name = prefs.getString("user_name", "Homie");
        if (binding != null) {
            binding.tvWelcome.setText("Chào " + name + "! 👋");
        }
    }

    // ==========================================================
    // CÁC HÀM GỌI API
    // ==========================================================

    // 1. Lấy Tên người dùng và ID chuẩn xác từ Server
    private void fetchUserInfo() {
        SharedPreferences prefs = getSharedPreferences("HomiePrefs", MODE_PRIVATE);
        String token = prefs.getString("token", "");

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getMe("Bearer " + token).enqueue(new Callback<ApiResponse<UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserResponse>> call, Response<ApiResponse<UserResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Lấy tên
                    String realUsername = response.body().getData().getUsername();
                    binding.tvWelcome.setText("Chào " + realUsername + "! 👋");

                    // 💡 LẤY VÀ LƯU USER_ID VÀO BỘ NHỚ (Khúc này quan trọng nè)
                    String realUserId = response.body().getData().getId();

                    prefs.edit()
                            .putString("user_name", realUsername)
                            .putString("user_id", realUserId) // Lưu ID vào đây!
                            .apply();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {}
        });
    }

    // 2. Lấy Tổng Số Dư từ Server
    private void fetchTotalBalance() {
        SharedPreferences prefs = getSharedPreferences("HomiePrefs", MODE_PRIVATE);
        String token = prefs.getString("token", "");

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getTotalBalance("Bearer " + token).enqueue(new Callback<ApiResponse<Double>>() {
            @Override
            public void onResponse(Call<ApiResponse<Double>> call, Response<ApiResponse<Double>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Double total = response.body().getData();
                    if (total == null) total = 0.0;

                    // Chuyển số thành định dạng tiền tệ (VD: 1.500.000 đ)
                    NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
                    binding.tvBalance.setText(format.format(total) + " đ");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Double>> call, Throwable t) {
                binding.tvBalance.setText("Lỗi tải dữ liệu");
            }
        });
    }

    private void fetchTotalIncome() {
        SharedPreferences prefs = getSharedPreferences("HomiePrefs", MODE_PRIVATE);
        String token = prefs.getString("token", "");

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getTotalIncome("Bearer " + token).enqueue(new Callback<ApiResponse<Double>>() {
            @Override
            public void onResponse(Call<ApiResponse<Double>> call, Response<ApiResponse<Double>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Double totalIncome = response.body().getData();
                    if (totalIncome == null) totalIncome = 0.0;

                    NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
                    binding.tvIncomeTotal.setText(format.format(totalIncome) + " đ");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Double>> call, Throwable t) {
                binding.tvIncomeTotal.setText("Lỗi tải dữ liệu");
            }
        });
    }

    private void fetchTotalExpense() {
        SharedPreferences prefs = getSharedPreferences("HomiePrefs", MODE_PRIVATE);
        String token = prefs.getString("token", "");

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getTotalExpense("Bearer " + token).enqueue(new Callback<ApiResponse<Double>>() {
            @Override
            public void onResponse(Call<ApiResponse<Double>> call, Response<ApiResponse<Double>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Double totalExpense = response.body().getData();
                    if (totalExpense == null) totalExpense = 0.0;

                    NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
                    binding.tvExpenseTotal.setText(format.format(totalExpense) + " đ");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Double>> call, Throwable t) {
                binding.tvExpenseTotal.setText("Lỗi tải dữ liệu");
            }
        });
    }

    private boolean isLoggedIn() {
        SharedPreferences prefs = getSharedPreferences("HomiePrefs", MODE_PRIVATE);
        return prefs.getString("token", null) != null;
    }
}