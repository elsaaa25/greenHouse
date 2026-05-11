package com.example.greenhouse.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.greenhouse.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuthException;

public class LoadingActivity extends AppCompatActivity {

    // Komponen dari activity_loading.xml
    private EditText etSearch;
    private View btnPakcoy, btnTomat, btnCabai, btnWortel;
    private Button btnStart;

    // Parent CardView dari masing-masing tanaman
    // Digunakan agar fitur search bisa menyembunyikan card penuh
    private View cardPakcoy, cardTomat, cardCabai, cardWortel;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // Data register dari RegistActivity
    private String nickName, fullName, email, password;

    // Data tanaman yang dipilih user
    private String selectedPlant = "";
    private String selectedPlantLatin = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        // Menghubungkan Java dengan ID XML
        etSearch = findViewById(R.id.etSearch);

        btnPakcoy = findViewById(R.id.btnPakcoy);
        btnTomat = findViewById(R.id.btnTomat);
        btnCabai = findViewById(R.id.btnCabai);
        btnWortel = findViewById(R.id.btnWortel);

        btnStart = findViewById(R.id.btnStart);
        btnStart.setText("Daftar Sekarang");

        // Mengambil parent CardView dari LinearLayout tanaman
        cardPakcoy = (View) btnPakcoy.getParent();
        cardTomat = (View) btnTomat.getParent();
        cardCabai = (View) btnCabai.getParent();
        cardWortel = (View) btnWortel.getParent();

        // Inisialisasi Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Ambil data dari RegistActivity
        boolean dataValid = ambilDataDariRegister();

        if (!dataValid) {
            finish();
            return;
        }

        // Pilih tanaman
        btnPakcoy.setOnClickListener(v ->
                pilihTanaman("Pakcoy", "Brassica rapa", btnPakcoy)
        );

        btnTomat.setOnClickListener(v ->
                pilihTanaman("Tomat", "Solanum lycopersicum", btnTomat)
        );

        btnCabai.setOnClickListener(v ->
                pilihTanaman("Cabai", "Capsicum annuum", btnCabai)
        );

        btnWortel.setOnClickListener(v ->
                pilihTanaman("Wortel", "Daucus carota", btnWortel)
        );

        // Tombol daftar sekarang
        btnStart.setOnClickListener(v -> daftarSekarang());

        // Fitur search tanaman
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Tidak digunakan
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTanaman(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Tidak digunakan
            }
        });
    }

    private boolean ambilDataDariRegister() {
        nickName = getIntent().getStringExtra("nickName");
        fullName = getIntent().getStringExtra("fullName");
        email = getIntent().getStringExtra("email");
        password = getIntent().getStringExtra("password");

        // Jika data kosong berarti LoadingActivity tidak dibuka dari RegistActivity
        if (nickName == null || fullName == null || email == null || password == null) {
            Toast.makeText(this, "Data register tidak lengkap", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void pilihTanaman(String namaTanaman, String namaLatin, View selectedButton) {
        // Simpan tanaman yang dipilih
        selectedPlant = namaTanaman;
        selectedPlantLatin = namaLatin;

        // Reset semua pilihan
        resetPilihanTanaman();

        // Beri efek pada tanaman terpilih
        selectedButton.setAlpha(0.65f);

        Toast.makeText(this, namaTanaman + " dipilih", Toast.LENGTH_SHORT).show();
    }

    private void resetPilihanTanaman() {
        btnPakcoy.setAlpha(1f);
        btnTomat.setAlpha(1f);
        btnCabai.setAlpha(1f);
        btnWortel.setAlpha(1f);
    }

    private void filterTanaman(String keyword) {
        keyword = keyword.toLowerCase().trim();

        setCardVisibility(cardPakcoy, "pakcoy brassica rapa", keyword);
        setCardVisibility(cardTomat, "tomat solanum lycopersicum", keyword);
        setCardVisibility(cardCabai, "cabai capsicum annuum", keyword);
        setCardVisibility(cardWortel, "wortel daucus carota", keyword);
    }

    private void setCardVisibility(View card, String dataTanaman, String keyword) {
        if (keyword.isEmpty() || dataTanaman.contains(keyword)) {
            card.setVisibility(View.VISIBLE);
        } else {
            card.setVisibility(View.GONE);
        }
    }

    private void daftarSekarang() {
        // User wajib memilih tanaman
        if (selectedPlant.isEmpty()) {
            Toast.makeText(this, "Pilih tanaman terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        // Matikan tombol agar tidak double klik
        btnStart.setEnabled(false);

        // Buat akun Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {

                    if (auth.getCurrentUser() == null) {
                        btnStart.setEnabled(true);
                        Toast.makeText(this, "Gagal mendapatkan data user", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String uid = auth.getCurrentUser().getUid();

                    // Data yang akan disimpan ke Firestore
                    Map<String, Object> user = new HashMap<>();
                    user.put("uid", uid);
                    user.put("nickName", nickName);
                    user.put("fullName", fullName);
                    user.put("email", email);
                    user.put("selectedPlant", selectedPlant);
                    user.put("selectedPlantLatin", selectedPlantLatin);
                    user.put("createdAt", FieldValue.serverTimestamp());

                    // Simpan data ke Firestore
                    db.collection("users")
                            .document(uid)
                            .set(user)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(
                                        LoadingActivity.this,
                                        "Daftar berhasil",
                                        Toast.LENGTH_SHORT
                                ).show();

                                masukKeMain();
                            })
                            .addOnFailureListener(e -> {
                                btnStart.setEnabled(true);

                                Toast.makeText(
                                        LoadingActivity.this,
                                        "Gagal menyimpan data: " + e.getMessage(),
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnStart.setEnabled(true);

                    e.printStackTrace();

                    Toast.makeText(
                            LoadingActivity.this,
                            "Daftar gagal: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void masukKeMain() {
        Intent intent = new Intent(LoadingActivity.this, MainActivity.class);

        // Hapus halaman sebelumnya dari back stack
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }
}
