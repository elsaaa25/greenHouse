package com.example.greenhouse.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.greenhouse.R;
import com.example.greenhouse.activity.LoginActivity;
import com.example.greenhouse.activity.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    // Komponen dari fragment_profile.xml
    private TextView tvInitial;
    private TextView tvfullName;
    private TextView tvEmail;
    private View cardInfoAkun;
    private View cardLogout;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        // Menghubungkan komponen Java dengan ID XML
        tvInitial = view.findViewById(R.id.tvInitial);
        tvfullName = view.findViewById(R.id.tvfullName);
        tvEmail = view.findViewById(R.id.tvEmail);
        cardInfoAkun = view.findViewById(R.id.cardInfoAkun);
        cardLogout = view.findViewById(R.id.cardLogout);

        // Inisialisasi Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Ambil data user dari Firestore
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

        // Ketika card Keluar diklik, tampilkan dialog konfirmasi logout
        if (cardLogout != null) {
            cardLogout.setOnClickListener(v -> tampilkanDialogLogout());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (auth != null && db != null) {
            ambilDataProfile();
        }
    }

    /**
     * Fungsi untuk mengambil data user dari Firestore.
     */
    private void ambilDataProfile() {
        if (auth.getCurrentUser() == null) {
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        String email = documentSnapshot.getString("email");

                        if (fullName == null || fullName.isEmpty()) {
                            fullName = "User";
                        }

                        if (email == null || email.isEmpty()) {
                            email = "-";
                        }

                        // Tampilkan data ke UI
                        if (tvfullName != null) {
                            tvfullName.setText(fullName);
                        }
                        if (tvEmail != null) {
                            tvEmail.setText(email);
                        }

                        // Set inisial nama (huruf pertama)
                        if (tvInitial != null && !fullName.isEmpty()) {
                            tvInitial.setText(fullName.substring(0, 1).toUpperCase());
                        }

                    } else {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Data user tidak ditemukan", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Gagal mengambil data profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void tampilkanDialogLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Keluar Akun")
                .setMessage("Apakah kamu yakin ingin keluar dari akun ini?")
                .setPositiveButton("Keluar", (dialog, which) -> logoutUser())
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void logoutUser() {
        auth.signOut();
        Toast.makeText(requireContext(), "Berhasil keluar", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
