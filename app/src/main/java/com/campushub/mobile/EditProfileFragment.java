package com.campushub.mobile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.campushub.mobile.models.User;
import com.campushub.mobile.utils.UserCache;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Objects;

public class EditProfileFragment extends Fragment implements UserCache.UserDataUpdateListener {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    private EditText nameEditText;
    private EditText emailEditText;
    private EditText phoneEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private ImageView profileImageView;
    private TextView nameDisplayTextView;
    private FrameLayout loadingOverlay;

    private UserCache userCache;
    private User currentUser;
    private Uri selectedImageUri;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    public EditProfileFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        userCache = UserCache.getInstance(requireContext());
        userCache.setUserDataUpdateListener(this);

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                selectedImageUri = result.getData().getData();
                if (selectedImageUri != null) {
                    Glide.with(this).load(selectedImageUri).transform(new CircleCrop()).into(profileImageView);
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        nameEditText = view.findViewById(R.id.email_field);
        emailEditText = view.findViewById(R.id.editTextTitle4);
        phoneEditText = view.findViewById(R.id.email_field7);
        passwordEditText = view.findViewById(R.id.editTextTitle6);
        confirmPasswordEditText = view.findViewById(R.id.editTextTitle7);
        profileImageView = view.findViewById(R.id.imageView7);
        nameDisplayTextView = view.findViewById(R.id.name);
        Button saveButton = view.findViewById(R.id.delete_button);
        Button cancelButton = view.findViewById(R.id.batal_button);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);

        cancelButton.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        saveButton.setOnClickListener(v -> saveProfileChanges());

        profileImageView.setOnClickListener(v -> openImagePicker());

        loadUserData();
    }

    private void loadUserData() {
        User cachedUser = userCache.getCachedUserData();
        if (cachedUser != null) {
            currentUser = cachedUser;
            updateUI(cachedUser);
            return;
        }

        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();

            db.collection("users").document(userId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        User userData = document.toObject(User.class);
                        if (userData != null) {
                            currentUser = userData;
                            userCache.cacheUserData(userId, userData);
                            updateUI(userData);
                        }
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateUI(User userData) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (userData.getNamaLengkap() != null && !userData.getNamaLengkap().isEmpty()) {
                    nameDisplayTextView.setText(userData.getNamaLengkap());
                    nameEditText.setText(userData.getNamaLengkap());
                }

                if (userData.getAlamatEmail() != null && !userData.getAlamatEmail().isEmpty()) {
                    emailEditText.setText(userData.getAlamatEmail());
                }

                if (userData.getNomorTelepon() != null && !userData.getNomorTelepon().isEmpty()) {
                    phoneEditText.setText(userData.getNomorTelepon());
                }

                if (userData.getPhotoUrl() != null && !userData.getPhotoUrl().isEmpty()) {
                    Glide.with(this).load(userData.getPhotoUrl()).transform(new CircleCrop()).into(profileImageView);
                }
            });
        }
    }

    @SuppressLint("IntentReset")
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    @SuppressLint("SetTextI18s")
    private void saveProfileChanges() {
        String newName = nameEditText.getText().toString().trim();
        String newEmail = emailEditText.getText().toString().trim();
        String newPhone = phoneEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(newName)) {
            nameEditText.setError("Name is required");
            return;
        }

        if (TextUtils.isEmpty(newEmail)) {
            emailEditText.setError("Email is required");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            emailEditText.setError("Please enter a valid email address");
            return;
        }

        if (!TextUtils.isEmpty(password) || !TextUtils.isEmpty(confirmPassword)) {
            if (password.length() < 6) {
                passwordEditText.setError("Password must be at least 6 characters");
                return;
            }

            if (!password.equals(confirmPassword)) {
                confirmPasswordEditText.setError("Passwords do not match");
                return;
            }
        }

        showLoading();

        User updatedUser = new User(currentUser.getIs_eo(), newName, newEmail, newPhone, currentUser.getPhotoUrl());

        saveUserData(updatedUser, password);
    }

    private void saveUserData(User updatedUser, String newPassword) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();

        assert firebaseUser != null;
        String userId = firebaseUser.getUid();

        if (selectedImageUri != null) {
            uploadProfileImage(userId, updatedUser, newPassword);
        } else {
            updateUserProfile(userId, updatedUser, newPassword);
        }
    }

    private void uploadProfileImage(String userId, User updatedUser, String newPassword) {
        StorageReference imageRef = storageRef.child("users/" + userId + ".jpg");

        imageRef.putFile(selectedImageUri).addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            updatedUser.setPhotoUrl(uri.toString());
            updateUserProfile(userId, updatedUser, newPassword);
        })).addOnFailureListener(e -> {
            hideLoading();
            Toast.makeText(getContext(), "Gagal mengunggah foto", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateUserProfile(String userId, User updatedUser, String newPassword) {
        db.collection("users").document(userId).set(updatedUser).addOnSuccessListener(aVoid -> {
            userCache.cacheUserData(userId, updatedUser);

            FirebaseUser firebaseUser = mAuth.getCurrentUser();
            if (firebaseUser != null && !Objects.equals(firebaseUser.getEmail(), updatedUser.getAlamatEmail())) {
                firebaseUser.updateEmail(updatedUser.getAlamatEmail()).addOnSuccessListener(aVoid1 -> updatePasswordIfNeeded(newPassword, updatedUser)).addOnFailureListener(e -> {
                    hideLoading();
                    Toast.makeText(getContext(), "Failed to update email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } else {
                updatePasswordIfNeeded(newPassword, updatedUser);
            }
        }).addOnFailureListener(e -> {
            hideLoading();
            Toast.makeText(getContext(), "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void updatePasswordIfNeeded(String newPassword, User updatedUser) {
        if (!TextUtils.isEmpty(newPassword)) {
            FirebaseUser firebaseUser = mAuth.getCurrentUser();
            if (firebaseUser != null) {
                firebaseUser.updatePassword(newPassword).addOnSuccessListener(aVoid -> onProfileUpdateSuccess(updatedUser)).addOnFailureListener(e -> {
                    hideLoading();
                    Toast.makeText(getContext(), "Failed to update password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } else {
                onProfileUpdateSuccess(updatedUser);
            }
        } else {
            onProfileUpdateSuccess(updatedUser);
        }
    }

    private void onProfileUpdateSuccess(User updatedUser) {
        currentUser = updatedUser;
        hideLoading();
        Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();

        getParentFragmentManager().popBackStack();
    }

    @Override
    public void onUserDataUpdated(User user) {
        if (user != null) {
            currentUser = user;
            updateUI(user);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (userCache != null) {
            userCache.setUserDataUpdateListener(null);
        }
    }

    private void showLoading() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(View.GONE);
        }
    }
}