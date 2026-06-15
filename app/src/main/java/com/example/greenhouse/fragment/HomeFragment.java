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
    }

    private void setupLineChart() {
        if (lineChart == null) return;
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.setExtraOffsets(0f, 10f, 0f, 10f);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleXEnabled(true);
        lineChart.setScaleYEnabled(false);

        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(100f);
        yAxis.setLabelCount(6, true);
        yAxis.setGridColor(Color.LTGRAY);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        updateLimitLines();
    }

    private void updateLimitLines() {
        if (lineChart == null) return;
        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.removeAllLimitLines();

        LimitLine llMaks = new LimitLine(batasMaks, "Maks " + (int) batasMaks + "%");
        llMaks.setLineColor(Color.parseColor("#E24B4A"));
        llMaks.setLineWidth(1.5f);
        llMaks.setTextColor(Color.parseColor("#E24B4A"));

        LimitLine llMin = new LimitLine(batasMin, "Min " + (int) batasMin + "%");
        llMin.setLineColor(Color.parseColor("#378ADD"));
        llMin.setLineWidth(1.5f);
        llMin.setTextColor(Color.parseColor("#378ADD"));

        yAxis.addLimitLine(llMaks);
        yAxis.addLimitLine(llMin);
        lineChart.invalidate();
    }

    private void setupSeekBars() {
        if (seekBarMin == null || seekBarMax == null) return;
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
            soilEntries.add(new Entry(soilIndex++, soilValue));
            if (soilEntries.size() > 50) soilEntries.remove(0);

            LineDataSet dataSet = new LineDataSet(new ArrayList<>(soilEntries), "Soil");
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            dataSet.setColor(Color.parseColor("#2196F3"));
            dataSet.setLineWidth(3f);
            dataSet.setDrawCircles(false);
            dataSet.setDrawValues(false);
            dataSet.setDrawFilled(true);
            dataSet.setFillColor(Color.parseColor("#2196F3"));
            dataSet.setFillAlpha(40);

            lineChart.setData(new LineData(dataSet));
            lineChart.setVisibleXRangeMaximum(40f);
            lineChart.notifyDataSetChanged();
            lineChart.moveViewToX(lineChart.getData().getEntryCount());
        } catch (Exception e) { e.printStackTrace(); }
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