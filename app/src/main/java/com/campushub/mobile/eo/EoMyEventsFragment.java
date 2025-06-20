package com.campushub.mobile.eo;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.Toast;

import com.campushub.mobile.adapters.EventsAdapter;
import com.campushub.mobile.R;
import com.campushub.mobile.models.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class EoMyEventsFragment extends Fragment {

    private static int lastSelectedButtonId = R.id.all_button_selector;
    private static int lastScrollX = 0;
    private Button selected = null;
    private HorizontalScrollView horizontalScrollView;
    private EventsAdapter eventsAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<Event> eventList;
    private List<Event> fullEventList;
    private String currentCategory = null;

    public EoMyEventsFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_eo_my_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.rv_events);

        horizontalScrollView = view.findViewById(R.id.horizontal_scroll_view);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        eventList = new ArrayList<>();
        fullEventList = new ArrayList<>();
        eventsAdapter = new EventsAdapter(eventList, EoDetailActivity.class);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(eventsAdapter);

        fetchEvents(null);

        SearchView searchView = view.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterEventsByQuery(newText);
                return true;
            }
        });

        Button all = view.findViewById(R.id.all_button_selector);
        Button seminar = view.findViewById(R.id.seminar_button_selector);
        Button webinar = view.findViewById(R.id.webinar_button_selector);
        Button kuliahTamu = view.findViewById(R.id.kuliah_tamu_button_selector);
        Button workshop = view.findViewById(R.id.workshop_button_selector);
        Button sertifikasi = view.findViewById(R.id.sertifikasi_button_selector);

        int originalBgColor = ContextCompat.getColor(requireContext(), R.color.darker_grey);
        int selectedBgColor = ContextCompat.getColor(requireContext(), R.color.primary);
        int selectedTextColor = ContextCompat.getColor(requireContext(), R.color.white);
        int originalTextColor = ContextCompat.getColor(requireContext(), R.color.black);

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

            String category = null;
            int id = clickedButton.getId();
            if (id == R.id.seminar_button_selector) category = "Seminar";
            else if (id == R.id.webinar_button_selector) category = "Webinar";
            else if (id == R.id.kuliah_tamu_button_selector) category = "Kuliah Tamu";
            else if (id == R.id.workshop_button_selector) category = "Workshop";
            else if (id == R.id.sertifikasi_button_selector) category = "Sertifikasi";
            currentCategory = category;
            fetchEvents(category);
        };

        all.setOnClickListener(buttonClickListener);
        seminar.setOnClickListener(buttonClickListener);
        webinar.setOnClickListener(buttonClickListener);
        kuliahTamu.setOnClickListener(buttonClickListener);
        workshop.setOnClickListener(buttonClickListener);
        sertifikasi.setOnClickListener(buttonClickListener);

        Button defaultSelected = view.findViewById(lastSelectedButtonId);
        defaultSelected.setBackgroundColor(selectedBgColor);
        defaultSelected.setTextColor(selectedTextColor);
        selected = defaultSelected;

        horizontalScrollView.post(() -> horizontalScrollView.scrollTo(lastScrollX, 0));
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchEvents(currentCategory);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchEvents(@Nullable String category) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        Query query = db.collection("events").whereEqualTo("creatorId", uid);
        if (category != null) query = query.whereEqualTo("category", category);
        query.get().addOnSuccessListener(snapshot -> {
            fullEventList.clear();
            eventList.clear();
            for (var doc : snapshot.getDocuments()) {
                Event event = doc.toObject(Event.class);
                if (event != null) {
                    fullEventList.add(event);
                    eventList.add(event);
                }
            }
            eventsAdapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> Toast.makeText(requireContext(), "Terjadi kesalahan pada koneksi internet", Toast.LENGTH_SHORT).show());
    }

    @SuppressLint("NotifyDataSetChanged")
    private void filterEventsByQuery(String query) {
        String lower = query.toLowerCase();
        eventList.clear();
        for (Event e : fullEventList) {
            if (e.getTitle().toLowerCase().contains(lower)) {
                eventList.add(e);
            } else if (e.getCategory() != null && e.getCategory().toLowerCase().contains(lower)) {
                eventList.add(e);
            } else if (e.getSpeakerName() != null && e.getSpeakerName().toLowerCase().contains(lower)) {
                eventList.add(e);
            }
        }
        eventsAdapter.notifyDataSetChanged();
    }
}