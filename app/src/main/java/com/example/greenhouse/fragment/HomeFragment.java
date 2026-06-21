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
    private TextView tvUserName, tvKelembapan, tvLdrValue, tvStatusAlat, tvStatus;
    private TextView tvLampuStatus, tvLampuMode, tvPompaStatus, tvPompaMode;
    private TextView tvLampuModeLabel, tvPompaModeLabel;
    private TextView tvHomePlantName, tvDeviceName, tvLocation;
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

    // MQTT — Topic constants
    private final String topicSoil         = "esp32/soil";
    private final String topicLdr          = "esp32/ldr";
    private final String topicDeviceStatus = "greenhouse/ben10/device/status";
    private final String topicLampStatus   = "esp32/lamp/status";
    private final String topicPumpStatus   = "esp32/pump/status";
    private final String topicLampMode     = "esp32/lamp/mode";
    private final String topicPumpMode     = "esp32/pump/mode";

    // MQTT — Topic Publish
    private final String topicLampCmd = "esp32/lamp/cmd";
    private final String topicPumpCmd = "esp32/pump/cmd";

    // DATA GRAFIK
    private float batasMin = 60f, batasMaks = 80f;

    // FLAGS — Untuk mencegah Race Condition
    private boolean isUpdatingFromSource = false;
    private long lastTelemetryTime = 0;
    private android.os.Handler heartbeatHandler = new android.os.Handler();
    private Runnable heartbeatRunnable;

    // Listener untuk menerima data dari MainActivity
    private MainActivity.OnMqttMessageListener mqttListener;

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
                    // Jika data tidak update lebih dari 30 detik, anggap offline
                    if (lastTelemetryTime > 0 && (now - lastTelemetryTime) > 30000) {
                        updateOfflineStatus();
                    }
                    heartbeatHandler.postDelayed(this, 10000); // Cek tiap 10 detik
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mqttListener = (topic, payload) -> {
            if (isAdded()) {
                handleIncomingMessage(topic, payload);
            }
        };

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).addMqttListener(mqttListener);
        }

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
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).removeMqttListener(mqttListener);
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

    private void removeRtdbListeners() {
        if (liveDb != null && rtdbLiveListener != null) {
            liveDb.removeEventListener(rtdbLiveListener);
        }
        if (settingsDb != null && rtdbSettingsListener != null) {
            settingsDb.removeEventListener(rtdbSettingsListener);
        }
        if (controlDb != null && rtdbControlListener != null) {
            controlDb.removeEventListener(rtdbControlListener);
        }
        if (historyDb != null && rtdbHistoryListener != null) {
            historyDb.removeEventListener(rtdbHistoryListener);
        }
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
        tvLocation = view.findViewById(R.id.tvLocation);
        ivPlant = view.findViewById(R.id.ivPlant);

        tvKelembapan  = findOptionalTextView(view, "tvKelembapan");
        tvLdrValue    = findOptionalTextView(view, "tvLdrValue");
        tvStatusAlat  = findOptionalTextView(view, "tvStatusAlat");
        tvLampuMode   = findOptionalTextView(view, "tvLampuMode");
        tvPompaMode   = findOptionalTextView(view, "tvPompaMode");
        tvLampuModeLabel = findOptionalTextView(view, "tvLampuModeLabel");
        tvPompaModeLabel = findOptionalTextView(view, "tvPompaModeLabel");
        switchLampuAuto = findOptionalSwitch(view, "switchLampuAuto");
        switchPompaAuto = findOptionalSwitch(view, "switchPompaAuto");

        if (tvStatus != null)     tvStatus.setText("Menghubungkan...");
        if (tvStatusAlat != null) tvStatusAlat.setText("WAITING...");

        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        if (tvUserName != null) tvUserName.setText(prefs.getString("nickName", "User"));
    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseDatabase rtdb = FirebaseDatabase.getInstance();
        
        // Path disesuaikan dengan struktur database
        liveDb = rtdb.getReference("telemetry").child("GH-001");
        settingsDb = rtdb.getReference("settings").child("GH-001");
        controlDb = rtdb.getReference("control").child("GH-001");
        historyDb = rtdb.getReference("history").child("GH-001").child("hourly");
    }

    private void integrasiRTDB() {
        // 1. Live Data (Kelembapan, Status Real-time)
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

        // 2. Settings (Batas Kelembapan)
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

        // 3. Control (Mode & Manual State)
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

        // 4. History (Untuk Grafik)
        integrasiRTDBHistory();
    }

    private void integrasiRTDBHistory() {
        rtdbHistoryListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                
                // Mendapatkan tanggal hari ini secara dinamis agar tidak reset saat berganti hari
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                String today = sdf.format(new java.util.Date());

                List<Entry> entries = new ArrayList<>();
                DataSnapshot todaySnapshot = snapshot.child(today);
                
                if (todaySnapshot.exists()) {
                    for (DataSnapshot hourSnapshot : todaySnapshot.getChildren()) {
                        try {
                            String timeKey = hourSnapshot.getKey(); // e.g. "00:00"
                            Float humidity = hourSnapshot.child("humidity").getValue(Float.class);
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

                // Update chart dengan data history (meskipun kosong, agar chart ter-reset di hari baru)
                updateChartWithHistory(entries);
                
                // Pindahkan view ke jam sekarang agar grafik akurat
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                float currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY) + (calendar.get(java.util.Calendar.MINUTE) / 60f);
                lineChart.setVisibleXRangeMaximum(1f); // Pastikan zoom detail aktif
                lineChart.moveViewToX(currentHour);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        historyDb.addValueEventListener(rtdbHistoryListener);
    }

    private void updateLiveUI(DeviceLive liveData) {
        isUpdatingFromSource = true;
        lastTelemetryTime = System.currentTimeMillis(); // Update heartbeat

        if (tvKelembapan != null) tvKelembapan.setText(liveData.getHumidity() + "%");
        addSoilToChart(String.valueOf(liveData.getHumidity()));
        
        String lampStr = liveData.isLampStatus() ? "ON" : "OFF";
        String pumpStr = liveData.isPumpStatus() ? "ON" : "OFF";
        
        if (tvLampuStatus != null) tvLampuStatus.setText(lampStr);
        if (tvPompaStatus != null) tvPompaStatus.setText(pumpStr);

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
        if (tvLampuMode != null) tvLampuMode.setText(lampMode);
        if (tvPompaMode != null) tvPompaMode.setText(pumpMode);

        boolean isLampAuto = "AUTO".equalsIgnoreCase(lampMode);
        boolean isPumpAuto = "AUTO".equalsIgnoreCase(pumpMode);

        // Update Label Manual/Otomatis
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
        LineData data = lineChart.getData();
        LineDataSet dataSet;
        
        if (data == null) {
            dataSet = new LineDataSet(entries, "Kelembapan");
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            dataSet.setColor(Color.parseColor("#1D9E75"));
            dataSet.setLineWidth(2.5f);
            dataSet.setDrawCircles(true); // Aktifkan lingkaran agar titik data terlihat akurat
            dataSet.setCircleColor(Color.parseColor("#1D9E75"));
            dataSet.setCircleRadius(3f);
            dataSet.setDrawValues(false);
            dataSet.setDrawFilled(true);
            dataSet.setFillColor(Color.parseColor("#1D9E75"));
            dataSet.setFillAlpha(35);
            data = new LineData(dataSet);
            lineChart.setData(data);
        } else {
            dataSet = (LineDataSet) data.getDataSetByIndex(0);
            dataSet.setValues(entries);
            data.notifyDataChanged();
        }
        lineChart.notifyDataSetChanged();
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
                    if (userPlant != null) {
                        updateSettingsUI(userPlant);
                    }
                });
    }

    private void updateSettingsUI(UserPlant userPlant) {
        isUpdatingFromSource = true;
        if (tvHomePlantName != null) tvHomePlantName.setText(userPlant.getDisplayName());
        if (tvLocation != null) tvLocation.setText("Lokasi: " + userPlant.getLocation());
        batasMin = userPlant.getHumidityMin();
        batasMaks = userPlant.getHumidityMax();
        if (seekBarMin != null) seekBarMin.setProgress((int) batasMin);
        if (seekBarMax != null) seekBarMax.setProgress((int) batasMaks);
        if (tvMinValue != null) tvMinValue.setText((int) batasMin + "%");
        if (tvMaxValue != null) tvMaxValue.setText((int) batasMaks + "%");
        updateLimitLines();
        boolean isLampAuto = "AUTO".equalsIgnoreCase(userPlant.getLampMode());
        boolean isPumpAuto = "AUTO".equalsIgnoreCase(userPlant.getPumpMode());
        if (switchLampuAuto != null) switchLampuAuto.setChecked(isLampAuto);
        if (switchPompaAuto != null) switchPompaAuto.setChecked(isPumpAuto);
        if (switchLampu != null) switchLampu.setEnabled(!isLampAuto);
        if (switchPompa != null) switchPompa.setEnabled(!isPumpAuto);
        if (tvLampuMode != null) tvLampuMode.setText(userPlant.getLampMode());
        if (tvPompaMode != null) tvPompaMode.setText(userPlant.getPumpMode());

        if (tvLampuModeLabel != null) tvLampuModeLabel.setText(isLampAuto ? "Otomatis" : "Manual");
        if (tvPompaModeLabel != null) tvPompaModeLabel.setText(isPumpAuto ? "Otomatis" : "Manual");

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
        lineChart.setExtraOffsets(10f, 40f, 10f, 20f);
        
        // Horizontal Scroll & Responsiveness
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleXEnabled(true);
        lineChart.setScaleYEnabled(false);
        lineChart.setPinchZoom(false);
        lineChart.setDoubleTapToZoomEnabled(true);

        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(100f);
        yAxis.setLabelCount(6, false);
        yAxis.setGranularity(20f);
        yAxis.setDrawAxisLine(false);
        yAxis.setGridColor(Color.parseColor("#E0E0E0"));
        yAxis.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) { return (int) value + "%"; }
        });

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#E0E0E0"));
        xAxis.setDrawAxisLine(false);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(24f);
        xAxis.setGranularity(5f / 60f); // Label per 5 menit
        xAxis.setGranularityEnabled(true);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int hour = (int) value;
                int minute = Math.round((value - hour) * 60);
                if (minute >= 60) { hour++; minute = 0; }
                return String.format("%02d:%02d", hour % 24, minute);
            }
        });
        lineChart.getAxisRight().setEnabled(false);
        
        // Tampilkan rentang 1 jam agar detail menit terlihat dan bisa digeser
        lineChart.setVisibleXRangeMaximum(1f); 

        updateLimitLines();
    }

    private void updateLimitLines() {
        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.removeAllLimitLines();
        LimitLine llMaks = new LimitLine(batasMaks, "Maks " + (int)batasMaks + "%");
        llMaks.setLineColor(Color.parseColor("#E24B4A"));
        llMaks.setLineWidth(1.2f);
        llMaks.setTextColor(Color.parseColor("#E24B4A"));
        llMaks.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        llMaks.setTextSize(9f);
        LimitLine llMin = new LimitLine(batasMin, "Min " + (int)batasMin + "%");
        llMin.setLineColor(Color.parseColor("#378ADD"));
        llMin.setLineWidth(1.2f);
        llMin.setTextColor(Color.parseColor("#378ADD"));
        llMin.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
        llMin.setTextSize(9f);
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
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                pushSettingsToDatabase();
            }
        };
        seekBarMin.setOnSeekBarChangeListener(listener);
        seekBarMax.setOnSeekBarChangeListener(listener);
    }

    private void pushSettingsToDatabase() {
        // 1. Update RTDB (Untuk ESP32)
        Map<String, Object> rtdbUpdates = new HashMap<>();
        rtdbUpdates.put("humidityMin", (int) batasMin);
        rtdbUpdates.put("humidityMax", (int) batasMaks);
        rtdbUpdates.put("updatedAt", System.currentTimeMillis());
        settingsDb.updateChildren(rtdbUpdates);

        // 2. Update Firestore (Untuk Persistence)
        if (!userPlantDocId.isEmpty()) {
            Map<String, Object> firestoreUpdates = new HashMap<>();
            firestoreUpdates.put("humidityMin", (int) batasMin);
            firestoreUpdates.put("humidityMax", (int) batasMaks);
            db.collection("userPlants").document(userPlantDocId).update(firestoreUpdates);
        }
    }

    private void setupSwitchListeners() {
        if (switchLampu != null) {
            switchLampu.setOnCheckedChangeListener((v, isChecked) -> {
                if (isUpdatingFromSource) return;
                
                // Update Firebase RTDB
                controlDb.child("lampManualState").setValue(isChecked);
                controlDb.child("updatedAt").setValue(System.currentTimeMillis());
                
                // Publish via MQTT (Opsional)
                publishCommand(topicLampCmd, isChecked ? "ON" : "OFF");
            });
        }
        if (switchPompa != null) {
            switchPompa.setOnCheckedChangeListener((v, isChecked) -> {
                if (isUpdatingFromSource) return;
                
                // Update Firebase RTDB
                controlDb.child("pumpManualState").setValue(isChecked);
                controlDb.child("updatedAt").setValue(System.currentTimeMillis());
                
                // Publish via MQTT (Opsional)
                publishCommand(topicPumpCmd, isChecked ? "ON" : "OFF");
            });
        }
        if (switchLampuAuto != null) {
            switchLampuAuto.setOnCheckedChangeListener((v, isChecked) -> {
                if (isUpdatingFromSource) return;
                String mode = isChecked ? "AUTO" : "MANUAL";
                
                // Update ke RTDB
                controlDb.child("lampMode").setValue(mode);
                controlDb.child("updatedAt").setValue(System.currentTimeMillis());

                // Update Firestore
                updateModeInDatabase("lampMode", mode);
                publishCommand(topicLampCmd, mode);
            });
        }
        if (switchPompaAuto != null) {
            switchPompaAuto.setOnCheckedChangeListener((v, isChecked) -> {
                if (isUpdatingFromSource) return;
                String mode = isChecked ? "AUTO" : "MANUAL";

                // Update ke RTDB
                controlDb.child("pumpMode").setValue(mode);
                controlDb.child("updatedAt").setValue(System.currentTimeMillis());

                // Update Firestore
                updateModeInDatabase("pumpMode", mode);
                publishCommand(topicPumpCmd, mode);
            });
        }
    }
    
    private void updateModeInDatabase(String field, String mode) {
        if (!userPlantDocId.isEmpty()) {
            db.collection("userPlants").document(userPlantDocId).update(field, mode);
        }
    }

    private void handleIncomingMessage(String topic, String payload) {
        if (topic.equals(topicLdr)) {
            if (tvLdrValue != null) tvLdrValue.setText(payload);
        } else if (topic.equals(topicDeviceStatus)) {
            if (tvStatusAlat != null) tvStatusAlat.setText(payload);
        }
    }

    private void publishCommand(String topic, String command) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).publishCommand(topic, command);
        }
    }

    private void addSoilToChart(String payload) {
        try {
            float soilValue = Float.parseFloat(payload);

            // Hitung nilai X berdasarkan waktu saat ini (0 - 24 jam)
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            float hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
            float minute = calendar.get(java.util.Calendar.MINUTE);
            float xValue = hour + (minute / 60f);

            LineData data = lineChart.getData();
            if (data == null) {
                LineDataSet dataSet = new LineDataSet(new ArrayList<>(), "Kelembapan");
                dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                dataSet.setColor(Color.parseColor("#1D9E75"));
                dataSet.setLineWidth(2.5f);
                dataSet.setDrawCircles(false);
                dataSet.setDrawValues(false);
                dataSet.setDrawFilled(true);
                dataSet.setFillColor(Color.parseColor("#1D9E75"));
                dataSet.setFillAlpha(35);
                data = new LineData(dataSet);
                lineChart.setData(data);
            }

            // Jika hari berganti (xValue baru sangat kecil dibandingkan XMax yang sudah mencapai akhir hari)
            if (data.getEntryCount() > 0 && xValue < 1.0f && data.getXMax() > 23.0f) {
                data.getDataSetByIndex(0).clear();
            }

            data.addEntry(new Entry(xValue, soilValue), 0);
            data.notifyDataChanged();
            lineChart.notifyDataSetChanged();

            // Set tampilan agar lebih detail dan bisa digeser
            lineChart.setVisibleXRangeMaximum(1f);
            lineChart.moveViewToX(xValue);
            lineChart.invalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private TextView findOptionalTextView(View view, String idName) {
        int id = getResources().getIdentifier(idName, "id", requireContext().getPackageName());
        return (id == 0) ? null : view.findViewById(id);
    }

    private SwitchMaterial findOptionalSwitch(View view, String idName) {
        int id = getResources().getIdentifier(idName, "id", requireContext().getPackageName());
        return (id == 0) ? null : view.findViewById(id);
    }
}
