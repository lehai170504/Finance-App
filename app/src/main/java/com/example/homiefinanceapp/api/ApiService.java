package com.example.homiefinanceapp.api;

import com.example.homiefinanceapp.models.ApiResponse;
import com.example.homiefinanceapp.models.CategoryReportItem;
import com.example.homiefinanceapp.models.Category;
import com.example.homiefinanceapp.models.Group;
import com.example.homiefinanceapp.models.GroupStatsData;
import com.example.homiefinanceapp.models.GroupRequest;
import com.example.homiefinanceapp.models.LoginRequest;
import com.example.homiefinanceapp.models.LoginResponse;
import com.example.homiefinanceapp.models.RegisterRequest;
import com.example.homiefinanceapp.models.UserResponse;
import com.example.homiefinanceapp.models.Wallet;
import com.example.homiefinanceapp.models.WalletRequest;
import com.example.homiefinanceapp.models.TransactionCreateRequest;
import com.example.homiefinanceapp.models.TransactionCreateResponse;
import com.example.homiefinanceapp.models.TransactionListItem;
import com.example.homiefinanceapp.models.TransactionsPageData;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

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

    @GET("transactions/total-income")
    Call<ApiResponse<Double>> getTotalIncome(@Header("Authorization") String token);

    @GET("transactions/total-expense")
    Call<ApiResponse<Double>> getTotalExpense(@Header("Authorization") String token);

    @GET("reports/categories")
    Call<ApiResponse<List<CategoryReportItem>>> getCategoryReport(
            @Header("Authorization") String token,
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );

    @Streaming
    @GET("reports/download-excel")
    Call<ResponseBody> downloadExcelReport(@Header("Authorization") String token);

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

    // Tạo giao dịch mới
    @POST("transactions/create")
    Call<ApiResponse<TransactionCreateResponse>> createTransaction(
            @Header("Authorization") String token,
            @Query("walletId") String walletId,
            @Query("categoryId") String categoryId,
            @Query("groupId") String groupId, // optional
            @Body TransactionCreateRequest request
    );

    @PUT("transactions/{id}")
    Call<ApiResponse<TransactionListItem>> updateTransaction(
            @Header("Authorization") String token,
            @Path("id") String transactionId,
            @Query("newWalletId") String newWalletId,
            @Query("categoryId") String categoryId,
            @Body TransactionCreateRequest request
    );

    @DELETE("transactions/{id}")
    Call<ApiResponse<String>> deleteTransaction(
            @Header("Authorization") String token,
            @Path("id") String transactionId
    );

    // Lấy danh sách transactions (phân trang)
    @GET("transactions")
    Call<ApiResponse<TransactionsPageData>> getTransactions(
            @Header("Authorization") String token,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("transactions/search")
    Call<ApiResponse<TransactionsPageData>> searchTransactions(
            @Header("Authorization") String token,
            @Query("keyword") String keyword,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("transactions/group/{groupId}")
    Call<ApiResponse<TransactionsPageData>> getTransactionsByGroup(
            @Header("Authorization") String token,
            @Path("groupId") String groupId,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("transactions/trash")
    Call<ApiResponse<List<TransactionListItem>>> getTrashedTransactions(
            @Header("Authorization") String token
    );

    @PUT("transactions/{id}/restore")
    Call<ApiResponse<TransactionListItem>> restoreTransaction(
            @Header("Authorization") String token,
            @Path("id") String transactionId
    );

    @DELETE("transactions/{id}/force")
    Call<ApiResponse<String>> forceDeleteTransaction(
            @Header("Authorization") String token,
            @Path("id") String transactionId
    );

    @Multipart
    @POST("transactions/{id}/upload-receipt")
    Call<ApiResponse<TransactionListItem>> uploadTransactionReceipt(
            @Header("Authorization") String token,
            @Path("id") String transactionId,
            @Part MultipartBody.Part file
    );

    @GET("categories")
    Call<ApiResponse<List<Category>>> getCategories(@Header("Authorization") String token);

    @GET("groups/me")
    Call<ApiResponse<List<Group>>> getMyGroups(@Header("Authorization") String token);

    @GET("groups/details/{groupId}/stats")
    Call<ApiResponse<GroupStatsData>> getGroupMonthlyStats(
            @Header("Authorization") String token,
            @Path("groupId") String groupId,
            @Query("month") int month,
            @Query("year") int year
    );

    // 1. Tạo nhóm mới (Sử dụng API thật - image_7.png - nhận Query 'name')
    @POST("groups/create")
    Call<ApiResponse<Group>> createGroup(@Header("Authorization") String token, @Query("name") String name);

    // 2. Rời khỏi nhóm
    @POST("groups/{id}/leave")
    Call<ApiResponse<String>> leaveGroup(@Header("Authorization") String token, @Path("id") String id);

    // 3. Chỉnh sửa tên nhóm
    @PUT("groups/{id}")
    Call<ApiResponse<Group>> updateGroup(
            @Header("Authorization") String token,
            @Path("id") String id,
            @Query("newName") String newName
    );

    // 4. Xóa nhóm hoàn toàn
    @DELETE("groups/{id}")
    Call<ApiResponse<String>> deleteGroup(@Header("Authorization") String token, @Path("id") String id);

    @POST("groups/join")
    Call<ApiResponse<Group>> joinGroup(@Header("Authorization") String token, @Query("inviteCode") String inviteCode);
}