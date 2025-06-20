package com.campushub.mobile.eo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.campushub.mobile.R;
import com.campushub.mobile.models.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class EoEventFormActivity extends AppCompatActivity {
    private final Map<String, Object> eventData = new HashMap<>();
    private boolean isEdit = false;
    private Event editingEvent;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private FirebaseAuth auth;
    private Uri eventImageUri;
    private Uri speakerImageUri;
    private View loadingOverlay;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("event")) {
            editingEvent = (Event) intent.getSerializableExtra("event");
            if (editingEvent != null) {
                isEdit = true;
                eventData.put("id", editingEvent.getId());
                eventData.put("category", editingEvent.getCategory());
                eventData.put("title", editingEvent.getTitle());
                eventData.put("eventDate", editingEvent.getEventDate());
                eventData.put("startTime", editingEvent.getStartTime());
                eventData.put("endTime", editingEvent.getEndTime());
                eventData.put("description", editingEvent.getDescription());
                eventData.put("location", editingEvent.getLocation());
                eventData.put("eventType", editingEvent.getEventType());
                eventData.put("ticketCount", editingEvent.getTicketCount());
                eventData.put("speakerName", editingEvent.getSpeakerName());
                eventData.put("speakerTitle", editingEvent.getSpeakerTitle());
                eventData.put("imageUrl", editingEvent.getImageUrl());
                eventData.put("speakerImageUrl", editingEvent.getSpeakerImageUrl());
                if (editingEvent.getImageUrl() != null)
                    eventImageUri = Uri.parse(editingEvent.getImageUrl());
                if (editingEvent.getSpeakerImageUrl() != null)
                    speakerImageUri = Uri.parse(editingEvent.getSpeakerImageUrl());
            }
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_eo_event_form);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        auth = FirebaseAuth.getInstance();
        AtomicInteger state = new AtomicInteger(1);
        AtomicReference<Fragment> selected = new AtomicReference<>();
        Button next = findViewById(R.id.next_button);
        Button prev = findViewById(R.id.prev_button);

        next.setOnClickListener(v -> {
            switch (state.get()) {
                case 1:
                    EoEventFormStepOneFragment fragmentOne = (EoEventFormStepOneFragment) getSupportFragmentManager().findFragmentById(R.id.create_event_container);

                    if (fragmentOne != null && fragmentOne.validateData()) {
                        Map<String, Object> stepOneData = fragmentOne.getEventData();
                        eventData.putAll(stepOneData);
                        eventImageUri = fragmentOne.getEventImageUri();

                        EoEventFormStepTwoFragment fragmentTwo = new EoEventFormStepTwoFragment();

                        if (eventData.containsKey("speakerName") || eventData.containsKey("speakerTitle") || eventData.containsKey("ticketCount") || eventData.containsKey("eventType") || eventData.containsKey("location") || speakerImageUri != null) {

                            Bundle bundle = new Bundle();
                            bundle.putString("speakerName", (String) eventData.get("speakerName"));
                            bundle.putString("speakerTitle", (String) eventData.get("speakerTitle"));

                            if (eventData.get("ticketCount") != null) {
                                if (eventData.get("ticketCount") instanceof Integer) {
                                    bundle.putInt("ticketCount", (Integer) eventData.get("ticketCount"));
                                } else if (eventData.get("ticketCount") instanceof String) {
                                    try {
                                        bundle.putInt("ticketCount", Integer.parseInt((String) eventData.get("ticketCount")));
                                    } catch (NumberFormatException ignored) {
                                    }
                                }
                            }

                            bundle.putString("eventType", (String) eventData.get("eventType"));
                            bundle.putString("location", (String) eventData.get("location"));

                            if (speakerImageUri != null) {
                                bundle.putString("speakerImageUri", speakerImageUri.toString());
                            }

                            fragmentTwo.setArguments(bundle);
                        }

                        selected.set(fragmentTwo);
                        state.set(2);
                        next.setText("Simpan");
                        prev.setText("Kembali");
                    } else {
                        Toast.makeText(this, "Harap lengkapi semua data", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    break;
                case 2:
                    EoEventFormStepTwoFragment fragmentTwo = (EoEventFormStepTwoFragment) getSupportFragmentManager().findFragmentById(R.id.create_event_container);

                    if (fragmentTwo != null && fragmentTwo.validateData()) {
                        Map<String, Object> stepTwoData = fragmentTwo.getEventData();
                        eventData.putAll(stepTwoData);
                        speakerImageUri = fragmentTwo.getSpeakerImageUri();

                        saveEventData();
                    } else {
                        Toast.makeText(this, "Harap lengkapi semua data", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }

            if (state.get() <= 2) {
                getSupportFragmentManager().beginTransaction().replace(R.id.create_event_container, selected.get()).commit();
            }
        });
        prev.setOnClickListener(v -> {
            switch (state.get()) {
                case 2:
                    EoEventFormStepTwoFragment currentFragmentTwo = (EoEventFormStepTwoFragment) getSupportFragmentManager().findFragmentById(R.id.create_event_container);
                    if (currentFragmentTwo != null) {
                        if (!TextUtils.isEmpty(currentFragmentTwo.getSpeakerName()) || !TextUtils.isEmpty(currentFragmentTwo.getSpeakerTitle()) || !TextUtils.isEmpty(currentFragmentTwo.getTicketCount()) || currentFragmentTwo.getSpeakerImageUri() != null) {

                            Map<String, Object> stepTwoData = currentFragmentTwo.getEventData();
                            eventData.putAll(stepTwoData);
                            speakerImageUri = currentFragmentTwo.getSpeakerImageUri();
                        }
                    }

                    EoEventFormStepOneFragment fragmentOne = new EoEventFormStepOneFragment();

                    Bundle bundle = new Bundle();
                    bundle.putString("category", (String) eventData.get("category"));
                    bundle.putString("title", (String) eventData.get("title"));
                    bundle.putString("eventDate", (String) eventData.get("eventDate"));
                    bundle.putString("startTime", (String) eventData.get("startTime"));
                    bundle.putString("endTime", (String) eventData.get("endTime"));
                    bundle.putString("description", (String) eventData.get("description"));
                    if (eventImageUri != null) {
                        bundle.putString("eventImageUri", eventImageUri.toString());
                    }
                    fragmentOne.setArguments(bundle);

                    selected.set(fragmentOne);
                    state.set(1);
                    next.setText("Selanjutnya");
                    prev.setText("Batal");
                    break;
                case 1:
                    finish();
                    return;
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.create_event_container, selected.get()).commit();
        });

        if (savedInstanceState == null) {
            EoEventFormStepOneFragment fragmentOne = new EoEventFormStepOneFragment();
            if (isEdit && editingEvent != null) {
                Bundle bundle = new Bundle();
                bundle.putString("category", editingEvent.getCategory());
                bundle.putString("title", editingEvent.getTitle());
                bundle.putString("eventDate", editingEvent.getEventDate());
                bundle.putString("startTime", editingEvent.getStartTime());
                bundle.putString("endTime", editingEvent.getEndTime());
                bundle.putString("description", editingEvent.getDescription());
                if (eventImageUri != null)
                    bundle.putString("eventImageUri", eventImageUri.toString());
                fragmentOne.setArguments(bundle);
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.create_event_container, fragmentOne).commit();
        }
    }

    private void saveEventData() {
        eventData.put("creatorId", auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "unknown");
        String eventId;
        if (isEdit && editingEvent != null) {
            eventId = editingEvent.getId();
        } else {
            eventData.put("createdAt", System.currentTimeMillis());
            eventId = UUID.randomUUID().toString();
            eventData.put("id", eventId);
        }
        uploadImages(eventId);
    }

    private void uploadImages(String eventId) {
        loadingOverlay.setVisibility(View.VISIBLE);
        boolean uploadEventImage = eventImageUri != null && !"http".equalsIgnoreCase(eventImageUri.getScheme()) && !"https".equalsIgnoreCase(eventImageUri.getScheme());
        boolean uploadSpeakerImage = speakerImageUri != null && !"http".equalsIgnoreCase(speakerImageUri.getScheme()) && !"https".equalsIgnoreCase(speakerImageUri.getScheme());
        final int TOTAL_UPLOADS = (uploadEventImage ? 1 : 0) + (uploadSpeakerImage ? 1 : 0);
        final AtomicInteger uploadsDone = new AtomicInteger(0);

        if (TOTAL_UPLOADS == 0) {
            saveEventToFirestore(eventId);
            return;
        }

        if (uploadEventImage) {
            StorageReference eventImageRef = storageRef.child("events/" + eventId + "/event_image.jpg");
            UploadTask uploadTask = eventImageRef.putFile(eventImageUri);

            uploadTask.addOnSuccessListener(taskSnapshot -> eventImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                eventData.put("imageUrl", uri.toString());
                if (uploadsDone.incrementAndGet() == TOTAL_UPLOADS) {
                    saveEventToFirestore(eventId);
                }
            })).addOnFailureListener(e -> {
                loadingOverlay.setVisibility(View.GONE);
                Toast.makeText(this, "Gagal mengunggah gambar event", Toast.LENGTH_SHORT).show();
            });
        }

        if (uploadSpeakerImage) {
            StorageReference speakerImageRef = storageRef.child("events/" + eventId + "/speaker_image.jpg");
            UploadTask uploadTask = speakerImageRef.putFile(speakerImageUri);

            uploadTask.addOnSuccessListener(taskSnapshot -> speakerImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                eventData.put("speakerImageUrl", uri.toString());
                if (uploadsDone.incrementAndGet() == TOTAL_UPLOADS) {
                    saveEventToFirestore(eventId);
                }
            })).addOnFailureListener(e -> {
                loadingOverlay.setVisibility(View.GONE);
                Toast.makeText(this, "Gagal mengunggah gambar pembicara", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void saveEventToFirestore(String eventId) {
        db.collection("events").document(eventId).set(eventData).addOnSuccessListener(aVoid -> {
            if (isEdit && editingEvent != null) {
                updateRegistrationStatuses(eventId);
            } else {
                loadingOverlay.setVisibility(View.GONE);
                Toast.makeText(this, "Event berhasil disimpan", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(e -> {
            loadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "Gagal menyimpan event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void updateRegistrationStatuses(String eventId) {
        int oldCount = editingEvent.getTicketCount();
        int newCount = (int) eventData.get("ticketCount");
        int delta = newCount - oldCount;

        if (delta == 0) {
            loadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "Event berhasil diperbarui", Toast.LENGTH_SHORT).show();
            finishEditResult();
            return;
        }

        db.collection("registrations").whereEqualTo("eventId", eventId).get().addOnSuccessListener(snapshot -> {
            if (snapshot.isEmpty()) {
                loadingOverlay.setVisibility(View.GONE);
                Toast.makeText(this, "Event berhasil diperbarui", Toast.LENGTH_SHORT).show();
                finishEditResult();
                return;
            }
            processRegistrationStatusUpdates(snapshot, delta);
        }).addOnFailureListener(e -> {
            loadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "Gagal memperbarui status pendaftaran: ", Toast.LENGTH_SHORT).show();
            finishEditResult();
        });
    }

    private void processRegistrationStatusUpdates(QuerySnapshot snapshot, int delta) {
        WriteBatch batch = db.batch();
        Map<String, List<DocumentSnapshot>> registrationsByStatus = new HashMap<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            String status = doc.getString("status");
            if (status != null) {
                if (!registrationsByStatus.containsKey(status)) {
                    registrationsByStatus.put(status, new ArrayList<>());
                }
                registrationsByStatus.get(status).add(doc);
            }
        }

        for (String status : registrationsByStatus.keySet()) {
            List<DocumentSnapshot> docs = registrationsByStatus.get(status);
            if (docs != null) {
                docs.sort((a, b) -> {
                    Long timeA = a.getLong("createdAt");
                    Long timeB = b.getLong("createdAt");
                    if (timeA == null) timeA = 0L;
                    if (timeB == null) timeB = 0L;
                    return timeA.compareTo(timeB);
                });
            }
        }

        if (delta > 0) {
            List<DocumentSnapshot> waitingList = registrationsByStatus.getOrDefault("Waiting List", new ArrayList<>());
            int count = Math.min(delta, waitingList.size());
            for (int i = 0; i < count; i++) {
                DocumentSnapshot doc = waitingList.get(i);
                batch.update(doc.getReference(), "status", "Terdaftar");
            }
        } else if (delta < 0) {
            List<DocumentSnapshot> registered = registrationsByStatus.getOrDefault("Terdaftar", new ArrayList<>());
            int removeCount = Math.min(-delta, registered.size());
            Collections.reverse(registered);
            for (int i = 0; i < removeCount; i++) {
                DocumentSnapshot doc = registered.get(i);
                batch.update(doc.getReference(), "status", "Waiting List");
            }
        }

        batch.commit().addOnSuccessListener(aVoid -> {
            loadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "Event dan status pendaftaran berhasil diperbarui", Toast.LENGTH_SHORT).show();
            finishEditResult();
        }).addOnFailureListener(e -> {
            loadingOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "Gagal memperbarui status pendaftaran: ", Toast.LENGTH_SHORT).show();
            finishEditResult();
        });
    }

    private void finishEditResult() {
        Event updated = new Event();
        updated.setId((String) eventData.get("id"));
        updated.setCategory((String) eventData.get("category"));
        updated.setTitle((String) eventData.get("title"));
        updated.setEventDate((String) eventData.get("eventDate"));
        updated.setStartTime((String) eventData.get("startTime"));
        updated.setEndTime((String) eventData.get("endTime"));
        updated.setDescription((String) eventData.get("description"));
        updated.setLocation((String) eventData.get("location"));
        updated.setEventType((String) eventData.get("eventType"));
        updated.setTicketCount((int) eventData.get("ticketCount"));
        updated.setSpeakerName((String) eventData.get("speakerName"));
        updated.setSpeakerTitle((String) eventData.get("speakerTitle"));
        updated.setImageUrl((String) eventData.get("imageUrl"));
        updated.setSpeakerImageUrl((String) eventData.get("speakerImageUrl"));
        Intent result = new Intent();
        result.putExtra("event", updated);
        setResult(RESULT_OK, result);
        finish();
    }
}