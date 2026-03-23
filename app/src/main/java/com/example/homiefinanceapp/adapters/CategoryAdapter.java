package com.example.homiefinanceapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homiefinanceapp.R;
import com.example.homiefinanceapp.models.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    private List<Category> categoryList = new ArrayList<>();

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    private final OnCategoryClickListener clickListener;

    public CategoryAdapter(OnCategoryClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public CategoryAdapter() {
        this.clickListener = null;
    }

    public void setCategories(List<Category> categories) {
        this.categoryList = categories;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.tvCategoryName.setText(category.getName());

        // Load Icon
        String iconName = category.getIcon();
        if (iconName == null) iconName = "";
        iconName = iconName.trim();
        // Normalize nếu backend trả kèm đuôi hoặc path
        if (iconName.endsWith(".png") || iconName.endsWith(".webp") || iconName.endsWith(".jpg") || iconName.endsWith(".jpeg")) {
            iconName = iconName.substring(0, iconName.lastIndexOf('.'));
        }
        if (iconName.contains("/")) {
            iconName = iconName.substring(iconName.lastIndexOf('/') + 1);
        }

        int resId = holder.itemView.getContext().getResources()
                .getIdentifier(iconName, "drawable", holder.itemView.getContext().getPackageName());

        if (resId != 0) {
            holder.ivCategoryIcon.setImageResource(resId);
        } else {
            // Fallback sang icon có sẵn để vẫn hiển thị "icon"
            int fallbackRes = getFallbackAndroidIconRes(iconName);
            if (fallbackRes != 0) {
                holder.ivCategoryIcon.setImageResource(fallbackRes);
            } else {
                holder.ivCategoryIcon.setImageResource(android.R.drawable.ic_menu_help);
            }
        }

        // Click thường: chỉ toast (để bạn phân biệt với long-press)
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onCategoryClick(category);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categoryList != null ? categoryList.size() : 0;
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCategoryIcon;
        TextView tvCategoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
        }
    }

    private int getFallbackAndroidIconRes(String iconName) {
        if (iconName == null || iconName.isEmpty()) return 0;
        switch (iconName.toLowerCase()) {
            case "ic_fastfood":
                return android.R.drawable.ic_menu_crop;
            case "ic_transport":
                return android.R.drawable.ic_menu_compass;
            case "ic_shopping":
                return android.R.drawable.ic_menu_slideshow;
            case "ic_health":
                return android.R.drawable.ic_menu_edit;
            case "ic_education":
                return android.R.drawable.ic_menu_agenda;
            case "ic_entertainment":
                return android.R.drawable.ic_media_play;
            case "ic_rent":
                return android.R.drawable.ic_menu_myplaces;
            case "ic_bills":
                return android.R.drawable.ic_menu_manage;
            case "ic_default":
            default:
                return android.R.drawable.ic_menu_help;
        }
    }
}