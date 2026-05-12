package com.example.greenhouse.fragment;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.greenhouse.R;
import com.example.greenhouse.activity.MainActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class AccountUserFragment extends Fragment {

    // Komponen foto profil
    private ImageView ivProfileImage;
    private TextView tvProfilePlaceholder;

    // Komponen warning
    private TextView tvWarning;

    // Input data profil
    private EditText etNickName, etFullName, etEmail;

    // Input password
    private EditText etOldPassword, etNewPassword;

    // Tombol aksi
    private Button btnUpdateProfile;
    private Button btnSaveChangesPassword;
    private Button btnCancel;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // URI foto yang dipilih dari galeri
    // Foto belum diupload sampai tombol Simpan Perubahan Profil ditekan
    private Uri selectedImageUri;

    // Loading dialog
    private AlertDialog loadingDialog;

    // Launcher untuk membuka galeri dan mengambil hasil foto
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        // Mengecek apakah user berhasil memilih gambar
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {

                            // Mengambil URI gambar yang dipilih
                            Uri imageUri = result.getData().getData();

                            if (imageUri != null) {

                                // Simpan URI foto sementara
                                // Foto belum disimpan ke Firebase di sini
                                selectedImageUri = imageUri;

                                // Preview foto ke ImageView
                                ivProfileImage.setImageURI(selectedImageUri);
                                ivProfileImage.setVisibility(View.VISIBLE);

                                // Sembunyikan placeholder huruf awal
                                tvProfilePlaceholder.setVisibility(View.GONE);
                            }
                        }
                    }
            );

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        // Menghubungkan fragment dengan layout fragment_account_user.xml
        return inflater.inflate(R.layout.fragment_account_user, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        // Menyembunyikan Bottom Navigation saat berada di halaman Info Akun
        toggleBottomNavigation(false);

        // Inisialisasi Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Menghubungkan komponen foto profil dari XML ke Java
        ivProfileImage = view.findViewById(R.id.ivProfileImage);
        tvProfilePlaceholder = view.findViewById(R.id.tvProfilePlaceholder);
        tvWarning = view.findViewById(R.id.tvWarning);

        // Tombol back
        ImageButton btnBack = view.findViewById(R.id.btnBack);

        // Card foto profil
        CardView profileImageCard = view.findViewById(R.id.profileImageCard);

        // Text tap untuk ganti foto
        TextView tvTapToChange = view.findViewById(R.id.tvTapToChange);

        // Input data profil
        etNickName = view.findViewById(R.id.etNickName);
        etFullName = view.findViewById(R.id.etFullName);
        etEmail = view.findViewById(R.id.etEmail);

        // Tombol simpan perubahan profil
        btnUpdateProfile = view.findViewById(R.id.btnUpdateProfile);

        // Input password
        etOldPassword = view.findViewById(R.id.etOldPassword);
        etNewPassword = view.findViewById(R.id.etNewPassword);

        // Tombol simpan password baru
        btnSaveChangesPassword = view.findViewById(R.id.btnSaveChangesPassword);

        // Tombol batal
        btnCancel = view.findViewById(R.id.btnCancel);

        // Mengambil data user dari Firestore
        ambilDataUser();

        // Listener untuk kembali ke halaman sebelumnya
        View.OnClickListener goBackListener = v -> {
            if (isAdded()) {
                getParentFragmentManager().popBackStack();
            }
        };

        btnBack.setOnClickListener(goBackListener);
        btnCancel.setOnClickListener(goBackListener);

        // Listener untuk memilih foto dari galeri
        View.OnClickListener pickImageListener = v -> {

            // Membuka galeri
            Intent intent = new Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            );

            imagePickerLauncher.launch(intent);
        };

        // Klik foto profil untuk memilih gambar
        profileImageCard.setOnClickListener(pickImageListener);

        // Klik teks tap untuk ganti foto
        if (tvTapToChange != null) {
            tvTapToChange.setOnClickListener(pickImageListener);
        }

        /*
         * Tombol Simpan Perubahan Profil.
         *
         * Di tombol ini proses dilakukan:
         * - update nama panggilan
         * - update nama lengkap
         * - update email
         * - upload foto profil jika user memilih foto baru
         */
        btnUpdateProfile.setOnClickListener(v -> updateDataProfilDanEmail());

        /*
         * Tombol Simpan Password.
         *
         * Password hanya disimpan di Firebase Authentication,
         * tidak disimpan di Firestore.
         */
        btnSaveChangesPassword.setOnClickListener(v -> updatePasswordUser());
    }

    private void showLoading(String message) {

        if (!isAdded()) {
            return;
        }

        if (loadingDialog != null && loadingDialog.isShowing()) {
            return;
        }

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 50, 60, 50);
        layout.setGravity(android.view.Gravity.CENTER);

        ProgressBar progressBar = new ProgressBar(requireContext());

        TextView tvMessage = new TextView(requireContext());
        tvMessage.setText(message);
        tvMessage.setTextSize(16);
        tvMessage.setPadding(0, 20, 0, 0);
        tvMessage.setGravity(android.view.Gravity.CENTER);

        layout.addView(progressBar);
        layout.addView(tvMessage);

        loadingDialog = new AlertDialog.Builder(requireContext())
                .setView(layout)
                .setCancelable(false)
                .create();

        loadingDialog.show();
    }

    private void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void ambilDataUser() {

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(requireContext(), "User belum login", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        String nickName = documentSnapshot.getString("nickName");
                        String fullName = documentSnapshot.getString("fullName");
                        String email = documentSnapshot.getString("email");
                        String photoUrl = documentSnapshot.getString("photoUrl");

                        if (nickName == null) {
                            nickName = "";
                        }

                        if (fullName == null) {
                            fullName = "";
                        }

                        if (email == null || email.isEmpty()) {
                            email = currentUser.getEmail();
                        }

                        if (email == null) {
                            email = "";
                        }

                        etNickName.setText(nickName);
                        etFullName.setText(fullName);
                        etEmail.setText(email);

                        // Tampilkan foto profil jika sudah ada
                        tampilkanFotoProfil(photoUrl, fullName);

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
                            "Gagal mengambil data akun: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void setProfileInitial(String name) {

        if (name != null && !name.trim().isEmpty()) {
            String initial = name.trim().substring(0, 1).toUpperCase();
            tvProfilePlaceholder.setText(initial);
        } else {
            tvProfilePlaceholder.setText("U");
        }
    }

    private void tampilkanFotoProfil(String photoUrl, String fullName) {

        if (photoUrl != null && !photoUrl.trim().isEmpty()) {

            ivProfileImage.setVisibility(View.VISIBLE);
            tvProfilePlaceholder.setVisibility(View.GONE);

            Glide.with(this)
                    .load(photoUrl)
                    .centerCrop()
                    .into(ivProfileImage);

        } else {

            ivProfileImage.setVisibility(View.GONE);
            tvProfilePlaceholder.setVisibility(View.VISIBLE);
            setProfileInitial(fullName);
        }
    }

    private void updateDataProfilDanEmail() {

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(requireContext(), "User belum login", Toast.LENGTH_SHORT).show();
            return;
        }

        String nickName = etNickName.getText().toString().trim();
        String fullName = etFullName.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();
        String oldPassword = etOldPassword.getText().toString().trim();

        if (nickName.isEmpty()) {
            etNickName.setError("Nama panggilan tidak boleh kosong");
            etNickName.requestFocus();
            return;
        }

        if (fullName.isEmpty()) {
            etFullName.setError("Nama lengkap tidak boleh kosong");
            etFullName.requestFocus();
            return;
        }

        if (newEmail.isEmpty()) {
            etEmail.setError("Email tidak boleh kosong");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            etEmail.setError("Format email tidak valid");
            etEmail.requestFocus();
            return;
        }

        btnUpdateProfile.setEnabled(false);
        tvWarning.setVisibility(View.GONE);

        String uid = currentUser.getUid();
        String currentEmail = currentUser.getEmail();

        if (currentEmail == null) {
            currentEmail = "";
        }

        /*
         * Jika email tidak berubah,
         * langsung update data Firestore dan upload foto jika ada.
         */
        if (newEmail.equals(currentEmail)) {
            updateFirestoreUser(uid, nickName, fullName, newEmail);
            return;
        }

        /*
         * Jika email berubah, user wajib memasukkan password lama.
         */
        if (oldPassword.isEmpty()) {
            btnUpdateProfile.setEnabled(true);

            tvWarning.setText("Isi Password Lama untuk mengubah email!");
            tvWarning.setVisibility(View.VISIBLE);

            etOldPassword.requestFocus();
            return;
        }

        showLoading("Menyimpan perubahan...");

        AuthCredential credential =
                EmailAuthProvider.getCredential(currentEmail, oldPassword);

        currentUser.reauthenticate(credential)
                .addOnSuccessListener(unused -> {

                    currentUser.updateEmail(newEmail)
                            .addOnSuccessListener(unusedEmail -> {

                                // Setelah email Firebase Auth berhasil berubah,
                                // lanjut update Firestore dan upload foto jika ada
                                updateFirestoreUser(uid, nickName, fullName, newEmail);

                            })
                            .addOnFailureListener(e -> {
                                hideLoading();
                                btnUpdateProfile.setEnabled(true);

                                Toast.makeText(
                                        requireContext(),
                                        "Gagal mengubah email: " + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            });

                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    btnUpdateProfile.setEnabled(true);

                    tvWarning.setText("Password lama salah atau verifikasi gagal!");
                    tvWarning.setVisibility(View.VISIBLE);

                    Toast.makeText(
                            requireContext(),
                            "Verifikasi gagal: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void updateFirestoreUser(
            String uid,
            String nickName,
            String fullName,
            String email
    ) {
        showLoading("Menyimpan perubahan...");

        /*
         * Jika user memilih foto baru,
         * upload foto ke Firebase Storage terlebih dahulu.
         */
        if (selectedImageUri != null) {

            StorageReference imageRef = storage
                    .getReference()
                    .child("profile_images")
                    .child(uid + ".jpg");

            imageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> {

                        imageRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {

                                    String photoUrl = uri.toString();

                                    // Simpan data user beserta URL foto ke Firestore
                                    simpanDataUserKeFirestore(
                                            uid,
                                            nickName,
                                            fullName,
                                            email,
                                            photoUrl
                                    );

                                })
                                .addOnFailureListener(e -> {
                                    hideLoading();
                                    btnUpdateProfile.setEnabled(true);

                                    Toast.makeText(
                                            requireContext(),
                                            "Gagal mengambil URL foto: " + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                });

                    })
                    .addOnFailureListener(e -> {
                        hideLoading();
                        btnUpdateProfile.setEnabled(true);

                        Toast.makeText(
                                requireContext(),
                                "Gagal upload foto: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    });

        } else {

            /*
             * Jika user tidak memilih foto baru,
             * hanya update nama, email, dan data profil lainnya.
             */
            simpanDataUserKeFirestore(
                    uid,
                    nickName,
                    fullName,
                    email,
                    null
            );
        }
    }

    private void simpanDataUserKeFirestore(
            String uid,
            String nickName,
            String fullName,
            String email,
            @Nullable String photoUrl
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("nickName", nickName);
        data.put("fullName", fullName);
        data.put("email", email);
        data.put("updatedAt", FieldValue.serverTimestamp());

        // Simpan URL foto hanya jika ada foto baru
        if (photoUrl != null) {
            data.put("photoUrl", photoUrl);
        }

        db.collection("users")
                .document(uid)
                .update(data)
                .addOnSuccessListener(unused -> {

                    hideLoading();
                    btnUpdateProfile.setEnabled(true);

                    // Reset URI foto agar tidak upload ulang
                    selectedImageUri = null;

                    setProfileInitial(fullName);

                    Toast.makeText(
                            requireContext(),
                            "Data akun berhasil diperbarui",
                            Toast.LENGTH_SHORT
                    ).show();

                    backToProfileSmooth();
                })
                .addOnFailureListener(e -> {

                    hideLoading();
                    btnUpdateProfile.setEnabled(true);

                    Toast.makeText(
                            requireContext(),
                            "Gagal memperbarui data: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void updatePasswordUser() {

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(requireContext(), "User belum login", Toast.LENGTH_SHORT).show();
            return;
        }

        String oldPassword = etOldPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        if (oldPassword.isEmpty() || newPassword.isEmpty()) {
            tvWarning.setText("Harap isi password secara lengkap!");
            tvWarning.setVisibility(View.VISIBLE);
            return;
        }

        if (newPassword.length() < 8) {
            etNewPassword.setError("Password baru minimal 8 karakter");
            etNewPassword.requestFocus();
            return;
        }

        String currentEmail = currentUser.getEmail();

        if (currentEmail == null || currentEmail.isEmpty()) {
            Toast.makeText(requireContext(), "Email user tidak ditemukan", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSaveChangesPassword.setEnabled(false);
        tvWarning.setVisibility(View.GONE);

        AuthCredential credential =
                EmailAuthProvider.getCredential(currentEmail, oldPassword);

        currentUser.reauthenticate(credential)
                .addOnSuccessListener(unused -> {

                    currentUser.updatePassword(newPassword)
                            .addOnSuccessListener(unusedPassword -> {

                                btnSaveChangesPassword.setEnabled(true);

                                etOldPassword.setText("");
                                etNewPassword.setText("");

                                Toast.makeText(
                                        requireContext(),
                                        "Kata sandi berhasil diperbarui",
                                        Toast.LENGTH_SHORT
                                ).show();

                            })
                            .addOnFailureListener(e -> {

                                btnSaveChangesPassword.setEnabled(true);

                                Toast.makeText(
                                        requireContext(),
                                        "Gagal memperbarui password: " + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            });

                })
                .addOnFailureListener(e -> {

                    btnSaveChangesPassword.setEnabled(true);

                    tvWarning.setText("Password lama salah!");
                    tvWarning.setVisibility(View.VISIBLE);

                    Toast.makeText(
                            requireContext(),
                            "Verifikasi gagal: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Menampilkan kembali Bottom Navigation ketika keluar dari halaman ini
        toggleBottomNavigation(true);
    }

    private void toggleBottomNavigation(boolean show) {

        if (getActivity() instanceof MainActivity) {

            BottomNavigationView bottomNav =
                    getActivity().findViewById(R.id.bottomNavigationView);

            if (bottomNav != null) {
                bottomNav.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void backToProfileSmooth() {

        if (!isAdded()) {
            return;
        }

        requireView().postDelayed(() -> {
            if (isAdded()) {
                getParentFragmentManager().popBackStack();
            }
        }, 250);
    }
}