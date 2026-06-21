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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.greenhouse.R;
import com.example.greenhouse.activity.MainActivity;
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

    // VIEWS — Informasi & Status
    private TextView tvUserName, tvKelembapan, tvStatusAlat, tvStatus;
    private TextView tvLampuStatus, tvPompaStatus;
    private TextView tvLampuModeLabel, tvPompaModeLabel;
    private TextView tvHomePlantName, tvDeviceName;
    private ImageView ivPlant;

    // VIEWS — Switch Kontrol
    private SwitchMaterial switchLampu, switchPompa, switchLampuAuto, switchPompaAuto;

    // VIEWS — Grafik & SeekBar
    private LineChart lineChart;
    private SeekBar seekBarMin, seekBarMax;
    private TextView tvMinValue, tvMaxValue;

    // FIREBASE
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private DatabaseReference liveDb, settingsDb, controlDb, historyDb;
    private ListenerRegistration deviceListener;
    private ListenerRegistration userPlantListener;
    private ValueEventListener rtdbLiveListener, rtdbSettingsListener, rtdbControlListener, rtdbHistoryListener;

    private String userPlantDocId = "";
    private String lastRecordedMinute = "";

    // DATA GRAFIK
    private float batasMin = 60f, batasMaks = 80f;

    // FLAGS
    private boolean isUpdatingFromSource = false;
    private long lastTelemetryTime = 0;
    private android.os.Handler heartbeatHandler = new android.os.Handler();
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
        if (tvStatusAlat != null) tvStatusAlat.setText("OFFLINE");
        if (tvStatus != null) {
            tvStatus.setText("Monitoring Nonaktif");
            tvStatus.setBackgroundResource(R.drawable.bg_status_watering);
        }
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
        if (heartbeatHandler != null && heartbeatRunnable != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
        }
        if (deviceListener != null) deviceListener.remove();
        if (userPlantListener != null) userPlantListener.remove();
        removeRtdbListeners();
    }

    private void removeRtdbListeners() {
        if (liveDb != null && rtdbLiveListener != null) liveDb.removeEventListener(rtdbLiveListener);
        if (settingsDb != null && rtdbSettingsListener != null) settingsDb.removeEventListener(rtdbSettingsListener);
        if (controlDb != null && rtdbControlListener != null) controlDb.removeEventListener(rtdbControlListener);
        if (historyDb != null && rtdbHistoryListener != null) historyDb.removeEventListener(rtdbHistoryListener);
    }

    private void initViews(View view) {
        tvUserName    = view.findViewById(R.id.tvUserName);
        tvLampuStatus = view.findViewById(R.id.tvLampuStatus);
        tvPompaStatus = view.findViewById(R.id.tvPompaStatus);
        tvStatus      = view.findViewById(R.id.tvStatus);
        switchLampu   = view.findViewById(R.id.switchLampu);
        switchPompa   = view.findViewById(R.id.switchPompa);
        lineChart     = view.findViewById(R.id.lineChart);
        seekBarMin    = view.findViewById(R.id.seekBarMin);
        seekBarMax    = view.findViewById(R.id.seekBarMax);
        tvMinValue    = view.findViewById(R.id.tvMinValue);
        tvMaxValue    = view.findViewById(R.id.tvMaxValue);

        tvHomePlantName = view.findViewById(R.id.tvHomePlantName);
        tvDeviceName = view.findViewById(R.id.tvDeviceName);
        ivPlant = view.findViewById(R.id.ivPlant);

        tvKelembapan  = view.findViewById(R.id.tvKelembapan);
        tvStatusAlat  = view.findViewById(R.id.tvStatusAlat);
        tvLampuModeLabel = view.findViewById(R.id.tvLampuModeLabel);
        tvPompaModeLabel = view.findViewById(R.id.tvPompaModeLabel);
        switchLampuAuto = view.findViewById(R.id.switchLampuAuto);
        switchPompaAuto = view.findViewById(R.id.switchPompaAuto);

        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        if (tvUserName != null) tvUserName.setText(prefs.getString("nickName", "User"));
    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseDatabase rtdb = FirebaseDatabase.getInstance();

        // Path disesuaikan dengan struktur database: history > GH-001 > minutely
        liveDb = rtdb.getReference("telemetry").child("GH-001");
        settingsDb = rtdb.getReference("settings").child("GH-001");
        controlDb = rtdb.getReference("control").child("GH-001");
        historyDb = rtdb.getReference("history").child("GH-001").child("minutely");
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
            @Override public void onCancelled(@NonNull DatabaseError error) {}
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
            @Override public void onCancelled(@NonNull DatabaseError error) {}
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
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        controlDb.addValueEventListener(rtdbControlListener);

        integrasiRTDBHistory();
    }

    private void integrasiRTDBHistory() {
        rtdbHistoryListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                List<Entry> entries = new ArrayList<>();
                DataSnapshot todaySnapshot = snapshot.child(today);

                if (todaySnapshot.exists()) {
                    for (DataSnapshot timeSnapshot : todaySnapshot.getChildren()) {
                        try {
                            String timeKey = timeSnapshot.getKey(); // e.g. "13:00"
                            Float humidity = timeSnapshot.child("humidity").getValue(Float.class);
                            if (timeKey != null && humidity != null) {
                                String[] parts = timeKey.split(":");
                                float hour = Float.parseFloat(parts[0]);
                                float minute = Float.parseFloat(parts[1]);
                                float xValue = hour + (minute / 60f);
                                entries.add(new Entry(xValue, humidity));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing history: " + e.getMessage());
                        }
                    }
                }
                updateChartWithHistory(entries);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        historyDb.addValueEventListener(rtdbHistoryListener);
    }

    private void updateLiveUI(DeviceLive liveData) {
        if (!isAdded() || liveData == null) return;

        // Tambahkan Log untuk melihat data yang masuk di Logcat
        Log.d(TAG, "Data Masuk -> Kelembapan: " + liveData.getHumidity());

        isUpdatingFromSource = true;
        lastTelemetryTime = System.currentTimeMillis();

        // Pastikan tvKelembapan di-update
        if (tvKelembapan != null) {
            tvKelembapan.setText(liveData.getHumidity() + "%");
        }

        // Update status alat (Online/Offline)
        if (tvStatusAlat != null) {
            tvStatusAlat.setText(liveData.isOnline() ? "ONLINE" : "OFFLINE");
            tvStatusAlat.setTextColor(liveData.isOnline() ? Color.parseColor("#1D9E75") : Color.RED);
        }

        // Status Monitoring
        if (tvStatus != null && liveData.isOnline()) {
            tvStatus.setText("Monitoring Aktif");
            tvStatus.setBackgroundResource(R.drawable.bg_status_watering);
        }

        saveDataToHistory(liveData.getHumidity());

        // Status Lampu & Pompa (Gunakan isLampStatus() dan isPumpStatus() sesuai Model)
        if (tvLampuStatus != null) tvLampuStatus.setText(liveData.isLampStatus() ? "ON" : "OFF");
        if (tvPompaStatus != null) tvPompaStatus.setText(liveData.isPumpStatus() ? "ON" : "OFF");

        isUpdatingFromSource = false;
    }

    private void saveDataToHistory(int humidity) {
        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        if (currentTime.equals(lastRecordedMinute)) return;

        lastRecordedMinute = currentTime;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Map<String, Object> historyData = new HashMap<>();
        historyData.put("humidity", humidity);
        historyData.put("timestamp", System.currentTimeMillis());

        historyDb.child(today).child(currentTime).setValue(historyData);
    }

    private void updateSettingsUIFromRtdb() {
        isUpdatingFromSource = true;
        if (seekBarMin != null) seekBarMin.setProgress((int) batasMin);
        if (seekBarMax != null) seekBarMax.setProgress((int) batasMaks);
        if (tvMinValue != null) tvMinValue.setText((int) batasMin + "%");
        if (tvMaxValue != null) tvMaxValue.setText((int) batasMaks + "%");
        updateLimitLines();
        isUpdatingFromSource = false;
    }

    private void updateControlUI(String lampMode, String pumpMode, Boolean lampManual, Boolean pumpManual) {
        isUpdatingFromSource = true;
        boolean isLampAuto = "AUTO".equalsIgnoreCase(lampMode);
        boolean isPumpAuto = "AUTO".equalsIgnoreCase(pumpMode);

        if (tvLampuModeLabel != null) tvLampuModeLabel.setText(isLampAuto ? "Otomatis" : "Manual");
        if (tvPompaModeLabel != null) tvPompaModeLabel.setText(isPumpAuto ? "Otomatis" : "Manual");

        if (switchLampuAuto != null) switchLampuAuto.setChecked(isLampAuto);
        if (switchPompaAuto != null) switchPompaAuto.setChecked(isPumpAuto);

        if (switchLampu != null) {
            switchLampu.setEnabled(!isLampAuto);
            if (lampManual != null) switchLampu.setChecked(lampManual);
        }
        if (switchPompa != null) {
            switchPompa.setEnabled(!isPumpAuto);
            if (pumpManual != null) switchPompa.setChecked(pumpManual);
        }
        isUpdatingFromSource = false;
    }

    private void updateChartWithHistory(List<Entry> entries) {
        if (lineChart == null) return;
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

        lineChart.setData(new LineData(dataSet));

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        float currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY) + (calendar.get(java.util.Calendar.MINUTE) / 60f);
        lineChart.setVisibleXRangeMaximum(4f);
        lineChart.moveViewToX(currentHour - 2f);
        lineChart.invalidate();
    }

    private void integrasiKoleksiUserPlants() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        DocumentReference userRef = db.collection("users").document(uid);

        userPlantListener = db.collection("userPlants")
                .whereEqualTo("ownerId", userRef)
                .limit(1)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null || snapshots.isEmpty()) return;
                    userPlantDocId = snapshots.getDocuments().get(0).getId();
                    UserPlant userPlant = snapshots.getDocuments().get(0).toObject(UserPlant.class);
                    if (userPlant != null) updateSettingsUI(userPlant);
                });
    }

    private void updateSettingsUI(UserPlant userPlant) {
        isUpdatingFromSource = true;
        if (tvHomePlantName != null) tvHomePlantName.setText(userPlant.getDisplayName());
        batasMin = userPlant.getHumidityMin();
        batasMaks = userPlant.getHumidityMax();
        if (seekBarMin != null) seekBarMin.setProgress((int) batasMin);
        if (seekBarMax != null) seekBarMax.setProgress((int) batasMaks);
        if (tvMinValue != null) tvMinValue.setText((int) batasMin + "%");
        if (tvMaxValue != null) tvMaxValue.setText((int) batasMaks + "%");
        updateLimitLines();
        isUpdatingFromSource = false;
    }

    private void integrasiKoleksiDevices() {
        if (auth.getCurrentUser() == null) return;
        deviceListener = db.collection("devices").document("GH-001")
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists() && isAdded()) {
                        String name = snapshot.getString("deviceName");
                        if (tvDeviceName != null) tvDeviceName.setText(name);
                    }
                });
    }

    private void ambilDataUserDanTanaman() {
        if (auth.getCurrentUser() == null) return;
        db.collection("users").document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && isAdded()) {
                        String name = doc.getString("nickName");
                        if (name != null && tvUserName != null) tvUserName.setText(name);
                    }
                });
    }

    private void setupLineChart() {
        if (lineChart == null) return;
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleXEnabled(true);
        lineChart.setScaleYEnabled(false);

        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(100f);
        yAxis.setDrawAxisLine(false);
        yAxis.setGridColor(Color.parseColor("#E0E0E0"));

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(24f);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                return String.format("%02d:00", (int) value % 24);
            }
        });
        lineChart.getAxisRight().setEnabled(false);
        updateLimitLines();
    }

    private void updateLimitLines() {
        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.removeAllLimitLines();
        LimitLine llMaks = new LimitLine(batasMaks, "Maks");
        llMaks.setLineColor(Color.parseColor("#E24B4A"));
        LimitLine llMin = new LimitLine(batasMin, "Min");
        llMin.setLineColor(Color.parseColor("#378ADD"));
        yAxis.addLimitLine(llMaks);
        yAxis.addLimitLine(llMin);
        lineChart.invalidate();
    }

    private void setupSeekBars() {
        if (seekBarMin == null || seekBarMax == null) return;
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || isUpdatingFromSource) return;
                if (seekBar.getId() == R.id.seekBarMin) {
                    batasMin = progress;
                    if (tvMinValue != null) tvMinValue.setText(progress + "%");
                } else {
                    batasMaks = progress;
                    if (tvMaxValue != null) tvMaxValue.setText(progress + "%");
                }
                updateLimitLines();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) { pushSettingsToDatabase(); }
        };
        seekBarMin.setOnSeekBarChangeListener(listener);
        seekBarMax.setOnSeekBarChangeListener(listener);
    }

    private void pushSettingsToDatabase() {
        Map<String, Object> rtdbUpdates = new HashMap<>();
        rtdbUpdates.put("humidityMin", (int) batasMin);
        rtdbUpdates.put("humidityMax", (int) batasMaks);
        rtdbUpdates.put("updatedAt", System.currentTimeMillis());
        settingsDb.updateChildren(rtdbUpdates);
        if (!userPlantDocId.isEmpty()) {
            db.collection("userPlants").document(userPlantDocId).update("humidityMin", (int) batasMin, "humidityMax", (int) batasMaks);
        }
    }

    private void setupSwitchListeners() {
        if (switchLampu != null) {
            switchLampu.setOnCheckedChangeListener((v, isChecked) -> {
                if (isUpdatingFromSource) return;
                controlDb.child("lampManualState").setValue(isChecked);
                controlDb.child("updatedAt").setValue(System.currentTimeMillis());
            });
        }
        if (switchPompa != null) {
            switchPompa.setOnCheckedChangeListener((v, isChecked) -> {
                if (isUpdatingFromSource) return;
                controlDb.child("pumpManualState").setValue(isChecked);
                controlDb.child("updatedAt").setValue(System.currentTimeMillis());
            });
        }
        if (switchLampuAuto != null) {
            switchLampuAuto.setOnCheckedChangeListener((v, isChecked) -> {
                if (isUpdatingFromSource) return;
                String mode = isChecked ? "AUTO" : "MANUAL";
                controlDb.child("lampMode").setValue(mode);
                controlDb.child("updatedAt").setValue(System.currentTimeMillis());
                updateModeInDatabase("lampMode", mode);
            });
        }
        if (switchPompaAuto != null) {
            switchPompaAuto.setOnCheckedChangeListener((v, isChecked) -> {
                if (isUpdatingFromSource) return;
                String mode = isChecked ? "AUTO" : "MANUAL";
                controlDb.child("pumpMode").setValue(mode);
                controlDb.child("updatedAt").setValue(System.currentTimeMillis());
                updateModeInDatabase("pumpMode", mode);
            });
        }
    }

    private void updateModeInDatabase(String field, String mode) {
        if (!userPlantDocId.isEmpty()) db.collection("userPlants").document(userPlantDocId).update(field, mode);
    }
}
