package com.example.greenhouse.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Patterns;

import androidx.appcompat.app.AppCompatActivity;

import com.example.greenhouse.R;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;

    // FirebaseAuth digunakan untuk proses login dengan email dan password
    private FirebaseAuth auth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inisialisasi view berdasarkan ID yang ada di activity_login.xml
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        auth = FirebaseAuth.getInstance();

        // Ketika tombol login diklik, jalankan fungsi loginUser()
        btnLogin.setOnClickListener(v -> loginUser());

        // Ketika teks daftar diklik, pindah ke halaman register
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegistActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Kalau user sudah login sebelumnya, langsung masuk MainActivity
        if (auth.getCurrentUser() != null) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

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

        // Mencegah tombol ditekan berkali-kali
        btnLogin.setEnabled(false);

        // Proses login Firebase
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    Toast.makeText(LoginActivity.this, "Login berhasil", Toast.LENGTH_SHORT).show();

                    // Setelah login berhasil masuk ke MainActivity
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    Toast.makeText(
                            LoginActivity.this,
                            "Login gagal: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }
}