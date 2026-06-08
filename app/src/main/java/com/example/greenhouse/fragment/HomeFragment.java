package com.example.greenhouse.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.greenhouse.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
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

    private TextView tvUserName;
    private TextView tvLampuStatus, tvPompaStatus;
    private TextView tvStatus;
    private TextView tvKelembapan, tvLdrValue, tvStatusAlat;
    private TextView tvLampuMode, tvPompaMode;
    private SwitchMaterial switchLampu, switchPompa;
    private SwitchMaterial switchLampuAuto, switchPompaAuto;
    private LineChart lineChart;
    private FirebaseAuth auth;
    private FirebaseFirestore db;


    // KONFIGURASI MQTT
    private MqttAsyncClient mqttClient;

    private final String broker = "tcp://broker.emqx.io:1883";

    // Data monitoring dari ESP32 ke Android
    private final String topicSoil = "esp32/soil";
    private final String topicLdr = "esp32/ldr";
    private final String topicDeviceStatus = "greenhouse/ben10/device/status";

    // Perintah dari Android ke ESP32
    private final String topicLampCmd = "esp32/lamp/cmd";
    private final String topicPumpCmd = "esp32/pump/cmd";

    // Status alat dari ESP32 ke Android
    private final String topicLampStatus = "esp32/lamp/status";
    private final String topicPumpStatus = "esp32/pump/status";

    // Status mode dari ESP32 ke Android
    private final String topicLampMode = "esp32/lamp/mode";
    private final String topicPumpMode = "esp32/pump/mode";

    // Mencegah switch mengirim perintah ulang ketika nilainya diubah dari MQTT
    private boolean updatingSwitchFromMqtt = false;

    // Data grafik kelembapan
    private final List<Entry> soilEntries = new ArrayList<>();
    private int soilIndex = 0;

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
                false
        );

        // =========================
        // INISIALISASI VIEW UTAMA
        // =========================
        tvUserName = view.findViewById(R.id.tvUserName);
        tvLampuStatus = view.findViewById(R.id.tvLampuStatus);
        tvPompaStatus = view.findViewById(R.id.tvPompaStatus);
        tvStatus = view.findViewById(R.id.tvStatus);

        if (tvStatus != null) {
            tvStatus.setText("Mencari Alat...");
        }
        if (tvStatusAlat != null) {
            tvStatusAlat.setText("CHECKING...");
        }

        switchLampu = view.findViewById(R.id.switchLampu);
        switchPompa = view.findViewById(R.id.switchPompa);

        lineChart = view.findViewById(R.id.lineChart);

        // =========================
        // INISIALISASI VIEW OPSIONAL
        // Kalau ID ini belum ada di XML, hasilnya null dan tidak error.
        // =========================
        tvKelembapan = findOptionalTextView(view, "tvKelembapan");
        tvLdrValue = findOptionalTextView(view, "tvLdrValue");
        tvStatusAlat = findOptionalTextView(view, "tvStatusAlat");

        tvLampuMode = findOptionalTextView(view, "tvLampuMode");
        tvPompaMode = findOptionalTextView(view, "tvPompaMode");

        switchLampuAuto = findOptionalSwitch(view, "switchLampuAuto");
        switchPompaAuto = findOptionalSwitch(view, "switchPompaAuto");

        // =========================
        // AMBIL NAMA USER DARI CACHE
        // =========================
        if (isAdded() && tvUserName != null) {
            SharedPreferences prefs = requireContext()
                    .getSharedPreferences("user_prefs", Context.MODE_PRIVATE);

            tvUserName.setText(prefs.getString("nickName", "User"));
        }

        // =========================
        // INISIALISASI FIREBASE
        // =========================
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ambilNamaPanggilan();

        // =========================
        // SETUP GRAFIK
        // =========================
        if (lineChart != null) {
            setupLineChart();
        }

        // =========================
        // KONEKSI MQTT
        // =========================
        connectMQTT();

        // =========================
        // SWITCH LAMPU MANUAL ON/OFF
        // Jika switch ini dipakai, ESP32 akan masuk mode MANUAL.
        // =========================
        if (switchLampu != null) {
            switchLampu.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (updatingSwitchFromMqtt) return;

                if (isChecked) {
                    publishCommand(topicLampCmd, "ON");

                    if (tvLampuStatus != null) {
                        tvLampuStatus.setText("Mengirim ON...");
                    }

                    Toast.makeText(requireContext(),
                            "Perintah Lampu ON dikirim",
                            Toast.LENGTH_SHORT).show();

                } else {
                    publishCommand(topicLampCmd, "OFF");

                    if (tvLampuStatus != null) {
                        tvLampuStatus.setText("Mengirim OFF...");
                    }

                    Toast.makeText(requireContext(),
                            "Perintah Lampu OFF dikirim",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        // =========================
        // SWITCH POMPA MANUAL ON/OFF
        // Jika switch ini dipakai, ESP32 akan masuk mode MANUAL.
        // =========================
        if (switchPompa != null) {
            switchPompa.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (updatingSwitchFromMqtt) return;

                if (isChecked) {
                    publishCommand(topicPumpCmd, "ON");

                    if (tvPompaStatus != null) {
                        tvPompaStatus.setText("Mengirim ON...");
                    }

                    Toast.makeText(requireContext(),
                            "Perintah Pompa ON dikirim",
                            Toast.LENGTH_SHORT).show();

                } else {
                    publishCommand(topicPumpCmd, "OFF");

                    if (tvPompaStatus != null) {
                        tvPompaStatus.setText("Mengirim OFF...");
                    }

                    Toast.makeText(requireContext(),
                            "Perintah Pompa OFF dikirim",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        // =========================
        // SWITCH MODE AUTO LAMPU
        // Kalau switch ini ON, Android mengirim AUTO.
        // Kalau dimatikan, tidak mengirim OFF otomatis.
        // Untuk keluar dari AUTO, gunakan switch lampu ON/OFF.
        // =========================
        if (switchLampuAuto != null) {
            switchLampuAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (updatingSwitchFromMqtt) return;

                if (isChecked) {
                    publishCommand(topicLampCmd, "AUTO");

                    if (tvLampuMode != null) {
                        tvLampuMode.setText("AUTO");
                    }

                    Toast.makeText(requireContext(),
                            "Mode Lampu AUTO dikirim",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        // =========================
        // SWITCH MODE AUTO POMPA
        // Kalau switch ini ON, Android mengirim AUTO.
        // Kalau dimatikan, tidak mengirim OFF otomatis.
        // Untuk keluar dari AUTO, gunakan switch pompa ON/OFF.
        // =========================
        if (switchPompaAuto != null) {
            switchPompaAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (updatingSwitchFromMqtt) return;

                if (isChecked) {
                    publishCommand(topicPumpCmd, "AUTO");

                    if (tvPompaMode != null) {
                        tvPompaMode.setText("AUTO");
                    }

                    Toast.makeText(requireContext(),
                            "Mode Pompa AUTO dikirim",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        return view;
    }

    // =========================
    // MENCARI TEXTVIEW OPSIONAL TANPA ERROR
    // =========================
    private TextView findOptionalTextView(View view, String idName) {
        if (!isAdded()) return null;

        int id = getResources().getIdentifier(
                idName,
                "id",
                requireContext().getPackageName()
        );

        if (id == 0) return null;

        return view.findViewById(id);
    }

    // =========================
    // MENCARI SWITCH OPSIONAL TANPA ERROR
    // =========================
    private SwitchMaterial findOptionalSwitch(View view, String idName) {
        if (!isAdded()) return null;

        int id = getResources().getIdentifier(
                idName,
                "id",
                requireContext().getPackageName()
        );

        if (id == 0) return null;

        return view.findViewById(id);
    }

    // =========================
    // FUNGSI KONEKSI MQTT
    // =========================
    private void connectMQTT() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                return;
            }

            String clientId = "AndroidGreenhouse_" + System.currentTimeMillis();

            mqttClient = new MqttAsyncClient(
                    broker,
                    clientId,
                    new MemoryPersistence()
            );

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            if (tvStatusAlat != null) {
                                tvStatusAlat.setText("OFFLINE");
                            }

                            if (tvStatus != null) {
                                tvStatus.setText("Monitoring Terputus (No Internet)");
                                tvStatus.setTextColor(Color.RED);
                            }
                            Toast.makeText(requireContext(), "Koneksi MQTT terputus", Toast.LENGTH_SHORT).show();
                        });
                    }
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload()).trim();

                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                handleIncomingMessage(topic, payload)
                        );
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Dipanggil saat pesan berhasil dikirim
                }
            });

            mqttClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(),
                                        "MQTT Terhubung",
                                        Toast.LENGTH_SHORT).show()
                        );
                    }

                    subscribeData();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(),
                                        "Gagal konek MQTT",
                                        Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // =========================
    // SUBSCRIBE DATA DARI ESP32
    // =========================
    private void subscribeData() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {

                // Monitoring sensor
                mqttClient.subscribe(topicSoil, 0);
                mqttClient.subscribe(topicLdr, 0);

                // Monitoring status alat
                mqttClient.subscribe(topicDeviceStatus, 0);
                mqttClient.subscribe(topicLampStatus, 0);
                mqttClient.subscribe(topicPumpStatus, 0);

                // Monitoring mode AUTO/MANUAL
                mqttClient.subscribe(topicLampMode, 0);
                mqttClient.subscribe(topicPumpMode, 0);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // =========================
    // MEMPROSES PESAN MQTT YANG MASUK
    // =========================
    private void handleIncomingMessage(String topic, String payload) {

        // Monitoring kelembapan tanah
        if (topic.equals(topicSoil)) {
            if (tvKelembapan != null) {
                tvKelembapan.setText(payload);
            }

            addSoilToChart(payload);
        }

        // Monitoring LDR
        else if (topic.equals(topicLdr)) {
            if (tvLdrValue != null) {
                tvLdrValue.setText(payload);
            }
        }

        // Monitoring status alat ESP32
        else if (topic.equals(topicDeviceStatus)) {
            if (tvStatusAlat != null) {
                tvStatusAlat.setText(payload);
            }
            if (tvStatus != null) {
                // payload harus "ON" untuk aktif
                if (payload.equalsIgnoreCase("ON") || payload.equalsIgnoreCase("ONLINE")) {
                    tvStatus.setText("Monitoring Aktif");
                } else {
                    // Jika payload "OFF" atau "OFFLINE" (dikirim oleh LWT ESP32)
                    tvStatus.setText("Monitoring Nonaktif");
                }
            }
        }

        // Status lampu dari ESP32
        else if (topic.equals(topicLampStatus)) {
            if (tvLampuStatus != null) {
                tvLampuStatus.setText(payload);
            }

            setSwitchFromMqtt(switchLampu, payload.equalsIgnoreCase("ON"));
        }

        // Status pompa dari ESP32
        else if (topic.equals(topicPumpStatus)) {
            if (tvPompaStatus != null) {
                tvPompaStatus.setText(payload);
            }

            setSwitchFromMqtt(switchPompa, payload.equalsIgnoreCase("ON"));
        }

        // Mode lampu dari ESP32
        else if (topic.equals(topicLampMode)) {
            if (tvLampuMode != null) {
                tvLampuMode.setText(payload);
            }

            setSwitchFromMqtt(switchLampuAuto, payload.equalsIgnoreCase("AUTO"));
        }

        // Mode pompa dari ESP32
        else if (topic.equals(topicPumpMode)) {
            if (tvPompaMode != null) {
                tvPompaMode.setText(payload);
            }

            setSwitchFromMqtt(switchPompaAuto, payload.equalsIgnoreCase("AUTO"));
        }
    }

    // =========================
    // MENGUBAH POSISI SWITCH DARI STATUS MQTT
    // TANPA MENGIRIM PERINTAH ULANG
    // =========================
    private void setSwitchFromMqtt(SwitchMaterial switchMaterial, boolean checked) {
        if (switchMaterial == null) return;

        updatingSwitchFromMqtt = true;
        switchMaterial.setChecked(checked);
        updatingSwitchFromMqtt = false;
    }

    // =========================
    // PUBLISH PERINTAH KE ESP32
    // command bisa: ON, OFF, AUTO
    // =========================
    private void publishCommand(String topic, String command) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                MqttMessage message = new MqttMessage();
                message.setPayload(command.getBytes(StandardCharsets.UTF_8));
                message.setQos(0);

                mqttClient.publish(topic, message);

            } else {
                if (isAdded()) {
                    Toast.makeText(requireContext(),
                            "MQTT belum terhubung",
                            Toast.LENGTH_SHORT).show();
                }
            }

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (auth != null && db != null && tvUserName != null) {
            ambilNamaPanggilan();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void ambilNamaPanggilan() {
        if (auth == null || auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nickName = documentSnapshot.getString("nickName");

                        if (nickName == null || nickName.isEmpty()) {
                            nickName = "User";
                        }

                        if (isAdded()) {
                            SharedPreferences.Editor editor = requireContext()
                                    .getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                    .edit();

                            editor.putString("nickName", nickName);
                            editor.apply();
                        }

                        if (tvUserName != null) {
                            tvUserName.setText(nickName);
                        }
                    }
                });
    }

    private void setupLineChart() {
        if (lineChart == null) return;

        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.getLegend().setEnabled(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(Color.LTGRAY);
        lineChart.getAxisLeft().setAxisMinimum(0f);
    }

    private void addSoilToChart(String payload) {
        if (lineChart == null) return;

        try {
            float soilValue = Float.parseFloat(payload);

            soilEntries.add(new Entry(soilIndex++, soilValue));

            // Batasi data agar grafik tidak terlalu penuh
            if (soilEntries.size() > 10) {
                soilEntries.remove(0);
            }

            LineDataSet dataSet = new LineDataSet(
                    new ArrayList<>(soilEntries),
                    "Kelembapan Tanah"
            );

            dataSet.setColor(Color.parseColor("#45553D"));
            dataSet.setCircleColor(Color.parseColor("#45553D"));
            dataSet.setLineWidth(2f);
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            dataSet.setDrawFilled(true);
            dataSet.setFillColor(Color.parseColor("#E1BEE7"));

            lineChart.setData(new LineData(dataSet));
            lineChart.notifyDataSetChanged();
            lineChart.invalidate();

        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }
}