package com.example.greenhouse.fragment;

import static android.app.Activity.RESULT_OK;

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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.greenhouse.R;
import com.example.greenhouse.activity.MainActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

public class AccountUserFragment extends Fragment {

    private ImageView ivProfileImage;

    private TextView tvProfilePlaceholder;

    private TextView tvWarning;

    private EditText etNickName, etFullName, etEmail;

    private EditText etOldPassword, etNewPassword;

    private Button btnUpdateProfile;

    private Button btnSaveChangesPassword;

    private Button btnCancel;

    private FirebaseAuth auth;

    private FirebaseFirestore db;

    private AlertDialog loadingDialog;
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        // Mengecek apakah user berhasil memilih gambar
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {

                            // Mengambil URI gambar yang dipilih
                            Uri imageUri = result.getData().getData();

                            if (imageUri != null) {

                                // Menampilkan gambar ke ImageView
                                ivProfileImage.setImageURI(imageUri);

                                // Menampilkan ImageView foto
                                ivProfileImage.setVisibility(View.VISIBLE);

                                // Menyembunyikan placeholder huruf awal nama
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
        // Menghubungkan fragment ini dengan layout fragment_account_user.xml
        return inflater.inflate(R.layout.fragment_account_user, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        // Menyembunyikan Bottom Navigation saat user berada di halaman Info Akun
        toggleBottomNavigation(false);

        // Inisialisasi Firebase Authentication
        auth = FirebaseAuth.getInstance();

        // Inisialisasi Firestore Database
        db = FirebaseFirestore.getInstance();

        // Menghubungkan komponen foto profil dari XML ke Java
        ivProfileImage = view.findViewById(R.id.ivProfileImage);
        tvProfilePlaceholder = view.findViewById(R.id.tvProfilePlaceholder);
        tvWarning = view.findViewById(R.id.tvWarning);

        // Tombol back di bagian header
        ImageButton btnBack = view.findViewById(R.id.btnBack);

        // Card foto profil yang bisa diklik untuk memilih foto
        CardView profileImageCard = view.findViewById(R.id.profileImageCard);

        // Teks "Tap untuk ganti foto"
        TextView tvTapToChange = view.findViewById(R.id.tvTapToChange);

        // Menghubungkan input data profil dari XML ke Java
        etNickName = view.findViewById(R.id.etNickName);
        etFullName = view.findViewById(R.id.etFullName);
        etEmail = view.findViewById(R.id.etEmail);

        // Tombol untuk menyimpan perubahan profil
        btnUpdateProfile = view.findViewById(R.id.btnUpdateProfile);

        // Menghubungkan input password dari XML ke Java
        etOldPassword = view.findViewById(R.id.etOldPassword);
        etNewPassword = view.findViewById(R.id.etNewPassword);

        // Tombol untuk menyimpan password baru
        btnSaveChangesPassword = view.findViewById(R.id.btnSaveChangesPassword);

        // Tombol batal
        btnCancel = view.findViewById(R.id.btnCancel);

        // Mengambil data user dari Firestore lalu menampilkan ke form
        ambilDataUser();

        // Listener untuk tombol kembali
        View.OnClickListener goBackListener = v -> {
            if (isAdded()) {
                getParentFragmentManager().popBackStack();
            }
        };

        // Tombol back menjalankan fungsi kembali
        btnBack.setOnClickListener(goBackListener);

        // Tombol batal juga menjalankan fungsi kembali
        btnCancel.setOnClickListener(goBackListener);

        // Listener untuk memilih foto dari galeri
        View.OnClickListener pickImageListener = v -> {

            // Intent untuk membuka galeri
            Intent intent = new Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            );

            // Membuka galeri
            imagePickerLauncher.launch(intent);
        };

        // Klik card foto untuk memilih gambar
        profileImageCard.setOnClickListener(pickImageListener);

        // Klik teks "Tap untuk ganti foto" juga memilih gambar
        if (tvTapToChange != null) {
            tvTapToChange.setOnClickListener(pickImageListener);
        }

        /*
         * Tombol Simpan Perubahan Profil.
         *
         * Tombol ini mengubah:
         * - nama panggilan
         * - nama lengkap
         * - email
         *
         * Kalau email tidak berubah, hanya update Firestore.
         * Kalau email berubah, user harus isi Password Lama.
         */
        btnUpdateProfile.setOnClickListener(v -> updateDataProfilDanEmail());

        /*
         * Tombol Simpan Password.
         *
         * Tombol ini hanya mengubah password Firebase Authentication.
         * Password tidak disimpan di Firestore.
         */
        btnSaveChangesPassword.setOnClickListener(v -> updatePasswordUser());
    }

    private void showLoading(String message) {

        if (!isAdded()) {
            return;
        }

        // Jika loading sudah tampil, jangan buat ulang
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

    /**
     * Menutup loading dialog.
     */
    private void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
    private void ambilDataUser() {

        // Mengambil user yang sedang login
        FirebaseUser currentUser = auth.getCurrentUser();

        // Jika tidak ada user login, hentikan proses
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User belum login", Toast.LENGTH_SHORT).show();
            return;
        }

        // UID adalah ID unik user dari Firebase Authentication
        String uid = currentUser.getUid();

        // Mengambil dokumen user dari collection users berdasarkan UID
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    // Jika dokumen user ditemukan
                    if (documentSnapshot.exists()) {

                        // Mengambil data dari Firestore
                        String nickName = documentSnapshot.getString("nickName");
                        String fullName = documentSnapshot.getString("fullName");
                        String email = documentSnapshot.getString("email");

                        // Jika data null, ubah menjadi string kosong agar tidak crash
                        if (nickName == null) {
                            nickName = "";
                        }

                        if (fullName == null) {
                            fullName = "";
                        }

                        /*
                         * Jika email di Firestore kosong,
                         * ambil email dari Firebase Authentication.
                         */
                        if (email == null || email.isEmpty()) {
                            email = currentUser.getEmail();
                        }

                        if (email == null) {
                            email = "";
                        }

                        // Menampilkan data ke form input
                        etNickName.setText(nickName);
                        etFullName.setText(fullName);
                        etEmail.setText(email);

                        // Mengatur huruf avatar dari nama panggilan
                        setProfileInitial(nickName);

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

            // Ambil huruf pertama dari nama panggilan
            String initial = name.trim().substring(0, 1).toUpperCase();

            // Tampilkan huruf pertama di placeholder
            tvProfilePlaceholder.setText(initial);

        } else {

            // Default jika nama kosong
            tvProfilePlaceholder.setText("U");
        }
    }
    private void updateDataProfilDanEmail() {

        // Mengambil user yang sedang login
        FirebaseUser currentUser = auth.getCurrentUser();

        // Jika user null, berarti belum login
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User belum login", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mengambil isi input dari form
        String nickName = etNickName.getText().toString().trim();
        String fullName = etFullName.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();
        String oldPassword = etOldPassword.getText().toString().trim();

        // Validasi nama panggilan
        if (nickName.isEmpty()) {
            etNickName.setError("Nama panggilan tidak boleh kosong");
            etNickName.requestFocus();
            return;
        }

        // Validasi nama lengkap
        if (fullName.isEmpty()) {
            etFullName.setError("Nama lengkap tidak boleh kosong");
            etFullName.requestFocus();
            return;
        }

        // Validasi email kosong
        if (newEmail.isEmpty()) {
            etEmail.setError("Email tidak boleh kosong");
            etEmail.requestFocus();
            return;
        }

        // Validasi format email
        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            etEmail.setError("Format email tidak valid");
            etEmail.requestFocus();
            return;
        }

        // Tombol dimatikan sementara agar tidak diklik berkali-kali
        btnUpdateProfile.setEnabled(false);

        // Sembunyikan warning jika sebelumnya tampil
        tvWarning.setVisibility(View.GONE);

        // Ambil UID user
        String uid = currentUser.getUid();

        // Ambil email lama dari Firebase Authentication
        String currentEmail = currentUser.getEmail();

        if (currentEmail == null) {
            currentEmail = "";
        }

        /*
         * Jika email tidak berubah,
         * tidak perlu verifikasi password lama.
         * Langsung update Firestore saja.
         */
        if (newEmail.equals(currentEmail)) {
            updateFirestoreUser(uid, nickName, fullName, newEmail);
            return;
        }

        if (oldPassword.isEmpty()) {
            btnUpdateProfile.setEnabled(true);

            tvWarning.setText("Isi Password Lama untuk mengubah email!");
            tvWarning.setVisibility(View.VISIBLE);

            etOldPassword.requestFocus();
            return;
        }

        showLoading("Menyimpan perubahan...");

        /*
         * Membuat credential dari email lama dan password lama.
         * Credential ini digunakan untuk verifikasi ulang user.
         */
        AuthCredential credential =
                EmailAuthProvider.getCredential(currentEmail, oldPassword);

        // Login ulang sebelum update email
        currentUser.reauthenticate(credential)
                .addOnSuccessListener(unused -> {

                    // Jika verifikasi berhasil, update email di Firebase Authentication
                    currentUser.updateEmail(newEmail)
                            .addOnSuccessListener(unusedEmail -> {

                                /*
                                 * Setelah email di Authentication berhasil berubah,
                                 * update data user di Firestore.
                                 */
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
        // Menyiapkan data yang akan diupdate ke Firestore
        Map<String, Object> data = new HashMap<>();
        data.put("nickName", nickName);
        data.put("fullName", fullName);
        data.put("email", email);
        data.put("updatedAt", FieldValue.serverTimestamp());

        // Update document users/{uid}
        db.collection("users")
                .document(uid)
                .update(data)
                .addOnSuccessListener(unused -> {

                    // Aktifkan kembali tombol
                    btnUpdateProfile.setEnabled(true);

                    // Update avatar huruf awal
                    setProfileInitial(fullName);

                    // Beri pesan berhasil
                    Toast.makeText(
                            requireContext(),
                            "Data akun berhasil diperbarui",
                            Toast.LENGTH_SHORT
                    ).show();

                    // Kembali ke ProfileFragment dengan transisi smooth
                    backToProfileSmooth();
                })
                .addOnFailureListener(e -> {

                    // Tutup loading
                    hideLoading();

                    // Aktifkan kembali tombol jika gagal
                    btnUpdateProfile.setEnabled(true);

                    Toast.makeText(
                            requireContext(),
                            "Gagal memperbarui data: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
    private void updatePasswordUser() {

        // Mengambil user yang sedang login
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(requireContext(), "User belum login", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mengambil input password lama dan password baru
        String oldPassword = etOldPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        // Validasi input password
        if (oldPassword.isEmpty() || newPassword.isEmpty()) {
            tvWarning.setText("Harap isi password secara lengkap!");
            tvWarning.setVisibility(View.VISIBLE);
            return;
        }

        // Validasi panjang password baru
        if (newPassword.length() < 8) {
            etNewPassword.setError("Password baru minimal 8 karakter");
            etNewPassword.requestFocus();
            return;
        }

        // Mengambil email user untuk membuat credential
        String currentEmail = currentUser.getEmail();

        if (currentEmail == null || currentEmail.isEmpty()) {
            Toast.makeText(requireContext(), "Email user tidak ditemukan", Toast.LENGTH_SHORT).show();
            return;
        }

        // Matikan tombol agar tidak diklik berkali-kali
        btnSaveChangesPassword.setEnabled(false);

        // Sembunyikan warning lama
        tvWarning.setVisibility(View.GONE);

        /*
         * Membuat credential dari email dan password lama.
         * Ini digunakan untuk memastikan password lama benar.
         */
        AuthCredential credential =
                EmailAuthProvider.getCredential(currentEmail, oldPassword);

        // Login ulang sebelum mengganti password
        currentUser.reauthenticate(credential)
                .addOnSuccessListener(unused -> {

                    // Jika password lama benar, update password baru
                    currentUser.updatePassword(newPassword)
                            .addOnSuccessListener(unusedPassword -> {

                                // Aktifkan kembali tombol
                                btnSaveChangesPassword.setEnabled(true);

                                // Kosongkan input password setelah berhasil
                                etOldPassword.setText("");
                                etNewPassword.setText("");

                                Toast.makeText(
                                        requireContext(),
                                        "Kata sandi berhasil diperbarui",
                                        Toast.LENGTH_SHORT
                                ).show();

                            })
                            .addOnFailureListener(e -> {

                                // Aktifkan kembali tombol jika gagal
                                btnSaveChangesPassword.setEnabled(true);

                                Toast.makeText(
                                        requireContext(),
                                        "Gagal memperbarui password: " + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            });

                })
                .addOnFailureListener(e -> {

                    // Aktifkan kembali tombol jika verifikasi gagal
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

        // Menampilkan kembali Bottom Navigation ketika keluar dari AccountUserFragment
        toggleBottomNavigation(true);
    }
    private void toggleBottomNavigation(boolean show) {

        // Pastikan fragment berada di MainActivity
        if (getActivity() instanceof MainActivity) {

            // Mengambil BottomNavigationView dari activity_main.xml
            BottomNavigationView bottomNav =
                    getActivity().findViewById(R.id.bottomNavigationView);

            if (bottomNav != null) {

                // Atur visibility sesuai parameter show
                bottomNav.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        }
    }
    /**
     * Kembali ke ProfileFragment dengan transisi yang sudah diatur
     * di MainActivity.loadFragment().
     */
    private void backToProfileSmooth() {

        if (!isAdded()) {
            return;
        }

        // Delay kecil agar Toast sempat muncul dan transisi tidak terasa mendadak
        requireView().postDelayed(() -> {
            if (isAdded()) {
                getParentFragmentManager().popBackStack();
            }
        }, 250);
    }
}