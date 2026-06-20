package com.example.greenhouse.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.greenhouse.R;
import com.example.greenhouse.adapter.PlantAdapter;
import com.example.greenhouse.model.Plant;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.util.Log;

public class LoadingActivity extends AppCompatActivity {

    // Komponen dari activity_loading.xml
    private EditText etSearch;
    private Button btnStart;
    private RecyclerView rvPlants;

    // Adapter untuk RecyclerView
    private PlantAdapter adapter;
    private List<Plant> plantList = new ArrayList<>();

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // Data register dari RegistActivity
    private String nickName, fullName, email, password;

    // Data tanaman yang dipilih user (Join Reference)
    private String selectedPlantId = "";
    private String selectedPlantName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        // Menghubungkan Java dengan ID XML
        etSearch = findViewById(R.id.etSearch);
        btnStart = findViewById(R.id.btnStart);
        rvPlants = findViewById(R.id.rvPlants);

        btnStart.setText("Daftar Sekarang");

        // Inisialisasi Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Ambil data dari RegistActivity
        boolean dataValid = ambilDataDariRegister();
        if (!dataValid) {
            finish();
            return;
        }

        setupRecyclerView();
        fetchPlantsFromFirestore();

        // Tombol daftar sekarang
        btnStart.setOnClickListener(v -> daftarSekarang());

        // Fitur search tanaman
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new PlantAdapter();
        rvPlants.setLayoutManager(new GridLayoutManager(this, 2));
        rvPlants.setAdapter(adapter);

        adapter.setOnPlantClickListener(plant -> {
            selectedPlantId = plant.getId();
            selectedPlantName = plant.getPlantName();
            adapter.setSelectedId(selectedPlantId);
            Toast.makeText(this, selectedPlantName + " dipilih", Toast.LENGTH_SHORT).show();
        });
    }

    private void fetchPlantsFromFirestore() {
        db.collection("plants")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    plantList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Plant plant = document.toObject(Plant.class);
                        plantList.add(plant);
                    }
                    adapter.setPlants(plantList);
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE", "Error fetching plants", e);
                    Toast.makeText(this, "Gagal mengambil data tanaman", Toast.LENGTH_SHORT).show();
                });
    }

    private boolean ambilDataDariRegister() {
        nickName = getIntent().getStringExtra("nickName");
        fullName = getIntent().getStringExtra("fullName");
        email = getIntent().getStringExtra("email");
        password = getIntent().getStringExtra("password");

        if (nickName == null || fullName == null || email == null || password == null) {
            Toast.makeText(this, "Data register tidak lengkap", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void daftarSekarang() {
        // User wajib memilih tanaman
        if (selectedPlantId.isEmpty()) {
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
                    
                    // Master data reference (Join)
                    user.put("plantId", selectedPlantId);
                    user.put("selectedPlant", selectedPlantName); // Keep for quick display
                    
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
                    Toast.makeText(
                            LoadingActivity.this,
                            "Daftar gagal: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void masukKeMain() {
        Intent intent = new Intent(LoadingActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
