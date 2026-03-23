package com.example.homiefinanceapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.homiefinanceapp.adapters.TransactionAdapter;
import com.example.homiefinanceapp.api.ApiService;
import com.example.homiefinanceapp.api.RetrofitClient;
import com.example.homiefinanceapp.databinding.ActivityTrashTransactionsBinding;
import com.example.homiefinanceapp.models.ApiResponse;
import com.example.homiefinanceapp.models.TransactionListItem;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrashTransactionsActivity extends AppCompatActivity {
    private ActivityTrashTransactionsBinding binding;
    private String token;
    private TransactionAdapter transactionAdapter;
    private final List<TransactionListItem> transactions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTrashTransactionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        token = getSharedPreferences("HomiePrefs", MODE_PRIVATE).getString("token", "");

        binding.btnBack.setOnClickListener(v -> finish());

        transactionAdapter = new TransactionAdapter(this::confirmRestoreTransaction);
        binding.rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTransactions.setAdapter(transactionAdapter);

        fetchTrashTransactions();
    }

    private void fetchTrashTransactions() {
        if (token == null || token.trim().isEmpty()) {
            Toast.makeText(this, "Chưa có token!", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getTrashedTransactions("Bearer " + token)
                .enqueue(new Callback<ApiResponse<List<TransactionListItem>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<TransactionListItem>>> call, Response<ApiResponse<List<TransactionListItem>>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            transactions.clear();
                            transactions.addAll(response.body().getData());
                            transactionAdapter.setTransactions(transactions);
                            updateEmptyState();
                        } else {
                            Toast.makeText(TrashTransactionsActivity.this, "Không lấy được thùng rác!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<TransactionListItem>>> call, Throwable t) {
                        Log.e("API_ERROR", "fetchTrashTransactions: " + t.getMessage(), t);
                        Toast.makeText(TrashTransactionsActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateEmptyState() {
        binding.tvEmptyTransactions.setVisibility(transactions.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void confirmRestoreTransaction(TransactionListItem item) {
        if (item == null || item.getId() == null || item.getId().trim().isEmpty()) {
            Toast.makeText(this, "Giao dịch không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }
        showTrashActionMenu(item);
    }

    private void showTrashActionMenu(TransactionListItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Tùy chọn thùng rác")
                .setItems(new CharSequence[]{"Khôi phục giao dịch", "Xóa vĩnh viễn"}, (dialog, which) -> {
                    if (which == 0) {
                        confirmRestoreDialog(item);
                    } else if (which == 1) {
                        confirmForceDeleteDialog(item);
                    }
                })
                .show();
    }

    private void confirmRestoreDialog(TransactionListItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Khôi phục giao dịch")
                .setMessage("Khôi phục giao dịch này khỏi thùng rác?")
                .setPositiveButton("Khôi phục", (d, w) -> restoreTransactionApi(item))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmForceDeleteDialog(TransactionListItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa vĩnh viễn")
                .setMessage("Hành động này không thể hoàn tác. Bạn chắc chắn muốn xóa vĩnh viễn?")
                .setPositiveButton("Xóa vĩnh viễn", (d, w) -> forceDeleteTransactionApi(item))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void restoreTransactionApi(TransactionListItem item) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.restoreTransaction("Bearer " + token, item.getId())
                .enqueue(new Callback<ApiResponse<TransactionListItem>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<TransactionListItem>> call, Response<ApiResponse<TransactionListItem>> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(
                                    TrashTransactionsActivity.this,
                                    resolveBeMessage(response, "Khôi phục thành công!"),
                                    Toast.LENGTH_SHORT
                            ).show();
                            removeTransactionFromTrash(item.getId());
                        } else {
                            Toast.makeText(
                                    TrashTransactionsActivity.this,
                                    resolveBeMessage(response, "Khôi phục thất bại!"),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<TransactionListItem>> call, Throwable t) {
                        Log.e("API_ERROR", "restoreTransactionApi: " + t.getMessage(), t);
                        Toast.makeText(TrashTransactionsActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void forceDeleteTransactionApi(TransactionListItem item) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.forceDeleteTransaction("Bearer " + token, item.getId())
                .enqueue(new Callback<ApiResponse<String>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(
                                    TrashTransactionsActivity.this,
                                    resolveBeMessage(response, "Xóa vĩnh viễn thành công!"),
                                    Toast.LENGTH_SHORT
                            ).show();
                            removeTransactionFromTrash(item.getId());
                        } else {
                            Toast.makeText(
                                    TrashTransactionsActivity.this,
                                    resolveBeMessage(response, "Xóa vĩnh viễn thất bại!"),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                        Log.e("API_ERROR", "forceDeleteTransactionApi: " + t.getMessage(), t);
                        Toast.makeText(TrashTransactionsActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void removeTransactionFromTrash(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) return;
        for (int i = 0; i < transactions.size(); i++) {
            TransactionListItem t = transactions.get(i);
            if (t != null && transactionId.equals(t.getId())) {
                transactions.remove(i);
                break;
            }
        }
        transactionAdapter.setTransactions(transactions);
        updateEmptyState();
    }

    private String resolveBeMessage(Response<?> response, String fallback) {
        if (response != null && response.body() instanceof ApiResponse) {
            ApiResponse<?> body = (ApiResponse<?>) response.body();
            if (body.getMessage() != null && !body.getMessage().trim().isEmpty()) {
                return body.getMessage();
            }
        }
        try {
            if (response != null && response.errorBody() != null) {
                String err = response.errorBody().string();
                JSONObject obj = new JSONObject(err);
                String beMsg = obj.optString("message", "");
                if (beMsg != null && !beMsg.trim().isEmpty()) return beMsg;
            }
        } catch (Exception ignored) {}
        return fallback;
    }
}

