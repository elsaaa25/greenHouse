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

import com.example.greenhouse.LineChartView;
import com.example.greenhouse.R;

public class GraphActivity extends Fragment {

    private TextView btnMinggu, btnBulan;
    private LineChartView lineChartSuhu, lineChartKelembapan;
    private LinearLayout labelsSuhu, labelsKelembapan;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_graph, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnMinggu = view.findViewById(R.id.btnMinggu);
        btnBulan = view.findViewById(R.id.btnBulan);
        lineChartSuhu = view.findViewById(R.id.lineChartSuhu);
        lineChartKelembapan = view.findViewById(R.id.lineChartKelembapan);
        labelsSuhu = view.findViewById(R.id.labelsSuhu);
        labelsKelembapan = view.findViewById(R.id.labelsKelembapan);

        btnMinggu.setOnClickListener(v -> selectMinggu());
        btnBulan.setOnClickListener(v -> selectBulan());

        // Default: Minggu
        selectMinggu();
    }

    private void selectMinggu() {
        btnMinggu.setBackgroundResource(R.drawable.bg_filter_selected);
        btnMinggu.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        btnBulan.setBackgroundResource(R.drawable.bg_filter_unselected);
        btnBulan.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_secondary));

        updateCharts("minggu");
    }

    private void selectBulan() {
        btnBulan.setBackgroundResource(R.drawable.bg_filter_selected);
        btnBulan.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        btnMinggu.setBackgroundResource(R.drawable.bg_filter_unselected);
        btnMinggu.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_secondary));

        updateCharts("bulan");
    }

    private void updateCharts(String filter) {
        labelsSuhu.removeAllViews();
        labelsKelembapan.removeAllViews();

        if (filter.equals("minggu")) {
            String[] days = {"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"};
            double[] values = {67.21, 36.52, 67.11, 48.68, 68.5, 94.26, 62.55};

            lineChartSuhu.setData(values);
            lineChartKelembapan.setData(values);

            for (String day : days) {
                addLabel(labelsSuhu, day);
                addLabel(labelsKelembapan, day);
            }
        } else {
            String[] dates = {"1", "5", "10", "15", "20", "25", "30"};
            double[] values = new double[dates.length];
            for (int i = 0; i < dates.length; i++) {
                values[i] = Math.random() * 80 + 20;
                addLabel(labelsSuhu, dates[i]);
                addLabel(labelsKelembapan, dates[i]);
            }
            lineChartSuhu.setData(values);
            lineChartKelembapan.setData(values);
        }
    }

    private void addLabel(LinearLayout container, String label) {
        if (getContext() == null) return;
        TextView tv = new TextView(getContext());
        tv.setText(label);
        tv.setTextSize(10);
        tv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(params);
        container.addView(tv);
    }
}
