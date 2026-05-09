package com.example.greenhouse;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.greenhouse.databinding.ActivityMainBinding;
import com.example.greenhouse.fragment.GraphActivity;
import com.example.greenhouse.fragment.HomeFragment;
import com.example.greenhouse.fragment.ProfileActivity;

public class BottomNavigasi {
    private final AppCompatActivity activity;
    private final ActivityMainBinding binding;

    public BottomNavigasi(AppCompatActivity activity, ActivityMainBinding binding) {
        this.activity = activity;
        this.binding = binding;
    }

    public void setup() {
        // Set default fragment
        replaceFragment(new HomeFragment());

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                replaceFragment(new HomeFragment());
            } else if (itemId == R.id.grafik) {
                replaceFragment(new GraphActivity());
            } else if (itemId == R.id.notifikasi) {
                replaceFragment(new HomeFragment());
            } else if (itemId == R.id.profil) {
                replaceFragment(new ProfileActivity());
            }
            return true;
        });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }
}
