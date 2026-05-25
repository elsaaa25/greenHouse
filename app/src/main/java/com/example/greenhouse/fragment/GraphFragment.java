package com.example.greenhouse.fragment;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.greenhouse.R;

public class GraphFragment extends Fragment {

    private TextView btnMinggu, btnBulan;
    private LineChartView lineChartKelembapan;
    private LinearLayout labelsKelembapan;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_graph, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inisialisasi komponen dengan aman
        btnMinggu = view.findViewById(R.id.btnMinggu);
        btnBulan = view.findViewById(R.id.btnBulan);
        lineChartKelembapan = view.findViewById(R.id.lineChartKelembapan);
        labelsKelembapan = view.findViewById(R.id.labelsKelembapan);

        // Set listener hanya jika komponen ditemukan di XML
        if (btnMinggu != null) {
            btnMinggu.setOnClickListener(v -> selectMinggu());
        }
        if (btnBulan != null) {
            btnBulan.setOnClickListener(v -> selectBulan());
        }

        // Jalankan pilihan default jika komponen ada
        if (btnMinggu != null) {
            selectMinggu();
        }
    }

    private void selectMinggu() {
        if (!isAdded()) return;
        
        if (btnMinggu != null) {
            btnMinggu.setBackgroundResource(R.drawable.bg_filter_selected);
            btnMinggu.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        }
        
        if (btnBulan != null) {
            btnBulan.setBackgroundResource(R.drawable.bg_filter_unselected);
            btnBulan.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_secondary));
        }

        updateCharts("minggu");
    }

    private void selectBulan() {
        if (!isAdded()) return;

        if (btnBulan != null) {
            btnBulan.setBackgroundResource(R.drawable.bg_filter_selected);
            btnBulan.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        }
        
        if (btnMinggu != null) {
            btnMinggu.setBackgroundResource(R.drawable.bg_filter_unselected);
            btnMinggu.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_secondary));
        }

        updateCharts("bulan");
    }

    private void updateCharts(String filter) {
        // PERBAIKAN: Jika labelsKelembapan tidak ada di XML, jangan jalankan kode di bawahnya
        if (labelsKelembapan == null) return;

        labelsKelembapan.removeAllViews();

        if (filter.equals("minggu")) {
            String[] days = {"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"};
            double[] values = {67.21, 36.52, 67.11, 48.68, 68.5, 94.26, 62.55};

            if (lineChartKelembapan != null) {
                lineChartKelembapan.setData(values);
            }

            for (String day : days) {
                addLabel(labelsKelembapan, day);
            }
        } else {
            String[] dates = {"1", "5", "10", "15", "20", "25", "30"};
            double[] values = new double[dates.length];
            for (int i = 0; i < dates.length; i++) {
                values[i] = Math.random() * 80 + 20;
                addLabel(labelsKelembapan, dates[i]);
            }
            if (lineChartKelembapan != null) {
                lineChartKelembapan.setData(values);
            }
        }
    }

    private void addLabel(LinearLayout container, String label) {
        if (getContext() == null || container == null) return;
        TextView tv = new TextView(getContext());
        tv.setText(label);
        tv.setTextSize(10);
        tv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(params);
        container.addView(tv);
    }
}
