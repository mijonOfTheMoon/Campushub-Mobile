package com.campushub.mobile.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.campushub.mobile.R;
import com.campushub.mobile.models.User;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class ParticipantsAdapter extends RecyclerView.Adapter<ParticipantsAdapter.ParticipantViewHolder> {
    private final List<User> participantList;

    public ParticipantsAdapter(List<User> participantList) {
        this.participantList = participantList;
    }

    @NonNull
    @Override
    public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_participants, parent, false);
        return new ParticipantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
        User participant = participantList.get(position);
        holder.tvName.setText(participant.getNamaLengkap());
        holder.tvEmail.setText(participant.getAlamatEmail());
        String phoneNumber = participant.getNomorTelepon();
        holder.tvPhone.setText(phoneNumber != null && !phoneNumber.isBlank() ? phoneNumber : "-");

        if (participant.getPhotoUrl() != null && !participant.getPhotoUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext()).load(participant.getPhotoUrl()).transform(new CircleCrop()).into(holder.ivImage);
        }
    }

    @Override
    public int getItemCount() {
        return participantList.size();
    }

    static class ParticipantViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivImage;
        TextView tvName;
        TextView tvEmail;
        TextView tvPhone;

        public ParticipantViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_event_image);
            tvName = itemView.findViewById(R.id.tv_event_title);
            tvEmail = itemView.findViewById(R.id.tv_event_date);
            tvPhone = itemView.findViewById(R.id.tv_event_speaker);
        }
    }
}
