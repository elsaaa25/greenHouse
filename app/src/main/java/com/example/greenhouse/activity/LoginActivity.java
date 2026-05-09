package com.example.greenhouse.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.greenhouse.R;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inisialisasi view berdasarkan ID yang ada di activity_login.xml
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        // Logika tombol Login
        if (btnLogin != null) {
            btnLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String email = etEmail.getText().toString().trim();
                    String password = etPassword.getText().toString().trim();

                    if (email.isEmpty() || password.isEmpty()) {
                        Toast.makeText(LoginActivity.this, "Silakan isi email dan password", Toast.LENGTH_SHORT).show();
                    } else {
                        // Di sini Anda bisa menambahkan logika untuk ke MainActivity setelah login sukses
                        Toast.makeText(LoginActivity.this, "Login Berhasil", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        // Logika teks Daftar (Pindah ke RegistActivity)
        if (tvRegister != null) {
            tvRegister.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(LoginActivity.this, RegistActivity.class);
                    startActivity(intent);
                }
            });
        }
    }
}
