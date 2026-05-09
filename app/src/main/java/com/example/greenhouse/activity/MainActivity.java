package com.example.greenhouse.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.greenhouse.R;
import com.example.greenhouse.fragment.NotifikasiFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Tampilkan HomeFragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameContainer, new NotifikasiFragment())
                .commit();
    }
}