package com.example.greenhouse.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.greenhouse.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigasi bottomNavigasi =
                new BottomNavigasi(this, binding);

        bottomNavigasi.setup();
    }
}