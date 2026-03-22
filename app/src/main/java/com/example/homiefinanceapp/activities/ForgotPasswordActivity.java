package com.example.homiefinanceapp.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.homiefinanceapp.api.ApiService;
import com.example.homiefinanceapp.api.RetrofitClient;
import com.example.homiefinanceapp.databinding.ActivityForgotPasswordBinding;
import com.example.homiefinanceapp.models.ApiResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {
    private ActivityForgotPasswordBinding binding;
    private boolean isStep2 = false; // Phân biệt đang ở bước nhập Email hay bước Reset

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnSubmit.setOnClickListener(v -> {
            if (!isStep2) {
                sendOtp();
            } else {
                resetPassword();
            }
        });
    }

    private void sendOtp() {
        String email = binding.etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Nhập email đã homie!", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Đang gửi mã OTP...");
        pd.show();

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.forgotPassword(email).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                pd.dismiss();
                if (response.isSuccessful()) {
                    Toast.makeText(ForgotPasswordActivity.this, "Đã gửi mã OTP đến email!", Toast.LENGTH_LONG).show();

                    // CHUYỂN SANG BƯỚC 2: Hiện form nhập OTP và đổi text nút bấm
                    binding.layoutStep2.setVisibility(View.VISIBLE);
                    binding.etEmail.setEnabled(false); // Khóa ô email lại không cho sửa nữa
                    binding.btnSubmit.setText("ĐẶT LẠI MẬT KHẨU");
                    isStep2 = true;
                } else {
                    Toast.makeText(ForgotPasswordActivity.this, "Email không tồn tại!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                pd.dismiss();
                Toast.makeText(ForgotPasswordActivity.this, "Lỗi mạng!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetPassword() {
        String email = binding.etEmail.getText().toString().trim();
        String otp = binding.etOtp.getText().toString().trim();
        String newPassword = binding.etNewPassword.getText().toString().trim();

        if (otp.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "Nhập đủ OTP và mật khẩu mới nhé!", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Đang đổi mật khẩu...");
        pd.show();

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.resetPassword(email, otp, newPassword).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                pd.dismiss();
                if (response.isSuccessful()) {
                    Toast.makeText(ForgotPasswordActivity.this, "Đổi mật khẩu thành công! Đăng nhập thôi 🚀", Toast.LENGTH_LONG).show();
                    finish(); // Trở về màn Login
                } else {
                    Toast.makeText(ForgotPasswordActivity.this, "Mã OTP sai hoặc đã hết hạn!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                pd.dismiss();
                Toast.makeText(ForgotPasswordActivity.this, "Lỗi mạng!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}