package com.example.greenhouse.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.greenhouse.R;
import com.example.greenhouse.activity.LoginActivity;
import com.example.greenhouse.activity.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    // ImageView untuk menampilkan foto profil dari Firebase Storage
    private ImageView ivProfileImage;

    // TextView untuk menampilkan huruf awal nama jika belum ada foto
    private TextView tvInitial;

    // TextView untuk menampilkan nama lengkap dan email user
    private TextView tvfullName;
    private TextView tvEmail;

    // Card menu pada halaman profile
    private View cardInfoAkun;
    private View cardLogout;

    // Firebase Authentication dan Firestore
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        // Menghubungkan fragment dengan layout fragment_profile.xml
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        // Menghubungkan komponen Java dengan ID dari XML
        ivProfileImage = view.findViewById(R.id.ivProfileImage);
        tvInitial = view.findViewById(R.id.tvInitial);
        tvfullName = view.findViewById(R.id.tvfullName);
        tvEmail = view.findViewById(R.id.tvEmail);
        cardInfoAkun = view.findViewById(R.id.cardInfoAkun);
        cardLogout = view.findViewById(R.id.cardLogout);

        // Inisialisasi Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Mengambil data user dari Firestore
        ambilDataProfile();

        // Ketika card Info Akun diklik, pindah ke AccountUserFragment
        if (cardInfoAkun != null) {
            cardInfoAkun.setOnClickListener(v -> {
                cardInfoAkun.setEnabled(false);

                cardInfoAkun.postDelayed(() -> {
                    if (isAdded() && getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).loadFragment(new AccountUserFragment());
                    }

                    cardInfoAkun.setEnabled(true);
                }, 300);
            });
        }

        // Ketika card Logout diklik, tampilkan dialog konfirmasi logout
        if (cardLogout != null) {
            cardLogout.setOnClickListener(v -> tampilkanDialogLogout());
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Refresh data ketika kembali dari AccountUserFragment
        if (auth != null && db != null) {
            ambilDataProfile();
        }
    }

    /**
     * Mengambil data user dari Firestore.
     * Data yang diambil:
     * - fullName
     * - email
     * - photoUrl
     */
    private void ambilDataProfile() {

        // Cek apakah user sedang login
        if (auth.getCurrentUser() == null) {
            return;
        }

        // Mengambil UID user yang sedang login
        String uid = auth.getCurrentUser().getUid();

        // Mengambil dokumen user dari collection users berdasarkan UID
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    // Jika dokumen user ditemukan
                    if (documentSnapshot.exists()) {

                        // Mengambil data dari Firestore
                        String fullName = documentSnapshot.getString("fullName");
                        String email = documentSnapshot.getString("email");
                        String photoUrl = documentSnapshot.getString("photoUrl");

                        // Jika nama kosong, gunakan default "User"
                        if (fullName == null || fullName.trim().isEmpty()) {
                            fullName = "User";
                        }

                        // Jika email kosong, gunakan "-"
                        if (email == null || email.trim().isEmpty()) {
                            email = "-";
                        }

                        // Menampilkan nama lengkap ke UI
                        if (tvfullName != null) {
                            tvfullName.setText(fullName);
                        }

                        // Menampilkan email ke UI
                        if (tvEmail != null) {
                            tvEmail.setText(email);
                        }

                        /*
                         * Jika user sudah punya foto profil,
                         * tampilkan foto dari URL menggunakan Glide.
                         */
                        if (photoUrl != null && !photoUrl.trim().isEmpty()) {

                            if (ivProfileImage != null) {
                                ivProfileImage.setVisibility(View.VISIBLE);

                                Glide.with(this)
                                        .load(photoUrl)
                                        .centerCrop()
                                        .into(ivProfileImage);
                            }

                            if (tvInitial != null) {
                                tvInitial.setVisibility(View.GONE);
                            }

                        } else {

                            /*
                             * Jika belum ada foto profil,
                             * tampilkan huruf awal dari nama user.
                             */
                            if (ivProfileImage != null) {
                                ivProfileImage.setVisibility(View.GONE);
                            }

                            if (tvInitial != null) {
                                tvInitial.setVisibility(View.VISIBLE);
                                tvInitial.setText(fullName.substring(0, 1).toUpperCase());
                            }
                        }

                    } else {
                        Toast.makeText(
                                requireContext(),
                                "Data user tidak ditemukan",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            requireContext(),
                            "Gagal mengambil data profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    /**
     * Menampilkan dialog konfirmasi sebelum logout.
     */
    private void tampilkanDialogLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Keluar Akun")
                .setMessage("Apakah kamu yakin ingin keluar dari akun ini?")
                .setPositiveButton("Keluar", (dialog, which) -> logoutUser())
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Logout user dari Firebase Authentication,
     * lalu kembali ke halaman LoginActivity.
     */
    private void logoutUser() {
        auth.signOut();

        Toast.makeText(
                requireContext(),
                "Berhasil keluar",
                Toast.LENGTH_SHORT
        ).show();

        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        requireActivity().finish();
    }
}