package com.example.greenhouse.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.greenhouse.R;
import com.example.greenhouse.activity.MainActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotifikasiFragment extends Fragment {

    private RecyclerView rvNotifikasi;
    private NotifikasiAdapter adapter;
    private List<NotifikasiModel> listNotif = new ArrayList<>();

    // Topic yang dipantau (Samakan dengan yang ada di MainActivity)
    private final String topicSoil = "esp32/soil";
    private final String topicStatus = "greenhouse/ben10/device/status";

    // Listener untuk menerima data dari MainActivity
    private MainActivity.OnMqttMessageListener mqttListener;

    // FLAG: Mencegah duplikasi notifikasi "Alat Terhubung" (Infinity loop prevention)
    private boolean isDeviceConnectedNotified = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifikasi, container, false);

        rvNotifikasi = view.findViewById(R.id.rvNotifikasi);
        rvNotifikasi.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new NotifikasiAdapter(listNotif);
        rvNotifikasi.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Inisialisasi Listener untuk menerima data dari MainActivity
        mqttListener = (topic, payload) -> {
            if (isAdded()) {
                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

                // Logika: Jika topic tanah kering
                if (topic.equals(topicSoil)) {
                    try {
                        int val = Integer.parseInt(payload);
                        if (val < 40) {
                            addNewNotif("Tanaman membutuhkan air!",
                                    "Kelembapan tanah turun ke " + val + "% di bawah batas optimal.",
                                    "Hari ini : " + time + " WIB", R.drawable.ic_air);
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
                // Logika: Jika alat terhubung
                else if (topic.equals(topicStatus) && payload.equalsIgnoreCase("ONLINE")) {
                    // Validasi flag: Mencegah duplikasi notifikasi
                    if (!isDeviceConnectedNotified) {
                        addNewNotif("Alat Terhubung",
                                "Monitoring tanaman anda dimulai sekarang.",
                                "Hari ini : " + time + " WIB", R.drawable.ic_verified);
                        
                        // Kunci flag agar tidak muncul berulang kali (mencegah infinity loop)
                        isDeviceConnectedNotified = true;
                    }
                }
            }
        };

        // 2. Daftarkan diri ke MainActivity
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).addMqttListener(mqttListener);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 3. Lepas listener agar tidak terjadi kebocoran memori saat pindah tab
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).removeMqttListener(mqttListener);
        }
    }

    private void addNewNotif(String judul, String pesan, String waktu, int iconRes) {
        // Tambah di index 0 (paling atas)
        listNotif.add(0, new NotifikasiModel(judul, pesan, waktu, iconRes));

        // notifyDataSetChanged agar posisi 0 berwarna oranye (logic di adapter)
        adapter.notifyDataSetChanged();
        rvNotifikasi.scrollToPosition(0);
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
