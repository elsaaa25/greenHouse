package com.example.greenhouse.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.greenhouse.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextView tvTabSuhu, tvTabKelembapan, tvUserName;
    private LineChart lineChart;

    // FirebaseAuth untuk mengambil user yang sedang login
    private FirebaseAuth auth;

    // Firestore untuk mengambil data user dari database
    private FirebaseFirestore db;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_home,
                container,
                false);

        // Inisialisasi View
        tvTabSuhu = view.findViewById(R.id.tvTabSuhu);
        tvTabKelembapan = view.findViewById(R.id.tvTabKelembapan);
        lineChart = view.findViewById(R.id.lineChart);

        // Inisialisasi nama user
        tvUserName = view.findViewById(R.id.tvUserName);

        // --- AMBIL NAMA DARI CACHE AGAR INSTAN (Mencegah Flicker) ---
        if (isAdded()) {
            SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            String savedName = prefs.getString("nickName", "User");
            tvUserName.setText(savedName);
        }

        // Inisialisasi Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Ambil nama panggilan terbaru dari database
        ambilNamaPanggilan();
        
        // Setup Chart
        setupLineChart();

        // Status awal
        tvTabSuhu.setSelected(true);
        showChartData("Suhu");

        // Klik Tab Suhu
        tvTabSuhu.setOnClickListener(v -> {
            tvTabSuhu.setSelected(true);
            tvTabKelembapan.setSelected(false);
            showChartData("Suhu");
        });

        // Klik Tab Kelembapan
        tvTabKelembapan.setOnClickListener(v -> {
            tvTabKelembapan.setSelected(true);
            tvTabSuhu.setSelected(false);
            showChartData("Kelembapan");
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Ambil ulang nama saat kembali ke HomeFragment
        if (auth != null && db != null && tvUserName != null) {
            ambilNamaPanggilan();
        }
    }

    private void ambilNamaPanggilan() {

        // Pastikan user sudah login
        if (auth.getCurrentUser() == null) {
            return;
        }

        // Ambil UID user yang sedang login
        String uid = auth.getCurrentUser().getUid();

        // Ambil data user dari Firestore
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        // Ambil field nickName dari Firestore
                        String nickName = documentSnapshot.getString("nickName");

                        // Jika nickName kosong, pakai default User
                        if (nickName == null || nickName.isEmpty()) {
                            nickName = "User";
                        }

                        // --- SIMPAN KE CACHE UNTUK PEMBUKAAN BERIKUTNYA ---
                        if (isAdded()) {
                            SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("nickName", nickName);
                            editor.apply();
                        }

                        // Tampilkan nama panggilan ke halaman beranda
                        if (tvUserName != null) {
                            tvUserName.setText(nickName);
                        }

                    } else {
                        if (getContext() != null) {
                            Toast.makeText(
                                    getContext(),
                                    "Data user tidak ditemukan",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(
                                getContext(),
                                "Gagal mengambil nama: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private void setupLineChart() {
        if (lineChart == null) return;
        
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.getLegend().setEnabled(false);

        // X Axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(7);

        xAxis.setValueFormatter(
                new IndexAxisValueFormatter(
                        new String[]{
                                "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"
                        }
                )
        );

        // Y Axis
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(Color.LTGRAY);
        lineChart.getAxisLeft().setAxisMinimum(0f);
    }

    private void showChartData(String type) {
        if (lineChart == null) return;

        List<Entry> entries = new ArrayList<>();

        if (type.equals("Suhu")) {
            entries.add(new Entry(0, 25f));
            entries.add(new Entry(1, 27f));
            entries.add(new Entry(2, 26f));
            entries.add(new Entry(3, 28f));
            entries.add(new Entry(4, 27f));
            entries.add(new Entry(5, 29f));
            entries.add(new Entry(6, 28f));

        } else {
            entries.add(new Entry(0, 60f));
            entries.add(new Entry(1, 65f));
            entries.add(new Entry(2, 62f));
            entries.add(new Entry(3, 70f));
            entries.add(new Entry(4, 68f));
            entries.add(new Entry(5, 75f));
            entries.add(new Entry(6, 72f));

        }

        LineDataSet dataSet = new LineDataSet(entries, type);
        dataSet.setColor(Color.parseColor("#45553D"));
        dataSet.setCircleColor(Color.parseColor("#45553D"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(true);
        dataSet.setValueTextSize(10f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#E1BEE7"));

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.animateX(1000);
        lineChart.invalidate();
    }
}
