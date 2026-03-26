package com.example.homiefinanceapp.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.homiefinanceapp.databinding.ActivityReceiptPreviewBinding;

public class ReceiptPreviewActivity extends AppCompatActivity {
    public static final String EXTRA_RECEIPT_URL = "extra_receipt_url";

    private ActivityReceiptPreviewBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReceiptPreviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String url = getIntent().getStringExtra(EXTRA_RECEIPT_URL);
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(this, "Khong co anh hoa don!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Glide.with(this)
                .load(url)
                .into(binding.ivReceiptZoom);

        binding.btnClose.setOnClickListener(v -> finish());
        binding.ivReceiptZoom.setOnClickListener(v -> finish());
    }

    public static Intent newIntent(Context context, String receiptUrl) {
        Intent intent = new Intent(context, ReceiptPreviewActivity.class);
        intent.putExtra(EXTRA_RECEIPT_URL, receiptUrl);
        return intent;
    }
}

