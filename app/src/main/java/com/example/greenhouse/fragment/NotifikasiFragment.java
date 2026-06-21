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
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotifikasiFragment extends Fragment {

    private static final String TAG = "NotifikasiFragment";
    
    private RecyclerView rvNotifikasi;
    private ProgressBar pbNotif;
    private TextView tvEmptyNotif;
    private NotificationAdapter adapter;
    private List<Notification> notificationList = new ArrayList<>();
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration notificationListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifikasi, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        rvNotifikasi = view.findViewById(R.id.rvNotifikasi);
        pbNotif = view.findViewById(R.id.pbNotif);
        tvEmptyNotif = view.findViewById(R.id.tvEmptyNotif);

        rvNotifikasi.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new NotificationAdapter(notificationList);
        rvNotifikasi.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        observeNotifications();
    }

    private void observeNotifications() {
        if (auth.getCurrentUser() == null) return;
        
        if (pbNotif != null) pbNotif.setVisibility(View.VISIBLE);
        
        String uid = auth.getCurrentUser().getUid();
        DocumentReference userRef = db.collection("users").document(uid);

        // Fetch notifications for this user, ordered by newest first
        notificationListener = db.collection("notifications")
                .whereEqualTo("ownerId", userRef)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (pbNotif != null) pbNotif.setVisibility(View.GONE);

                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (snapshots != null) {
                        notificationList.clear();
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshots) {
                            Notification notification = doc.toObject(Notification.class);
                            notificationList.add(notification);
                        }
                        adapter.notifyDataSetChanged();
                        
                        // Handle empty state
                        if (tvEmptyNotif != null) {
                            tvEmptyNotif.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }

    // --- ADAPTER ---
    public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
        private List<Notification> list;
        private SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());

        public NotificationAdapter(List<Notification> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notifikasi, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Notification item = list.get(position);
            holder.tvJudul.setText(item.getTitle());
            holder.tvPesan.setText(item.getMessage());
            
            if (item.getCreatedAt() != null) {
                holder.tvWaktu.setText(sdf.format(item.getCreatedAt().toDate()));
            }

            // Logic to set icon based on notification type
            int iconRes;
            int iconBg;
            
            String type = item.getType() != null ? item.getType().toUpperCase() : "";
            switch (type) {
                case "LOW HUMIDITY":
                case "LOW_HUMIDITY":
                case "HIGH HUMIDITY":
                case "HIGH_HUMIDITY":
                    iconRes = R.drawable.ic_humidity;
                    iconBg = R.drawable.bg_orange_rounded;
                    break;
                case "PUMP AUTO ON":
                case "PUMP_AUTO_ON":
                case "PUMP ON":
                case "PUMP_ON":
                    iconRes = R.drawable.ic_pump;
                    iconBg = R.drawable.bg_orange_rounded;
                    break;
                case "LAMP ON":
                case "LAMP_ON":
                    iconRes = R.drawable.ic_light;
                    iconBg = R.drawable.bg_orange_rounded;
                    break;
                case "DEVICE ONLINE":
                case "DEVICE_ONLINE":
                    iconRes = R.drawable.ic_verified;
                    iconBg = R.drawable.bg_verified_rounded;
                    break;
                default:
                    iconRes = R.drawable.ic_notif;
                    iconBg = R.drawable.bg_verified_rounded;
                    break;
            }
            
            holder.ivIcon.setImageResource(iconRes);
            holder.ivIcon.setBackgroundResource(iconBg);

            // Highlight unread notifications
            if (!item.isRead()) {
                holder.container.setBackgroundResource(R.drawable.bg_notif_warning);
            } else {
                holder.container.setBackgroundResource(R.drawable.bg_notif_default);
            }
            
            // Mark as read on click
            holder.itemView.setOnClickListener(v -> {
                if (!item.isRead() && item.getId() != null) {
                    db.collection("notifications").document(item.getId()).update("isRead", true);
                }
            });
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
