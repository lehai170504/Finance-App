package com.example.homiefinanceapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.homiefinanceapp.activities.LoginActivity;
import com.example.homiefinanceapp.R;
import com.example.homiefinanceapp.api.ApiService;
import com.example.homiefinanceapp.api.RetrofitClient;
import com.example.homiefinanceapp.databinding.ActivityProfileBinding;
import com.example.homiefinanceapp.models.ApiResponse;
import com.example.homiefinanceapp.models.LoginResponse;
import com.example.homiefinanceapp.models.UserResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {
    private ActivityProfileBinding binding;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences prefs = getSharedPreferences("HomiePrefs", MODE_PRIVATE);
        token = prefs.getString("token", "");

        loadUserProfile();

        // 1. Sự kiện đổi tên hiển thị
        binding.btnEditProfile.setOnClickListener(v -> showEditNameDialog());

        // 2. Sự kiện đổi mật khẩu
        binding.btnChangePass.setOnClickListener(v -> showChangePassDialog());

        // 3. Sự kiện đăng xuất
        binding.btnLogoutProfile.setOnClickListener(v -> handleLogout());

        // 4. Tự động Refresh Token khi vào Profile
        handleRefreshToken();

        binding.btnBackProfile.setOnClickListener(v -> finish());
    }

    private void loadUserProfile() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getMe("Bearer " + token).enqueue(new Callback<ApiResponse<UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserResponse>> call, Response<ApiResponse<UserResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse user = response.body().getData();
                    binding.tvProfileName.setText(user.getUsername());
                    binding.tvProfileEmail.setText("Email: " + user.getEmail());
                    binding.tvProfileRole.setText(user.getRole());
                } else if (response.code() == 401) {
                    clearDataAndJumpToLogin();
                }
            }
            @Override public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {}
        });
    }

    private void showChangePassDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        EditText etOld = dialogView.findViewById(R.id.etOldPassword);
        EditText etNew = dialogView.findViewById(R.id.etNewPassword);

        new AlertDialog.Builder(this)
                .setTitle("Đổi mật khẩu")
                .setView(dialogView)
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    String oldP = etOld.getText().toString().trim();
                    String newP = etNew.getText().toString().trim();
                    if (!oldP.isEmpty() && !newP.isEmpty()) {
                        handleUpdatePassword(oldP, newP);
                    } else {
                        Toast.makeText(this, "Vui lòng nhập đủ 2 mật khẩu!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void handleUpdatePassword(String oldPass, String newPass) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.changePassword("Bearer " + token, oldPass, newPass).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ProfileActivity.this, "Đã đổi mật khẩu thành công! 🔐", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ProfileActivity.this, "Mật khẩu cũ không đúng!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<ApiResponse<String>> call, Throwable t) {}
        });
    }

    // 💡 FIX: Sửa lại kiểu dữ liệu trả về cho khớp với LoginResponse phẳng
    private void handleRefreshToken() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

        // Gọi ApiResponse<LoginResponse> vì class LoginResponse giờ chứa accessToken trực tiếp
        apiService.refreshToken("Bearer " + token).enqueue(new Callback<ApiResponse<LoginResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LoginResponse>> call, Response<ApiResponse<LoginResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {

                    // Lấy object data (chính là LoginResponse)
                    LoginResponse data = response.body().getData();
                    String newToken = data.getAccessToken();

                    SharedPreferences.Editor editor = getSharedPreferences("HomiePrefs", MODE_PRIVATE).edit();
                    editor.putString("token", newToken);
                    editor.apply();

                    token = newToken; // Cập nhật biến local
                    Log.d("AUTH", "Token refreshed!");
                }
            }
            @Override public void onFailure(Call<ApiResponse<LoginResponse>> call, Throwable t) {
                Log.e("AUTH", "Refresh fail: " + t.getMessage());
            }
        });
    }

    private void showEditNameDialog() {
        final EditText etNewName = new EditText(this);
        etNewName.setHint("Nhập tên hiển thị mới...");
        etNewName.setPadding(60, 40, 60, 40);

        new AlertDialog.Builder(this)
                .setTitle("Đổi tên hiển thị")
                .setView(etNewName)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String name = etNewName.getText().toString().trim();
                    if (!name.isEmpty()) handleUpdateUsername(name);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void handleUpdateUsername(String newName) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.updateUsername("Bearer " + token, newName).enqueue(new Callback<ApiResponse<UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserResponse>> call, Response<ApiResponse<UserResponse>> response) {
                if (response.isSuccessful()) {
                    binding.tvProfileName.setText(newName);
                    getSharedPreferences("HomiePrefs", MODE_PRIVATE).edit().putString("user_name", newName).apply();
                    Toast.makeText(ProfileActivity.this, "Đã đổi tên! 🎉", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<ApiResponse<UserResponse>> call, Throwable t) {}
        });
    }

    private void handleLogout() {
        android.app.ProgressDialog pd = new android.app.ProgressDialog(this);
        pd.setMessage("Đang đăng xuất...");
        pd.show();

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.logout("Bearer " + token).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                pd.dismiss();
                clearDataAndJumpToLogin();
            }
            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                pd.dismiss();
                clearDataAndJumpToLogin();
            }
        });
    }

    private void clearDataAndJumpToLogin() {
        getSharedPreferences("HomiePrefs", MODE_PRIVATE).edit().clear().apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Hẹn gặp lại homie! 👋", Toast.LENGTH_SHORT).show();
    }
}