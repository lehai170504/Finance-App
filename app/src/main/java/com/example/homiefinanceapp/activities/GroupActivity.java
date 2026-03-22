package com.example.homiefinanceapp.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.homiefinanceapp.adapters.GroupAdapter;
import com.example.homiefinanceapp.api.ApiService;
import com.example.homiefinanceapp.api.RetrofitClient;
import com.example.homiefinanceapp.databinding.ActivityGroupBinding;
import com.example.homiefinanceapp.models.ApiResponse;
import com.example.homiefinanceapp.models.Group;
import com.example.homiefinanceapp.models.GroupRequest;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupActivity extends AppCompatActivity {
    private ActivityGroupBinding binding;
    private GroupAdapter groupAdapter;
    private String token, currentUserId; // 💡 Thêm biến userId của tôi

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences prefs = getSharedPreferences("HomiePrefs", MODE_PRIVATE);
        token = prefs.getString("token", "");
        currentUserId = prefs.getString("user_id", ""); // 💡 Lấy userId của tôi từ Shared Preferences (Nhớ cập nhật MainActivity để lưu cái này nha homie!)

        binding.btnBack.setOnClickListener(v -> finish());

        // Nút Thêm nhóm màu xanh dương bự chà bá
        binding.btnAddGroup.setOnClickListener(v -> {
            showAddGroupDialog(null); // NULL mang ý nghĩa là TẠO MỚI nhóm
        });

        // Xử lý nút Tham gia nhóm
        binding.btnJoinGroup.setOnClickListener(v -> {
            showJoinGroupDialog();
        });

        setupRecyclerView();
        fetchMyGroups(); // Gọi API lấy dữ liệu ban đầu
    }

    private void setupRecyclerView() {
        // 💡 Cập nhật Constructor Adapter và implement Interface listener
        groupAdapter = new GroupAdapter(currentUserId, new GroupAdapter.OnGroupInteractionListener() {
            @Override
            public void onEditClick(Group group) {
                // Trưởng nhóm bấm Sửa -> Mở Dialog
                showAddGroupDialog(group); // Truyền object nhóm -> mang ý nghĩa là SỬA
            }

            @Override
            public void onDeleteClick(Group group) {
                // Trưởng nhóm bấm Xóa -> Hỏi lại cho chắc
                new AlertDialog.Builder(GroupActivity.this)
                        .setTitle("Xóa nhóm " + group.getName())
                        .setMessage("Homie có chắc muốn xóa nhóm không? Thành viên khác sẽ bị mất nhóm và không thể khôi phục nha.")
                        .setPositiveButton("Xóa", (dialog, which) -> deleteGroupApi(group.getId()))
                        .setNegativeButton("Hủy", null)
                        .show();
            }

            @Override
            public void onLeaveClick(Group group) {
                // Thành viên bấm Rời nhóm -> Hỏi lại cho chắc
                new AlertDialog.Builder(GroupActivity.this)
                        .setTitle("Rời nhóm " + group.getName())
                        .setMessage("Homie có chắc muốn rời khỏi không gian chung này không?")
                        .setPositiveButton("Rời nhóm", (dialog, which) -> leaveGroupApi(group.getId()))
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });
        binding.rvGroups.setLayoutManager(new LinearLayoutManager(this));
        binding.rvGroups.setAdapter(groupAdapter);
    }

    // ==========================================================
    // 💡 DIALOG DÙNG CHUNG CHO TẠO NHÓM MỚI VÀ CHỈNH SỬA TÊN NHÓM
    // ==========================================================
    private void showAddGroupDialog(Group groupToEdit) {
        // Tái sử dụng logic Wallet/Category Lượt trước cực chuyên nghiệp
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText etName = new EditText(this);
        etName.setHint("Tên nhóm (Ví dụ: Nhóm Ăn Chơi, Nhóm Bỉm Sữa...)");

        // Nếu là sửa thì điền tên cũ vào EditText
        if (groupToEdit != null) {
            etName.setText(groupToEdit.getName());
        }

        layout.addView(etName);

        String title = groupToEdit == null ? "Tạo không gian chung" : "Chỉnh sửa tên nhóm";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(layout)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Nhập tên nhóm vào nha homie!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (groupToEdit == null) {
                        // Gọi API TẠO MỚI
                        createGroupApi(name);
                    } else {
                        // Gọi API SỬA TÊN
                        updateGroupApi(groupToEdit.getId(), name);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ==========================================================
    // CÁC HÀM GỌI API (BAO GỒM CẢ GIẢ ĐỊNH)
    // ==========================================================

    private void fetchMyGroups() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getMyGroups("Bearer " + token).enqueue(new Callback<ApiResponse<List<Group>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Group>>> call, Response<ApiResponse<List<Group>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Group> groups = response.body().getData();
                    if (groups != null && !groups.isEmpty()) {
                        groupAdapter.setGroups(groups);
                    } else {
                        Toast.makeText(GroupActivity.this, "Bạn chưa tham gia nhóm nào!", Toast.LENGTH_SHORT).show();
                        groupAdapter.setGroups(new ArrayList<>()); // Trả list rỗng để F5 UI
                    }
                } else {
                    Toast.makeText(GroupActivity.this, "Lỗi lấy dữ liệu nhóm!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Group>>> call, Throwable t) {
                Log.e("API_ERROR", "Lỗi mạng: " + t.getMessage());
                Toast.makeText(GroupActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createGroupApi(String name) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.createGroup("Bearer " + token, name).enqueue(new Callback<ApiResponse<Group>>() {
            @Override
            public void onResponse(Call<ApiResponse<Group>> call, Response<ApiResponse<Group>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(GroupActivity.this, "Tạo nhóm thành công! 🎉", Toast.LENGTH_SHORT).show();
                    fetchMyGroups(); // Load lại danh sách nhóm ngay lập tức
                } else {
                    Toast.makeText(GroupActivity.this, "Lỗi tạo nhóm!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Group>> call, Throwable t) {
                Toast.makeText(GroupActivity.this, "Lỗi kết nối!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateGroupApi(String groupId, String name) {
        GroupRequest request = new GroupRequest(name);
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.updateGroup("Bearer " + token, groupId, request).enqueue(new Callback<ApiResponse<Group>>() {
            @Override
            public void onResponse(Call<ApiResponse<Group>> call, Response<ApiResponse<Group>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(GroupActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    fetchMyGroups(); // Load lại
                } else {
                    Toast.makeText(GroupActivity.this, "Không thể cập nhật! Lỗi API giả định!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Group>> call, Throwable t) {
                Toast.makeText(GroupActivity.this, "Lỗi kết nối API giả định!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteGroupApi(String groupId) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.deleteGroup("Bearer " + token, groupId).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(GroupActivity.this, "Đã xóa nhóm!", Toast.LENGTH_SHORT).show();
                    fetchMyGroups(); // Load lại
                } else {
                    Toast.makeText(GroupActivity.this, "Lỗi xóa nhóm giả định!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Toast.makeText(GroupActivity.this, "Lỗi kết nối API giả định!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void leaveGroupApi(String groupId) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.leaveGroup("Bearer " + token, groupId).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(GroupActivity.this, "Rời nhóm cái vèo! 🚶", Toast.LENGTH_SHORT).show();
                    fetchMyGroups(); // Load lại
                } else {
                    Toast.makeText(GroupActivity.this, "Rời nhóm thất bại!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                Toast.makeText(GroupActivity.this, "Lỗi mạng!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void joinGroupApi(String inviteCode) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.joinGroup("Bearer " + token, inviteCode).enqueue(new Callback<ApiResponse<Group>>() {
            @Override
            public void onResponse(Call<ApiResponse<Group>> call, Response<ApiResponse<Group>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Group joinedGroup = response.body().getData();
                    Toast.makeText(GroupActivity.this, "Gia nhập thành công nhóm: " + joinedGroup.getName() + " 🎉", Toast.LENGTH_SHORT).show();
                    fetchMyGroups(); // Load lại danh sách nhóm để hiện nhóm mới
                } else {
                    // Xử lý lỗi (Ví dụ mã sai, hoặc đã ở trong nhóm rồi)
                    Toast.makeText(GroupActivity.this, "Mã mời không đúng hoặc homie đã ở trong nhóm này rồi!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Group>> call, Throwable t) {
                Toast.makeText(GroupActivity.this, "Lỗi mạng! Không thể tham gia nhóm.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showJoinGroupDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText etCode = new EditText(this);
        etCode.setHint("Nhập mã mời (Ví dụ: 4FC1D9)");
        layout.addView(etCode);

        new AlertDialog.Builder(this)
                .setTitle("Tham gia nhóm")
                .setView(layout)
                .setPositiveButton("Tham gia", (dialog, which) -> {
                    String code = etCode.getText().toString().trim();
                    if (code.isEmpty()) {
                        Toast.makeText(this, "Nhập mã mời vào homie ơi!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    joinGroupApi(code);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}