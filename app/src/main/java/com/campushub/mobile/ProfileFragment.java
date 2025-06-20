package com.campushub.mobile;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.campushub.mobile.models.User;
import com.campushub.mobile.utils.UserCache;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

public class ProfileFragment extends Fragment implements UserCache.UserDataUpdateListener {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView nameTextView;
    private TextView emailTextView;
    private TextView phoneTextView;
    private ImageView profileImageView;
    private ProgressBar profileImageProgressBar;
    private UserCache userCache;

    public ProfileFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userCache = UserCache.getInstance(requireContext());
        userCache.setUserDataUpdateListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        nameTextView = view.findViewById(R.id.name);
        emailTextView = view.findViewById(R.id.email_field);
        phoneTextView = view.findViewById(R.id.editTextTitle4);
        profileImageView = view.findViewById(R.id.imageView7);
        profileImageProgressBar = view.findViewById(R.id.profile_image_progress);

        if (profileImageProgressBar != null) {
            profileImageProgressBar.setVisibility(View.VISIBLE);
        }
        if (profileImageView != null) {
            profileImageView.setVisibility(View.INVISIBLE);
        }

        Button editProfileButton = view.findViewById(R.id.edit_profile_button);
        editProfileButton.setOnClickListener(v -> {
            EditProfileFragment editProfileFragment = new EditProfileFragment();
            Bundle args = new Bundle();
            editProfileFragment.setArguments(args);

            requireActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, editProfileFragment).addToBackStack(null).commit();
        });

        Button logoutButton = view.findViewById(R.id.login_button7);
        logoutButton.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi")
            .setMessage("Apakah anda yakin ingin logout?")
            .setPositiveButton("Ya", (dialog, which) -> {
                userCache.clearCache();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().finish();
                }
            })
            .setNegativeButton("Tidak", null)
            .show());

        Button deleteAccountButton = view.findViewById(R.id.login_button8);
        deleteAccountButton.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi")
            .setMessage("Apakah anda yakin ingin menghapus akun ini? Semua data anda akan dihapus secara permanen.")
            .setPositiveButton("Ya", (dialog, which) -> {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    currentUser.delete().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = currentUser.getUid();
                            db.collection("events").whereEqualTo("ownerId", uid).get().addOnSuccessListener(eventsSnapshot -> {
                                WriteBatch batch = db.batch();
                                for (DocumentSnapshot eventDoc : eventsSnapshot.getDocuments()) {
                                    batch.delete(eventDoc.getReference());
                                }
                                db.collection("registrations").whereEqualTo("userId", uid).get().addOnSuccessListener(regSnapshot -> {
                                    for (DocumentSnapshot regDoc : regSnapshot.getDocuments()) {
                                        batch.delete(regDoc.getReference());
                                    }
                                    batch.delete(db.collection("users").document(uid));
                                    batch.commit().addOnCompleteListener(batchTask -> {
                                        if (batchTask.isSuccessful()) {
                                            userCache.clearCache();
                                            Intent intent = new Intent(getActivity(), LoginActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(intent);
                                        }
                                    });
                                });
                            });
                        } else {
                            Exception e = task.getException();
                            if (e instanceof FirebaseAuthRecentLoginRequiredException) {
                                Toast.makeText(getContext(), "Silakan relogin sebelum menghapus akun.", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            })
            .setNegativeButton("Tidak", null)
            .show());

        loadUserData();
    }

    private void loadUserData() {
        User cachedUser = userCache.getCachedUserData();
        if (cachedUser != null) {
            updateUI(cachedUser);
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            db.collection("users").document(userId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    User userData = document.toObject(User.class);
                    if (userData != null) {
                        userCache.cacheUserData(userId, userData);
                        updateUI(userData);
                    }
                }
            });
        }
    }

    @Override
    public void onUserDataUpdated(User user) {
        updateUI(user);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (userCache != null) {
            userCache.setUserDataUpdateListener(null);
        }
    }

    private void updateUI(User userData) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (userData.getNamaLengkap() != null && !userData.getNamaLengkap().isEmpty()) {
                    nameTextView.setText(userData.getNamaLengkap());
                }

                if (userData.getAlamatEmail() != null && !userData.getAlamatEmail().isEmpty()) {
                    emailTextView.setText(userData.getAlamatEmail());
                }

                if (userData.getNomorTelepon() != null && !userData.getNomorTelepon().isEmpty()) {
                    phoneTextView.setText(userData.getNomorTelepon());
                }

                if (userData.getPhotoUrl() != null && !userData.getPhotoUrl().isEmpty()) {
                    if (profileImageProgressBar != null) {
                        profileImageProgressBar.setVisibility(View.VISIBLE);
                    }
                    if (profileImageView != null) {
                        profileImageView.setVisibility(View.INVISIBLE);
                    }

                    Glide.with(this).load(userData.getPhotoUrl()).transform(new CircleCrop()).listener(new com.bumptech.glide.request.RequestListener<>() {
                        @Override
                        public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, @NonNull com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            if (profileImageProgressBar != null) {
                                profileImageProgressBar.setVisibility(View.GONE);
                            }
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(@NonNull android.graphics.drawable.Drawable resource, @NonNull Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, @NonNull com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            if (profileImageProgressBar != null) {
                                profileImageProgressBar.setVisibility(View.GONE);
                            }
                            if (profileImageView != null) {
                                profileImageView.setVisibility(View.VISIBLE);
                            }
                            return false;
                        }
                    }).into(profileImageView);
                } else {
                    if (profileImageProgressBar != null) {
                        profileImageProgressBar.setVisibility(View.GONE);
                    }
                    if (profileImageView != null) {
                        profileImageView.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }
}