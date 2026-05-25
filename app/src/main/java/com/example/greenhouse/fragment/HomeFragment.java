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
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextView tvUserName;
    private TextView tvLampuStatus, tvPompaStatus;
    private SwitchMaterial switchLampu, switchPompa;
    private LineChart lineChart;

    private FirebaseAuth auth;
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
        tvUserName = view.findViewById(R.id.tvUserName);
        tvLampuStatus = view.findViewById(R.id.tvLampuStatus);
        tvPompaStatus = view.findViewById(R.id.tvPompaStatus);
        
        switchLampu = view.findViewById(R.id.switchLampu);
        switchPompa = view.findViewById(R.id.switchPompa);
        
        lineChart = view.findViewById(R.id.lineChart);

        // --- AMBIL NAMA DARI CACHE ---
        if (isAdded() && tvUserName != null) {
            SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            tvUserName.setText(prefs.getString("nickName", "User"));
        }

        // Inisialisasi Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ambilNamaPanggilan();
        
        // Setup Grafik
        if (lineChart != null) {
            setupLineChart();
            showChartData();
        }

        // --- LOGIKA SWITCH LAMPU ---
        if (switchLampu != null) {
            switchLampu.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (tvLampuStatus != null) tvLampuStatus.setText("On");
                    Toast.makeText(requireContext(), "Lampu Dinyalakan", Toast.LENGTH_SHORT).show();
                } else {
                    if (tvLampuStatus != null) tvLampuStatus.setText("Off");
                    Toast.makeText(requireContext(), "Lampu Dimatikan", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // --- LOGIKA SWITCH POMPA ---
        if (switchPompa != null) {
            switchPompa.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (tvPompaStatus != null) tvPompaStatus.setText("On");
                    Toast.makeText(requireContext(), "Pompa Air ON", Toast.LENGTH_SHORT).show();
                } else {
                    if (tvPompaStatus != null) tvPompaStatus.setText("Off");
                    Toast.makeText(requireContext(), "Pompa Air OFF", Toast.LENGTH_SHORT).show();
                }
            });
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (auth != null && db != null && tvUserName != null) {
            ambilNamaPanggilan();
        }
    }

    private void ambilNamaPanggilan() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nickName = documentSnapshot.getString("nickName");
                        if (nickName == null || nickName.isEmpty()) {
                            nickName = "User";
                        }

                        if (isAdded()) {
                            SharedPreferences.Editor editor = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit();
                            editor.putString("nickName", nickName);
                            editor.apply();
                        }

                        if (tvUserName != null) {
                            tvUserName.setText(nickName);
                        }
                    }
                });
    }

    private void setupLineChart() {
        if (lineChart == null) return;
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.getLegend().setEnabled(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"}));

        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(Color.LTGRAY);
        lineChart.getAxisLeft().setAxisMinimum(0f);
    }

    private void showChartData() {
        if (lineChart == null) return;

        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 60f));
        entries.add(new Entry(1, 65f));
        entries.add(new Entry(2, 62f));
        entries.add(new Entry(3, 70f));
        entries.add(new Entry(4, 68f));
        entries.add(new Entry(5, 75f));
        entries.add(new Entry(6, 72f));

        LineDataSet dataSet = new LineDataSet(entries, "Kelembapan");
        dataSet.setColor(Color.parseColor("#45553D"));
        dataSet.setCircleColor(Color.parseColor("#45553D"));
        dataSet.setLineWidth(2f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#E1BEE7"));

        lineChart.setData(new LineData(dataSet));
        lineChart.invalidate();
    }
}
