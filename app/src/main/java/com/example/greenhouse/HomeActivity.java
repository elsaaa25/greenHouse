package com.example.greenhouse;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private TextView tvTabSuhu, tvTabKelembapan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 1. Inisialisasi TextView untuk Tab
        tvTabSuhu = findViewById(R.id.tvTabSuhu);
        tvTabKelembapan = findViewById(R.id.tvTabKelembapan);

        // 2. Set status awal: Suhu terpilih secara default
        if (tvTabSuhu != null) {
            tvTabSuhu.setSelected(true);
        }

        // 3. Logika klik untuk Tab Suhu
        if (tvTabSuhu != null) {
            tvTabSuhu.setOnClickListener(v -> {
                tvTabSuhu.setSelected(true);
                if (tvTabKelembapan != null) {
                    tvTabKelembapan.setSelected(false);
                }
                // Tambahkan logika untuk mengganti data grafik ke Suhu di sini
            });
        }

        // 4. Logika klik untuk Tab Kelembapan
        if (tvTabKelembapan != null) {
            tvTabKelembapan.setOnClickListener(v -> {
                tvTabKelembapan.setSelected(true);
                if (tvTabSuhu != null) {
                    tvTabSuhu.setSelected(false);
                }
                // Tambahkan logika untuk mengganti data grafik ke Kelembapan di sini
            });
        }
    }
}
