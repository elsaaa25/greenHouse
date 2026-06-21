package com.example.greenhouse.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.greenhouse.R;
import com.example.greenhouse.model.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class NotifikasiFragment extends Fragment {

    private static final String TAG = "NotifikasiFragment";

    private RecyclerView rvNotifikasi;
    private ProgressBar pbNotif;
    private TextView tvEmptyNotif;

    private NotificationAdapter adapter;
    private final List<Notification> notificationList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration notificationListener;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_notifikasi, container, false);

        initFirebase();
        initViews(view);
        setupRecyclerView();

        return view;
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);
        observeNotifications();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }

    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private void initViews(View view) {
        rvNotifikasi = view.findViewById(R.id.rvNotifikasi);
        pbNotif = view.findViewById(R.id.pbNotif);
        tvEmptyNotif = view.findViewById(R.id.tvEmptyNotif);
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(notificationList);

        if (rvNotifikasi != null) {
            rvNotifikasi.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvNotifikasi.setAdapter(adapter);
        }
    }

    private void observeNotifications() {
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "User tidak login");

            if (pbNotif != null) {
                pbNotif.setVisibility(View.GONE);
            }

            if (tvEmptyNotif != null) {
                tvEmptyNotif.setText("User belum login");
                tvEmptyNotif.setVisibility(View.VISIBLE);
            }

            if (rvNotifikasi != null) {
                rvNotifikasi.setVisibility(View.GONE);
            }

            return;
        }

        if (pbNotif != null) {
            pbNotif.setVisibility(View.VISIBLE);
        }

        if (tvEmptyNotif != null) {
            tvEmptyNotif.setVisibility(View.GONE);
        }

        String uid = auth.getCurrentUser().getUid();
        DocumentReference userRef = db.collection("users").document(uid);

        Log.d(TAG, "UID Login: " + uid);
        Log.d(TAG, "UserRef Query: " + userRef.getPath());

        /*
         * Query ini cocok jika field ownerId di Firestore bertipe DocumentReference:
         *
         * ownerId: /users/UID_USER
         *
         * orderBy("createdAt") sengaja tidak dipakai agar tidak terkena error
         * composite index Firestore. Pengurutan dilakukan manual di aplikasi.
         */
        notificationListener = db.collection("notifications")
                .whereEqualTo("ownerId", userRef)
                .addSnapshotListener((snapshots, e) -> {
                    if (pbNotif != null) {
                        pbNotif.setVisibility(View.GONE);
                    }

                    if (e != null) {
                        Log.e(TAG, "Gagal membaca notifikasi dari Firestore", e);

                        if (tvEmptyNotif != null) {
                            tvEmptyNotif.setText("Gagal memuat notifikasi");
                            tvEmptyNotif.setVisibility(View.VISIBLE);
                        }

                        if (rvNotifikasi != null) {
                            rvNotifikasi.setVisibility(View.GONE);
                        }

                        return;
                    }

                    notificationList.clear();

                    if (snapshots != null) {
                        Log.d(TAG, "Jumlah dokumen notifikasi dari Firestore: " + snapshots.size());

                        for (QueryDocumentSnapshot doc : snapshots) {
                            try {
                                Notification notification = doc.toObject(Notification.class);

                                /*
                                 * ID dokumen wajib diset manual agar nanti bisa update isRead.
                                 */
                                notification.setId(doc.getId());

                                notificationList.add(notification);

                                Log.d(TAG,
                                        "Notif dimuat: "
                                                + notification.getTitle()
                                                + " | type: "
                                                + notification.getType()
                                                + " | id: "
                                                + doc.getId()
                                );

                            } catch (Exception ex) {
                                Log.e(TAG, "Gagal konversi dokumen ke model Notification", ex);
                            }
                        }

                        /*
                         * Urutkan manual dari terbaru ke terlama berdasarkan createdAt.
                         */
                        Collections.sort(notificationList, (a, b) -> {
                            if (a.getCreatedAt() == null && b.getCreatedAt() == null) {
                                return 0;
                            }

                            if (a.getCreatedAt() == null) {
                                return 1;
                            }

                            if (b.getCreatedAt() == null) {
                                return -1;
                            }

                            return b.getCreatedAt().compareTo(a.getCreatedAt());
                        });
                    }

                    adapter.notifyDataSetChanged();
                    updateEmptyState();

                    Log.d(TAG, "Total data tampil di adapter: " + notificationList.size());
                });
    }

    private void updateEmptyState() {
        if (notificationList.isEmpty()) {
            if (tvEmptyNotif != null) {
                tvEmptyNotif.setText("Belum ada notifikasi");
                tvEmptyNotif.setVisibility(View.VISIBLE);
            }

            if (rvNotifikasi != null) {
                rvNotifikasi.setVisibility(View.GONE);
            }
        } else {
            if (tvEmptyNotif != null) {
                tvEmptyNotif.setVisibility(View.GONE);
            }

            if (rvNotifikasi != null) {
                rvNotifikasi.setVisibility(View.VISIBLE);
            }
        }
    }

    // ==========================
    // ADAPTER NOTIFIKASI
    // ==========================
    public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

        private final List<Notification> list;
        private final SimpleDateFormat sdf =
                new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID"));

        public NotificationAdapter(List<Notification> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType
        ) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notifikasi, parent, false);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                @NonNull ViewHolder holder,
                int position
        ) {
            Notification item = list.get(position);

            String title = item.getTitle() != null ? item.getTitle() : "Notifikasi";
            String message = item.getMessage() != null ? item.getMessage() : "-";

            holder.tvJudul.setText(title);
            holder.tvPesan.setText(message);

            if (item.getCreatedAt() != null) {
                holder.tvWaktu.setText(sdf.format(item.getCreatedAt().toDate()));
            } else {
                holder.tvWaktu.setText("-");
            }

            setupIcon(holder, item);
            setupReadState(holder, item);
            setupClickListener(holder, item);
        }

        private void setupIcon(ViewHolder holder, Notification item) {
            int iconRes = R.drawable.ic_notif;
            int iconBg = R.drawable.bg_verified_rounded;

            String type = item.getType() != null ? item.getType().toUpperCase(Locale.ROOT) : "";

            if (type.contains("HUMIDITY")) {
                iconRes = R.drawable.ic_humidity;
                iconBg = R.drawable.bg_orange_rounded;

            } else if (type.contains("PUMP") || type.contains("WATERING")) {
                iconRes = R.drawable.ic_pump;
                iconBg = R.drawable.bg_orange_rounded;

            } else if (type.contains("LAMP")) {
                iconRes = R.drawable.ic_light;
                iconBg = R.drawable.bg_orange_rounded;

            } else if (type.contains("ONLINE") || type.contains("DEVICE")) {
                iconRes = R.drawable.ic_verified;
                iconBg = R.drawable.bg_verified_rounded;
            }

            holder.ivIcon.setImageResource(iconRes);
            holder.ivIcon.setBackgroundResource(iconBg);
        }

        private void setupReadState(ViewHolder holder, Notification item) {
            if (!item.isRead()) {
                holder.container.setBackgroundResource(R.drawable.bg_notif_warning);
            } else {
                holder.container.setBackgroundResource(android.R.color.transparent);
            }
        }

        private void setupClickListener(ViewHolder holder, Notification item) {
            holder.itemView.setOnClickListener(v -> {
                int currentPosition = holder.getAdapterPosition();

                if (currentPosition == RecyclerView.NO_POSITION) {
                    return;
                }

                if (item.isRead()) {
                    return;
                }

                if (item.getId() == null || item.getId().isEmpty()) {
                    Log.e(TAG, "ID notifikasi kosong, tidak bisa update isRead");
                    return;
                }

                db.collection("notifications")
                        .document(item.getId())
                        .update("isRead", true)
                        .addOnSuccessListener(unused -> {
                            item.setRead(true);
                            notifyItemChanged(currentPosition);
                            Log.d(TAG, "Notifikasi ditandai sudah dibaca: " + item.getId());
                        })
                        .addOnFailureListener(err ->
                                Log.e(TAG, "Gagal update status baca notifikasi", err)
                        );
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            TextView tvJudul, tvPesan, tvWaktu;
            ImageView ivIcon;
            LinearLayout container;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);

                tvJudul = itemView.findViewById(R.id.tvJudulNotif);
                tvPesan = itemView.findViewById(R.id.tvPesanNotif);
                tvWaktu = itemView.findViewById(R.id.tvWaktuNotif);
                ivIcon = itemView.findViewById(R.id.ivIconNotif);
                container = itemView.findViewById(R.id.containerNotif);
            }
        }
    }
}