package com.example.homiefinanceapp.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.tabs.TabLayout;

import com.example.homiefinanceapp.adapters.TransactionAdapter;
import com.example.homiefinanceapp.api.ApiService;
import com.example.homiefinanceapp.api.RetrofitClient;
import com.example.homiefinanceapp.databinding.ActivityGroupTransactionsBinding;
import com.example.homiefinanceapp.models.ApiResponse;
import com.example.homiefinanceapp.models.Category;
import com.example.homiefinanceapp.models.TransactionCreateRequest;
import com.example.homiefinanceapp.models.TransactionListItem;
import com.example.homiefinanceapp.models.TransactionsPageData;
import com.example.homiefinanceapp.models.Wallet;
import org.json.JSONObject;

import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupTransactionsActivity extends AppCompatActivity {
    public static final String EXTRA_GROUP_ID = "extra_group_id";
    public static final String EXTRA_GROUP_NAME = "extra_group_name";

    private ActivityGroupTransactionsBinding binding;
    private String token;
    private String selectedGroupId;
    private String selectedType = "ALL";
    private String pendingReceiptTransactionId;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private TransactionAdapter transactionAdapter;
    private final List<Category> allCategories = new ArrayList<>();
    private final List<Wallet> allWallets = new ArrayList<>();
    private final List<TransactionListItem> allTransactions = new ArrayList<>();
    private final List<TransactionListItem> currentTransactions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupTransactionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        token = getSharedPreferences("HomiePrefs", MODE_PRIVATE).getString("token", "");
        selectedGroupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        String groupName = getIntent().getStringExtra(EXTRA_GROUP_NAME);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::handlePickedReceiptImage
        );
        loadCategories();
        loadWallets();

        binding.tvTitle.setText("Giao dịch nhóm - " + (groupName != null ? groupName : ""));
        binding.btnBack.setOnClickListener(v -> finish());

        transactionAdapter = new TransactionAdapter(this::showReceiptActionDialog);
        binding.rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTransactions.setAdapter(transactionAdapter);

        binding.btnSearch.setOnClickListener(v -> submitSearch());
        binding.etSearchNote.setOnEditorActionListener((v, actionId, event) -> {
            boolean isSearchAction = actionId == EditorInfo.IME_ACTION_SEARCH;
            boolean isEnter = event != null
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
            if (isSearchAction || isEnter) {
                submitSearch();
                return true;
            }
            return false;
        });

        binding.tabTransactionType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 1) {
                    selectedType = "EXPENSE";
                } else if (tab.getPosition() == 2) {
                    selectedType = "INCOME";
                } else {
                    selectedType = "ALL";
                }
                submitSearch();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        fetchTransactionsByGroup(selectedGroupId);
    }

    private void showReceiptActionDialog(TransactionListItem item) {
        if (item == null || item.getId() == null || item.getId().trim().isEmpty()) {
            Toast.makeText(this, "Giao dịch không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Tùy chọn giao dịch")
                .setItems(new CharSequence[]{"Sửa giao dịch", "Thêm hóa đơn", "Xóa giao dịch"}, (dialog, which) -> {
                    if (which == 0) {
                        showEditTransactionDialog(item);
                    } else if (which == 1) {
                        pendingReceiptTransactionId = item.getId();
                        imagePickerLauncher.launch("image/*");
                    } else if (which == 2) {
                        confirmDeleteTransaction(item);
                    }
                })
                .show();
    }

    private void confirmDeleteTransaction(TransactionListItem item) {
        if (item == null || item.getId() == null || item.getId().trim().isEmpty()) {
            Toast.makeText(this, "Giao dịch không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Xóa giao dịch")
                .setMessage("Giao dịch sẽ được chuyển vào thùng rác tạm. Bạn chắc chắn chứ?")
                .setPositiveButton("Xóa", (d, w) -> deleteTransactionApi(item.getId()))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteTransactionApi(String transactionId) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.deleteTransaction("Bearer " + token, transactionId)
                .enqueue(new Callback<ApiResponse<String>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(
                                    GroupTransactionsActivity.this,
                                    resolveBeMessage(response, "Xóa giao dịch thành công!"),
                                    Toast.LENGTH_SHORT
                            ).show();
                            refreshGroupTransactionsImmediately();
                        } else {
                            Toast.makeText(
                                    GroupTransactionsActivity.this,
                                    resolveBeMessage(response, "Xóa giao dịch thất bại!"),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                        Log.e("API_ERROR", "deleteTransactionApi: " + t.getMessage(), t);
                        Toast.makeText(GroupTransactionsActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showEditTransactionDialog(TransactionListItem item) {
        if (item == null || item.getId() == null || item.getId().trim().isEmpty()) {
            Toast.makeText(this, "Giao dịch không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (allWallets.isEmpty() || allCategories.isEmpty()) {
            Toast.makeText(this, "Đang tải danh mục/ví, thử lại sau!", Toast.LENGTH_SHORT).show();
            loadCategories();
            loadWallets();
            return;
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad / 2);

        EditText etAmount = new EditText(this);
        etAmount.setHint("Số tiền");
        etAmount.setText(item.getAmount() != null ? String.valueOf(item.getAmount()) : "");
        layout.addView(etAmount);

        EditText etNote = new EditText(this);
        etNote.setHint("Ghi chú");
        etNote.setText(item.getNote() != null ? item.getNote() : "");
        layout.addView(etNote);

        EditText etDate = new EditText(this);
        etDate.setHint("yyyy-MM-dd");
        etDate.setText(item.getDate() != null ? item.getDate() : "");
        layout.addView(etDate);

        TextView tvWalletLabel = new TextView(this);
        tvWalletLabel.setText("Ví mới");
        tvWalletLabel.setPadding(0, pad / 2, 0, pad / 4);
        layout.addView(tvWalletLabel);

        Spinner spWallet = new Spinner(this);
        List<String> walletNames = new ArrayList<>();
        int selectedWalletIndex = 0;
        String currentWalletId = findWalletIdByName(item.getWalletName());
        for (int i = 0; i < allWallets.size(); i++) {
            Wallet w = allWallets.get(i);
            walletNames.add(w.getName() != null ? w.getName() : "Wallet");
            if (currentWalletId != null && currentWalletId.equals(w.getId())) {
                selectedWalletIndex = i;
            }
        }
        ArrayAdapter<String> walletAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, walletNames);
        walletAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spWallet.setAdapter(walletAdapter);
        spWallet.setSelection(selectedWalletIndex);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        spinnerParams.bottomMargin = pad / 2;
        layout.addView(spWallet, spinnerParams);

        TextView tvCategoryLabel = new TextView(this);
        tvCategoryLabel.setText("Danh mục mới");
        tvCategoryLabel.setPadding(0, 0, 0, pad / 4);
        layout.addView(tvCategoryLabel);

        Spinner spCategory = new Spinner(this);
        List<Category> filteredCategories = new ArrayList<>();
        for (Category c : allCategories) {
            if (c == null) continue;
            if (item.getCategoryType() == null || c.getType() == null || c.getType().equalsIgnoreCase(item.getCategoryType())) {
                filteredCategories.add(c);
            }
        }
        if (filteredCategories.isEmpty()) filteredCategories.addAll(allCategories);
        List<String> categoryNames = new ArrayList<>();
        int selectedCategoryIndex = 0;
        String currentCategoryId = findCategoryIdByNameAndType(item.getCategoryName(), item.getCategoryType());
        for (int i = 0; i < filteredCategories.size(); i++) {
            Category c = filteredCategories.get(i);
            categoryNames.add(c.getName() != null ? c.getName() : "Category");
            if (currentCategoryId != null && currentCategoryId.equals(c.getId())) {
                selectedCategoryIndex = i;
            }
        }
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryNames);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(categoryAdapter);
        spCategory.setSelection(selectedCategoryIndex);
        layout.addView(spCategory, spinnerParams);

        new AlertDialog.Builder(this)
                .setTitle("Sửa giao dịch")
                .setView(layout)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String amountStr = etAmount.getText().toString().trim();
                    String note = etNote.getText().toString().trim();
                    String date = etDate.getText().toString().trim();
                    if (amountStr.isEmpty() || date.isEmpty()) {
                        Toast.makeText(this, "Thiếu số tiền hoặc ngày!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String selectedWalletId = null;
                    if (spWallet.getSelectedItemPosition() >= 0 && spWallet.getSelectedItemPosition() < allWallets.size()) {
                        selectedWalletId = allWallets.get(spWallet.getSelectedItemPosition()).getId();
                    }
                    String selectedCategoryId = null;
                    if (spCategory.getSelectedItemPosition() >= 0 && spCategory.getSelectedItemPosition() < filteredCategories.size()) {
                        selectedCategoryId = filteredCategories.get(spCategory.getSelectedItemPosition()).getId();
                    }
                    if (selectedWalletId == null || selectedWalletId.trim().isEmpty()
                            || selectedCategoryId == null || selectedCategoryId.trim().isEmpty()) {
                        Toast.makeText(this, "Chưa chọn ví hoặc danh mục!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        Double amount = Double.parseDouble(amountStr);
                        updateTransactionApi(item, selectedWalletId, selectedCategoryId, amount, note, date);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số tiền không hợp lệ!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateTransactionApi(TransactionListItem sourceItem, String walletId, String categoryId, Double amount, String note, String date) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        TransactionCreateRequest body = new TransactionCreateRequest(amount, note, date);
        apiService.updateTransaction("Bearer " + token, sourceItem.getId(), walletId, categoryId, body)
                .enqueue(new Callback<ApiResponse<TransactionListItem>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<TransactionListItem>> call, Response<ApiResponse<TransactionListItem>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String msg = resolveBeMessage(response, "Sửa giao dịch thành công!");
                            Toast.makeText(GroupTransactionsActivity.this, msg, Toast.LENGTH_SHORT).show();
                            TransactionListItem data = response.body().getData();
                            if (data != null) {
                                replaceTransactionInLists(sourceItem.getId(), data);
                            } else {
                                sourceItem.setAmount(amount);
                                sourceItem.setNote(note);
                                sourceItem.setDate(date);
                                String walletName = findWalletNameById(walletId);
                                if (walletName != null) sourceItem.setWalletName(walletName);
                                Category c = findCategoryById(categoryId);
                                if (c != null) {
                                    sourceItem.setCategoryName(c.getName());
                                    sourceItem.setCategoryType(c.getType());
                                }
                                transactionAdapter.setTransactions(currentTransactions);
                                updateEmptyState();
                            }
                            refreshGroupTransactionsImmediately();
                        } else {
                            Toast.makeText(
                                    GroupTransactionsActivity.this,
                                    resolveBeMessage(response, "Sửa giao dịch thất bại!"),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<TransactionListItem>> call, Throwable t) {
                        Log.e("API_ERROR", "updateTransactionApi: " + t.getMessage(), t);
                        Toast.makeText(GroupTransactionsActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handlePickedReceiptImage(Uri uri) {
        if (uri == null) return;
        if (pendingReceiptTransactionId == null || pendingReceiptTransactionId.trim().isEmpty()) {
            Toast.makeText(this, "Thiếu transactionId để upload hóa đơn!", Toast.LENGTH_SHORT).show();
            return;
        }
        uploadReceiptForTransaction(pendingReceiptTransactionId, uri);
    }

    private void uploadReceiptForTransaction(String transactionId, Uri uri) {
        if (token == null || token.trim().isEmpty()) {
            Toast.makeText(this, "Chưa có token!", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File tempFile = copyUriToTempFile(uri);
            String mimeType = getContentResolver().getType(uri);
            if (mimeType == null || mimeType.trim().isEmpty()) mimeType = "image/*";

            RequestBody body = RequestBody.create(MediaType.parse(mimeType), tempFile);
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", tempFile.getName(), body);

            ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
            apiService.uploadTransactionReceipt("Bearer " + token, transactionId, filePart)
                    .enqueue(new Callback<ApiResponse<TransactionListItem>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<TransactionListItem>> call, Response<ApiResponse<TransactionListItem>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                String msg = resolveBeMessage(response, "Upload hóa đơn thành công!");
                                Toast.makeText(GroupTransactionsActivity.this, msg, Toast.LENGTH_SHORT).show();
                                if (response.body().getData() != null) {
                                    syncReceiptUrlToLists(
                                            response.body().getData().getId(),
                                            response.body().getData().getReceiptUrl()
                                    );
                                }
                            } else {
                                Toast.makeText(
                                        GroupTransactionsActivity.this,
                                        resolveBeMessage(response, "Upload hóa đơn thất bại!"),
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<TransactionListItem>> call, Throwable t) {
                            Log.e("API_ERROR", "uploadReceiptForTransaction: " + t.getMessage());
                            Toast.makeText(GroupTransactionsActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            Log.e("RECEIPT_UPLOAD", "Cannot prepare file: " + e.getMessage(), e);
            Toast.makeText(this, "Không đọc được ảnh để upload!", Toast.LENGTH_SHORT).show();
        }
    }

    private File copyUriToTempFile(Uri uri) throws Exception {
        String ext = ".jpg";
        String mimeType = getContentResolver().getType(uri);
        if ("image/png".equalsIgnoreCase(mimeType)) ext = ".png";
        File file = File.createTempFile("receipt_", ext, getCacheDir());

        try (InputStream in = getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(file)) {
            if (in == null) throw new IllegalStateException("Input stream is null");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        }
        return file;
    }

    private void syncReceiptUrlToLists(String transactionId, String receiptUrl) {
        if (transactionId == null || transactionId.trim().isEmpty()) return;
        for (TransactionListItem item : allTransactions) {
            if (item != null && transactionId.equals(item.getId())) {
                item.setReceiptUrl(receiptUrl);
                break;
            }
        }
        for (TransactionListItem item : currentTransactions) {
            if (item != null && transactionId.equals(item.getId())) {
                item.setReceiptUrl(receiptUrl);
                break;
            }
        }
        transactionAdapter.setTransactions(currentTransactions);
        updateEmptyState();
    }

    private void replaceTransactionInLists(String transactionId, TransactionListItem newData) {
        if (transactionId == null || newData == null) return;
        for (int i = 0; i < allTransactions.size(); i++) {
            TransactionListItem old = allTransactions.get(i);
            if (old != null && transactionId.equals(old.getId())) {
                allTransactions.set(i, newData);
                break;
            }
        }
        for (int i = 0; i < currentTransactions.size(); i++) {
            TransactionListItem old = currentTransactions.get(i);
            if (old != null && transactionId.equals(old.getId())) {
                currentTransactions.set(i, newData);
                break;
            }
        }
        transactionAdapter.setTransactions(currentTransactions);
        updateEmptyState();
    }

    private String findWalletIdByName(String walletName) {
        if (walletName == null) return null;
        for (Wallet w : allWallets) {
            if (w != null && w.getName() != null && w.getName().equalsIgnoreCase(walletName)) {
                return w.getId();
            }
        }
        return null;
    }

    private String findWalletNameById(String walletId) {
        if (walletId == null) return null;
        for (Wallet w : allWallets) {
            if (w != null && walletId.equals(w.getId())) return w.getName();
        }
        return null;
    }

    private String findCategoryIdByNameAndType(String categoryName, String categoryType) {
        if (categoryName == null) return null;
        for (Category c : allCategories) {
            if (c == null || c.getName() == null) continue;
            boolean nameMatch = c.getName().equalsIgnoreCase(categoryName);
            boolean typeMatch = categoryType == null || c.getType() == null || c.getType().equalsIgnoreCase(categoryType);
            if (nameMatch && typeMatch) return c.getId();
        }
        return null;
    }

    private Category findCategoryById(String categoryId) {
        if (categoryId == null) return null;
        for (Category c : allCategories) {
            if (c != null && categoryId.equals(c.getId())) return c;
        }
        return null;
    }

    private void loadCategories() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getCategories("Bearer " + token).enqueue(new Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Category>>> call, Response<ApiResponse<List<Category>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    allCategories.clear();
                    allCategories.addAll(response.body().getData());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Category>>> call, Throwable t) {
                Log.e("API_ERROR", "loadCategories: " + t.getMessage());
            }
        });
    }

    private void loadWallets() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getWallets("Bearer " + token).enqueue(new Callback<ApiResponse<List<Wallet>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Wallet>>> call, Response<ApiResponse<List<Wallet>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    allWallets.clear();
                    allWallets.addAll(response.body().getData());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Wallet>>> call, Throwable t) {
                Log.e("API_ERROR", "loadWallets: " + t.getMessage());
            }
        });
    }

    private void refreshGroupTransactionsImmediately() {
        fetchTransactionsByGroup(selectedGroupId);
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

    private void submitSearch() {
        applyLocalFilter();
    }

    private void fetchTransactionsByGroup(String groupId) {
        if (token == null || token.trim().isEmpty()) {
            Toast.makeText(this, "Chưa có token!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (groupId == null || groupId.trim().isEmpty()) {
            Toast.makeText(this, "Thiếu groupId!", Toast.LENGTH_SHORT).show();
            return;
        }

        int page = 0;
        int size = 5;

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getTransactionsByGroup("Bearer " + token, groupId, page, size)
                .enqueue(new Callback<ApiResponse<TransactionsPageData>>() {
            @Override
            public void onResponse(Call<ApiResponse<TransactionsPageData>> call, Response<ApiResponse<TransactionsPageData>> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().getData() != null
                        && response.body().getData().getContent() != null) {
                    allTransactions.clear();
                    allTransactions.addAll(response.body().getData().getContent());
                    applyLocalFilter();
                } else {
                    Toast.makeText(GroupTransactionsActivity.this, "Không lấy được giao dịch nhóm!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<TransactionsPageData>> call, Throwable t) {
                Log.e("API_ERROR", "fetchTransactionsByGroup: " + t.getMessage());
                Toast.makeText(GroupTransactionsActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyLocalFilter() {
        String keyword = binding.etSearchNote.getText() != null
                ? binding.etSearchNote.getText().toString().trim()
                : "";
        boolean isSearch = !keyword.isEmpty();
        boolean hasTypeFilter = !"ALL".equalsIgnoreCase(selectedType);

        currentTransactions.clear();
        for (TransactionListItem item : allTransactions) {
            if (item == null) continue;
            boolean typeMatch = !hasTypeFilter
                    || (item.getCategoryType() != null
                    && item.getCategoryType().equalsIgnoreCase(selectedType));
            boolean keywordMatch = !isSearch
                    || (item.getNote() != null
                    && item.getNote().toLowerCase().contains(keyword.toLowerCase()));
            if (typeMatch && keywordMatch) {
                currentTransactions.add(item);
            }
        }
        transactionAdapter.setTransactions(currentTransactions);
        updateEmptyState();
    }

    private void updateEmptyState() {
        binding.tvEmptyTransactions.setVisibility(currentTransactions.isEmpty() ? View.VISIBLE : View.GONE);
    }

    public static Intent newIntent(Context context, String groupId, String groupName) {
        Intent i = new Intent(context, GroupTransactionsActivity.class);
        i.putExtra(EXTRA_GROUP_ID, groupId);
        i.putExtra(EXTRA_GROUP_NAME, groupName);
        return i;
    }
}
