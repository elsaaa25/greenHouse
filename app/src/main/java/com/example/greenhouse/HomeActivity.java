package com.example.greenhouse;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private TextView tvTabSuhu, tvTabKelembapan;
    private LineChart lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 1. Inisialisasi View
        tvTabSuhu = findViewById(R.id.tvTabSuhu);
        tvTabKelembapan = findViewById(R.id.tvTabKelembapan);
        lineChart = findViewById(R.id.lineChart);

        // 2. Setup Chart Style
        setupLineChart();

        // 3. Set status awal & Load Data Suhu
        if (tvTabSuhu != null) {
            tvTabSuhu.setSelected(true);
            showChartData("Suhu");
        }

        // 4. Logika Klik Tab Suhu
        if (tvTabSuhu != null) {
            tvTabSuhu.setOnClickListener(v -> {
                tvTabSuhu.setSelected(true);
                if (tvTabKelembapan != null) {
                    tvTabKelembapan.setSelected(false);
                }
                showChartData("Suhu");
            });
        }

        // 5. Logika Klik Tab Kelembapan
        if (tvTabKelembapan != null) {
            tvTabKelembapan.setOnClickListener(v -> {
                tvTabKelembapan.setSelected(true);
                if (tvTabSuhu != null) {
                    tvTabSuhu.setSelected(false);
                }
                showChartData("Kelembapan");
            });
        }
    }

    private void setupLineChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.getLegend().setEnabled(false);

        // X-Axis (jam)
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(8);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"00:00", "03:00", "06:00", "09:00", "12:00", "15:00", "18:00", "21:00"}));

        // Y-Axis
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(Color.LTGRAY);
        lineChart.getAxisLeft().setAxisMinimum(0f);
    }

    private void showChartData(String type) {
        List<Entry> entries = new ArrayList<>();

        if (type.equals("Suhu")) {
            // Contoh data rata-rata suhu harian
            entries.add(new Entry(0, 25f));
            entries.add(new Entry(1, 27f));
            entries.add(new Entry(2, 26f));
            entries.add(new Entry(3, 28f));
            entries.add(new Entry(4, 27f));
            entries.add(new Entry(5, 29f));
            entries.add(new Entry(6, 28f));
            entries.add(new Entry(7, 28f));
        } else {
            // Contoh data rata-rata kelembapan harian
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
        dataSet.setColor(Color.parseColor("#45553D")); // Warna ungu sesuai desain
        dataSet.setCircleColor(Color.parseColor("#45553D"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(true);
        dataSet.setValueTextSize(10f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Membuat garis melengkung
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#E1BEE7")); // Area bawah garis

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.animateX(1000);
        lineChart.invalidate(); // Refresh chart
    }
}
