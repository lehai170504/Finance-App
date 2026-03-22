package com.example.homiefinanceapp.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.homiefinanceapp.api.ApiService;
import com.example.homiefinanceapp.api.RetrofitClient;
import com.example.homiefinanceapp.databinding.ActivityRegisterBinding;
import com.example.homiefinanceapp.models.ApiResponse;
import com.example.homiefinanceapp.models.RegisterRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Bấm nút Đăng ký
        binding.btnRegister.setOnClickListener(v -> handleRegister());

        // Bấm chữ Quay lại đăng nhập
        binding.tvBackToLogin.setOnClickListener(v -> finish()); // Đóng màn hình này, quay lại Login
    }

    private void handleRegister() {
        String username = binding.etUsername.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        // 1. Validate sương sương
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Homie ơi, điền đủ thông tin đi!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Hiện Dialog cho nó pro
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Đang tạo tài khoản...");
        pd.show();

        // 3. Gọi API
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        RegisterRequest request = new RegisterRequest(username, email, password);

        apiService.registerUser(request).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                pd.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công! Đăng nhập thôi 🚀", Toast.LENGTH_LONG).show();
                    finish(); // Đá về màn hình Login
                } else {
                    Toast.makeText(RegisterActivity.this, "Tài khoản hoặc Email đã tồn tại!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                pd.dismiss();
                Toast.makeText(RegisterActivity.this, "Lỗi mạng rùi homie!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}