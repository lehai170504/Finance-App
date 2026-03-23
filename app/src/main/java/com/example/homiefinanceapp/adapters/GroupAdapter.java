package com.example.homiefinanceapp.adapters;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homiefinanceapp.R;
import com.example.homiefinanceapp.models.Group;

import java.util.ArrayList;
import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {
    private List<Group> groupList = new ArrayList<>();
    private final String currentUserId; // 💡 Nhận userId của user đang đăng nhập
    private final OnGroupInteractionListener listener; // 💡 Interface listener

    // 💡 Interface để giao tiếp với Activity
    public interface OnGroupInteractionListener {
        void onOpenTransactionsClick(Group group);
        void onEditClick(Group group);
        void onDeleteClick(Group group);
        void onLeaveClick(Group group);
    }

    // 💡 Constructor cập nhật
    public GroupAdapter(String currentUserId, OnGroupInteractionListener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setGroups(List<Group> groups) {
        this.groupList = groups;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        Group group = groupList.get(position);

        holder.tvGroupName.setText(group.getName());
        holder.tvInviteCode.setText(group.getInviteCode());

        int memberCount = group.getMembers() != null ? group.getMembers().size() : 0;
        holder.tvGroupMembers.setText("Thành viên: " + memberCount);

        // 💡 LOGIC CỰC QUAN TRỌNG: So sánh quyền TRƯỞNG NHÓM (Admin)
        boolean isOwner = group.getOwner() != null && group.getOwner().getId().equals(currentUserId);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOpenTransactionsClick(group);
            }
        });

        // 💡 ẤN GIỮ VÀO ITEM ĐỂ HIỆN MENU TÙY CHỌN (Sửa/Xóa/Rời)
        holder.itemView.setOnLongClickListener(v -> {
            // Chuẩn bị các tùy chọn dựa vào quyền của user
            List<CharSequence> options = new ArrayList<>();
            if (isOwner) {
                // Chỉ Trưởng nhóm mới thấy 2 chức năng này
                options.add("✏️ Sửa tên nhóm");
                options.add("🗑 Xóa nhóm");
            } else {
                // Thành viên chỉ thấy Rời nhóm
                options.add("🚶 Rời khỏi nhóm");
            }

            // Hiện AlertDialog menu
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Tùy chọn cho nhóm: " + group.getName())
                    .setItems(options.toArray(new CharSequence[0]), (dialog, which) -> {
                        if (isOwner) {
                            // Logic cho Admin (mặc định whoich=0 là Sửa, which=1 là Xóa)
                            if (which == 0) listener.onEditClick(group); // Gọi hàm Sửa ở Activity
                            else if (which == 1) listener.onDeleteClick(group); // Gọi hàm Xóa ở Activity
                        } else {
                            // Logic cho Thành viên (chỉ có một option là Rời)
                            if (which == 0) listener.onLeaveClick(group); // Gọi hàm Rời ở Activity
                        }
                    })
                    .show();
            return true; // Trả về true để nó không gọi sự kiện click bình thường
        });
    }

    @Override
    public int getItemCount() {
        return groupList != null ? groupList.size() : 0;
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView tvGroupName, tvGroupMembers, tvInviteCode;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupName = itemView.findViewById(R.id.tvGroupName);
            tvGroupMembers = itemView.findViewById(R.id.tvGroupMembers);
            tvInviteCode = itemView.findViewById(R.id.tvInviteCode);
        }
    }
}