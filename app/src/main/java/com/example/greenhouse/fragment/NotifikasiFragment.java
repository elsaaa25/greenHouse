package com.example.greenhouse.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.greenhouse.R;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotifikasiFragment extends Fragment {

    private RecyclerView rvNotifikasi;
    private NotifikasiAdapter adapter;
    private List<NotifikasiModel> listNotif = new ArrayList<>();
    private MqttAsyncClient mqttClient;

    // Topic yang dipantau
    private final String topicSoil = "esp32/soil";
    private final String topicStatus = "greenhouse/ben10/device/status";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifikasi, container, false);

        rvNotifikasi = view.findViewById(R.id.rvNotifikasi);
        rvNotifikasi.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new NotifikasiAdapter(listNotif);
        rvNotifikasi.setAdapter(adapter);

        connectMQTT();
        return view;
    }

    private void connectMQTT() {
        String serverUri = "tcp://broker.emqx.io:1883";
        String clientId = "Green_Notif_" + System.currentTimeMillis();

        try {
            mqttClient = new MqttAsyncClient(serverUri, clientId, new MemoryPersistence());
            mqttClient.setCallback(new MqttCallback() {
                @Override public void connectionLost(Throwable cause) {}

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());

                    if (isAdded()) {
                        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

                        if (topic.equals(topicSoil)) {
                            int val = Integer.parseInt(payload);
                            if (val < 40) {
                                addNewNotif("Tanaman membutuhkan air!",
                                        "Kelembapan tanah turun ke " + val + "% di bawah batas optimal.",
                                        "Hari ini : " + time + " WIB", R.drawable.ic_air);
                            }
                        } else if (topic.equals(topicStatus) && payload.equalsIgnoreCase("ONLINE")) {
                            addNewNotif("Alat Terhubung",
                                    "Monitoring tanaman anda dimulai sekarang.",
                                    "Hari ini : " + time + " WIB", R.drawable.ic_verified);
                        }
                    }
                }

                @Override public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            mqttClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    try {
                        mqttClient.subscribe(topicSoil, 1);
                        mqttClient.subscribe(topicStatus, 1);
                    } catch (MqttException e) { e.printStackTrace(); }
                }
                @Override public void onFailure(IMqttToken asyncActionToken, Throwable exception) {}
            });
        } catch (MqttException e) { e.printStackTrace(); }
    }

    private void addNewNotif(String judul, String pesan, String waktu, int iconRes) {
        requireActivity().runOnUiThread(() -> {
            // Tambah di index 0 (paling atas)
            listNotif.add(0, new NotifikasiModel(judul, pesan, waktu, iconRes));

            // notifyDataSetChanged agar posisi 0 berubah jadi oranye dan posisi 1++ jadi abu-abu
            adapter.notifyDataSetChanged();
            rvNotifikasi.scrollToPosition(0);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try { if (mqttClient != null) mqttClient.disconnect(); } catch (Exception e) {}
    }

    // --- INNER CLASS MODEL ---
    public static class NotifikasiModel {
        String judul, pesan, waktu;
        int iconRes;
        public NotifikasiModel(String judul, String pesan, String waktu, int iconRes) {
            this.judul = judul; this.pesan = pesan; this.waktu = waktu; this.iconRes = iconRes;
        }
    }

    // --- INNER CLASS ADAPTER (LOGIKA WARNA POSISI) ---
    public class NotifikasiAdapter extends RecyclerView.Adapter<NotifikasiAdapter.ViewHolder> {
        private List<NotifikasiModel> list;
        public NotifikasiAdapter(List<NotifikasiModel> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notifikasi, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NotifikasiModel item = list.get(position);
            holder.tvJudul.setText(item.judul);
            holder.tvPesan.setText(item.pesan);
            holder.tvWaktu.setText(item.waktu);
            holder.ivIcon.setImageResource(item.iconRes);

            // LOGIKA WARNA: Posisi 0 (Terbaru) = Oranye, Lainnya = Abu-abu
            if (position == 0) {
                holder.container.setBackgroundResource(R.drawable.bg_notif_warning);
                holder.ivIcon.setBackgroundResource(R.drawable.bg_orange_rounded);
            } else {
                holder.container.setBackgroundResource(R.drawable.bg_notif_default);
                holder.ivIcon.setBackgroundResource(R.drawable.bg_verified_rounded);
            }
        }

        @Override public int getItemCount() { return list.size(); }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvJudul, tvPesan, tvWaktu;
            ImageView ivIcon;
            LinearLayout container;
            public ViewHolder(View v) {
                super(v);
                tvJudul = v.findViewById(R.id.tvJudulNotif);
                tvPesan = v.findViewById(R.id.tvPesanNotif);
                tvWaktu = v.findViewById(R.id.tvWaktuNotif);
                ivIcon = v.findViewById(R.id.ivIconNotif);
                container = v.findViewById(R.id.containerNotif);
            }
        }
    }
}