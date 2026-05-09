package com.example.greenhouse;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import com.example.greenhouse.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inisialisasi dan setup Bottom Navigation
        BottomNavigasi bottomNavigasi = new BottomNavigasi(this, binding);
        bottomNavigasi.setup();
    }

    /**
     * Metode untuk memuat Fragment ke dalam frame_layout
     */
    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .addToBackStack(null) // Agar bisa kembali ke fragment sebelumnya dengan tombol back
                .commit();
    }
}
