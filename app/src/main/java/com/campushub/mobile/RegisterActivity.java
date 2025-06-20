package com.campushub.mobile;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.campushub.mobile.eo.EoActivity;
import com.campushub.mobile.mhs.MhsActivity;
import com.campushub.mobile.models.User;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.button.MaterialButton;

import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CredentialManager credentialManager;

    private EditText nameField;
    private EditText emailFieldReg;
    private EditText phoneField;
    private EditText passwordFieldReg;
    private Button registerButton;
    private MaterialButton googleButtonReg;
    private TextView loginLink;
    private ProgressBar registerProgress;
    private ProgressBar googleProgressReg;

    @NonNull
    private static String getErrorMessage(Task<AuthResult> task) {
        String errorMessage = "Registrasi gagal";
        if (task.getException() != null) {
            String exceptionMessage = task.getException().getMessage();
            if (exceptionMessage != null) {
                if (exceptionMessage.contains("email address is already in use")) {
                    errorMessage = "Email sudah terdaftar";
                } else if (exceptionMessage.contains("weak password")) {
                    errorMessage = "Password terlalu lemah";
                }
            }
        }
        return errorMessage;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        credentialManager = CredentialManager.create(getBaseContext());

        nameField = findViewById(R.id.email_field2);
        emailFieldReg = findViewById(R.id.email_field3);
        phoneField = findViewById(R.id.phone_number);
        passwordFieldReg = findViewById(R.id.email_field5);

        registerButton = findViewById(R.id.login_button5);
        googleButtonReg = findViewById(R.id.login_button6);
        loginLink = findViewById(R.id.login);

        registerProgress = findViewById(R.id.register_progress);
        googleProgressReg = findViewById(R.id.google_progress_register);

        registerButton.setOnClickListener(v -> {
            disableAllUIForRegister();
            String name = nameField.getText().toString().trim();
            String email = emailFieldReg.getText().toString().trim();
            String phone = phoneField.getText().toString().trim();
            String password = passwordFieldReg.getText().toString().trim();
            if (validateRegistrationData(name, email, phone, password)) {
                registerWithEmailPassword(name, email, phone, password);
            } else {
                enableAllUIForRegister();
            }
        });

        googleButtonReg.setOnClickListener(v -> {
            disableAllUIForGoogleReg();
            signInWithGoogle();
        });

        loginLink.setOnClickListener(v -> finish());
    }

    private boolean validateRegistrationData(String name, String email, String phone, String password) {
        if (name.isEmpty()) {
            Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (email.isEmpty()) {
            Toast.makeText(this, "Email tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Format email tidak valid", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (phone.isEmpty()) {
            Toast.makeText(this, "Nomor telepon tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "Password tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void registerWithEmailPassword(String name, String email, String phone, String password) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                if (firebaseUser != null) {
                    User user = new User(false, name, email, phone, null);
                    saveUserDataToFirestore(firebaseUser.getUid(), user);
                }
            } else {
                String errorMessage = getErrorMessage(task);
                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                enableAllUIForRegister();
            }
        });
    }

    private void signInWithGoogle() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false).setServerClientId(getString(R.string.web_client_id)).setAutoSelectEnabled(true).build();

        GetCredentialRequest request = new GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build();

        credentialManager.getCredentialAsync(this, request, null, Executors.newSingleThreadExecutor(), new CredentialManagerCallback<>() {
            @Override
            public void onResult(GetCredentialResponse result) {
                handleGoogleSignIn(result);
            }

            @Override
            public void onError(@NonNull GetCredentialException e) {
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Google Sign-In gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                runOnUiThread(RegisterActivity.this::enableAllUIForGoogleReg);
            }
        });
    }

    private void handleGoogleSignIn(GetCredentialResponse result) {
        GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.getCredential().getData());

        String idToken = googleIdTokenCredential.getIdToken();
        AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(firebaseCredential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    saveGoogleUserDataToFirestore(user, googleIdTokenCredential);
                }
            } else {
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show());
                runOnUiThread(RegisterActivity.this::enableAllUIForGoogleReg);
            }
        });
    }

    private void saveGoogleUserDataToFirestore(FirebaseUser firebaseUser, GoogleIdTokenCredential googleCredential) {
        String userId = firebaseUser.getUid();

        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (!document.exists()) {
                    String displayName = googleCredential.getDisplayName();
                    String email = googleCredential.getId();
                    String photoUrl = String.valueOf(googleCredential.getProfilePictureUri());
                    String phoneNumber = googleCredential.getPhoneNumber();

                    User user = new User(false, displayName, email, phoneNumber, photoUrl);

                    saveUserDataToFirestore(userId, user);
                } else {
                    updateUIForExistingUser();
                }
            } else {
                updateUIForExistingUser();
            }
        });
    }

    private void saveUserDataToFirestore(String userId, User user) {
        db.collection("users").document(userId).set(user).addOnSuccessListener(aVoid -> {
            Toast.makeText(RegisterActivity.this, "Registrasi berhasil!", Toast.LENGTH_SHORT).show();
            updateUI(user);
        }).addOnFailureListener(e -> {
            Toast.makeText(RegisterActivity.this, "Gagal menyimpan data pengguna", Toast.LENGTH_SHORT).show();
            updateUI(user);
        });
    }

    private void updateUI(User user) {
        Intent intent;
        if (user.getIs_eo()) {
            intent = new Intent(RegisterActivity.this, EoActivity.class);
        } else {
            intent = new Intent(RegisterActivity.this, MhsActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void updateUIForExistingUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Intent intent = getIntent(task);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(RegisterActivity.this, NoConnectionActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            });
        }
    }

    @NonNull
    private Intent getIntent(Task<DocumentSnapshot> task) {
        DocumentSnapshot document = task.getResult();
        User userData = document.toObject(User.class);
        Intent intent;
        if (userData != null && userData.getIs_eo()) {
            intent = new Intent(RegisterActivity.this, EoActivity.class);
        } else {
            intent = new Intent(RegisterActivity.this, MhsActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    private void disableAllUIForRegister() {
        registerButton.setText("");
        nameField.setEnabled(false);
        emailFieldReg.setEnabled(false);
        phoneField.setEnabled(false);
        passwordFieldReg.setEnabled(false);
        registerButton.setEnabled(false);
        googleButtonReg.setEnabled(false);
        loginLink.setEnabled(false);
        registerProgress.setVisibility(View.VISIBLE);
    }

    @SuppressLint("SetTextI18n")
    private void enableAllUIForRegister() {
        registerButton.setText("Register");
        nameField.setEnabled(true);
        emailFieldReg.setEnabled(true);
        phoneField.setEnabled(true);
        passwordFieldReg.setEnabled(true);
        registerButton.setEnabled(true);
        googleButtonReg.setEnabled(true);
        loginLink.setEnabled(true);
        registerProgress.setVisibility(View.GONE);
    }

    private void disableAllUIForGoogleReg() {
        googleButtonReg.setText("");
        googleButtonReg.setIcon(null);
        nameField.setEnabled(false);
        emailFieldReg.setEnabled(false);
        phoneField.setEnabled(false);
        passwordFieldReg.setEnabled(false);
        registerButton.setEnabled(false);
        googleButtonReg.setEnabled(false);
        loginLink.setEnabled(false);
        googleProgressReg.setVisibility(View.VISIBLE);
    }

    @SuppressLint("SetTextI18n")
    private void enableAllUIForGoogleReg() {
        googleButtonReg.setText("Lanjutkan dengan Google");
        googleButtonReg.setIconResource(R.drawable.icon_google);
        nameField.setEnabled(true);
        emailFieldReg.setEnabled(true);
        phoneField.setEnabled(true);
        passwordFieldReg.setEnabled(true);
        registerButton.setEnabled(true);
        googleButtonReg.setEnabled(true);
        loginLink.setEnabled(true);
        googleProgressReg.setVisibility(View.GONE);
    }
}