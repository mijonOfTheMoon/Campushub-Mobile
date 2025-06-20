package com.campushub.mobile.adapters;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.campushub.mobile.R;
import com.campushub.mobile.models.Event;

import java.util.List;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    private final List<Event> eventList;
    private final Class<? extends Activity> detailActivityClass;

    public EventsAdapter(List<Event> eventList, Class<? extends Activity> detailActivityClass) {
        this.eventList = eventList;
        this.detailActivityClass = detailActivityClass;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_events, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.tvEventTitle.setText(event.getTitle());
        holder.tvEventSubtitle.setText(event.getCategory());
        holder.tvEventDate.setText(event.getEventDate());
        holder.tvEventSpeaker.setText(event.getSpeakerName());

        String imageUrl = event.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext()).load(imageUrl).into(holder.ivEventImage);
        }

        holder.btnDetail.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), detailActivityClass);
            intent.putExtra("event", event);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        ImageView ivEventImage;
        TextView tvEventTitle;
        TextView tvEventSubtitle;
        TextView tvEventDate;
        TextView tvEventSpeaker;
        View btnDetail;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEventImage = itemView.findViewById(R.id.iv_event_image);
            tvEventTitle = itemView.findViewById(R.id.tv_event_title);
            tvEventSubtitle = itemView.findViewById(R.id.tv_event_subtitle);
            tvEventDate = itemView.findViewById(R.id.tv_event_date);
            tvEventSpeaker = itemView.findViewById(R.id.tv_event_speaker);
            btnDetail = itemView.findViewById(R.id.btn_detail);
        }
    }
}
