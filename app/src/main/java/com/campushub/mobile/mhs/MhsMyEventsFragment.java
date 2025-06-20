package com.campushub.mobile.mhs;

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

import com.campushub.mobile.models.Event;
import com.campushub.mobile.adapters.EventsAdapter;
import com.campushub.mobile.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class MhsMyEventsFragment extends Fragment {
    private static int lastSelectedButtonId = R.id.all_button_selector;
    private static int lastScrollX = 0;
    private Button selected = null;
    private HorizontalScrollView horizontalScrollView;
    private EventsAdapter eventsAdapter;
    private List<Event> eventList;
    private List<Event> fullEventList;
    private String currentFilter = null;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public MhsMyEventsFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mhs_my_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        eventList = new ArrayList<>();
        fullEventList = new ArrayList<>();
        RecyclerView recyclerView = view.findViewById(R.id.rv_events);
        eventsAdapter = new EventsAdapter(eventList, MhsDetailActivity.class);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(eventsAdapter);

        horizontalScrollView = view.findViewById(R.id.horizontal_scroll_view);

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
        Button terdaftar = view.findViewById(R.id.terdaftar_button_selector);
        Button dibatalkan = view.findViewById(R.id.dibatalkan_button_selector);
        Button hadir = view.findViewById(R.id.hadir_button_selector);
        Button tidakHadir = view.findViewById(R.id.tidak_hadir_button_selector);
        Button waitingList = view.findViewById(R.id.waiting_list_button_selector);

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

            int id = clickedButton.getId();
            if (id == R.id.all_button_selector) currentFilter = null;
            else if (id == R.id.terdaftar_button_selector) currentFilter = "Terdaftar";
            else if (id == R.id.dibatalkan_button_selector) currentFilter = "Dibatalkan";
            else if (id == R.id.hadir_button_selector) currentFilter = "Hadir";
            else if (id == R.id.tidak_hadir_button_selector) currentFilter = "Tidak Hadir";
            else if (id == R.id.waiting_list_button_selector) currentFilter = "Waiting List";
            else currentFilter = null;
            fetchEvents(currentFilter);
        };

        all.setOnClickListener(buttonClickListener);
        terdaftar.setOnClickListener(buttonClickListener);
        dibatalkan.setOnClickListener(buttonClickListener);
        hadir.setOnClickListener(buttonClickListener);
        tidakHadir.setOnClickListener(buttonClickListener);
        waitingList.setOnClickListener(buttonClickListener);

        Button defaultSelected = view.findViewById(lastSelectedButtonId);
        defaultSelected.setBackgroundColor(selectedBgColor);
        defaultSelected.setTextColor(selectedTextColor);
        selected = defaultSelected;

        horizontalScrollView.post(() -> horizontalScrollView.scrollTo(lastScrollX, 0));

    }

    @Override
    public void onResume() {
        super.onResume();
        fetchEvents(currentFilter);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchEvents(@Nullable String status) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        Query regQuery = db.collection("registrations").whereEqualTo("userId", uid);
        if (status != null) regQuery = regQuery.whereEqualTo("status", status);
        regQuery.get().addOnSuccessListener(regSnap -> {
            List<String> eventIds = new ArrayList<>();
            for (var doc : regSnap.getDocuments()) {
                String eid = doc.getString("eventId");
                if (eid != null) eventIds.add(eid);
            }
            if (eventIds.isEmpty()) {
                fullEventList.clear();
                eventList.clear();
                eventsAdapter.notifyDataSetChanged();
                return;
            }

            db.collection("events").whereIn("id", eventIds).get().addOnSuccessListener(eventSnap -> {
                fullEventList.clear();
                eventList.clear();
                for (var edoc : eventSnap.getDocuments()) {
                    Event event = edoc.toObject(Event.class);
                    if (event != null) {
                        fullEventList.add(event);
                        eventList.add(event);
                    }
                }
                eventsAdapter.notifyDataSetChanged();
            }).addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed loading events: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }).addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed loading registrations: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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