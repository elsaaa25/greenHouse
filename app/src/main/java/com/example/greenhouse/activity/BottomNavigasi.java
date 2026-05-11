package com.example.greenhouse.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.greenhouse.R;
import com.example.greenhouse.databinding.ActivityMainBinding;
import com.example.greenhouse.fragment.GraphFragment;
import com.example.greenhouse.fragment.HomeFragment;
import com.example.greenhouse.fragment.NotifikasiFragment;
import com.example.greenhouse.fragment.ProfileFragment;

public class BottomNavigasi {

    // Activity tempat BottomNavigation berada
    private final AppCompatActivity activity;

    // Binding dari activity_main.xml
    private final ActivityMainBinding binding;

    public BottomNavigasi(AppCompatActivity activity, ActivityMainBinding binding) {
        this.activity = activity;
        this.binding = binding;
    }

    public void setup() {

        // Menampilkan HomeFragment sebagai halaman pertama
        replaceFragment(new HomeFragment());

        // Mengatur aksi ketika item bottom navigation diklik
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {

            int itemId = item.getItemId();

            // Menu Beranda
            if (itemId == R.id.home) {

                replaceFragment(new HomeFragment());
                return true;

            }

            // Menu Grafik
            else if (itemId == R.id.grafik) {

                replaceFragment(new GraphFragment());
                return true;

            }

            // Menu Notifikasi
            else if (itemId == R.id.notifikasi) {

                replaceFragment(new NotifikasiFragment());
                return true;

            }

            // Menu Profil
            else if (itemId == R.id.profil) {

                replaceFragment(new ProfileFragment());
                return true;

            }

            return false;
        });
    }

    // Fungsi untuk mengganti fragment di dalam frame_layout
    private void replaceFragment(Fragment fragment) {

        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // frame_layout berasal dari activity_main.xml
        fragmentTransaction.replace(R.id.frame_layout, fragment);

        // Tidak memakai addToBackStack supaya bottom navigation tidak menumpuk history fragment
        fragmentTransaction.commit();
    }
}