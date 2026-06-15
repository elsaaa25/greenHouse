package com.example.greenhouse.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.greenhouse.R;
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
import com.google.firebase.firestore.FirebaseFirestore;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    // =========================================================
    // VIEWS — Informasi & Status
    // =========================================================
    private TextView tvUserName;
    private TextView tvKelembapan;     // Nilai kelembapan tanah (dari MQTT)
    private TextView tvLdrValue;       // Nilai sensor LDR (dari MQTT)
    private TextView tvStatusAlat;     // Status alat ESP32 (ONLINE/OFFLINE)
    private TextView tvStatus;         // Label monitoring aktif/nonaktif

    // VIEWS — Status & Mode Lampu
    private TextView tvLampuStatus;    // Status lampu (ON/OFF)
    private TextView tvLampuMode;      // Mode lampu (AUTO/MANUAL)

    // VIEWS — Status & Mode Pompa
    private TextView tvPompaStatus;    // Status pompa (ON/OFF)
    private TextView tvPompaMode;      // Mode pompa (AUTO/MANUAL)

    // =========================================================
    // VIEWS — Switch Kontrol
    // =========================================================
    private SwitchMaterial switchLampu;      // Toggle ON/OFF lampu (mode MANUAL)
    private SwitchMaterial switchPompa;      // Toggle ON/OFF pompa (mode MANUAL)
    private SwitchMaterial switchLampuAuto;  // Toggle mode AUTO lampu
    private SwitchMaterial switchPompaAuto;  // Toggle mode AUTO pompa

    // =========================================================
    // VIEWS — Grafik & SeekBar Batas Kelembapan
    // =========================================================
    private LineChart lineChart;
    private SeekBar seekBarMin;   // Slider batas minimum kelembapan
    private SeekBar seekBarMax;   // Slider batas maksimum kelembapan
    private TextView tvMinValue;  // Label nilai batas minimum
    private TextView tvMaxValue;  // Label nilai batas maksimum

    // =========================================================
    // FIREBASE
    // =========================================================
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // =========================================================
    // MQTT — Konfigurasi Broker
    // =========================================================
    private MqttAsyncClient mqttClient;
    private final String broker = "tcp://broker.emqx.io:1883";

    // MQTT — Topic Subscribe (ESP32 → Android)
    private final String topicSoil         = "esp32/soil";
    private final String topicLdr          = "esp32/ldr";
    private final String topicDeviceStatus = "greenhouse/ben10/device/status";
    private final String topicLampStatus   = "esp32/lamp/status";
    private final String topicPumpStatus   = "esp32/pump/status";
    private final String topicLampMode     = "esp32/lamp/mode";
    private final String topicPumpMode     = "esp32/pump/mode";

    // MQTT — Topic Publish (Android → ESP32)
    private final String topicLampCmd = "esp32/lamp/cmd";
    private final String topicPumpCmd = "esp32/pump/cmd";

    // =========================================================
    // DATA GRAFIK
    // =========================================================
    private final List<Entry> soilEntries = new ArrayList<>();
    private int soilIndex = 0;

    // =========================================================
    // BATAS KELEMBAPAN (untuk LimitLine grafik)
    // =========================================================
    private float batasMin = 60f;
    private float batasMaks = 80f;

    // Range SeekBar: Min (30–70), Maks (70–100)
    private static final int MIN_OFFSET = 30;
    private static final int MAX_OFFSET = 70;

    // =========================================================
    // FLAG
    // =========================================================
    /** Mencegah switch mengirim perintah ulang saat nilainya diubah dari MQTT */
    private boolean updatingSwitchFromMqtt = false;

    /** Memastikan Toast "MQTT Terhubung" hanya muncul sekali selama app berjalan */
    private static boolean isFirstConnect = true;

    // =========================================================
    // REQUIRED EMPTY CONSTRUCTOR
    // =========================================================
    public HomeFragment() {}

    // =========================================================
    // LIFECYCLE — onCreateView
    // =========================================================
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);
        initFirebase();
        ambilNamaPanggilan();
        setupLineChart();
        setupSeekBars();
        connectMQTT();
        setupSwitchListeners();

        return view;
    }

    // =========================================================
    // LIFECYCLE — onResume
    // =========================================================
    @Override
    public void onResume() {
        super.onResume();
        // Refresh nama panggilan setiap kali fragment aktif kembali
        if (auth != null && db != null && tvUserName != null) {
            ambilNamaPanggilan();
        }
    }

    // =========================================================
    // LIFECYCLE — onDestroyView
    // =========================================================
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Putuskan koneksi MQTT saat fragment dihancurkan
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // INISIALISASI VIEW
    // =========================================================
    private void initViews(View view) {
        // View wajib
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

        // View opsional (tidak error jika ID belum ada di XML)
        tvKelembapan  = findOptionalTextView(view, "tvKelembapan");
        tvLdrValue    = findOptionalTextView(view, "tvLdrValue");
        tvStatusAlat  = findOptionalTextView(view, "tvStatusAlat");
        tvLampuMode   = findOptionalTextView(view, "tvLampuMode");
        tvPompaMode   = findOptionalTextView(view, "tvPompaMode");
        switchLampuAuto = findOptionalSwitch(view, "switchLampuAuto");
        switchPompaAuto = findOptionalSwitch(view, "switchPompaAuto");

        // Status awal
        if (tvStatus != null)     tvStatus.setText("Mencari Alat...");
        if (tvStatusAlat != null) tvStatusAlat.setText("CHECKING...");
        if (tvMinValue != null)   tvMinValue.setText((int) batasMin + "%");
        if (tvMaxValue != null)   tvMaxValue.setText((int) batasMaks + "%");

        // Ambil nama dari cache SharedPreferences
        if (isAdded() && tvUserName != null) {
            SharedPreferences prefs = requireContext()
                    .getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            tvUserName.setText(prefs.getString("nickName", "User"));
        }
    }

    // =========================================================
    // INISIALISASI FIREBASE
    // =========================================================
    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();
    }

    // =========================================================
    // SETUP GRAFIK LINE CHART
    // =========================================================
    private void setupLineChart() {
        if (lineChart == null) return;

        // Sembunyikan komponen yang tidak perlu
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.setDrawGridBackground(false);

        // Aktifkan interaksi
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(false);

        // Teks saat belum ada data
        lineChart.setNoDataText("Menunggu data sensor...");
        lineChart.setNoDataTextColor(Color.parseColor("#E8A000"));
        lineChart.setExtraBottomOffset(8f);
        lineChart.setExtraLeftOffset(4f);

        // Sumbu Y kiri
        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(95f);
        yAxis.setGranularity(10f);
        yAxis.setTextColor(Color.parseColor("#AAAAAA"));
        yAxis.setTextSize(9f);
        yAxis.setGridColor(Color.LTGRAY);
        yAxis.setGridLineWidth(0.5f);
        yAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + "%";
            }
        });

        // Sumbu X bawah
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.parseColor("#AAAAAA"));
        xAxis.setTextSize(9f);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + "s";
            }
        });

        // Gambar LimitLine sesuai nilai batasMin & batasMaks saat ini
        updateLimitLines();

        lineChart.invalidate();
    }

    // =========================================================
    // UPDATE LIMIT LINE (Batas Min & Maks pada grafik)
    // =========================================================
    private void updateLimitLines() {
        if (lineChart == null) return;

        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.removeAllLimitLines();

        // Garis batas maksimum (merah)
        LimitLine llMaks = new LimitLine(batasMaks, "Maks " + (int) batasMaks + "%");
        llMaks.setLineColor(Color.parseColor("#E24B4A"));
        llMaks.setLineWidth(1.5f);
        llMaks.enableDashedLine(10f, 5f, 0f);
        llMaks.setTextColor(Color.parseColor("#E24B4A"));
        llMaks.setTextSize(9f);
        llMaks.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);

        // Garis batas minimum (biru)
        LimitLine llMin = new LimitLine(batasMin, "Min " + (int) batasMin + "%");
        llMin.setLineColor(Color.parseColor("#378ADD"));
        llMin.setLineWidth(1.5f);
        llMin.enableDashedLine(10f, 5f, 0f);
        llMin.setTextColor(Color.parseColor("#378ADD"));
        llMin.setTextSize(9f);
        llMin.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);

        yAxis.addLimitLine(llMaks);
        yAxis.addLimitLine(llMin);
        yAxis.setDrawLimitLinesBehindData(true);

        lineChart.invalidate();
    }

    // =========================================================
    // SETUP SEEKBAR (Slider batas Min & Maks)
    // =========================================================
    private void setupSeekBars() {
        if (seekBarMin == null || seekBarMax == null) return;

        // SeekBar Min: rentang 30–70, nilai awal 60 → progress = 60 - 30 = 30
        seekBarMin.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                batasMin = progress + MIN_OFFSET;
                if (tvMinValue != null) tvMinValue.setText((int) batasMin + "%");
                updateLimitLines();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // SeekBar Maks: rentang 70–100, nilai awal 80 → progress = 80 - 70 = 10
        seekBarMax.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                batasMaks = progress + MAX_OFFSET;
                if (tvMaxValue != null) tvMaxValue.setText((int) batasMaks + "%");
                updateLimitLines();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    // =========================================================
    // SETUP SWITCH LISTENERS
    // =========================================================
    private void setupSwitchListeners() {

        // --- Switch Lampu ON/OFF (mode MANUAL) ---
        if (switchLampu != null) {
            switchLampu.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (updatingSwitchFromMqtt) return;
                String cmd = isChecked ? "ON" : "OFF";
                publishCommand(topicLampCmd, cmd);
                if (tvLampuStatus != null) tvLampuStatus.setText("Mengirim " + cmd + "...");
                Toast.makeText(requireContext(), "Perintah Lampu " + cmd + " dikirim", Toast.LENGTH_SHORT).show();
            });
        }

        // --- Switch Pompa ON/OFF (mode MANUAL) ---
        if (switchPompa != null) {
            switchPompa.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (updatingSwitchFromMqtt) return;
                String cmd = isChecked ? "ON" : "OFF";
                publishCommand(topicPumpCmd, cmd);
                if (tvPompaStatus != null) tvPompaStatus.setText("Mengirim " + cmd + "...");
                Toast.makeText(requireContext(), "Perintah Pompa " + cmd + " dikirim", Toast.LENGTH_SHORT).show();
            });
        }

        // --- Switch Mode AUTO Lampu ---
        // Mengirim "AUTO" saat diaktifkan. Untuk keluar dari AUTO, gunakan switchLampu.
        if (switchLampuAuto != null) {
            switchLampuAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (updatingSwitchFromMqtt) return;
                if (isChecked) {
                    publishCommand(topicLampCmd, "AUTO");
                    if (tvLampuMode != null) tvLampuMode.setText("AUTO");
                    Toast.makeText(requireContext(), "Mode Lampu AUTO dikirim", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // --- Switch Mode AUTO Pompa ---
        // Mengirim "AUTO" saat diaktifkan. Untuk keluar dari AUTO, gunakan switchPompa.
        if (switchPompaAuto != null) {
            switchPompaAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (updatingSwitchFromMqtt) return;
                if (isChecked) {
                    publishCommand(topicPumpCmd, "AUTO");
                    if (tvPompaMode != null) tvPompaMode.setText("AUTO");
                    Toast.makeText(requireContext(), "Mode Pompa AUTO dikirim", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // =========================================================
    // MQTT — Koneksi ke Broker
    // =========================================================
    private void connectMQTT() {
        try {
            // Hindari koneksi ulang jika sudah terhubung
            if (mqttClient != null && mqttClient.isConnected()) return;

            String clientId = "AndroidGreenhouse_" + System.currentTimeMillis();
            mqttClient = new MqttAsyncClient(broker, clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    // Koneksi terputus — update UI
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        if (tvStatusAlat != null) tvStatusAlat.setText("OFFLINE");
                        if (tvStatus != null)     tvStatus.setText("Monitoring Terputus");
                        Toast.makeText(requireContext(), "Koneksi MQTT terputus", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    // Pesan diterima dari broker — proses di UI thread
                    String payload = new String(message.getPayload()).trim();
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> handleIncomingMessage(topic, payload));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Dipanggil saat pesan berhasil terkirim (tidak perlu aksi)
                }
            });

            mqttClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // Koneksi berhasil — tampilkan Toast hanya satu kali
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            if (isFirstConnect) {
                                Toast.makeText(requireContext(), "MQTT Terhubung", Toast.LENGTH_SHORT).show();
                                isFirstConnect = false;
                            }
                        });
                    }
                    subscribeTopics();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "Gagal konek MQTT", Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // MQTT — Subscribe Semua Topic
    // =========================================================
    private void subscribeTopics() {
        if (mqttClient == null || !mqttClient.isConnected()) return;
        try {
            mqttClient.subscribe(topicSoil, 0);          // Kelembapan tanah
            mqttClient.subscribe(topicLdr, 0);           // Nilai LDR
            mqttClient.subscribe(topicDeviceStatus, 0);  // Status alat ESP32
            mqttClient.subscribe(topicLampStatus, 0);    // Status lampu
            mqttClient.subscribe(topicPumpStatus, 0);    // Status pompa
            mqttClient.subscribe(topicLampMode, 0);      // Mode lampu
            mqttClient.subscribe(topicPumpMode, 0);      // Mode pompa
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // MQTT — Proses Pesan Masuk
    // =========================================================
    private void handleIncomingMessage(String topic, String payload) {

        if (topic.equals(topicSoil)) {
            // Data kelembapan tanah → tampilkan di TextView & grafik
            if (tvKelembapan != null) tvKelembapan.setText(payload);
            addSoilToChart(payload);

        } else if (topic.equals(topicLdr)) {
            // Data sensor LDR
            if (tvLdrValue != null) tvLdrValue.setText(payload);

        } else if (topic.equals(topicDeviceStatus)) {
            // Status alat ESP32 (ONLINE / OFFLINE / LWT)
            if (tvStatusAlat != null) tvStatusAlat.setText(payload);
            if (tvStatus != null) {
                boolean online = payload.equalsIgnoreCase("ON") || payload.equalsIgnoreCase("ONLINE");
                tvStatus.setText(online ? "Monitoring Aktif" : "Monitoring Nonaktif");
            }

        } else if (topic.equals(topicLampStatus)) {
            // Status ON/OFF lampu dari ESP32
            if (tvLampuStatus != null) tvLampuStatus.setText(payload);
            setSwitchFromMqtt(switchLampu, payload.equalsIgnoreCase("ON"));

        } else if (topic.equals(topicPumpStatus)) {
            // Status ON/OFF pompa dari ESP32
            if (tvPompaStatus != null) tvPompaStatus.setText(payload);
            setSwitchFromMqtt(switchPompa, payload.equalsIgnoreCase("ON"));

        } else if (topic.equals(topicLampMode)) {
            // Mode AUTO/MANUAL lampu dari ESP32
            if (tvLampuMode != null) tvLampuMode.setText(payload);
            setSwitchFromMqtt(switchLampuAuto, payload.equalsIgnoreCase("AUTO"));

        } else if (topic.equals(topicPumpMode)) {
            // Mode AUTO/MANUAL pompa dari ESP32
            if (tvPompaMode != null) tvPompaMode.setText(payload);
            setSwitchFromMqtt(switchPompaAuto, payload.equalsIgnoreCase("AUTO"));
        }
    }

    // =========================================================
    // MQTT — Publish Perintah ke ESP32
    // Nilai command: "ON", "OFF", atau "AUTO"
    // =========================================================
    private void publishCommand(String topic, String command) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                MqttMessage message = new MqttMessage();
                message.setPayload(command.getBytes(StandardCharsets.UTF_8));
                message.setQos(0);
                mqttClient.publish(topic, message);
            } else {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "MQTT belum terhubung", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // MQTT — Update Switch dari Status MQTT (tanpa trigger listener)
    // =========================================================
    private void setSwitchFromMqtt(SwitchMaterial sw, boolean checked) {
        if (sw == null) return;
        updatingSwitchFromMqtt = true;
        sw.setChecked(checked);
        updatingSwitchFromMqtt = false;
    }

    // =========================================================
    // GRAFIK — Tambah Data Kelembapan ke LineChart
    // =========================================================
    private void addSoilToChart(String payload) {
        if (lineChart == null) return;

        try {
            float soilValue = Float.parseFloat(payload);
            soilEntries.add(new Entry(soilIndex++, soilValue));

            // Batasi maksimal 60 data agar grafik tetap rapi
            if (soilEntries.size() > 60) {
                soilEntries.remove(0);
            }

            LineDataSet dataSet = new LineDataSet(new ArrayList<>(soilEntries), "Kelembapan Tanah");
            dataSet.setColor(Color.parseColor("#1D9E75"));
            dataSet.setCircleColor(Color.parseColor("#1D9E75"));
            dataSet.setLineWidth(2f);
            dataSet.setDrawCircles(false);
            dataSet.setDrawValues(false);
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            dataSet.setDrawFilled(true);
            dataSet.setFillColor(Color.parseColor("#1D9E75"));
            dataSet.setFillAlpha(40);

            lineChart.setData(new LineData(dataSet));
            lineChart.notifyDataSetChanged();
            lineChart.moveViewToX(soilIndex);
            lineChart.invalidate();

        } catch (NumberFormatException e) {
            // Abaikan jika payload bukan angka
            e.printStackTrace();
        }
    }

    // =========================================================
    // FIREBASE — Ambil Nama Panggilan dari Firestore
    // =========================================================
    private void ambilNamaPanggilan() {
        if (auth == null || auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) return;

                    String nickName = documentSnapshot.getString("nickName");
                    if (nickName == null || nickName.isEmpty()) nickName = "User";

                    // Simpan ke cache SharedPreferences
                    if (isAdded()) {
                        requireContext()
                                .getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putString("nickName", nickName)
                                .apply();
                    }

                    if (tvUserName != null) tvUserName.setText(nickName);
                });
    }

    // =========================================================
    // HELPER — Cari TextView Opsional (aman jika ID tidak ada di XML)
    // =========================================================
    private TextView findOptionalTextView(View view, String idName) {
        if (!isAdded()) return null;
        int id = getResources().getIdentifier(idName, "id", requireContext().getPackageName());
        return (id == 0) ? null : view.findViewById(id);
    }

    // =========================================================
    // HELPER — Cari SwitchMaterial Opsional (aman jika ID tidak ada di XML)
    // =========================================================
    private SwitchMaterial findOptionalSwitch(View view, String idName) {
        if (!isAdded()) return null;
        int id = getResources().getIdentifier(idName, "id", requireContext().getPackageName());
        return (id == 0) ? null : view.findViewById(id);
    }
}