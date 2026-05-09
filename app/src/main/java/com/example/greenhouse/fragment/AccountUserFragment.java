package com.example.greenhouse.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import static android.app.Activity.RESULT_OK;

import com.example.greenhouse.R;
import com.example.greenhouse.activity.MainActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AccountUserFragment extends Fragment {

    private ImageView ivProfileImage;
    private TextView tvProfilePlaceholder;
    private TextView tvWarning;
    private EditText etOldPassword, etNewPassword;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        ivProfileImage.setImageURI(imageUri);
                        ivProfileImage.setVisibility(View.VISIBLE);
                        tvProfilePlaceholder.setVisibility(View.GONE);
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Sembunyikan Bottom Navigation saat fragment ini ditampilkan
        toggleBottomNavigation(false);

        // Initialize Views
        ivProfileImage = view.findViewById(R.id.ivProfileImage);
        tvProfilePlaceholder = view.findViewById(R.id.tvProfilePlaceholder);
        tvWarning = view.findViewById(R.id.tvWarning);
        etOldPassword = view.findViewById(R.id.etOldPassword);
        etNewPassword = view.findViewById(R.id.etNewPassword);
        ImageButton btnBack = view.findViewById(R.id.btnBack);
        CardView profileImageCard = view.findViewById(R.id.profileImageCard);
        Button btnSaveChanges = view.findViewById(R.id.btnSaveChanges);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        // Back Navigation
        View.OnClickListener goBackListener = v -> {
            if (getActivity() != null) {
                getActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        };
        
        btnBack.setOnClickListener(goBackListener);
        btnCancel.setOnClickListener(goBackListener);

        // Image Selection
        View.OnClickListener pickImageListener = v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        };
        profileImageCard.setOnClickListener(pickImageListener);
        
        TextView tvTapToChange = view.findViewById(R.id.tvTapToChange);
        if (tvTapToChange != null) {
            tvTapToChange.setOnClickListener(pickImageListener);
        }

        // Validation & Save
        btnSaveChanges.setOnClickListener(v -> {
            String oldPass = etOldPassword.getText().toString().trim();
            String newPass = etNewPassword.getText().toString().trim();

            if (oldPass.isEmpty() || newPass.isEmpty()) {
                tvWarning.setVisibility(View.VISIBLE);
            } else {
                tvWarning.setVisibility(View.GONE);
                if (getActivity() != null) {
                    getActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Tampilkan kembali Bottom Navigation saat fragment ini ditutup
        toggleBottomNavigation(true);
    }

    private void toggleBottomNavigation(boolean show) {
        if (getActivity() instanceof MainActivity) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottomNavigationView);
            if (bottomNav != null) {
                bottomNav.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        }
    }
}
