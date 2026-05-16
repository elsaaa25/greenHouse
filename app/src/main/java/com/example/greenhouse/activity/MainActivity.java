package com.example.greenhouse.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.greenhouse.R;
import com.example.greenhouse.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // BottomNavigasi akan otomatis membuka HomeFragment
        BottomNavigasi bottomNavigasi = new BottomNavigasi(this, binding);
        bottomNavigasi.setup();
    }

    /**
     * Fungsi ini dipakai ketika fragment ingin membuka fragment lain.
     * Contoh: ProfileFragment membuka AccountUserFragment.
     */
    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()

                // Animasi saat masuk fragment baru dan saat kembali
                .setCustomAnimations(
                        R.anim.slide_up,    // Masuk: Geser ke atas
                        R.anim.stay,        // Keluar: Diam (halaman profile tetap di bawah)
                        R.anim.stay,        // Pop Masuk: Diam
                        R.anim.slide_down   // Pop Keluar: Geser ke bawah (halaman Info Akun turun)
                )

                // Ganti isi frame_layout dengan fragment baru
                .replace(R.id.frame_layout, fragment)

                // Simpan fragment sebelumnya agar bisa kembali
                .addToBackStack(null)

                .commit();
    }
}