package com.campushub.mobile.eo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.campushub.mobile.R;

import android.app.Activity;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.ArrayList;

import com.campushub.mobile.models.Event;
import com.google.android.material.imageview.ShapeableImageView;

public class EoDetailActivity extends AppCompatActivity {
    private static final int EDIT_EVENT_REQUEST = 100;
    private TextView tvTitle, tvSubtitle, tvDate, tvTime, tvLocation, tvTickets, tvDescription, profileName, profileRole;
    private ImageView posterImage;
    private ShapeableImageView profileImage;
    private Event currentEvent;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_eo_detail);
        db = FirebaseFirestore.getInstance();
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

        Button presensi = findViewById(R.id.presensi_button);
        Button editButton = findViewById(R.id.prev_button);
        Button deleteButton = findViewById(R.id.delete_event_button);

        deleteButton.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Konfirmasi").setMessage("Apakah anda yakin ingin menghapus event ini? Semua pendaftaran untuk event ini juga akan dihapus.").setPositiveButton("Ya", (dialog, which) -> {
            if (currentEvent != null) {
                String eventId = currentEvent.getId();
                db.collection("registrations").whereEqualTo("eventId", eventId).get().addOnSuccessListener((QuerySnapshot regSnap) -> {
                    List<Task<Void>> deleteTasks = new ArrayList<>();
                    for (DocumentSnapshot regDoc : regSnap.getDocuments()) {
                        deleteTasks.add(regDoc.getReference().delete());
                    }
                    Tasks.whenAll(deleteTasks).addOnSuccessListener(aVoid -> {

                        List<Task<Void>> finalTasks = new ArrayList<>();
                        if (currentEvent.getImageUrl() != null) {
                            StorageReference posterRef = FirebaseStorage.getInstance().getReferenceFromUrl(currentEvent.getImageUrl());
                            finalTasks.add(posterRef.delete());
                        }
                        if (currentEvent.getSpeakerImageUrl() != null) {
                            StorageReference spkRef = FirebaseStorage.getInstance().getReferenceFromUrl(currentEvent.getSpeakerImageUrl());
                            finalTasks.add(spkRef.delete());
                        }
                        finalTasks.add(db.collection("events").document(eventId).delete());
                        Tasks.whenAll(finalTasks).addOnSuccessListener(aVoid2 -> {
                            Toast.makeText(this, "Event, images and registrations deleted", Toast.LENGTH_SHORT).show();
                            setResult(Activity.RESULT_OK);
                            finish();
                        }).addOnFailureListener(e -> {
                            Toast.makeText(this, "Deletion completed with some errors", Toast.LENGTH_SHORT).show();
                            setResult(Activity.RESULT_OK);
                            finish();
                        });
                    }).addOnFailureListener(e -> Toast.makeText(this, "Failed to delete registrations", Toast.LENGTH_SHORT).show());
                }).addOnFailureListener(e -> Toast.makeText(this, "Failed to query registrations", Toast.LENGTH_SHORT).show());
            }
        }).setNegativeButton("Tidak", null).show());

        backButton.setOnClickListener(v -> finish());

        currentEvent = (Event) getIntent().getSerializableExtra("event");
        populateFields(currentEvent);

        presensi.setOnClickListener(v -> {
            Intent intent = new Intent(this, EoParticipantsActivity.class);
            intent.putExtra("eventId", currentEvent.getId());
            startActivity(intent);
        });

        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, EoEventFormActivity.class);
            intent.putExtra("event", currentEvent);
            startActivityForResult(intent, EDIT_EVENT_REQUEST);
        });
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_EVENT_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Event updated = (Event) data.getSerializableExtra("event");
            if (updated != null) {
                currentEvent = updated;
                populateFields(updated);
            }
        }
    }
}