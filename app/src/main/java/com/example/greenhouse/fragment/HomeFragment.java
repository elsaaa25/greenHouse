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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.greenhouse.R;
import com.example.greenhouse.activity.MainActivity;
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

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    // VIEWS — Informasi & Status
    private TextView tvUserName, tvKelembapan, tvLdrValue, tvStatusAlat, tvStatus;
    private TextView tvLampuStatus, tvLampuMode, tvPompaStatus, tvPompaMode;

    // VIEWS — Switch Kontrol
    private SwitchMaterial switchLampu, switchPompa, switchLampuAuto, switchPompaAuto;

    // VIEWS — Grafik & SeekBar
    private LineChart lineChart;
    private SeekBar seekBarMin, seekBarMax;
    private TextView tvMinValue, tvMaxValue;

    // FIREBASE
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // MQTT — Topic constants (untuk pengecekan pesan masuk)
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
    private final List<Entry> soilEntries = new ArrayList<>();
    private int soilIndex = 0;
    private float batasMin = 60f, batasMaks = 80f;
    private static final int MIN_OFFSET = 30, MAX_OFFSET = 70;

    private boolean updatingSwitchFromMqtt = false;

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
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Inisialisasi Listener MQTT
        mqttListener = (topic, payload) -> {
            if (isAdded()) {
                handleIncomingMessage(topic, payload);
            }
        };

        // 2. Daftarkan diri ke MainActivity agar bisa menerima data tanpa koneksi ulang
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).addMqttListener(mqttListener);
        }

        ambilNamaPanggilan();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 3. Lepas listener saat tab pindah agar tidak memory leak
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).removeMqttListener(mqttListener);
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

        tvKelembapan  = findOptionalTextView(view, "tvKelembapan");
        tvLdrValue    = findOptionalTextView(view, "tvLdrValue");
        tvStatusAlat  = findOptionalTextView(view, "tvStatusAlat");
        tvLampuMode   = findOptionalTextView(view, "tvLampuMode");
        tvPompaMode   = findOptionalTextView(view, "tvPompaMode");
        switchLampuAuto = findOptionalSwitch(view, "switchLampuAuto");
        switchPompaAuto = findOptionalSwitch(view, "switchPompaAuto");

        if (tvStatus != null)     tvStatus.setText("Menghubungkan...");
        if (tvStatusAlat != null) tvStatusAlat.setText("WAITING...");

        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        if (tvUserName != null) tvUserName.setText(prefs.getString("nickName", "User"));

        if (seekBarMin != null && seekBarMax != null) {
            batasMin = seekBarMin.getProgress();
            batasMaks = seekBarMax.getProgress();
            tvMinValue.setText((int)batasMin + "%");
            tvMaxValue.setText((int)batasMaks + "%");
        }
    }

    private void setupLineChart() {
        if (lineChart == null) return;

        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setExtraOffsets(10f, 40f, 10f, 20f); // Ruang untuk label Maks/Min dan Sumbu X

        // Sumbu Y (Kiri)
        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setAxisMinimum(40f); // Mulai dari 40% agar grafik terlihat "naik"
        yAxis.setAxisMaximum(100f);
        yAxis.setLabelCount(6, false); // Agar tidak dempet
        yAxis.setGranularity(5f);
        yAxis.setDrawAxisLine(false); // Hilangkan garis pinggir hitam
        yAxis.setGridColor(Color.parseColor("#E0E0E0")); // Grid halus
        yAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + "%";
            }
        });

        // Sumbu X (Bawah)
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setAxisMinimum(0f); // Cegah angka -1
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (lineChart.getData() == null) return "";
                float max = lineChart.getData().getXMax();
                int diff = (int) (value - max);
                if (diff == 0) return "Skrng";
                if (diff < 0 && diff % 30 == 0) return diff + "s"; // Muncul per 30 detik
                return "";
            }
        });

        lineChart.getAxisRight().setEnabled(false);
        updateLimitLines();
    }


    private void updateLimitLines() {
        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.removeAllLimitLines(); // Hapus garis lama agar tidak menumpuk

        // Garis Maks
        LimitLine llMaks = new LimitLine(batasMaks, "Maks " + (int)batasMaks + "%");
        llMaks.setLineColor(Color.parseColor("#E24B4A"));
        llMaks.setLineWidth(1.2f);
        llMaks.setTextColor(Color.parseColor("#E24B4A"));
        llMaks.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        llMaks.setTextSize(9f);

        // Garis Min
        LimitLine llMin = new LimitLine(batasMin, "Min " + (int)batasMin + "%");
        llMin.setLineColor(Color.parseColor("#378ADD"));
        llMin.setLineWidth(1.2f);
        llMin.setTextColor(Color.parseColor("#378ADD"));
        llMin.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
        llMin.setTextSize(9f);

        yAxis.addLimitLine(llMaks);
        yAxis.addLimitLine(llMin);

        lineChart.invalidate(); // REFRESH GRAFIK
    }

    private void setupSeekBars() {
        if (seekBarMin == null || seekBarMax == null) return;

        seekBarMin.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Jika progress 0-100, kita ambil langsung nilainya
                batasMin = progress;
                if (tvMinValue != null) tvMinValue.setText((int) batasMin + "%");
                updateLimitLines();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarMax.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                batasMaks = progress;
                if (tvMaxValue != null) tvMaxValue.setText((int) batasMaks + "%");
                updateLimitLines();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupSwitchListeners() {
        if (switchLampu != null) {
            switchLampu.setOnCheckedChangeListener((v, isChecked) -> {
                if (updatingSwitchFromMqtt) return;
                publishCommand(topicLampCmd, isChecked ? "ON" : "OFF");
            });
        }
        if (switchPompa != null) {
            switchPompa.setOnCheckedChangeListener((v, isChecked) -> {
                if (updatingSwitchFromMqtt) return;
                publishCommand(topicPumpCmd, isChecked ? "ON" : "OFF");
            });
        }
        if (switchLampuAuto != null) {
            switchLampuAuto.setOnCheckedChangeListener((v, isChecked) -> {
                if (updatingSwitchFromMqtt) return;
                if (isChecked) publishCommand(topicLampCmd, "AUTO");
            });
        }
        if (switchPompaAuto != null) {
            switchPompaAuto.setOnCheckedChangeListener((v, isChecked) -> {
                if (updatingSwitchFromMqtt) return;
                if (isChecked) publishCommand(topicPumpCmd, "AUTO");
            });
        }
    }

    private void handleIncomingMessage(String topic, String payload) {
        if (topic.equals(topicSoil)) {
            if (tvKelembapan != null) tvKelembapan.setText(payload + "%");
            addSoilToChart(payload);
        } else if (topic.equals(topicLdr)) {
            if (tvLdrValue != null) tvLdrValue.setText(payload);
        } else if (topic.equals(topicDeviceStatus)) {
            if (tvStatusAlat != null) tvStatusAlat.setText(payload);
            boolean online = payload.equalsIgnoreCase("ON") || payload.equalsIgnoreCase("ONLINE");
            if (tvStatus != null) tvStatus.setText(online ? "Monitoring Aktif" : "Monitoring Nonaktif");
        } else if (topic.equals(topicLampStatus)) {
            if (tvLampuStatus != null) tvLampuStatus.setText(payload);
            setSwitchFromMqtt(switchLampu, payload.equalsIgnoreCase("ON"));
        } else if (topic.equals(topicPumpStatus)) {
            if (tvPompaStatus != null) tvPompaStatus.setText(payload);
            setSwitchFromMqtt(switchPompa, payload.equalsIgnoreCase("ON"));
        } else if (topic.equals(topicLampMode)) {
            if (tvLampuMode != null) tvLampuMode.setText(payload);
            setSwitchFromMqtt(switchLampuAuto, payload.equalsIgnoreCase("AUTO"));
        } else if (topic.equals(topicPumpMode)) {
            if (tvPompaMode != null) tvPompaMode.setText(payload);
            setSwitchFromMqtt(switchPompaAuto, payload.equalsIgnoreCase("AUTO"));
        }
    }

    private void publishCommand(String topic, String command) {
        if (getActivity() instanceof MainActivity) {
            // Memanggil fungsi publish dari MainActivity pusat
            ((MainActivity) getActivity()).publishCommand(topic, command);
        }
    }

    private void addSoilToChart(String payload) {
        try {
            float soilValue = Float.parseFloat(payload);

            // 1. Ambil data yang sudah ada di chart (agar lebih ringan daripada membuat baru setiap detik)
            LineData data = lineChart.getData();

            if (data == null) {
                // Jika data belum ada, buat dataset pertama kali
                LineDataSet dataSet = new LineDataSet(new ArrayList<>(), "Kelembapan");

                // --- STYLING PROFESIONAL ---
                dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Membuat garis melengkung halus (tidak kaku)
                dataSet.setColor(Color.parseColor("#1D9E75"));  // Warna Teal yang lebih elegan
                dataSet.setLineWidth(2.5f);                     // Garis sedikit lebih tebal
                dataSet.setDrawCircles(false);                  // Hilangkan titik bulat data
                dataSet.setDrawValues(false);                   // Hilangkan angka di atas garis

                // Area di bawah garis (Gradient fill)
                dataSet.setDrawFilled(true);
                dataSet.setFillColor(Color.parseColor("#1D9E75"));
                dataSet.setFillAlpha(35);                       // Transparansi area bawah

                data = new LineData(dataSet);
                lineChart.setData(data);
            }

            // 2. Tambahkan entry baru ke dataset yang sudah ada
            data.addEntry(new Entry(soilIndex++, soilValue), 0);

            // 3. Batasi memori (hapus data lama jika sudah lebih dari 600 data/10 menit)
            if (data.getEntryCount() > 600) {
                data.getDataSetByIndex(0).removeEntry(0);
            }

            // 4. Beritahu chart bahwa data berubah
            data.notifyDataChanged();
            lineChart.notifyDataSetChanged();

            // --- KUNCI TAMPILAN 3 MENIT ---
            // Menampilkan 180 unit (detik) terakhir agar grafik tidak terlihat kerdil/sempit
            lineChart.setVisibleXRangeMaximum(180f);

            // Selalu geser ke arah data terbaru (paling kanan)
            lineChart.moveViewToX(data.getXMax());

            // Refresh tampilan
            lineChart.invalidate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setSwitchFromMqtt(SwitchMaterial sw, boolean checked) {
        if (sw == null) return;
        updatingSwitchFromMqtt = true;
        sw.setChecked(checked);
        updatingSwitchFromMqtt = false;
    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void ambilNamaPanggilan() {
        if (auth == null || auth.getCurrentUser() == null) return;
        db.collection("users").document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && isAdded()) {
                        String name = doc.getString("nickName");
                        if (name != null && tvUserName != null) tvUserName.setText(name);
                    }
                });
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