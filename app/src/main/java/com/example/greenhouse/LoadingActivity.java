package com.example.greenhouse;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class LoadingActivity extends AppCompatActivity {

    private LinearLayout btnPakcoy, btnTomat, btnCabai, btnWortel;
    private Button btnStart;
    private List<View> plantButtons = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        // Inisialisasi View
        btnPakcoy = findViewById(R.id.btnPakcoy);
        btnTomat = findViewById(R.id.btnTomat);
        btnCabai = findViewById(R.id.btnCabai);
        btnWortel = findViewById(R.id.btnWortel);
        btnStart = findViewById(R.id.btnStart);

        // Masukkan ke list untuk memudahkan pengelolaan state
        plantButtons.add(btnPakcoy);
        plantButtons.add(btnTomat);
        plantButtons.add(btnCabai);
        plantButtons.add(btnWortel);

        // Set Click Listener untuk setiap pilihan
        for (View view : plantButtons) {
            if (view != null) {
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selectPlant(v);
                    }
                });
            }
        }

        // Logika tombol Mulai Budidaya
        if (btnStart != null) {
            btnStart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Tampilkan notifikasi berhasil daftar
                    Toast.makeText(LoadingActivity.this, "Berhasil Daftar! Selamat berbudidaya.", Toast.LENGTH_LONG).show();
                    
                    // Navigasi ke BerandaActivity atau MainActivity
//                    Intent intent = new Intent(LoadingActivity.this, BerandaActivity.class);
//                    startActivity(intent);
//                    finish();
                }
            });
        }
    }

    private void selectPlant(View selectedView) {
        // Reset semua pilihan
        for (View view : plantButtons) {
            if (view != null) {
                view.setSelected(false);
            }
        }
        // Set pilihan yang diklik
        selectedView.setSelected(true);
    }
}
