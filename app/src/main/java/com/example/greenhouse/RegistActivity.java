package com.example.greenhouse;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

        if (btnRegister != null) {
            btnRegister.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String nickName = etNickName.getText().toString().trim();
                    String fullName = etFullName.getText().toString().trim();
                    String email = etEmail.getText().toString().trim();
                    String password = etPassword.getText().toString().trim();

                    if (nickName.isEmpty() || fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                        Toast.makeText(RegistActivity.this, "Mohon lengkapi semua data", Toast.LENGTH_SHORT).show();
                    } else if (password.length() < 8) {
                        // Validasi minimal 8 karakter untuk password
                        etPassword.setError("Password minimal harus 8 karakter");
                        etPassword.requestFocus();
                    } else {
                        // Pindah ke LoadingActivity setelah klik Daftar
                        Intent intent = new Intent(RegistActivity.this, LoadingActivity.class);
                        startActivity(intent);
                    }
                }
            });
        }

        if (tvLogin != null) {
            tvLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(RegistActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
        }
    }
}
