package com.campushub.mobile.eo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campushub.mobile.adapters.ParticipantsAdapter;
import com.campushub.mobile.R;
import com.campushub.mobile.models.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class EoParticipantsActivity extends AppCompatActivity {

    private static int lastSelectedButtonId = R.id.all_button_selector;
    private static int lastScrollX = 0;
    private Button selected = null;
    private HorizontalScrollView horizontalScrollView;
    private String currentEvent;
    private FirebaseFirestore db;
    private List<User> participantList;
    private ParticipantsAdapter participantsAdapter;
    private String statusFilter;
    private List<User> fullParticipantList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_eo_participants);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        currentEvent = getIntent().getStringExtra("eventId");

        horizontalScrollView = findViewById(R.id.horizontal_scroll_view);

        Button all = findViewById(R.id.all_button_selector);
        Button hadir = findViewById(R.id.hadir_button_selector);
        Button tidakHadir = findViewById(R.id.tidak_hadir_button_selector);
        Button terdaftar = findViewById(R.id.terdaftar_button_selector);
        Button dibatalkan = findViewById(R.id.dibatalkan_button_selector);
        Button waitingList = findViewById(R.id.waiting_list_button_selector);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        int originalBgColor = ContextCompat.getColor(this, R.color.darker_grey);
        int selectedBgColor = ContextCompat.getColor(this, R.color.primary);
        int selectedTextColor = ContextCompat.getColor(this, R.color.white);
        int originalTextColor = ContextCompat.getColor(this, R.color.black);

        db = FirebaseFirestore.getInstance();
        statusFilter = null;

        setupRecyclerView();
        fetchParticipants(null);

        SearchView searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterParticipantsByQuery(newText);
                return true;
            }
        });
        View.OnClickListener buttonClickListener = v -> {
            if (selected != null) {
                selected.setBackgroundColor(originalBgColor);
                selected.setTextColor(originalTextColor);
            }

            Button clickedButton = (Button) v;
            clickedButton.setBackgroundColor(selectedBgColor);
            clickedButton.setTextColor(selectedTextColor);

            selected = clickedButton;
            lastSelectedButtonId = clickedButton.getId();
            lastScrollX = horizontalScrollView.getScrollX();

            int id = v.getId();
            if (id == R.id.all_button_selector) {
                statusFilter = null;
            } else if (id == R.id.hadir_button_selector) {
                statusFilter = "Hadir";
            } else if (id == R.id.tidak_hadir_button_selector) {
                statusFilter = "Tidak Hadir";
            } else if (id == R.id.terdaftar_button_selector) {
                statusFilter = "Terdaftar";
            } else if (id == R.id.dibatalkan_button_selector) {
                statusFilter = "Dibatalkan";
            } else if (id == R.id.waiting_list_button_selector) {
                statusFilter = "Waiting List";
            }
            searchView.setQuery("", false);

            fetchParticipants(statusFilter);
        };

        all.setOnClickListener(buttonClickListener);
        hadir.setOnClickListener(buttonClickListener);
        tidakHadir.setOnClickListener(buttonClickListener);
        terdaftar.setOnClickListener(buttonClickListener);
        dibatalkan.setOnClickListener(buttonClickListener);
        waitingList.setOnClickListener(buttonClickListener);

        Button defaultSelected = findViewById(lastSelectedButtonId);
        defaultSelected.setBackgroundColor(selectedBgColor);
        defaultSelected.setTextColor(selectedTextColor);
        selected = defaultSelected;

        horizontalScrollView.post(() -> horizontalScrollView.scrollTo(lastScrollX, 0));

        Button attendance = findViewById(R.id.start_attendance);

        attendance.setOnClickListener(v -> {
            Intent intent = new Intent(this, EoAttendanceActivity.class);
            intent.putExtra("eventId", currentEvent);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.rv_events);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        participantList = new ArrayList<>();
        fullParticipantList = new ArrayList<>();
        participantsAdapter = new ParticipantsAdapter(participantList);
        recyclerView.setAdapter(participantsAdapter);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchParticipants(String status) {
        Query query = db.collection("registrations").whereEqualTo("eventId", currentEvent);
        if (status != null) {
            query = query.whereEqualTo("status", status);
        }

        query.get().addOnSuccessListener(snapshot -> {
            fullParticipantList.clear();
            participantList.clear();
            if (snapshot.isEmpty()) {
                participantsAdapter.notifyDataSetChanged();
                return;
            }

            List<DocumentReference> refs = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                String userId = doc.getString("userId");
                if (userId != null) {
                    refs.add(db.collection("users").document(userId));
                }
            }
            if (refs.isEmpty()) {
                participantsAdapter.notifyDataSetChanged();
                return;
            }

            List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
            for (DocumentReference ref : refs) {
                tasks.add(ref.get());
            }
            Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                for (Object obj : results) {
                    DocumentSnapshot userDoc = (DocumentSnapshot) obj;
                    User user = userDoc.toObject(User.class);
                    if (user != null) {
                        fullParticipantList.add(user);
                        participantList.add(user);
                    }
                }
                participantsAdapter.notifyDataSetChanged();
            });

        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to fetch participants", Toast.LENGTH_SHORT).show());
    }

    @SuppressLint("NotifyDataSetChanged")
    private void filterParticipantsByQuery(String query) {
        String lower = query.toLowerCase();
        participantList.clear();

        if (query.isEmpty()) {
            participantList.addAll(fullParticipantList);
        } else {
            for (User u : fullParticipantList) {
                if (u.getNamaLengkap() != null && u.getNamaLengkap().toLowerCase().contains(lower)) {
                    participantList.add(u);
                } else if (u.getAlamatEmail() != null && u.getAlamatEmail().toLowerCase().contains(lower)) {
                    participantList.add(u);
                } else if (u.getNomorTelepon() != null && u.getNomorTelepon().toLowerCase().contains(lower)) {
                    participantList.add(u);
                }
            }
        }

        participantsAdapter.notifyDataSetChanged();
    }
}