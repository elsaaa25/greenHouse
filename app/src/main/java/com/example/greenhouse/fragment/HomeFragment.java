package com.example.greenhouse.fragment;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.greenhouse.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextView tvTabSuhu, tvTabKelembapan;
    private LineChart lineChart;

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

    private void setupLineChart() {

        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.getLegend().setEnabled(false);

        // X Axis
        XAxis xAxis = lineChart.getXAxis();

        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        xAxis.setLabelCount(8);

        xAxis.setValueFormatter(
                new IndexAxisValueFormatter(
                        new String[]{
                                "00:00",
                                "03:00",
                                "06:00",
                                "09:00",
                                "12:00",
                                "15:00",
                                "18:00",
                                "21:00"
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

        List<Entry> entries = new ArrayList<>();

        if (type.equals("Suhu")) {

            entries.add(new Entry(0, 25f));
            entries.add(new Entry(1, 27f));
            entries.add(new Entry(2, 26f));
            entries.add(new Entry(3, 28f));
            entries.add(new Entry(4, 27f));
            entries.add(new Entry(5, 29f));
            entries.add(new Entry(6, 28f));
            entries.add(new Entry(7, 28f));

        } else {

            entries.add(new Entry(0, 60f));
            entries.add(new Entry(1, 65f));
            entries.add(new Entry(2, 62f));
            entries.add(new Entry(3, 70f));
            entries.add(new Entry(4, 68f));
            entries.add(new Entry(5, 75f));
            entries.add(new Entry(6, 72f));
            entries.add(new Entry(7, 70f));
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