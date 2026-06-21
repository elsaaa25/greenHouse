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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class GraphFragment extends Fragment {

    private static final String TAG = "GraphFragment";
    private static final String DEVICE_ID = "GH-001";

    private TextView btnHarian, btnMingguan, tvLabelKelembapan, tvAvgKelembapan;
    private LineChart lineChartHistory;

    private DatabaseReference historyDb;
    private DatabaseReference activeHistoryRef;
    private ValueEventListener historyListener;

    private String currentFilter = "harian";

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_graph, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initFirebase();
        setupLineChart();
        setupButtonListeners();

        selectHarian();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        removeHistoryListener();
    }

    private void initViews(View view) {
        btnHarian = view.findViewById(R.id.btnHarian);
        btnMingguan = view.findViewById(R.id.btnMingguan);
        lineChartHistory = view.findViewById(R.id.lineChartHistory);
        tvLabelKelembapan = view.findViewById(R.id.tvLabelKelembapan);
        tvAvgKelembapan = view.findViewById(R.id.tvAvgKelembapan);
    }

    private void initFirebase() {
        historyDb = FirebaseDatabase.getInstance()
                .getReference("history")
                .child(DEVICE_ID);
    }

    private void setupButtonListeners() {
        if (btnHarian != null) {
            btnHarian.setOnClickListener(v -> selectHarian());
        }

        if (btnMingguan != null) {
            btnMingguan.setOnClickListener(v -> selectMingguan());
        }
    }

    private void removeHistoryListener() {
        if (activeHistoryRef != null && historyListener != null) {
            activeHistoryRef.removeEventListener(historyListener);
            historyListener = null;
            activeHistoryRef = null;
        }
    }

    private void setupLineChart() {
        if (lineChartHistory == null) return;

        lineChartHistory.getDescription().setEnabled(false);
        lineChartHistory.getLegend().setEnabled(false);
        lineChartHistory.setNoDataText("Belum ada data kelembapan");
        lineChartHistory.setExtraOffsets(10f, 20f, 10f, 10f);

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
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setYOffset(10f);
    }

    private void selectHarian() {
        if (!isAdded()) return;

        currentFilter = "harian";

        if (btnHarian != null) {
            btnHarian.setBackgroundResource(R.drawable.bg_filter_selected);
            btnHarian.setTextColor(Color.WHITE);
        }

        if (btnMingguan != null) {
            btnMingguan.setBackgroundResource(R.drawable.bg_filter_unselected);
            btnMingguan.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_secondary));
        }

        if (tvLabelKelembapan != null) {
            tvLabelKelembapan.setText("Data Sensor Kelembapan (Harian)");
        }

        fetchHarianFromMinutely();
    }

    private void selectMingguan() {
        if (!isAdded()) return;

        currentFilter = "mingguan";

        if (btnMingguan != null) {
            btnMingguan.setBackgroundResource(R.drawable.bg_filter_selected);
            btnMingguan.setTextColor(Color.WHITE);
        }

        if (btnHarian != null) {
            btnHarian.setBackgroundResource(R.drawable.bg_filter_unselected);
            btnHarian.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_secondary));
        }

        if (tvLabelKelembapan != null) {
            tvLabelKelembapan.setText("Data Sensor Kelembapan (Mingguan)");
        }

        fetchMingguanFromDaily();
    }

    /*
     * HARlAN:
     * Membaca data dari:
     * history/GH-001/minutely/yyyy-MM-dd/HH:mm/humidity
     *
     * Lalu:
     * 1. data per menit dikelompokkan berdasarkan jam
     * 2. dihitung rata-rata per jam
     * 3. hasil rata-rata disimpan ke history/GH-001/hourly/yyyy-MM-dd/HH:00
     * 4. hasil rata-rata ditampilkan ke grafik harian
     */
    private void fetchHarianFromMinutely() {
        removeHistoryListener();

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        DatabaseReference minutelyTodayRef = historyDb
                .child("minutely")
                .child(today);

        activeHistoryRef = minutelyTodayRef;

        historyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                TreeMap<Integer, List<Float>> hourlyGroups = new TreeMap<>();

                float totalHumidity = 0f;
                int totalMinuteData = 0;

                if (snapshot.exists()) {
                    for (DataSnapshot minuteSnapshot : snapshot.getChildren()) {
                        try {
                            String timeKey = minuteSnapshot.getKey(); // contoh: "14:05"
                            Number humidityNumber = minuteSnapshot.child("humidity").getValue(Number.class);

                            if (timeKey == null || humidityNumber == null) continue;

                            String[] parts = timeKey.split(":");

                            if (parts.length != 2) continue;

                            int hour = Integer.parseInt(parts[0]);
                            float humidity = humidityNumber.floatValue();

                            if (!hourlyGroups.containsKey(hour)) {
                                hourlyGroups.put(hour, new ArrayList<>());
                            }

                            hourlyGroups.get(hour).add(humidity);

                            totalHumidity += humidity;
                            totalMinuteData++;

                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing minutely data: " + e.getMessage());
                        }
                    }
                }

                List<Entry> entries = buildHourlyEntries(hourlyGroups);

                // Simpan hasil rata-rata per jam ke Firebase hourly
                saveHourlyAverageToFirebase(today, hourlyGroups);

                updateHarianChart(entries);

                if (tvAvgKelembapan != null) {
                    if (totalMinuteData > 0) {
                        int avg = Math.round(totalHumidity / totalMinuteData);
                        tvAvgKelembapan.setText("Rata-rata : " + avg + "%");
                    } else {
                        tvAvgKelembapan.setText("Rata-rata : --");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Harian database error: " + error.getMessage());
            }
        };

        minutelyTodayRef.addValueEventListener(historyListener);
    }

    private List<Entry> buildHourlyEntries(TreeMap<Integer, List<Float>> hourlyGroups) {
        List<Entry> entries = new ArrayList<>();

        if (hourlyGroups == null || hourlyGroups.isEmpty()) {
            return entries;
        }

        for (Map.Entry<Integer, List<Float>> group : hourlyGroups.entrySet()) {
            int hour = group.getKey();
            List<Float> values = group.getValue();

            if (values == null || values.isEmpty()) continue;

            float sum = 0f;

            for (float value : values) {
                sum += value;
            }

            float avgPerHour = sum / values.size();

            entries.add(new Entry(hour, avgPerHour));
        }

        return entries;
    }

    private void saveHourlyAverageToFirebase(String today, TreeMap<Integer, List<Float>> hourlyGroups) {
        if (historyDb == null || today == null || hourlyGroups == null || hourlyGroups.isEmpty()) return;

        DatabaseReference hourlyTodayRef = historyDb
                .child("hourly")
                .child(today);

        for (Map.Entry<Integer, List<Float>> group : hourlyGroups.entrySet()) {
            int hour = group.getKey();
            List<Float> values = group.getValue();

            if (values == null || values.isEmpty()) continue;

            float sum = 0f;

            for (float value : values) {
                sum += value;
            }

            int avgPerHour = Math.round(sum / values.size());
            String hourKey = String.format(Locale.getDefault(), "%02d:00", hour);

            Map<String, Object> hourlyData = new HashMap<>();
            hourlyData.put("humidity", avgPerHour);
            hourlyData.put("sampleCount", values.size());
            hourlyData.put("timestamp", System.currentTimeMillis());

            hourlyTodayRef.child(hourKey)
                    .setValue(hourlyData)
                    .addOnSuccessListener(unused ->
                            Log.d(TAG, "Hourly saved: " + today + " " + hourKey + " = " + avgPerHour)
                    )
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed save hourly: " + e.getMessage())
                    );
        }
    }

    /*
     * MINGGUAN:
     * Membaca data dari:
     * history/GH-001/daily/yyyy-MM/yyyy-MM-dd/avgHumidity
     */
    private void fetchMingguanFromDaily() {
        removeHistoryListener();

        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                .format(new Date());

        DatabaseReference dailyMonthRef = historyDb
                .child("daily")
                .child(currentMonth);

        activeHistoryRef = dailyMonthRef;

        historyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                TreeMap<Integer, Float> dailyData = new TreeMap<>();

                float totalHumidity = 0f;
                int totalDays = 0;

                if (snapshot.exists()) {
                    for (DataSnapshot daySnapshot : snapshot.getChildren()) {
                        try {
                            String dateKey = daySnapshot.getKey(); // contoh: "2026-06-21"
                            Number avgHumidityNumber = daySnapshot.child("avgHumidity").getValue(Number.class);

                            if (dateKey == null || avgHumidityNumber == null) continue;

                            String[] parts = dateKey.split("-");

                            if (parts.length != 3) continue;

                            int day = Integer.parseInt(parts[2]);
                            float avgHumidity = avgHumidityNumber.floatValue();

                            dailyData.put(day, avgHumidity);

                            totalHumidity += avgHumidity;
                            totalDays++;

                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing daily data: " + e.getMessage());
                        }
                    }
                }

                List<Entry> entries = new ArrayList<>();

                for (Map.Entry<Integer, Float> entry : dailyData.entrySet()) {
                    entries.add(new Entry(entry.getKey(), entry.getValue()));
                }

                updateMingguanChart(entries);

                if (tvAvgKelembapan != null) {
                    if (totalDays > 0) {
                        int avg = Math.round(totalHumidity / totalDays);
                        tvAvgKelembapan.setText("Rata-rata : " + avg + "%");
                    } else {
                        tvAvgKelembapan.setText("Rata-rata : --");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Mingguan database error: " + error.getMessage());
            }
        };

        dailyMonthRef.addValueEventListener(historyListener);
    }

    private void updateHarianChart(List<Entry> entries) {
        if (lineChartHistory == null) return;

        XAxis xAxis = lineChartHistory.getXAxis();

        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(23f);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setLabelCount(8, false);

        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%02d.00", (int) value % 24);
            }
        });

        applyChartData(entries);

        lineChartHistory.setVisibleXRangeMaximum(8f);

        if (entries != null && !entries.isEmpty()) {
            lineChartHistory.moveViewToX(entries.get(entries.size() - 1).getX());
        }
    }

    private void updateMingguanChart(List<Entry> entries) {
        if (lineChartHistory == null) return;

        XAxis xAxis = lineChartHistory.getXAxis();

        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setLabelCount(7, false);

        if (entries != null && !entries.isEmpty()) {
            float minX = entries.get(0).getX();
            float maxX = entries.get(entries.size() - 1).getX();

            xAxis.setAxisMinimum(minX - 0.5f);
            xAxis.setAxisMaximum(maxX + 0.5f);
        } else {
            xAxis.setAxisMinimum(1f);
            xAxis.setAxisMaximum(31f);
        }

        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        applyChartData(entries);

        lineChartHistory.setVisibleXRangeMaximum(7f);

        if (entries != null && !entries.isEmpty()) {
            lineChartHistory.moveViewToX(entries.get(entries.size() - 1).getX());
        }
    }

    private void applyChartData(List<Entry> entries) {
        if (lineChartHistory == null) return;

        if (entries == null || entries.isEmpty()) {
            lineChartHistory.clear();

            if ("harian".equals(currentFilter)) {
                lineChartHistory.setNoDataText("Belum ada data kelembapan hari ini");
            } else {
                lineChartHistory.setNoDataText("Belum ada data kelembapan minggu ini");
            }

            lineChartHistory.invalidate();
            return;
        }

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
}