package com.campushub.mobile;

import static com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
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
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.button.MaterialButton;

import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CredentialManager credentialManager;
    private GoogleIdTokenCredential googleIdTokenCredential;
    private EditText emailField;
    private EditText passwordField;
    private Button loginButton;
    private MaterialButton googleButton;
    private TextView daftar;
    private ProgressBar loginProgress;
    private ProgressBar googleProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        emailField = findViewById(R.id.email_field);
        passwordField = findViewById(R.id.password_field);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        credentialManager = CredentialManager.create(getBaseContext());

        daftar = findViewById(R.id.daftar);
        loginButton = findViewById(R.id.login_button);
        googleButton = findViewById(R.id.google_button);
        loginProgress = findViewById(R.id.login_progress);
        googleProgress = findViewById(R.id.google_progress);

        daftar.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));

        loginButton.setOnClickListener(v -> {
            disableAllUIForLogin();
            firebaseAuthWithEmailAndPassword(emailField.getText().toString(), passwordField.getText().toString());
        });

        googleButton.setOnClickListener(v -> {
            disableAllUIForGoogle();
            launchCredentialManager();
        });

    }

    private void firebaseAuthWithEmailAndPassword(String email, String password) {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(LoginActivity.this, "Email and Password cannot be empty.", Toast.LENGTH_SHORT).show();
            enableAllUIForLogin();
        } else {
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
                enableAllUIForLogin();
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    updateUI(user, null);
                } else {
                    updateUI(null, "Authentication Failed. Please check your email and password.");
                }
            });
        }
    }

    private void launchCredentialManager() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false).setServerClientId(getString(R.string.web_client_id)).build();

        GetCredentialRequest request = new GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build();

        credentialManager.getCredentialAsync(getBaseContext(), request, new CancellationSignal(), Executors.newSingleThreadExecutor(), new CredentialManagerCallback<>() {
            @Override
            public void onResult(GetCredentialResponse result) {
                handleGoogleSignIn(result.getCredential());
            }

            @Override
            public void onError(@NonNull GetCredentialException e) {
                updateUI(null, "Failed to retrieve Google ID Token. Please try again.");
                runOnUiThread(LoginActivity.this::enableAllUIForGoogle);
            }
        });
    }

    private void handleGoogleSignIn(Credential credential) {
        if (credential instanceof CustomCredential customCredential && credential.getType().equals(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
            Bundle credentialData = customCredential.getData();
            googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credentialData);

            firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());
        } else {
            Toast.makeText(LoginActivity.this, "Credential is not of type Google ID!", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            enableAllUIForGoogle();
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    saveGoogleUserDataToFirestore(user, googleIdTokenCredential);
                }
            } else {
                updateUI(null, "Authentication Failed.");
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

                    db.collection("users").document(userId).set(user).addOnSuccessListener(aVoid -> updateUI(firebaseUser, null)).addOnFailureListener(e -> updateUI(firebaseUser, null));
                } else {
                    updateUI(firebaseUser, null);
                }
            } else {
                updateUI(null, "Terjadi kesalahan, silahkan coba lagi");
            }
        });
    }

    private void updateUI(final FirebaseUser user, final String message) {
        if (user != null) {
            db.collection("users").document(user.getUid()).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Intent intent = getIntent(task);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(LoginActivity.this, NoConnectionActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            });
        } else {
            runOnUiThread(() -> {
                try {
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                } catch (Exception ignored) {
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
            intent = new Intent(LoginActivity.this, EoActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, MhsActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    private void disableAllUIForLogin() {
        loginButton.setText("");
        emailField.setEnabled(false);
        passwordField.setEnabled(false);
        loginButton.setEnabled(false);
        googleButton.setEnabled(false);
        daftar.setEnabled(false);
        loginProgress.setVisibility(View.VISIBLE);
    }

    @SuppressLint("SetTextI18n")
    private void enableAllUIForLogin() {
        loginButton.setText("Login");
        emailField.setEnabled(true);
        passwordField.setEnabled(true);
        loginButton.setEnabled(true);
        googleButton.setEnabled(true);
        daftar.setEnabled(true);
        loginProgress.setVisibility(View.GONE);
    }

    private void disableAllUIForGoogle() {
        googleButton.setText("");
        googleButton.setIcon(null);
        emailField.setEnabled(false);
        passwordField.setEnabled(false);
        loginButton.setEnabled(false);
        googleButton.setEnabled(false);
        daftar.setEnabled(false);
        googleProgress.setVisibility(View.VISIBLE);
    }

    @SuppressLint("SetTextI18n")
    private void enableAllUIForGoogle() {
        googleButton.setText("Lanjutkan dengan Google");
        googleButton.setIconResource(R.drawable.icon_google);
        emailField.setEnabled(true);
        passwordField.setEnabled(true);
        loginButton.setEnabled(true);
        googleButton.setEnabled(true);
        daftar.setEnabled(true);
        googleProgress.setVisibility(View.GONE);
    }
}