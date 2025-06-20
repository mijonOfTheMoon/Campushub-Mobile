package com.campushub.mobile.mhs;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.campushub.mobile.R;
import com.campushub.mobile.models.Event;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class MhsDetailActivity extends AppCompatActivity {
    private TextView tvTitle, tvSubtitle, tvDate, tvTime, tvLocation, tvTickets, tvDescription, profileName, profileRole;
    private ImageView posterImage;
    private ShapeableImageView profileImage;
    private Event currentEvent;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private TableLayout registeredState;
    private LinearLayout cancelledState;
    private LinearLayout absentState;
    private LinearLayout attendedState;
    private LinearLayout waitlistState;
    private Button registerButton;
    private Button presensiButton;
    private String registrationDocId;
    private String registrationStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mhs_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageView backButton = findViewById(R.id.back_button);
        posterImage = findViewById(R.id.poster_image);
        tvTitle = findViewById(R.id.tv_event_title2);
        tvSubtitle = findViewById(R.id.tv_event_subtitle2);
        tvDate = findViewById(R.id.tv_event_date2);
        tvTime = findViewById(R.id.tv_event_speaker2);
        tvLocation = findViewById(R.id.tv_event_date3);
        tvTickets = findViewById(R.id.tv_event_speaker3);
        tvDescription = findViewById(R.id.textView17);
        profileImage = findViewById(R.id.profile_image);
        profileName = findViewById(R.id.profile_name);
        profileRole = findViewById(R.id.profile_role);
        registeredState = findViewById(R.id.registered_state);
        cancelledState = findViewById(R.id.cancelled_state);
        registerButton = findViewById(R.id.not_yet_registered_state);
        presensiButton = findViewById(R.id.presensi_button);
        absentState = findViewById(R.id.absent_state);
        attendedState = findViewById(R.id.attended_state);
        waitlistState = findViewById(R.id.waitlist_state);
        registeredState.setVisibility(View.GONE);
        cancelledState.setVisibility(View.GONE);
        registerButton.setVisibility(View.GONE);
        presensiButton.setVisibility(View.GONE);
        absentState.setVisibility(View.GONE);
        attendedState.setVisibility(View.GONE);
        waitlistState.setVisibility(View.GONE);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        backButton.setOnClickListener(v -> finish());

        currentEvent = (Event) getIntent().getSerializableExtra("event");
        populateFields(currentEvent);
        checkRegistration();

        registerButton.setOnClickListener(v -> createRegistration());
    }

    @SuppressLint("SetTextI18n")
    private void populateFields(@Nullable Event event) {
        if (event == null) return;
        tvTitle.setText(event.getTitle());
        tvSubtitle.setText(event.getCategory());
        tvDate.setText(event.getEventDate());
        tvTime.setText(event.getStartTime() + " - " + event.getEndTime());
        tvLocation.setText(event.getLocation());
        tvTickets.setText(String.valueOf(event.getTicketCount()));
        tvDescription.setText(event.getDescription());
        if (event.getImageUrl() != null)
            Glide.with(this).load(event.getImageUrl()).into(posterImage);
        if (event.getSpeakerImageUrl() != null)
            Glide.with(this).load(event.getSpeakerImageUrl()).into(profileImage);
        profileName.setText(event.getSpeakerName());
        profileRole.setText(event.getSpeakerTitle());
    }

    private void checkRegistration() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null || currentEvent == null) {
            showRegisterState();
            return;
        }
        db.collection("registrations").whereEqualTo("userId", uid).whereEqualTo("eventId", currentEvent.getId()).get().addOnSuccessListener(snapshot -> {
            if (!snapshot.isEmpty()) {
                var doc = snapshot.getDocuments().get(0);
                registrationDocId = doc.getId();
                registrationStatus = doc.getString("status");
                if ("Terdaftar".equalsIgnoreCase(registrationStatus)) {
                    showRegisteredState();
                } else if ("Dibatalkan".equalsIgnoreCase(registrationStatus)) {
                    showCancelledState();
                } else if ("Hadir".equalsIgnoreCase(registrationStatus)) {
                    showAttendedState();
                } else if ("Tidak Hadir".equalsIgnoreCase(registrationStatus)) {
                    showAbsentState();
                } else if ("Waiting List".equalsIgnoreCase(registrationStatus)) {
                    showWaitlistState();
                }
            } else {
                showRegisterState();
            }
        }).addOnFailureListener(e -> showRegisterState());
    }

    private boolean isEventExpired() {
        if (currentEvent == null) return true;
        String dateStr = currentEvent.getEventDate();
        String endStr = currentEvent.getEndTime();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date endDate = sdf.parse(dateStr + " " + endStr);
            return endDate != null && endDate.before(new Date());
        } catch (ParseException e) {
            return false;
        }
    }

    private void createRegistration() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null || currentEvent == null) return;
        if (isEventExpired()) {
            Toast.makeText(this, "Registrasi ditutup karena waktu event telah berlalu", Toast.LENGTH_SHORT).show();
            return;
        }
        String code = generateCode();
        Map<String, Object> data = new HashMap<>();
        data.put("userId", uid);
        data.put("eventId", currentEvent.getId());
        data.put("code", code);
        String status = (currentEvent.getTicketCount() < 1) ? "Waiting List" : "Terdaftar";
        data.put("status", status);
        data.put("createdAt", System.currentTimeMillis());
        db.collection("registrations").add(data).addOnSuccessListener(docRef -> {
            registrationDocId = docRef.getId();
            registrationStatus = status;
            checkRegistration();
        }).addOnFailureListener(e -> Toast.makeText(this, "Registrasi gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void cancelRegistration() {
        if (registrationDocId == null) return;
        Map<String, Object> update = new HashMap<>();
        update.put("status", "Dibatalkan");
        db.collection("registrations").document(registrationDocId).update(update).addOnSuccessListener(aVoid -> showCancelledState()).addOnFailureListener(e -> Toast.makeText(this, "Gagal membatalkan: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private String generateCode() {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    private void showRegisteredState() {
        registeredState.setVisibility(View.VISIBLE);
        presensiButton.setVisibility(View.VISIBLE);
        cancelledState.setVisibility(View.GONE);
        registerButton.setVisibility(View.GONE);
        absentState.setVisibility(View.GONE);
        attendedState.setVisibility(View.GONE);
        waitlistState.setVisibility(View.GONE);
        Button cancelBtn = findViewById(R.id.prev_button);
        cancelBtn.setOnClickListener(v -> new AlertDialog.Builder(MhsDetailActivity.this).setTitle("Konfirmasi").setMessage("Apakah anda yakin ingin membatalkan pendaftaran untuk acara ini? Anda tidak akan dapat mendaftar kembali.").setPositiveButton("Ya", (dialog, which) -> cancelRegistration()).setNegativeButton("Tidak", null).show());
        presensiButton.setOnClickListener(v -> {
            if (registrationDocId != null) {
                db.collection("registrations").document(registrationDocId).get().addOnSuccessListener(doc -> {
                    String code = doc.getString("code");
                    Intent intent = new Intent(MhsDetailActivity.this, MhsKodeActivity.class);
                    intent.putExtra("code", code);
                    intent.putExtra("title", currentEvent.getTitle());
                    intent.putExtra("category", currentEvent.getCategory());
                    intent.putExtra("location", currentEvent.getLocation());
                    intent.putExtra("date", currentEvent.getEventDate());
                    intent.putExtra("time", currentEvent.getStartTime() + " - " + currentEvent.getEndTime());
                    startActivity(intent);
                }).addOnFailureListener(e -> Toast.makeText(MhsDetailActivity.this, "Gagal memuat kode: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showRegisterState() {
        registeredState.setVisibility(View.GONE);
        cancelledState.setVisibility(View.GONE);
        registerButton.setVisibility(View.VISIBLE);
        absentState.setVisibility(View.GONE);
        attendedState.setVisibility(View.GONE);
        waitlistState.setVisibility(View.GONE);
    }

    private void showCancelledState() {
        registeredState.setVisibility(View.GONE);
        cancelledState.setVisibility(View.VISIBLE);
        registerButton.setVisibility(View.GONE);
        absentState.setVisibility(View.GONE);
        attendedState.setVisibility(View.GONE);
        waitlistState.setVisibility(View.GONE);
    }

    private void showAbsentState() {
        registeredState.setVisibility(View.GONE);
        cancelledState.setVisibility(View.GONE);
        registerButton.setVisibility(View.GONE);
        absentState.setVisibility(View.VISIBLE);
        attendedState.setVisibility(View.GONE);
        waitlistState.setVisibility(View.GONE);
    }

    private void showAttendedState() {
        registeredState.setVisibility(View.GONE);
        cancelledState.setVisibility(View.GONE);
        registerButton.setVisibility(View.GONE);
        absentState.setVisibility(View.GONE);
        attendedState.setVisibility(View.VISIBLE);
        waitlistState.setVisibility(View.GONE);
    }

    private void showWaitlistState() {
        registeredState.setVisibility(View.GONE);
        cancelledState.setVisibility(View.GONE);
        registerButton.setVisibility(View.GONE);
        absentState.setVisibility(View.GONE);
        attendedState.setVisibility(View.GONE);
        waitlistState.setVisibility(View.VISIBLE);
    }
}