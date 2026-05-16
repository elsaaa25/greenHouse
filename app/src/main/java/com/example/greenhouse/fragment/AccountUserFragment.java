package com.example.greenhouse.fragment;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import androidx.activity.OnBackPressedCallback;
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

import java.util.HashMap;
import java.util.Map;

import com.example.greenhouse.api.CloudinaryApi;
import com.example.greenhouse.api.CloudinaryResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

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

    // URI foto yang dipilih dari galeri
    private Uri selectedImageUri;

    // Loading dialog
    private AlertDialog loadingDialog;

    // Launcher untuk membuka galeri dan mengambil hasil foto
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri imageUri = result.getData().getData();
                            if (imageUri != null) {
                                selectedImageUri = imageUri;
                                ivProfileImage.setImageURI(selectedImageUri);
                                ivProfileImage.setVisibility(View.VISIBLE);
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
        return inflater.inflate(R.layout.fragment_account_user, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        // Menghilangkan Bottom Navigation dengan animasi halus
        toggleBottomNavigation(false);

        // Menangani tombol back sistem agar sinkron dengan animasi
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                backToProfileSmooth();
            }
        });

        // Inisialisasi Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind View
        ivProfileImage = view.findViewById(R.id.ivProfileImage);
        tvProfilePlaceholder = view.findViewById(R.id.tvProfilePlaceholder);
        tvWarning = view.findViewById(R.id.tvWarning);
        ImageButton btnBack = view.findViewById(R.id.btnBack);
        CardView profileImageCard = view.findViewById(R.id.profileImageCard);
        TextView tvTapToChange = view.findViewById(R.id.tvTapToChange);
        etNickName = view.findViewById(R.id.etNickName);
        etFullName = view.findViewById(R.id.etFullName);
        etEmail = view.findViewById(R.id.etEmail);
        btnUpdateProfile = view.findViewById(R.id.btnUpdateProfile);
        etOldPassword = view.findViewById(R.id.etOldPassword);
        etNewPassword = view.findViewById(R.id.etNewPassword);
        btnSaveChangesPassword = view.findViewById(R.id.btnSaveChangesPassword);
        btnCancel = view.findViewById(R.id.btnCancel);

        // Beri jeda pengambilan data agar animasi masuk selesai dulu (cegah lag)
        view.postDelayed(() -> {
            if (isAdded()) {
                ambilDataUser();
            }
        }, 300);

        // Listener Tombol
        View.OnClickListener goBackListener = v -> backToProfileSmooth();
        btnBack.setOnClickListener(goBackListener);
        btnCancel.setOnClickListener(goBackListener);

        View.OnClickListener pickImageListener = v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        };
        profileImageCard.setOnClickListener(pickImageListener);
        if (tvTapToChange != null) tvTapToChange.setOnClickListener(pickImageListener);

        btnUpdateProfile.setOnClickListener(v -> updateDataProfilDanEmail());
        btnSaveChangesPassword.setOnClickListener(v -> updatePasswordUser());
    }

    private void toggleBottomNavigation(boolean show) {
        if (getActivity() instanceof MainActivity) {
            View bottomNav = getActivity().findViewById(R.id.bottomNavigationView);
            if (bottomNav != null) {
                if (show) {
                    bottomNav.animate().translationY(0).setDuration(300).start();
                } else {
                    // Turunkan menu ke bawah layar agar tidak menghalangi fragment
                    bottomNav.animate().translationY(bottomNav.getHeight()).setDuration(300).start();
                }
            }
        }
    }

    private void backToProfileSmooth() {
        if (!isAdded()) return;

        // Sembunyikan keyboard
        View focus = requireActivity().getCurrentFocus();
        if (focus != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                    requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }

        getParentFragmentManager().popBackStack();
        toggleBottomNavigation(true);
    }

    private void ambilDataUser() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nickName = documentSnapshot.getString("nickName");
                        String fullName = documentSnapshot.getString("fullName");
                        String email = documentSnapshot.getString("email");
                        String photoUrl = documentSnapshot.getString("photoUrl");

                        etNickName.setText(nickName != null ? nickName : "");
                        etFullName.setText(fullName != null ? fullName : "");
                        etEmail.setText(email != null ? email : currentUser.getEmail());

                        tampilkanFotoProfil(photoUrl, fullName);
                    }
                });
    }

    private void tampilkanFotoProfil(String photoUrl, String fullName) {
        if (photoUrl != null && !photoUrl.trim().isEmpty()) {
            ivProfileImage.setVisibility(View.VISIBLE);
            tvProfilePlaceholder.setVisibility(View.GONE);
            Glide.with(this).load(photoUrl).centerCrop().into(ivProfileImage);
        } else {
            ivProfileImage.setVisibility(View.GONE);
            tvProfilePlaceholder.setVisibility(View.VISIBLE);
            if (fullName != null && !fullName.isEmpty()) {
                tvProfilePlaceholder.setText(fullName.substring(0, 1).toUpperCase());
            }
        }
    }

    private void updateDataProfilDanEmail() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String nickName = etNickName.getText().toString().trim();
        String fullName = etFullName.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();
        String oldPassword = etOldPassword.getText().toString().trim();

        if (nickName.isEmpty() || fullName.isEmpty() || newEmail.isEmpty()) {
            tvWarning.setVisibility(View.VISIBLE);
            return;
        }

        btnUpdateProfile.setEnabled(false);
        String currentEmail = currentUser.getEmail();

        if (newEmail.equals(currentEmail)) {
            updateFirestoreUser(currentUser.getUid(), nickName, fullName, newEmail);
        } else {
            if (oldPassword.isEmpty()) {
                btnUpdateProfile.setEnabled(true);
                tvWarning.setText("Isi Password Lama untuk mengubah email!");
                tvWarning.setVisibility(View.VISIBLE);
                return;
            }
            showLoading("Menyimpan perubahan...");
            AuthCredential credential = EmailAuthProvider.getCredential(currentEmail, oldPassword);
            currentUser.reauthenticate(credential).addOnSuccessListener(unused -> {
                currentUser.updateEmail(newEmail).addOnSuccessListener(unusedEmail -> {
                    updateFirestoreUser(currentUser.getUid(), nickName, fullName, newEmail);
                }).addOnFailureListener(e -> {
                    hideLoading();
                    btnUpdateProfile.setEnabled(true);
                    Toast.makeText(requireContext(), "Gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }).addOnFailureListener(e -> {
                hideLoading();
                btnUpdateProfile.setEnabled(true);
                tvWarning.setText("Password salah!");
                tvWarning.setVisibility(View.VISIBLE);
            });
        }
    }

    private void updateFirestoreUser(String uid, String nickName, String fullName, String email) {
        showLoading("Menyimpan...");
        if (selectedImageUri != null) {
            uploadFotoKeCloudinary(uid, nickName, fullName, email);
        } else {
            simpanDataUserKeFirestore(uid, nickName, fullName, email, null);
        }
    }

    private void uploadFotoKeCloudinary(String uid, String nickName, String fullName, String email) {
        try {
            File file = uriToFile(selectedImageUri);
            RequestBody requestFile = RequestBody.create(file, MediaType.parse("image/*"));
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
            RequestBody uploadPreset = RequestBody.create("greenhouse_profile", MediaType.parse("text/plain"));

            Retrofit retrofit = new Retrofit.Builder().baseUrl("https://api.cloudinary.com/").addConverterFactory(GsonConverterFactory.create()).build();
            CloudinaryApi api = retrofit.create(CloudinaryApi.class);

            api.uploadImage("ddver48sg", body, uploadPreset).enqueue(new retrofit2.Callback<CloudinaryResponse>() {
                @Override
                public void onResponse(retrofit2.Call<CloudinaryResponse> call, retrofit2.Response<CloudinaryResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        simpanDataUserKeFirestore(uid, nickName, fullName, email, response.body().getSecureUrl());
                    } else {
                        hideLoading();
                        btnUpdateProfile.setEnabled(true);
                        Toast.makeText(requireContext(), "Upload gagal", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(retrofit2.Call<CloudinaryResponse> call, Throwable t) {
                    hideLoading();
                    btnUpdateProfile.setEnabled(true);
                }
            });
        } catch (Exception e) {
            hideLoading();
            btnUpdateProfile.setEnabled(true);
        }
    }

    private void simpanDataUserKeFirestore(String uid, String nickName, String fullName, String email, @Nullable String photoUrl) {
        Map<String, Object> data = new HashMap<>();
        data.put("nickName", nickName);
        data.put("fullName", fullName);
        data.put("email", email);
        data.put("updatedAt", FieldValue.serverTimestamp());
        if (photoUrl != null) data.put("photoUrl", photoUrl);

        db.collection("users").document(uid).update(data).addOnSuccessListener(unused -> {
            hideLoading();
            Toast.makeText(requireContext(), "Berhasil diperbarui", Toast.LENGTH_SHORT).show();
            backToProfileSmooth();
        }).addOnFailureListener(e -> {
            hideLoading();
            btnUpdateProfile.setEnabled(true);
        });
    }

    private void updatePasswordUser() {
        FirebaseUser user = auth.getCurrentUser();
        String oldPass = etOldPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();

        if (oldPass.isEmpty() || newPass.length() < 8) {
            tvWarning.setText("Password minimal 8 karakter!");
            tvWarning.setVisibility(View.VISIBLE);
            return;
        }

        btnSaveChangesPassword.setEnabled(false);
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPass);
        user.reauthenticate(credential).addOnSuccessListener(unused -> {
            user.updatePassword(newPass).addOnSuccessListener(a -> {
                btnSaveChangesPassword.setEnabled(true);
                etOldPassword.setText("");
                etNewPassword.setText("");
                Toast.makeText(requireContext(), "Password diperbarui", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> btnSaveChangesPassword.setEnabled(true));
    }

    private File uriToFile(Uri uri) throws Exception {
        File file = new File(requireContext().getCacheDir(), "temp_img.jpg");
        InputStream is = requireContext().getContentResolver().openInputStream(uri);
        FileOutputStream os = new FileOutputStream(file);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) > 0) os.write(buffer, 0, length);
        os.close(); is.close();
        return file;
    }

    private void showLoading(String msg) {
        if (!isAdded()) return;
        ProgressBar pb = new ProgressBar(requireContext());
        loadingDialog = new AlertDialog.Builder(requireContext()).setView(pb).setCancelable(false).create();
        loadingDialog.show();
    }

    private void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
    }
}
