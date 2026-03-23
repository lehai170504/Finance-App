package com.example.homiefinanceapp.activities;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.card.MaterialCardView;

import com.example.homiefinanceapp.adapters.CategoryAdapter;
import com.example.homiefinanceapp.api.ApiService;
import com.example.homiefinanceapp.api.RetrofitClient;
import com.example.homiefinanceapp.databinding.ActivityCategoryBinding;
import com.example.homiefinanceapp.models.ApiResponse;
import com.example.homiefinanceapp.models.Category;
import com.google.android.material.tabs.TabLayout;
import com.example.homiefinanceapp.R;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    // Khi project không có drawable icon "ic_..." thực tế,
    // ta map sang các drawable có sẵn của Android để vẫn hiển thị icon cho UI.
    private int getFallbackAndroidIconRes(String iconName) {
        if (iconName == null) return 0;
        switch (iconName.toLowerCase()) {
            case "ic_fastfood":
                return android.R.drawable.ic_menu_crop; // biểu tượng thực đơn (gần giống)
            case "ic_transport":
                return android.R.drawable.ic_menu_compass;
            case "ic_shopping":
                return android.R.drawable.ic_menu_slideshow;
            case "ic_health":
                return android.R.drawable.ic_menu_edit;
            case "ic_education":
                return android.R.drawable.ic_menu_agenda;
            case "ic_entertainment":
                return android.R.drawable.ic_media_play;
            case "ic_rent":
                return android.R.drawable.ic_menu_myplaces;
            case "ic_bills":
                return android.R.drawable.ic_menu_manage;
            case "ic_default":
            default:
                return android.R.drawable.ic_menu_help;
        }
    }

    // Lấy danh sách icon trong project (các drawable bắt đầu bằng "ic_")
    private List<String> getAvailableIconNames() {
        try {
            Field[] fields = R.drawable.class.getFields();
            List<String> names = new ArrayList<>();
            for (Field f : fields) {
                String name = f.getName(); // ví dụ: ic_fastfood
                if (name == null) continue;
                if (name.startsWith("ic_") && !name.startsWith("ic_launcher")) {
                    names.add(name);
                }
            }
            Collections.sort(names);
            return names;
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private void updateIconStrokes(GridLayout grid, String selectedIcon) {
        for (int i = 0; i < grid.getChildCount(); i++) {
            View child = grid.getChildAt(i);
            if (!(child instanceof MaterialCardView)) continue;

            MaterialCardView card = (MaterialCardView) child;
            Object tag = card.getTag();
            if (!(tag instanceof String)) continue;
            String iconName = (String) tag;

            boolean isSelected = selectedIcon != null && selectedIcon.equalsIgnoreCase(iconName);
            card.setStrokeWidth(isSelected ? dp(2) : 0);
            card.setStrokeColor(isSelected ? Color.BLACK : Color.TRANSPARENT);
            card.setCardElevation(isSelected ? dp(2) : 0);
        }
    }

    private View buildIconPicker(String initialIcon, final String[] selectedIconHolder) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);

        TextView tvSelected = new TextView(this);
        tvSelected.setTextSize(14f);
        wrapper.addView(tvSelected);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        wrapper.addView(grid);

        List<String> icons = getAvailableIconNames();
        if (icons.isEmpty()) {
            // Fallback: project hiện chưa có file drawable icon thực tế (ic_...),
            // nên vẫn hiển thị list theo tên để người dùng chọn đúng string "icon".
            icons = Arrays.asList(
                    "ic_default",
                    "ic_fastfood",
                    "ic_transport",
                    "ic_shopping",
                    "ic_health",
                    "ic_education",
                    "ic_entertainment",
                    "ic_rent",
                    "ic_bills"
            );
        }

        String normalized = icons.get(0);
        if (initialIcon != null) {
            for (String c : icons) {
                if (c.equalsIgnoreCase(initialIcon)) {
                    normalized = c;
                    break;
                }
            }
        }

        selectedIconHolder[0] = normalized;
        tvSelected.setText("Icon: " + normalized);

        for (String iconName : icons) {
            MaterialCardView card = new MaterialCardView(this);
            card.setTag(iconName);
            card.setCardBackgroundColor(Color.parseColor("#F2F2F2"));

            int resId = getResources().getIdentifier(iconName, "drawable", getPackageName());
            // Luôn hiển thị thứ gì đó để thấy danh sách icon
            if (resId != 0) {
                ImageView iv = new ImageView(this);
                iv.setImageResource(resId);
                iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                iv.setLayoutParams(new ViewGroup.LayoutParams(dp(28), dp(28)));
                int pad = dp(6);
                iv.setPadding(pad, pad, pad, pad);
                card.addView(iv);
            } else {
                int fallbackResId = getFallbackAndroidIconRes(iconName);
                if (fallbackResId != 0) {
                    ImageView iv = new ImageView(this);
                    iv.setImageResource(fallbackResId);
                    iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    iv.setLayoutParams(new ViewGroup.LayoutParams(dp(28), dp(28)));
                    int pad = dp(6);
                    iv.setPadding(pad, pad, pad, pad);
                    card.addView(iv);
                } else {
                    TextView tv = new TextView(this);
                    tv.setText(iconName);
                    tv.setTextSize(10f);
                    tv.setTextColor(Color.DKGRAY);
                    tv.setGravity(android.view.Gravity.CENTER);
                    tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    card.addView(tv);
                }
            }

            card.setRadius(dp(12));
            card.setUseCompatPadding(false);
            card.setStrokeWidth(0);
            card.setStrokeColor(Color.TRANSPARENT);
            card.setCardElevation(0);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = dp(52);
            lp.height = dp(52);
            lp.setMargins(dp(4), dp(6), dp(4), dp(6));
            card.setLayoutParams(lp);

            card.setOnClickListener(v -> {
                selectedIconHolder[0] = iconName;
                tvSelected.setText("Icon: " + iconName);
                updateIconStrokes(grid, iconName);
            });

            grid.addView(card);
        }

        updateIconStrokes(grid, selectedIconHolder[0]);
        return wrapper;
    }

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
        categoryAdapter = new CategoryAdapter(category -> {
            // Mở màn hình hiển thị giao dịch riêng cho category đã chọn
            startActivity(CategoryTransactionsActivity.newIntent(this, category));
        });
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
                // Tab đổi chỉ cần lọc lại list category.
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
                    if (allCategories != null) {
                        for (Category c : allCategories) {
                            Log.d("CATEGORIES_ICON", "name=" + c.getName() + ", type=" + c.getType() + ", icon=" + c.getIcon());
                        }
                    }
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

    private void showAddCategoryDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText etName = new EditText(this);
        etName.setHint("Tên danh mục (ví dụ: Ăn uống)");
        layout.addView(etName);

        // Chọn loại: EXPENSE/INCOME
        TextView tvType = new TextView(this);
        tvType.setText("Loại");
        tvType.setTextSize(14f);
        layout.addView(tvType);

        RadioGroup rgType = new RadioGroup(this);
        rgType.setOrientation(RadioGroup.HORIZONTAL);

        RadioButton rbExpense = new RadioButton(this);
        rbExpense.setText("EXPENSE");
        rgType.addView(rbExpense);

        RadioButton rbIncome = new RadioButton(this);
        rbIncome.setText("INCOME");
        rgType.addView(rbIncome);

        // Default theo tab đang chọn
        if ("INCOME".equalsIgnoreCase(currentType)) {
            rbIncome.setChecked(true);
        } else {
            rbExpense.setChecked(true);
        }
        layout.addView(rgType);

        final String[] selectedIcon = new String[]{ "ic_fastfood" };
        layout.addView(buildIconPicker(selectedIcon[0], selectedIcon));

        new AlertDialog.Builder(this)
                .setTitle("Thêm danh mục")
                .setView(layout)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String icon = selectedIcon[0];
                    String type = rbExpense.isChecked() ? "EXPENSE" : "INCOME";

                    if (name.isEmpty() || icon.isEmpty()) {
                        Toast.makeText(CategoryActivity.this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Toast.makeText(CategoryActivity.this, "Tính năng thêm danh mục đã tắt", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showEditCategoryDialog(Category category) {
        if (category == null) return;
        String categoryId = category.getId();
        if (categoryId == null || categoryId.trim().isEmpty()) {
            Toast.makeText(this, "Category ID bị thiếu!", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText etName = new EditText(this);
        etName.setText(category.getName());
        etName.setHint("Tên danh mục");
        layout.addView(etName);

        // Chọn loại: EXPENSE/INCOME
        TextView tvType = new TextView(this);
        tvType.setText("Loại");
        tvType.setTextSize(14f);
        layout.addView(tvType);

        RadioGroup rgType = new RadioGroup(this);
        rgType.setOrientation(RadioGroup.HORIZONTAL);

        RadioButton rbExpense = new RadioButton(this);
        rbExpense.setText("EXPENSE");
        rgType.addView(rbExpense);

        RadioButton rbIncome = new RadioButton(this);
        rbIncome.setText("INCOME");
        rgType.addView(rbIncome);

        String existingType = category.getType();
        if ("INCOME".equalsIgnoreCase(existingType)) {
            rbIncome.setChecked(true);
        } else {
            rbExpense.setChecked(true);
        }
        layout.addView(rgType);

        final String[] selectedIcon = new String[]{category.getIcon()};
        layout.addView(buildIconPicker(selectedIcon[0], selectedIcon));

        new AlertDialog.Builder(this)
                .setTitle("Chỉnh sửa danh mục")
                .setView(layout)
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String icon = selectedIcon[0];
                    String type = rbExpense.isChecked() ? "EXPENSE" : "INCOME";

                    if (name.isEmpty() || icon == null || icon.trim().isEmpty()) {
                        Toast.makeText(CategoryActivity.this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Toast.makeText(CategoryActivity.this, "Tính năng sửa danh mục đã tắt", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDeleteCategoryDialog(Category category) {
        if (category == null) return;
        String categoryId = category.getId();
        if (categoryId == null || categoryId.trim().isEmpty()) {
            Toast.makeText(this, "Category ID bị thiếu!", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Xóa danh mục")
                .setMessage("Xóa '" + category.getName() + "'? (Có thể không hoàn tác)")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    Toast.makeText(CategoryActivity.this, "Tính năng xóa danh mục đã tắt", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}