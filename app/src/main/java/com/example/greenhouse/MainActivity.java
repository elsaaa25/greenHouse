package com.example.greenhouse;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

/**
 * MainActivity berfungsi sebagai entry point.
 * Di sini kita langsung mengarahkan user ke LoginActivity.
 */
public class MainActivity extends AppCompatActivity {

    private Button btnGoToProfile;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inisialisasi tombol berdasarkan Profile Activity ID yang ada di XML
        btnGoToProfile = findViewById(R.id.goToProfile);

        // Memberikan logika saat tombol ditekan
        btnGoToProfile.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
//              Berpindah ke ProfileActivity
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
            }
        });
    }
}