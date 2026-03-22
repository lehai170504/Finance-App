package com.example.homiefinanceapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.homiefinanceapp.MainActivity;
import com.example.homiefinanceapp.api.ApiService;
import com.example.homiefinanceapp.api.RetrofitClient;
import com.example.homiefinanceapp.databinding.ActivityLoginBinding;
import com.example.homiefinanceapp.models.ApiResponse;
import com.example.homiefinanceapp.models.LoginRequest;
import com.example.homiefinanceapp.models.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Sử dụng ViewBinding để ánh xạ giao diện
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 💡 BƯỚC MỚI THÊM: Bấm chữ "Chưa có tài khoản" -> Chuyển sang Đăng ký
        binding.tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        binding.tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        // Xử lý nút Đăng nhập
        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String pass = binding.etPassword.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Gọi hàm xử lý API
            handleLogin(email, pass);
        });
    }

    private void handleLogin(String email, String password) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

        apiService.login(new LoginRequest(email, password)).enqueue(new Callback<ApiResponse<LoginResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LoginResponse>> call, Response<ApiResponse<LoginResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {

                    // 💡 CHỈ CẦN 1 BƯỚC: Lấy data ra là có Token luôn
                    LoginResponse data = response.body().getData();

                    String token = data.getAccessToken();
                    String username = data.getUsername();
                    String refreshToken = data.getRefreshToken(); // 💡 Lấy refresh token ra

                    // 💡 Truyền cả 3 vào hàm lưu
                    saveUserData(token, username, refreshToken);

                    Toast.makeText(LoginActivity.this, "Chào mừng " + username + "! 🚀", Toast.LENGTH_SHORT).show();

                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Sai email hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LoginResponse>> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 💡 ĐÃ FIX LẠI: Tách hàm này ra ngoài độc lập cho code sạch sẽ dễ nhìn
    private void saveUserData(String token, String username, String refreshToken) {
        SharedPreferences sharedPreferences = getSharedPreferences("HomiePrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("token", token);
        editor.putString("user_name", username);
        editor.putString("refresh_token", refreshToken); // 💡 Dùng tham số truyền vào, không dùng "data."

        editor.apply();
    }
}