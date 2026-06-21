package com.example.greenhouse.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class GraphFragment extends Fragment {

    private static final String TAG = "GraphFragment";
    private TextView btnHarian, btnMingguan, tvLabelKelembapan, tvAvgKelembapan;
    private LineChart lineChartHistory;

    
    private DatabaseReference historyDb;
    private ValueEventListener historyListener;
    private String currentFilter = "harian";

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

        historyDb = FirebaseDatabase.getInstance().getReference("history").child("GH-001");

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        removeHistoryListener();
    }

    private void removeHistoryListener() {
        if (historyDb != null && historyListener != null) {
            historyDb.removeEventListener(historyListener);
            historyListener = null;
        }
    }

    private void setupLineChart() {
        if (lineChartHistory == null) return;
        
        lineChartHistory.getDescription().setEnabled(false);
        lineChartHistory.getLegend().setEnabled(false);
        lineChartHistory.setExtraOffsets(10f, 20f, 10f, 10f);
        
        // Aktifkan Fitur Responsif & Scroll
        lineChartHistory.setTouchEnabled(true);
        lineChartHistory.setDragEnabled(true);
        lineChartHistory.setScaleXEnabled(true);
        lineChartHistory.setScaleYEnabled(false);
        lineChartHistory.setPinchZoom(false);
        lineChartHistory.setDoubleTapToZoomEnabled(true);

        YAxis yAxis = lineChartHistory.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(100f);
        yAxis.setLabelCount(6, false);
        yAxis.setGranularity(20f);
        yAxis.setDrawAxisLine(false);
        yAxis.setGridColor(Color.parseColor("#E0E0E0"));
        yAxis.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) { return (int) value + "%"; }
        });

        lineChartHistory.getAxisRight().setEnabled(false);

        XAxis xAxis = lineChartHistory.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#E0E0E0"));
        xAxis.setDrawAxisLine(false);
        xAxis.setGranularity(1f); // Grid setiap 1 jam
        xAxis.setGranularityEnabled(true);
        xAxis.setYOffset(10f);
    }

    private void selectHarian() {
        if (!isAdded()) return;
        currentFilter = "harian";
        
        btnHarian.setBackgroundResource(R.drawable.bg_filter_selected);
        btnHarian.setTextColor(Color.WHITE);
        
        btnMingguan.setBackgroundResource(R.drawable.bg_filter_unselected);
        btnMingguan.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_secondary));

        tvLabelKelembapan.setText("Data Sensor Kelembapan (Harian)");
        fetchDataFromFirebase();
    }

    private void selectMingguan() {
        if (!isAdded()) return;
        currentFilter = "mingguan";

        btnMingguan.setBackgroundResource(R.drawable.bg_filter_selected);
        btnMingguan.setTextColor(Color.WHITE);
        
        btnHarian.setBackgroundResource(R.drawable.bg_filter_unselected);
        btnHarian.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_secondary));

        tvLabelKelembapan.setText("Data Sensor Kelembapan (Mingguan)");
        fetchDataFromFirebase();
    }

    private void fetchDataFromFirebase() {
        removeHistoryListener();

        DatabaseReference targetRef;
        if (currentFilter.equals("harian")) {
            targetRef = historyDb.child("hourly");
        } else {
            targetRef = historyDb.child("daily");
        }

        historyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                
                SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat sdfMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
                String today = sdfDate.format(new Date());
                String currentMonth = sdfMonth.format(new Date());

                List<Entry> entries = new ArrayList<>();
                float totalHumidity = 0;
                int count = 0;

                if (currentFilter.equals("harian")) {
                    DataSnapshot todaySnapshot = snapshot.child(today);
                    if (todaySnapshot.exists()) {
                        // Grouping data by hour to calculate average per hour
                        TreeMap<Integer, List<Float>> hourlyGroups = new TreeMap<>();
                        for (DataSnapshot hourSnapshot : todaySnapshot.getChildren()) {
                            try {
                                String timeKey = hourSnapshot.getKey(); // "HH:mm"
                                Float humidity = hourSnapshot.child("humidity").getValue(Float.class);
                                if (timeKey != null && humidity != null) {
                                    int hour = Integer.parseInt(timeKey.split(":")[0]);
                                    if (!hourlyGroups.containsKey(hour)) {
                                        hourlyGroups.put(hour, new ArrayList<>());
                                    }
                                    hourlyGroups.get(hour).add(humidity);
                                    totalHumidity += humidity;
                                    count++;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing hourly group: " + e.getMessage());
                            }
                        }

                        // Create entries from averaged hourly data
                        for (Map.Entry<Integer, List<Float>> entry : hourlyGroups.entrySet()) {
                            float sum = 0;
                            for (float val : entry.getValue()) sum += val;
                            float avg = sum / entry.getValue().size();
                            entries.add(new Entry(entry.getKey(), avg));
                        }
                    }
                    updateHarianChart(entries);
                } else {
                    DataSnapshot monthSnapshot = snapshot.child(currentMonth);
                    // Sort by day using TreeMap
                    TreeMap<Integer, Float> dailyData = new TreeMap<>();
                    if (monthSnapshot.exists()) {
                        for (DataSnapshot daySnapshot : monthSnapshot.getChildren()) {
                            try {
                                String dateKey = daySnapshot.getKey(); // "2026-06-18"
                                Float avgHumidity = daySnapshot.child("avgHumidity").getValue(Float.class);
                                if (dateKey != null && avgHumidity != null) {
                                    String[] parts = dateKey.split("-");
                                    int day = Integer.parseInt(parts[2]);
                                    dailyData.put(day, avgHumidity);
                                    totalHumidity += avgHumidity;
                                    count++;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing daily: " + e.getMessage());
                            }
                        }
                    }
                    
                    for (Map.Entry<Integer, Float> entry : dailyData.entrySet()) {
                        entries.add(new Entry(entry.getKey(), entry.getValue()));
                    }
                    updateMingguanChart(entries);
                }

                if (count > 0) {
                    int avg = Math.round(totalHumidity / count);
                    tvAvgKelembapan.setText("Rata-rata : " + avg + "%");
                } else {
                    tvAvgKelembapan.setText("Rata-rata : --");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        };
        targetRef.addValueEventListener(historyListener);
    }

    private void updateHarianChart(List<Entry> entries) {
        if (lineChartHistory == null) return;

        XAxis xAxis = lineChartHistory.getXAxis();
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(24f);
        xAxis.setLabelCount(9, false); // Menampilkan label yang pas (00, 03, 06, dst)
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%02d.00", (int) value % 24);
            }
        });
        
        applyChartData(entries);
        
        // Batasi tampilan hanya 6-8 jam agar bisa di-scroll secara horizontal
        lineChartHistory.setVisibleXRangeMaximum(8f); 
        
        if (!entries.isEmpty()) {
            // Geser ke data terbaru
            lineChartHistory.moveViewToX(entries.get(entries.size() - 1).getX());
        }
    }

    private void updateMingguanChart(List<Entry> entries) {
        if (lineChartHistory == null) return;

        XAxis xAxis = lineChartHistory.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(7, false);
        
        // Find min and max days to set axis range
        if (!entries.isEmpty()) {
            float minX = entries.get(0).getX();
            float maxX = entries.get(entries.size() - 1).getX();
            xAxis.setAxisMinimum(minX - 0.5f);
            xAxis.setAxisMaximum(maxX + 0.5f);
        }

        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        applyChartData(entries);
        lineChartHistory.setVisibleXRangeMaximum(7f);
    }

    private void applyChartData(List<Entry> entries) {
        LineDataSet dataSet = new LineDataSet(entries, "Kelembapan");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setColor(Color.parseColor("#1D9E75"));
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(Color.parseColor("#1D9E75"));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#1D9E75"));
        dataSet.setFillAlpha(35);

        LineData data = new LineData(dataSet);
        lineChartHistory.setData(data);
        lineChartHistory.notifyDataSetChanged();
        lineChartHistory.invalidate();
    }

    // Menghapus updateCharts lama karena sudah diganti dengan logika Firebase
    private void updateCharts(String filter) {
        // Method ini tidak lagi digunakan secara langsung
    }
}
