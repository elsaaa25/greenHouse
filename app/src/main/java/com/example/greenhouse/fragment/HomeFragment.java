package com.example.greenhouse.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.greenhouse.R;
import com.example.greenhouse.model.DeviceLive;
import com.example.greenhouse.model.UserPlant;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final String DEVICE_ID = "GH-001";

    private TextView tvUserName, tvKelembapan, tvStatusAlat, tvStatus;
    private TextView tvLampuStatus, tvPompaStatus;
    private TextView tvLampuModeLabel, tvPompaModeLabel;
    private TextView tvHomePlantName, tvDeviceName;
    private ImageView ivPlant;

    private SwitchMaterial switchLampu, switchPompa, switchLampuAuto, switchPompaAuto;

    private LineChart lineChart;
    private SeekBar seekBarMin, seekBarMax;
    private TextView tvMinValue, tvMaxValue;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private DatabaseReference liveDb, settingsDb, controlDb, historyDb;

    private ListenerRegistration deviceListener;
    private ListenerRegistration userPlantListener;

    private ValueEventListener rtdbLiveListener;
    private ValueEventListener rtdbSettingsListener;
    private ValueEventListener rtdbControlListener;
    private ValueEventListener rtdbHistoryListener;

    private String userPlantDocId = "";
    private String lastSavedMinuteKey = "";

    private float batasMin = 60f;
    private float batasMaks = 80f;

    private boolean isUpdatingFromSource = false;
    private long lastTelemetryTime = 0;

    private final android.os.Handler heartbeatHandler = new android.os.Handler();
    private Runnable heartbeatRunnable;

    public HomeFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);
        initFirebase();
        setupLineChart();
        setupSeekBars();
        setupSwitchListeners();
        setupHeartbeatCheck();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ambilDataUserDanTanaman();
        integrasiKoleksiDevices();
        integrasiKoleksiUserPlants();
        integrasiRTDB();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (heartbeatRunnable != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
        }

        if (deviceListener != null) {
            deviceListener.remove();
            deviceListener = null;
        }

        if (userPlantListener != null) {
            userPlantListener.remove();
            userPlantListener = null;
        }

        removeRtdbListeners();
    }

    private void initViews(View view) {
        tvUserName = view.findViewById(R.id.tvUserName);
        tvKelembapan = view.findViewById(R.id.tvKelembapan);
        tvStatusAlat = view.findViewById(R.id.tvStatusAlat);
        tvStatus = view.findViewById(R.id.tvStatus);

        tvLampuStatus = view.findViewById(R.id.tvLampuStatus);
        tvPompaStatus = view.findViewById(R.id.tvPompaStatus);
        tvLampuModeLabel = view.findViewById(R.id.tvLampuModeLabel);
        tvPompaModeLabel = view.findViewById(R.id.tvPompaModeLabel);

        tvHomePlantName = view.findViewById(R.id.tvHomePlantName);
        tvDeviceName = view.findViewById(R.id.tvDeviceName);
        ivPlant = view.findViewById(R.id.ivPlant);

        switchLampu = view.findViewById(R.id.switchLampu);
        switchPompa = view.findViewById(R.id.switchPompa);
        switchLampuAuto = view.findViewById(R.id.switchLampuAuto);
        switchPompaAuto = view.findViewById(R.id.switchPompaAuto);

        lineChart = view.findViewById(R.id.lineChart);
        seekBarMin = view.findViewById(R.id.seekBarMin);
        seekBarMax = view.findViewById(R.id.seekBarMax);
        tvMinValue = view.findViewById(R.id.tvMinValue);
        tvMaxValue = view.findViewById(R.id.tvMaxValue);

        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);

        if (tvUserName != null) {
            tvUserName.setText(prefs.getString("nickName", "User"));
        }

        if (tvStatus != null) {
            tvStatus.setText("Menghubungkan...");
        }

        if (tvStatusAlat != null) {
            tvStatusAlat.setText("WAITING...");
        }
    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseDatabase rtdb = FirebaseDatabase.getInstance();

        liveDb = rtdb.getReference("telemetry").child(DEVICE_ID);
        settingsDb = rtdb.getReference("settings").child(DEVICE_ID);
        controlDb = rtdb.getReference("control").child(DEVICE_ID);

        // Data grafik disimpan di:
        // history/GH-001/minutely/yyyy-MM-dd/HH:mm
        historyDb = rtdb.getReference("history")
                .child(DEVICE_ID)
                .child("minutely");
    }

    private void setupHeartbeatCheck() {
        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAdded()) {
                    long now = System.currentTimeMillis();

                    if (lastTelemetryTime > 0 && (now - lastTelemetryTime) > 30000) {
                        updateOfflineStatus();
                    }

                    heartbeatHandler.postDelayed(this, 10000);
                }
            }
        };

        heartbeatHandler.postDelayed(heartbeatRunnable, 10000);
    }

    private void updateOfflineStatus() {
        if (tvStatusAlat != null) {
            tvStatusAlat.setText("OFFLINE");
        }

        if (tvStatus != null) {
            tvStatus.setText("Monitoring Nonaktif");
            tvStatus.setBackgroundResource(R.drawable.bg_status_watering);
        }
    }

    private void integrasiRTDB() {
        rtdbLiveListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                DeviceLive liveData = snapshot.getValue(DeviceLive.class);

                if (liveData != null) {
                    updateLiveUI(liveData);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Live listener cancelled: " + error.getMessage());
            }
        };

        liveDb.addValueEventListener(rtdbLiveListener);

        rtdbSettingsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                Integer min = snapshot.child("humidityMin").getValue(Integer.class);
                Integer max = snapshot.child("humidityMax").getValue(Integer.class);

                if (min != null) batasMin = min;
                if (max != null) batasMaks = max;

                updateSettingsUIFromRtdb();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Settings listener cancelled: " + error.getMessage());
            }
        };

        settingsDb.addValueEventListener(rtdbSettingsListener);

        rtdbControlListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                String lampMode = snapshot.child("lampMode").getValue(String.class);
                String pumpMode = snapshot.child("pumpMode").getValue(String.class);
                Boolean lampManual = snapshot.child("lampManualState").getValue(Boolean.class);
                Boolean pumpManual = snapshot.child("pumpManualState").getValue(Boolean.class);

                updateControlUI(lampMode, pumpMode, lampManual, pumpManual);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Control listener cancelled: " + error.getMessage());
            }
        };

        controlDb.addValueEventListener(rtdbControlListener);

        integrasiRTDBHistory();
    }

    private void integrasiRTDBHistory() {
        rtdbHistoryListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || lineChart == null) return;

                // 1. Dapatkan tanggal hari ini
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                // LOG UNTUK DEBUG (Lihat di Logcat)
                Log.d(TAG, "Mencari data untuk tanggal: " + today);
                Log.d(TAG, "Path database saat ini: " + snapshot.getRef().toString());

                DataSnapshot todaySnapshot = snapshot.child(today);
                List<Entry> entries = new ArrayList<>();

                if (todaySnapshot.exists()) {
                    Log.d(TAG, "Data ditemukan, memproses item...");
                    for (DataSnapshot timeSnapshot : todaySnapshot.getChildren()) {
                        try {
                            String timeKey = timeSnapshot.getKey(); // Format HH:mm

                            // Gunakan Object agar aman, lalu cek tipenya
                            Object val = timeSnapshot.child("humidity").getValue();
                            float humidity = 0;

                            if (val instanceof Number) {
                                humidity = ((Number) val).floatValue();
                            } else if (val instanceof String) {
                                humidity = Float.parseFloat((String) val);
                            }

                            if (timeKey != null) {
                                String[] parts = timeKey.split(":");
                                if (parts.length == 2) {
                                    float hour = Float.parseFloat(parts[0]);
                                    float minute = Float.parseFloat(parts[1]);
                                    float xValue = hour + (minute / 60f); // Konversi jam ke desimal
                                    entries.add(new Entry(xValue, humidity));
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Gagal parse data pada: " + timeSnapshot.getKey());
                        }
                    }
                } else {
                    Log.w(TAG, "Snapshot untuk tanggal " + today + " TIDAK ADA di database.");
                }

                // Urutkan data berdasarkan waktu (X)
                entries.sort((e1, e2) -> Float.compare(e1.getX(), e2.getX()));

                if (!entries.isEmpty()) {
                    updateChartWithHistory(entries);
                    // Sembunyikan pesan "Belum ada data" jika ada variabel TextView-nya
                } else {
                    Log.w(TAG, "List entries kosong setelah proses loop.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "History listener error: " + error.getMessage());
            }
        };

        historyDb.addValueEventListener(rtdbHistoryListener);
    }

    private void removeRtdbListeners() {
        if (liveDb != null && rtdbLiveListener != null) {
            liveDb.removeEventListener(rtdbLiveListener);
            rtdbLiveListener = null;
        }

        if (settingsDb != null && rtdbSettingsListener != null) {
            settingsDb.removeEventListener(rtdbSettingsListener);
            rtdbSettingsListener = null;
        }

        if (controlDb != null && rtdbControlListener != null) {
            controlDb.removeEventListener(rtdbControlListener);
            rtdbControlListener = null;
        }

        if (historyDb != null && rtdbHistoryListener != null) {
            historyDb.removeEventListener(rtdbHistoryListener);
            rtdbHistoryListener = null;
        }
    }

    private void updateLiveUI(DeviceLive liveData) {
        isUpdatingFromSource = true;
        lastTelemetryTime = System.currentTimeMillis();

        int humidity = liveData.getHumidity();

        if (tvKelembapan != null) {
            tvKelembapan.setText(humidity + "%");
        }

        // Ini yang membuat data grafik otomatis masuk ke Firebase.
        saveDataToHistory(humidity);

        if (tvLampuStatus != null) {
            tvLampuStatus.setText(liveData.isLampStatus() ? "ON" : "OFF");
        }

        if (tvPompaStatus != null) {
            tvPompaStatus.setText(liveData.isPumpStatus() ? "ON" : "OFF");
        }

        if (tvStatusAlat != null) {
            if (liveData.isOnline()) {
                tvStatusAlat.setText("ONLINE");
            } else {
                updateOfflineStatus();
            }
        }

        if (tvStatus != null && liveData.isOnline()) {
            tvStatus.setText("Monitoring Aktif");
            tvStatus.setBackgroundResource(R.drawable.bg_status_watering);
        }

        isUpdatingFromSource = false;
    }

    private void saveDataToHistory(int humidity) {
        if (historyDb == null) return;

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());

        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(new Date());

        String uniqueMinuteKey = today + "_" + currentTime;

        // Supaya data tidak ditulis berkali-kali dalam menit yang sama.
        if (uniqueMinuteKey.equals(lastSavedMinuteKey)) {
            return;
        }

        lastSavedMinuteKey = uniqueMinuteKey;

        Map<String, Object> historyData = new HashMap<>();
        historyData.put("humidity", humidity);
        historyData.put("timestamp", System.currentTimeMillis());

        historyDb.child(today)
                .child(currentTime)
                .setValue(historyData)
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "History saved: " + today + " " + currentTime)
                )
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to save history: " + e.getMessage())
                );
    }

    private void updateChartWithHistory(List<Entry> entries) {
        if (lineChart == null || entries == null || entries.isEmpty()) return;

        LineData data = lineChart.getData();
        LineDataSet dataSet;

        if (data == null || data.getDataSetCount() == 0) {
            dataSet = createHumidityDataSet(entries);
            data = new LineData(dataSet);
            lineChart.setData(data);
        } else {
            dataSet = (LineDataSet) data.getDataSetByIndex(0);
            dataSet.setValues(entries);
            data.notifyDataChanged();
        }

        lineChart.notifyDataSetChanged();

        java.util.Calendar calendar = java.util.Calendar.getInstance();

        float currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                + (calendar.get(java.util.Calendar.MINUTE) / 60f);

        lineChart.setVisibleXRangeMaximum(4f);
        lineChart.moveViewToX(Math.max(0f, currentHour - 2f));
        lineChart.invalidate();
    }

    private LineDataSet createHumidityDataSet(List<Entry> entries) {
        LineDataSet dataSet = new LineDataSet(entries, "Kelembapan");

        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setColor(Color.parseColor("#1D9E75"));
        dataSet.setLineWidth(2.5f);

        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(Color.parseColor("#1D9E75"));
        dataSet.setCircleRadius(3f);

        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#1D9E75"));
        dataSet.setFillAlpha(35);

        return dataSet;
    }

    private void setupLineChart() {
        if (lineChart == null) return;

        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setNoDataText("Belum ada data kelembapan hari ini");

        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleXEnabled(true);
        lineChart.setScaleYEnabled(false);
        lineChart.setPinchZoom(false);
        lineChart.setDoubleTapToZoomEnabled(true);
        lineChart.setExtraOffsets(10f, 20f, 10f, 20f);

        YAxis yAxis = lineChart.getAxisLeft();
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

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#E0E0E0"));
        xAxis.setDrawAxisLine(false);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(24f);

        // Data disimpan per menit.
        // Label dibuat per 5 menit agar tidak terlalu penuh.
        xAxis.setGranularity(5f / 60f);
        xAxis.setGranularityEnabled(true);

        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int hour = (int) value;
                int minute = Math.round((value - hour) * 60);

                if (minute >= 60) {
                    hour++;
                    minute = 0;
                }

                return String.format(Locale.getDefault(), "%02d:%02d", hour % 24, minute);
            }
        });

        lineChart.getAxisRight().setEnabled(false);
        lineChart.setVisibleXRangeMaximum(4f);

        updateLimitLines();
    }

    private void updateLimitLines() {
        if (lineChart == null) return;

        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.removeAllLimitLines();

        LimitLine llMaks = new LimitLine(batasMaks, "Maks " + (int) batasMaks + "%");
        llMaks.setLineColor(Color.parseColor("#E24B4A"));
        llMaks.setLineWidth(1.2f);
        llMaks.setTextColor(Color.parseColor("#E24B4A"));
        llMaks.setTextSize(9f);
        llMaks.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);

        LimitLine llMin = new LimitLine(batasMin, "Min " + (int) batasMin + "%");
        llMin.setLineColor(Color.parseColor("#378ADD"));
        llMin.setLineWidth(1.2f);
        llMin.setTextColor(Color.parseColor("#378ADD"));
        llMin.setTextSize(9f);
        llMin.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);

        yAxis.addLimitLine(llMaks);
        yAxis.addLimitLine(llMin);

        lineChart.invalidate();
    }

    private void updateSettingsUIFromRtdb() {
        isUpdatingFromSource = true;

        if (seekBarMin != null) {
            seekBarMin.setProgress((int) batasMin);
        }

        if (seekBarMax != null) {
            seekBarMax.setProgress((int) batasMaks);
        }

        if (tvMinValue != null) {
            tvMinValue.setText((int) batasMin + "%");
        }

        if (tvMaxValue != null) {
            tvMaxValue.setText((int) batasMaks + "%");
        }

        updateLimitLines();

        isUpdatingFromSource = false;
    }

    private void updateControlUI(String lampMode, String pumpMode, Boolean lampManual, Boolean pumpManual) {
        isUpdatingFromSource = true;

        boolean isLampAuto = "AUTO".equalsIgnoreCase(lampMode);
        boolean isPumpAuto = "AUTO".equalsIgnoreCase(pumpMode);

        if (tvLampuModeLabel != null) {
            tvLampuModeLabel.setText(isLampAuto ? "Otomatis" : "Manual");
        }

        if (tvPompaModeLabel != null) {
            tvPompaModeLabel.setText(isPumpAuto ? "Otomatis" : "Manual");
        }

        if (switchLampuAuto != null) {
            switchLampuAuto.setChecked(isLampAuto);
        }

        if (switchPompaAuto != null) {
            switchPompaAuto.setChecked(isPumpAuto);
        }

        if (switchLampu != null) {
            switchLampu.setEnabled(!isLampAuto);
            if (lampManual != null) {
                switchLampu.setChecked(lampManual);
            }
        }

        if (switchPompa != null) {
            switchPompa.setEnabled(!isPumpAuto);
            if (pumpManual != null) {
                switchPompa.setChecked(pumpManual);
            }
        }

        isUpdatingFromSource = false;
    }

    private void integrasiKoleksiUserPlants() {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        DocumentReference userRef = db.collection("users").document(uid);

        userPlantListener = db.collection("userPlants")
                .whereEqualTo("ownerId", userRef)
                .limit(1)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "UserPlants listener error: " + e.getMessage());
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) return;

                    userPlantDocId = snapshots.getDocuments().get(0).getId();

                    UserPlant userPlant = snapshots.getDocuments().get(0).toObject(UserPlant.class);

                    if (userPlant != null) {
                        updateSettingsUI(userPlant);
                    }
                });
    }

    private void updateSettingsUI(UserPlant userPlant) {
        isUpdatingFromSource = true;

        if (tvHomePlantName != null) {
            tvHomePlantName.setText(userPlant.getDisplayName());
        }

        batasMin = userPlant.getHumidityMin();
        batasMaks = userPlant.getHumidityMax();

        if (seekBarMin != null) {
            seekBarMin.setProgress((int) batasMin);
        }

        if (seekBarMax != null) {
            seekBarMax.setProgress((int) batasMaks);
        }

        if (tvMinValue != null) {
            tvMinValue.setText((int) batasMin + "%");
        }

        if (tvMaxValue != null) {
            tvMaxValue.setText((int) batasMaks + "%");
        }

        updateLimitLines();

        isUpdatingFromSource = false;
    }

    private void integrasiKoleksiDevices() {
        if (auth.getCurrentUser() == null) return;

        deviceListener = db.collection("devices")
                .document(DEVICE_ID)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Device listener error: " + e.getMessage());
                        return;
                    }

                    if (snapshot != null && snapshot.exists() && isAdded()) {
                        String name = snapshot.getString("deviceName");

                        if (name != null && tvDeviceName != null) {
                            tvDeviceName.setText(name);
                        }
                    }
                });
    }

    private void ambilDataUserDanTanaman() {
        if (auth.getCurrentUser() == null) return;

        db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && isAdded()) {
                        String name = doc.getString("nickName");

                        if (name != null && tvUserName != null) {
                            tvUserName.setText(name);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed get user: " + e.getMessage())
                );
    }

    private void setupSeekBars() {
        if (seekBarMin == null || seekBarMax == null) return;

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || isUpdatingFromSource) return;

                if (seekBar.getId() == R.id.seekBarMin) {
                    batasMin = progress;

                    if (tvMinValue != null) {
                        tvMinValue.setText(progress + "%");
                    }
                } else if (seekBar.getId() == R.id.seekBarMax) {
                    batasMaks = progress;

                    if (tvMaxValue != null) {
                        tvMaxValue.setText(progress + "%");
                    }
                }

                updateLimitLines();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                pushSettingsToDatabase();
            }
        };

        seekBarMin.setOnSeekBarChangeListener(listener);
        seekBarMax.setOnSeekBarChangeListener(listener);
    }

    private void pushSettingsToDatabase() {
        Map<String, Object> rtdbUpdates = new HashMap<>();
        rtdbUpdates.put("humidityMin", (int) batasMin);
        rtdbUpdates.put("humidityMax", (int) batasMaks);
        rtdbUpdates.put("updatedAt", System.currentTimeMillis());

        settingsDb.updateChildren(rtdbUpdates)
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed update settings RTDB: " + e.getMessage())
                );

        if (!userPlantDocId.isEmpty()) {
            Map<String, Object> firestoreUpdates = new HashMap<>();
            firestoreUpdates.put("humidityMin", (int) batasMin);
            firestoreUpdates.put("humidityMax", (int) batasMaks);

            db.collection("userPlants")
                    .document(userPlantDocId)
                    .update(firestoreUpdates)
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed update settings Firestore: " + e.getMessage())
                    );
        }
    }

    private void setupSwitchListeners() {
        // --- Switch Lampu ON/OFF (Manual) ---
        if (switchLampu != null) {
            switchLampu.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isUpdatingFromSource) return;

                // PERBAIKAN: Ubah teks status secara instan di UI
                if (tvLampuStatus != null) {
                    tvLampuStatus.setText(isChecked ? "ON" : "OFF");
                }

                controlDb.child("lampManualState").setValue(isChecked);
                controlDb.child("updatedAt").setValue(System.currentTimeMillis());
            });
        }

        // --- Switch Pompa ON/OFF (Manual) ---
        if (switchPompa != null) {
            switchPompa.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isUpdatingFromSource) return;

                // PERBAIKAN: Ubah teks status secara instan di UI
                if (tvPompaStatus != null) {
                    tvPompaStatus.setText(isChecked ? "ON" : "OFF");
                }

                controlDb.child("pumpManualState").setValue(isChecked);
                controlDb.child("updatedAt").setValue(System.currentTimeMillis());
            });
        }

        // --- Switch Mode AUTO Lampu ---
        if (switchLampuAuto != null) {
            switchLampuAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isUpdatingFromSource) return;

                String mode = isChecked ? "AUTO" : "MANUAL";
                if (tvLampuModeLabel != null) {
                    tvLampuModeLabel.setText(isChecked ? "Otomatis" : "Manual");
                }

                if (switchLampu != null) {
                    switchLampu.setEnabled(!isChecked);
                }

                controlDb.child("lampMode").setValue(mode);
                controlDb.child("updatedAt").setValue(System.currentTimeMillis());
                updateModeInDatabase("lampMode", mode);
            });
        }

        // --- Switch Mode AUTO Pompa ---
        if (switchPompaAuto != null) {
            switchPompaAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isUpdatingFromSource) return;

                String mode = isChecked ? "AUTO" : "MANUAL";
                if (tvPompaModeLabel != null) {
                    tvPompaModeLabel.setText(isChecked ? "Otomatis" : "Manual");
                }

                if (switchPompa != null) {
                    switchPompa.setEnabled(!isChecked);
                }

                controlDb.child("pumpMode").setValue(mode);
                controlDb.child("updatedAt").setValue(System.currentTimeMillis());
                updateModeInDatabase("pumpMode", mode);
            });
        }
    }

    private void updateModeInDatabase(String field, String mode) {
        if (!userPlantDocId.isEmpty()) {
            db.collection("userPlants")
                    .document(userPlantDocId)
                    .update(field, mode)
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed update mode: " + e.getMessage())
                    );
        }
    }
}