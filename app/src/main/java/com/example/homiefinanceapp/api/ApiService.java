package com.example.homiefinanceapp.api;

import com.example.homiefinanceapp.models.ApiResponse;
import com.example.homiefinanceapp.models.Category;
import com.example.homiefinanceapp.models.LoginRequest;
import com.example.homiefinanceapp.models.LoginResponse;
import com.example.homiefinanceapp.models.RegisterRequest;
import com.example.homiefinanceapp.models.UserResponse;
import com.example.homiefinanceapp.models.Wallet;
import com.example.homiefinanceapp.models.WalletRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @POST("auth/login")
    Call<ApiResponse<LoginResponse>> login(@Body LoginRequest loginRequest);

    @GET("auth/me")
    Call<ApiResponse<UserResponse>> getMe(@Header("Authorization") String token);

    @PUT("auth/profile")
    Call<ApiResponse<UserResponse>> updateUsername(
            @Header("Authorization") String token,
            @Query("newUsername") String newUsername
    );

    @POST("auth/logout")
    Call<ApiResponse<String>> logout(@Header("Authorization") String token);

    @POST("auth/register")
    Call<ApiResponse<String>> registerUser(@Body RegisterRequest request);

    @POST("auth/forgot-password")
    Call<ApiResponse<String>> forgotPassword(@Query("email") String email);

    @POST("auth/reset-password")
    Call<ApiResponse<String>> resetPassword(
            @Query("email") String email,
            @Query("otp") String otp,
            @Query("newPassword") String newPassword
    );

    @POST("auth/change-password")
    Call<ApiResponse<String>> changePassword(
            @Header("Authorization") String token,
            @Query("oldPass") String oldPass,
            @Query("newPass") String newPass
    );

    // 💡 FIX: Dùng trực tiếp LoginResponse vì class này đã phẳng rồi
    @POST("auth/refresh")
    Call<ApiResponse<LoginResponse>> refreshToken(
            @Header("Authorization") String token
    );

    @GET("wallets")
    Call<ApiResponse<List<Wallet>>> getWallets(@Header("Authorization") String token);

    // Lấy tổng số dư các ví
    @GET("wallets/total-balance")
    Call<ApiResponse<Double>> getTotalBalance(@Header("Authorization") String token);

    // Thêm ví mới
    @POST("wallets")
    Call<ApiResponse<Wallet>> createWallet(@Header("Authorization") String token, @Body WalletRequest request);

    // Cập nhật ví (Đổi tên, màu, số dư)
    @PUT("wallets/{id}")
    Call<ApiResponse<Wallet>> updateWallet(@Header("Authorization") String token, @Path("id") String id, @Body WalletRequest request);

    // Xóa ví
    @DELETE("wallets/{id}")
    Call<ApiResponse<String>> deleteWallet(@Header("Authorization") String token, @Path("id") String id);

    // Chuyển tiền giữa các ví
    @POST("wallets/transfer")
    Call<ApiResponse<String>> transferMoney(
            @Header("Authorization") String token,
            @Query("fromId") String fromId,
            @Query("toId") String toId,
            @Query("amount") Double amount
    );

    @GET("categories")
    Call<ApiResponse<List<Category>>> getCategories(@Header("Authorization") String token);
}