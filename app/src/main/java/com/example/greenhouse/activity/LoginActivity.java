package com.example.greenhouse.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Patterns;

import androidx.appcompat.app.AppCompatActivity;

import com.example.greenhouse.R;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    // Gunakan AutoCompleteTextView untuk email
    private AutoCompleteTextView etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private FirebaseAuth auth;

    // Konstanta untuk SharedPreferences Saran Akun
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_ACCOUNT_LIST = "accountList";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Inisialisasi View
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        auth = FirebaseAuth.getInstance();

        // 2. Tampilkan daftar email yang pernah login sebelumnya
        setupAccountSuggestions();

        // 3. Tombol Login
        btnLogin.setOnClickListener(v -> loginUser());

        // 4. Navigasi ke Register
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegistActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validasi (Email & Password)
        if (email.isEmpty()) { etEmail.setError("Email wajib diisi"); return; }
        if (password.isEmpty()) { etPassword.setError("Password wajib diisi"); return; }

        btnLogin.setEnabled(false);

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    // SIMPAN EMAIL KE RIWAYAT SAAT LOGIN BERHASIL
                    saveAccountToHistory(email);

                    Toast.makeText(LoginActivity.this, "Login berhasil", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Login gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupAccountSuggestions() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedAccounts = prefs.getString(KEY_ACCOUNT_LIST, "");

        if (!savedAccounts.isEmpty()) {
            String[] accountArray = savedAccounts.split(",");
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, accountArray);
            etEmail.setAdapter(adapter);
        }
    }

    private void saveAccountToHistory(String email) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedAccounts = prefs.getString(KEY_ACCOUNT_LIST, "");

        List<String> accountList = new ArrayList<>(Arrays.asList(savedAccounts.split(",")));

        if (!accountList.contains(email)) {
            if (savedAccounts.isEmpty()) {
                savedAccounts = email;
            } else {
                savedAccounts = savedAccounts + "," + email;
            }
            prefs.edit().putString(KEY_ACCOUNT_LIST, savedAccounts).apply();
            setupAccountSuggestions(); // Refresh daftar
        }
    }
}