package com.example.homiefinanceapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.ResponseBody;
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

        // 3. Sự kiện xuất file excel
        binding.btnExportExcel.setOnClickListener(v -> handleExportExcel());

        // 4. Sự kiện đăng xuất
        binding.btnLogoutProfile.setOnClickListener(v -> handleLogout());

        // 5. Tự động Refresh Token khi vào Profile
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

    private void handleExportExcel() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.downloadExcelReport("Bearer " + token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ProfileActivity.this, "Tải file thất bại!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String contentType = response.headers().get("Content-Type");
                if (contentType != null) {
                    String normalized = contentType.toLowerCase(Locale.ROOT);
                    boolean looksLikeExcel = normalized.contains("sheet")
                            || normalized.contains("excel")
                            || normalized.contains("octet-stream");
                    if (!looksLikeExcel) {
                        String rawBody = "";
                        try {
                            rawBody = response.body().string();
                        } catch (Exception ignored) {
                        }
                        Toast.makeText(
                                ProfileActivity.this,
                                "Server không trả file Excel hợp lệ!",
                                Toast.LENGTH_LONG
                        ).show();
                        Log.e("PROFILE_EXPORT", "Unexpected content type: " + contentType + " body: " + rawBody);
                        return;
                    }
                }

                String fileName = resolveExcelFileName(response);
                boolean saved = writeResponseBodyToPublicDownloads(response.body(), fileName);
                if (saved) {
                    Toast.makeText(ProfileActivity.this, "Đã tải file vào Download: " + fileName, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(ProfileActivity.this, "Lưu file thất bại!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Không thể tải file: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean writeResponseBodyToPublicDownloads(ResponseBody body, String fileName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            values.put(MediaStore.Downloads.IS_PENDING, 1);

            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                return false;
            }

            try (InputStream inputStream = body.byteStream();
                 OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                if (outputStream == null) {
                    return false;
                }
                byte[] firstBytes = new byte[2];
                int firstRead = inputStream.read(firstBytes);
                if (firstRead < 2 || firstBytes[0] != 'P' || firstBytes[1] != 'K') {
                    Log.e("PROFILE_EXPORT", "Downloaded file is not valid XLSX (missing PK header)");
                    return false;
                }
                outputStream.write(firstBytes, 0, 2);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();

                ContentValues doneValues = new ContentValues();
                doneValues.put(MediaStore.Downloads.IS_PENDING, 0);
                getContentResolver().update(uri, doneValues, null, null);
                return true;
            } catch (IOException e) {
                Log.e("PROFILE_EXPORT", "Write download file error: " + e.getMessage(), e);
                getContentResolver().delete(uri, null, null);
                return false;
            }
        }

        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (downloadDir == null || (!downloadDir.exists() && !downloadDir.mkdirs())) {
            return false;
        }
        File outputFile = new File(downloadDir, fileName);
        try (InputStream inputStream = body.byteStream();
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            byte[] firstBytes = new byte[2];
            int firstRead = inputStream.read(firstBytes);
            if (firstRead < 2 || firstBytes[0] != 'P' || firstBytes[1] != 'K') {
                Log.e("PROFILE_EXPORT", "Downloaded file is not valid XLSX (missing PK header)");
                return false;
            }
            outputStream.write(firstBytes, 0, 2);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            return true;
        } catch (IOException e) {
            Log.e("PROFILE_EXPORT", "Write legacy download file error: " + e.getMessage(), e);
            return false;
        }
    }

    private String resolveExcelFileName(Response<ResponseBody> response) {
        String contentDisposition = response.headers().get("Content-Disposition");
        if (contentDisposition != null) {
            Matcher matcher = Pattern.compile("filename\\*=UTF-8''([^;]+)|filename=\"?([^\";]+)\"?", Pattern.CASE_INSENSITIVE)
                    .matcher(contentDisposition);
            if (matcher.find()) {
                String fileName = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                if (fileName != null && !fileName.trim().isEmpty()) {
                    fileName = fileName.trim().replace("\"", "");
                    if (!fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
                        fileName = fileName + ".xlsx";
                    }
                    return fileName;
                }
            }
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return "homie_report_" + timestamp + ".xlsx";
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