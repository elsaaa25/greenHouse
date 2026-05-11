package com.example.greenhouse.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.greenhouse.R;

public class RegistActivity extends AppCompatActivity {

    private EditText etNickName, etFullName, etEmail, etPassword;
    private Button btnRegister;
    private TextView tvLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regist);

        etNickName = findViewById(R.id.etNickName);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

// Tombol register diubah fungsinya menjadi lanjut ke pilih tanaman
        btnRegister.setText("Lanjutkan");

        // Klik tombol lanjutkan
        btnRegister.setOnClickListener(v -> lanjutKePilihTanaman());

        // Klik teks masuk, kembali ke LoginActivity
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegistActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void lanjutKePilihTanaman() {
        String nickName = etNickName.getText().toString().trim();
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validasi nama panggilan
        if (nickName.isEmpty()) {
            etNickName.setError("Nama panggilan wajib diisi");
            etNickName.requestFocus();
            return;
        }

        // Validasi nama lengkap
        if (fullName.isEmpty()) {
            etFullName.setError("Nama lengkap wajib diisi");
            etFullName.requestFocus();
            return;
        }

        // Validasi email kosong
        if (email.isEmpty()) {
            etEmail.setError("Email wajib diisi");
            etEmail.requestFocus();
            return;
        }

        // Validasi format email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Format email tidak valid");
            etEmail.requestFocus();
            return;
        }

        // Validasi password kosong
        if (password.isEmpty()) {
            etPassword.setError("Password wajib diisi");
            etPassword.requestFocus();
            return;
        }

        // Validasi panjang password
        if (password.length() < 8) {
            etPassword.setError("Password minimal harus 8 karakter");
            etPassword.requestFocus();
            return;
        }

        // Kirim data ke LoadingActivity.
        // Akun Firebase belum dibuat di sini.
        Intent intent = new Intent(RegistActivity.this, LoadingActivity.class);
        intent.putExtra("nickName", nickName);
        intent.putExtra("fullName", fullName);
        intent.putExtra("email", email);
        intent.putExtra("password", password);
        startActivity(intent);
    }
}
