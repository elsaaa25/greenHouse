package com.example.greenhouse;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AccountUserActivity extends AppCompatActivity {

    private ImageView ivProfileImage;
    private TextView tvProfilePlaceholder;
    private TextView tvWarning;
    private EditText etOldPassword, etNewPassword;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    ivProfileImage.setImageURI(imageUri);
                    ivProfileImage.setVisibility(View.VISIBLE);
                    tvProfilePlaceholder.setVisibility(View.GONE);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account_user);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Views
        ivProfileImage = findViewById(R.id.ivProfileImage);
        tvProfilePlaceholder = findViewById(R.id.tvProfilePlaceholder);
        tvWarning = findViewById(R.id.tvWarning);
        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        ImageButton btnBack = findViewById(R.id.btnBack);
        CardView profileImageCard = findViewById(R.id.profileImageCard);
        Button btnSaveChanges = findViewById(R.id.btnSaveChanges);
        Button btnCancel = findViewById(R.id.btnCancel);

        // Back Navigation
        btnBack.setOnClickListener(v -> finish());

        // Image Selection
        View.OnClickListener pickImageListener = v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        };
        profileImageCard.setOnClickListener(pickImageListener);
        findViewById(R.id.tvTapToChange).setOnClickListener(pickImageListener);

        // Validation & Save
        btnSaveChanges.setOnClickListener(v -> {
            String oldPass = etOldPassword.getText().toString().trim();
            String newPass = etNewPassword.getText().toString().trim();

            if (oldPass.isEmpty() || newPass.isEmpty()) {
                tvWarning.setVisibility(View.VISIBLE);
            } else {
                tvWarning.setVisibility(View.GONE);
                // Logika simpan: Di sini kita hanya kembali ke profil.
                // Data sebelumnya di halaman profil tidak akan berubah karena kita tidak mengirim data balik atau menyimpannya di DB/SharedPrefs.
                // Ini sesuai permintaan: "ikon info akun ditekan, maka masih menampilkan data sebelumnya"
                finish();
            }
        });

        btnCancel.setOnClickListener(v -> finish());
    }
}