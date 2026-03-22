package com.example.homiefinanceapp.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.homiefinanceapp.databinding.ActivityAddTransactionBinding;

public class AddTransactionActivity extends AppCompatActivity {
    private ActivityAddTransactionBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddTransactionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Xử lý nút Back
        binding.btnBack.setOnClickListener(v -> finish());

        // Nhấn chọn Danh mục (Nhánh sau sẽ mở CategoryActivity lên để lấy kết quả)
        binding.layoutSelectCategory.setOnClickListener(v -> {
            Toast.makeText(this, "[Nhánh sau] Sẽ mở màn hình chọn Danh Mục!", Toast.LENGTH_SHORT).show();
        });

        // Nhấn chọn Ví
        binding.layoutSelectWallet.setOnClickListener(v -> {
            Toast.makeText(this, "[Nhánh sau] Sẽ mở BottomSheet chọn Ví!", Toast.LENGTH_SHORT).show();
        });

        // Nhấn Lưu Giao Dịch
        binding.btnSaveTransaction.setOnClickListener(v -> {
            String amount = binding.etAmount.getText().toString();
            String note = binding.etNote.getText().toString();
            boolean isExpense = binding.rbExpense.isChecked();

            if (amount.isEmpty()) {
                Toast.makeText(this, "Homie chưa nhập số tiền kìa!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Giả lập lưu thành công trước khi có API
            Toast.makeText(this, "Đã lưu " + (isExpense ? "Chi tiêu" : "Thu nhập") + " thành công!", Toast.LENGTH_SHORT).show();
            finish(); // Thoát màn hình
        });
    }
}