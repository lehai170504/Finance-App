package com.example.homiefinanceapp.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.homiefinanceapp.adapters.CategoryAdapter;
import com.example.homiefinanceapp.api.ApiService;
import com.example.homiefinanceapp.api.RetrofitClient;
import com.example.homiefinanceapp.databinding.ActivityCategoryBinding;
import com.example.homiefinanceapp.models.ApiResponse;
import com.example.homiefinanceapp.models.Category;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryActivity extends AppCompatActivity {
    private ActivityCategoryBinding binding;
    private CategoryAdapter categoryAdapter;
    private String token;

    // Lưu lại toàn bộ cục data API trả về
    private List<Category> allCategories = new ArrayList<>();

    // Mặc định vô là xem CHI TIÊU
    private String currentType = "EXPENSE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        token = getSharedPreferences("HomiePrefs", MODE_PRIVATE).getString("token", "");

        binding.btnBack.setOnClickListener(v -> finish());

        setupRecyclerView();
        setupTabLayout();

        // 💡 CHỈ CÒN DUY NHẤT LỆNH LẤY DỮ LIỆU
        fetchCategories();
    }

    private void setupRecyclerView() {
        categoryAdapter = new CategoryAdapter();
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCategories.setAdapter(categoryAdapter);
    }

    // 💡 Xử lý logic Tab Thu/Chi
    private void setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    currentType = "EXPENSE";
                    binding.tabLayout.setSelectedTabIndicatorColor(android.graphics.Color.parseColor("#F44336")); // Đỏ
                } else {
                    currentType = "INCOME";
                    binding.tabLayout.setSelectedTabIndicatorColor(android.graphics.Color.parseColor("#4CAF50")); // Xanh
                }
                filterList(); // Cập nhật lại danh sách ngay lập tức
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    // 💡 Lọc danh sách theo Tab đang chọn
    private void filterList() {
        List<Category> filteredList = new ArrayList<>();
        for (Category c : allCategories) {
            if (c.getType().equals(currentType)) {
                filteredList.add(c);
            }
        }
        categoryAdapter.setCategories(filteredList);
    }

    // ================== GỌI API GET CATEGORY ==================
    private void fetchCategories() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getCategories("Bearer " + token).enqueue(new Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Category>>> call, Response<ApiResponse<List<Category>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allCategories = response.body().getData();
                    filterList(); // Load xong thì lọc theo Tab hiện tại (Mặc định là EXPENSE)
                } else {
                    Toast.makeText(CategoryActivity.this, "Không lấy được danh mục!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Category>>> call, Throwable t) {
                Log.e("API_ERROR", "Lỗi mạng: " + t.getMessage());
                Toast.makeText(CategoryActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}