package com.example.greenhouse.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.greenhouse.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;

public class GraphFragment extends Fragment {

    private TextView btnHarian, btnMingguan, tvLabelKelembapan, tvAvgKelembapan;
    private LineChart lineChartHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_graph, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnHarian = view.findViewById(R.id.btnHarian);
        btnMingguan = view.findViewById(R.id.btnMingguan);
        lineChartHistory = view.findViewById(R.id.lineChartHistory);
        tvLabelKelembapan = view.findViewById(R.id.tvLabelKelembapan);
        tvAvgKelembapan = view.findViewById(R.id.tvAvgKelembapan);

        setupLineChart();

        if (btnHarian != null) {
            btnHarian.setOnClickListener(v -> selectHarian());
        }
        if (btnMingguan != null) {
            btnMingguan.setOnClickListener(v -> selectMingguan());
        }

        // Set default to Harian
        selectHarian();
    }

    private void setupLineChart() {
        if (lineChartHistory == null) return;
        
        lineChartHistory.getDescription().setEnabled(false);
        lineChartHistory.getLegend().setEnabled(false);
        lineChartHistory.setExtraOffsets(10f, 20f, 10f, 10f);
        lineChartHistory.setTouchEnabled(true);
        lineChartHistory.setDragEnabled(true);
        lineChartHistory.setScaleXEnabled(true);
        lineChartHistory.setScaleYEnabled(false);
        lineChartHistory.setPinchZoom(false);

        YAxis yAxis = lineChartHistory.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(100f);
        yAxis.setLabelCount(6, false);
        yAxis.setGranularity(20f);
        yAxis.setDrawAxisLine(false);
        yAxis.setGridColor(Color.parseColor("#E0E0E0"));
        yAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + "%";
            }
        });

        lineChartHistory.getAxisRight().setEnabled(false);

        XAxis xAxis = lineChartHistory.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#E0E0E0"));
        xAxis.setDrawAxisLine(false);
        xAxis.setYOffset(10f);
    }

    private void selectHarian() {
        if (!isAdded()) return;
        
        btnHarian.setBackgroundResource(R.drawable.bg_filter_selected);
        btnHarian.setTextColor(Color.WHITE);
        
        btnMingguan.setBackgroundResource(R.drawable.bg_filter_unselected);
        btnMingguan.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_secondary));

        tvLabelKelembapan.setText("Data Sensor Kelembapan (Harian)");
        updateCharts("harian");
    }

    private void selectMingguan() {
        if (!isAdded()) return;

        btnMingguan.setBackgroundResource(R.drawable.bg_filter_selected);
        btnMingguan.setTextColor(Color.WHITE);
        
        btnHarian.setBackgroundResource(R.drawable.bg_filter_unselected);
        btnHarian.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_secondary));

        tvLabelKelembapan.setText("Data Sensor Kelembapan (Mingguan)");
        updateCharts("mingguan");
    }

    private void updateCharts(String filter) {
        if (lineChartHistory == null) return;

        List<Entry> entries = new ArrayList<>();
        final String[] labels;

        if (filter.equals("harian")) {
            labels = new String[]{"00.00", "04.00", "08.00", "12.00", "16.00", "20.00", "00.00"};
            float[] values = {65f, 70f, 55f, 60f, 75f, 68f, 65f}; // Dummy data
            for (int i = 0; i < values.length; i++) {
                entries.add(new Entry(i * 4f, values[i]));
            }
            
            XAxis xAxis = lineChartHistory.getXAxis();
            xAxis.setAxisMinimum(0f);
            xAxis.setAxisMaximum(24f);
            xAxis.setGranularity(4f);
            xAxis.setLabelCount(7, true);
            xAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    int index = (int) (value / 4f);
                    if (index >= 0 && index < labels.length) return labels[index];
                    return "";
                }
            });
            lineChartHistory.setVisibleXRangeMaximum(12f);
            tvAvgKelembapan.setText("Rata-rata : 65%");
            
        } else {
            labels = new String[]{"Senin", "Selasa", "Rabu", "Kamis", "Jum'at", "Sabtu", "Ahad"};
            float[] values = {67f, 36f, 67f, 48f, 68f, 94f, 62f}; // Dummy data
            for (int i = 0; i < values.length; i++) {
                entries.add(new Entry(i, values[i]));
            }

            XAxis xAxis = lineChartHistory.getXAxis();
            xAxis.setAxisMinimum(0f);
            xAxis.setAxisMaximum(6f);
            xAxis.setGranularity(1f);
            xAxis.setLabelCount(7, true);
            xAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    int index = (int) value;
                    if (index >= 0 && index < labels.length) return labels[index];
                    return "";
                }
            });
            lineChartHistory.setVisibleXRangeMaximum(4f);
            tvAvgKelembapan.setText("Rata-rata : 63%");
        }

        LineDataSet dataSet = new LineDataSet(entries, "Kelembapan");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setColor(Color.parseColor("#1D9E75"));
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(Color.parseColor("#1D9E75"));
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#1D9E75"));
        dataSet.setFillAlpha(35);

        LineData data = new LineData(dataSet);
        lineChartHistory.setData(data);
        lineChartHistory.notifyDataSetChanged();
        lineChartHistory.invalidate();
    }
}
