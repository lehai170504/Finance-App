package com.example.homiefinanceapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.data.PieDataSet.ValuePosition;
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
import com.example.homiefinanceapp.models.CategoryReportItem;
import com.example.homiefinanceapp.models.UserResponse;

import java.text.NumberFormat;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private String currentChartStartDate;
    private String currentChartEndDate;

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
        setupChartDateFilter();
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
        if (currentChartStartDate == null || currentChartEndDate == null) {
            setDefaultChartRangeForCurrentMonth();
        }
        fetchCategoryPieReport(currentChartStartDate, currentChartEndDate);
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

    private void setupChartDateFilter() {
        setDefaultChartRangeForCurrentMonth();
        binding.btnApplyPieFilter.setOnClickListener(v -> {
            String start = binding.etStartDate.getText() != null ? binding.etStartDate.getText().toString().trim() : "";
            String end = binding.etEndDate.getText() != null ? binding.etEndDate.getText().toString().trim() : "";
            if (!isValidDate(start) || !isValidDate(end)) {
                Toast.makeText(this, "Ngày không hợp lệ! Định dạng yyyy-MM-dd", Toast.LENGTH_SHORT).show();
                return;
            }
            currentChartStartDate = start;
            currentChartEndDate = end;
            fetchCategoryPieReport(start, end);
        });
    }

    private void setDefaultChartRangeForCurrentMonth() {
        YearMonth ym = YearMonth.now();
        currentChartStartDate = ym.atDay(1).toString();
        currentChartEndDate = ym.atEndOfMonth().toString();
        binding.etStartDate.setText(currentChartStartDate);
        binding.etEndDate.setText(currentChartEndDate);
    }

    private boolean isValidDate(String value) {
        return value != null && value.matches("^\\d{4}-\\d{2}-\\d{2}$");
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

    private void fetchCategoryPieReport(String startDate, String endDate) {
        SharedPreferences prefs = getSharedPreferences("HomiePrefs", MODE_PRIVATE);
        String token = prefs.getString("token", "");
        binding.tvPieRange.setText("Kỳ: " + startDate + " đến " + endDate);

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getCategoryReport("Bearer " + token, startDate, endDate)
                .enqueue(new Callback<ApiResponse<List<CategoryReportItem>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<CategoryReportItem>>> call, Response<ApiResponse<List<CategoryReportItem>>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            renderCategoryPieChart(response.body().getData());
                        } else {
                            showPieEmptyState(true);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<CategoryReportItem>>> call, Throwable t) {
                        showPieEmptyState(true);
                    }
                });
    }

    private void renderCategoryPieChart(List<CategoryReportItem> data) {
        if (binding == null) return;
        PieChart chart = binding.pieCategoryReport;
        List<PieEntry> entries = new ArrayList<>();

        if (data != null) {
            for (CategoryReportItem item : data) {
                if (item == null || item.getTotalAmount() == null || item.getTotalAmount() <= 0) continue;
                String label = item.getCategoryName() != null ? item.getCategoryName() : "Khác";
                entries.add(new PieEntry(item.getTotalAmount().floatValue(), label));
            }
        }

        if (entries.isEmpty()) {
            showPieEmptyState(true);
            chart.clear();
            return;
        }

        showPieEmptyState(false);
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{
                0xFF1E88E5, 0xFFE53935, 0xFF43A047, 0xFFFDD835, 0xFF8E24AA, 0xFFFB8C00, 0xFF00ACC1
        });
        dataSet.setSliceSpace(2f);
        dataSet.setValueTextColor(0xFF222222);
        dataSet.setValueTextSize(10f);
        dataSet.setValueLinePart1OffsetPercentage(78f);
        dataSet.setValueLinePart1Length(0.35f);
        dataSet.setValueLinePart2Length(0.45f);
        dataSet.setValueLineColor(0xFF9E9E9E);
        dataSet.setYValuePosition(ValuePosition.OUTSIDE_SLICE);
        dataSet.setXValuePosition(ValuePosition.OUTSIDE_SLICE);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getPieLabel(float value, PieEntry pieEntry) {
                NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
                return format.format(value) + " đ";
            }
        });

        chart.setUsePercentValues(false);
        chart.setData(pieData);
        chart.getDescription().setEnabled(false);
        // Dùng label ngoài lát cắt để tránh chồng chữ trên pie.
        chart.setDrawEntryLabels(false);
        chart.setCenterText("Tỷ lệ danh mục");
        chart.setCenterTextSize(13f);
        chart.setHoleRadius(45f);
        chart.setTransparentCircleRadius(50f);

        Legend legend = chart.getLegend();
        legend.setEnabled(true);
        legend.setTextSize(11f);
        legend.setWordWrapEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);

        chart.animateY(700);
        chart.invalidate();
    }

    private void showPieEmptyState(boolean show) {
        if (binding == null) return;
        binding.tvPieEmpty.setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private boolean isLoggedIn() {
        SharedPreferences prefs = getSharedPreferences("HomiePrefs", MODE_PRIVATE);
        return prefs.getString("token", null) != null;
    }
}