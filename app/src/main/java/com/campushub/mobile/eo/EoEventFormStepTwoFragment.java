package com.campushub.mobile.eo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.campushub.mobile.R;
import com.bumptech.glide.Glide;

import java.util.HashMap;
import java.util.Map;

public class EoEventFormStepTwoFragment extends Fragment {
    private EditText speakerNameEditText;
    private EditText speakerTitleEditText;
    private EditText ticketCountEditText;
    private Spinner eventTypeSpinner;
    private TextView locationLabel;
    private EditText locationEditText;
    private View imageUploadLayout;
    private ImageView speakerImageView;
    private ImageView uploadIconView;
    private TextView uploadImageText;

    private Uri speakerImageUri;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    public EoEventFormStepTwoFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                speakerImageUri = result.getData().getData();
                if (speakerImageUri != null) {
                    displaySelectedImage();
                } else {
                    Toast.makeText(getContext(), "Gagal mendapatkan gambar", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Terjadi kesalahan saat memilih gambar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_eo_event_form_step_two, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        speakerNameEditText = view.findViewById(R.id.namaPembicara);
        speakerTitleEditText = view.findViewById(R.id.editTextTitle);
        ticketCountEditText = view.findViewById(R.id.editTextTitle2);
        eventTypeSpinner = view.findViewById(R.id.editTextTitle3);
        locationEditText = view.findViewById(R.id.editTextLocation);
        locationLabel = view.findViewById(R.id.textViewLocation);
        imageUploadLayout = view.findViewById(R.id.constraintLayout2);
        uploadImageText = view.findViewById(R.id.textView6);
        speakerImageView = view.findViewById(R.id.speaker_image);
        uploadIconView = view.findViewById(R.id.imageView8);
        ImageView backButton = view.findViewById(R.id.back_button);

        backButton.setOnClickListener(v -> requireActivity().onBackPressed());
        locationEditText.setMovementMethod(new ScrollingMovementMethod());

        setupEventTypeSpinner();
        setupImageUpload();

        restoreSavedData();
    }

    private void restoreSavedData() {
        Bundle args = getArguments();
        if (args != null) {
            String speakerName = args.getString("speakerName");
            if (speakerName != null) {
                speakerNameEditText.setText(speakerName);
            }

            String speakerTitle = args.getString("speakerTitle");
            if (speakerTitle != null) {
                speakerTitleEditText.setText(speakerTitle);
            }

            if (args.containsKey("ticketCount")) {
                ticketCountEditText.setText(String.valueOf(args.getInt("ticketCount")));
            }

            String eventType = args.getString("eventType");
            if (eventType != null) {
                for (int i = 0; i < eventTypeSpinner.getAdapter().getCount(); i++) {
                    if (eventTypeSpinner.getAdapter().getItem(i).toString().equals(eventType)) {
                        eventTypeSpinner.setSelection(i);
                        break;
                    }
                }
            }

            String location = args.getString("location");
            if (location != null) {
                locationEditText.setText(location);
            }

            String imageUriStr = args.getString("speakerImageUri");
            if (imageUriStr != null) {
                speakerImageUri = Uri.parse(imageUriStr);
                displaySelectedImage();
            }
        }
    }

    private void setupEventTypeSpinner() {
        String[] categories = getResources().getStringArray(R.array.jenis);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.item_selected, categories);
        adapter.setDropDownViewResource(R.layout.item_dropdown);
        eventTypeSpinner.setAdapter(adapter);
        eventTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                String type = parent.getItemAtPosition(pos).toString();
                if ("Online".equalsIgnoreCase(type)) {
                    locationLabel.setVisibility(View.GONE);
                    locationEditText.setVisibility(View.GONE);
                } else {
                    locationLabel.setVisibility(View.VISIBLE);
                    locationEditText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        int defaultPos = adapter.getPosition("Online");
        if (defaultPos >= 0) eventTypeSpinner.setSelection(defaultPos);
    }

    private void setupImageUpload() {
        imageUploadLayout.setOnClickListener(v -> openImagePicker());

        speakerImageView.setOnClickListener(v -> openImagePicker());
    }

    @SuppressLint("IntentReset")
    private void openImagePicker() {
        try {
            @SuppressLint("IntentReset") Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Tidak dapat membuka galeri: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void displaySelectedImage() {
        if (speakerImageUri != null) {
            speakerImageView.setVisibility(View.VISIBLE);
            uploadIconView.setVisibility(View.GONE);
            uploadImageText.setVisibility(View.GONE);

            Glide.with(this).load(speakerImageUri).centerCrop().placeholder(R.drawable.border_upload_image).error(R.drawable.border_upload_image).into(speakerImageView);
        }
    }

    public boolean validateData() {
        boolean isValid = true;

        if (speakerImageUri == null) {
            Toast.makeText(getContext(), "Harap pilih foto pembicara", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (TextUtils.isEmpty(speakerNameEditText.getText())) {
            speakerNameEditText.setError("Nama pembicara tidak boleh kosong");
            isValid = false;
        }

        if (TextUtils.isEmpty(speakerTitleEditText.getText())) {
            speakerTitleEditText.setError("Jabatan tidak boleh kosong");
            isValid = false;
        }

        if (TextUtils.isEmpty(ticketCountEditText.getText())) {
            ticketCountEditText.setError("Jumlah tiket tidak boleh kosong");
            isValid = false;
        }

        String type = eventTypeSpinner.getSelectedItem().toString();
        if (!"Online".equalsIgnoreCase(type) && TextUtils.isEmpty(locationEditText.getText())) {
            locationEditText.setError("Lokasi tidak boleh kosong");
            isValid = false;
        }

        return isValid;
    }

    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();

        data.put("speakerName", speakerNameEditText.getText().toString().trim());
        data.put("speakerTitle", speakerTitleEditText.getText().toString().trim());

        try {
            String ticketCountStr = ticketCountEditText.getText().toString().trim();
            if (!TextUtils.isEmpty(ticketCountStr)) {
                data.put("ticketCount", Integer.parseInt(ticketCountStr));
            }
        } catch (NumberFormatException e) {
            data.put("ticketCount", 0);
        }

        String type = eventTypeSpinner.getSelectedItem().toString();
        data.put("eventType", type);
        if ("Online".equalsIgnoreCase(type)) {
            data.put("location", "Online");
        } else {
            data.put("location", locationEditText.getText().toString().trim());
        }

        return data;
    }

    public Uri getSpeakerImageUri() {
        return speakerImageUri;
    }

    public String getSpeakerName() {
        return speakerNameEditText != null ? speakerNameEditText.getText().toString() : "";
    }

    public String getSpeakerTitle() {
        return speakerTitleEditText != null ? speakerTitleEditText.getText().toString() : "";
    }

    public String getTicketCount() {
        return ticketCountEditText != null ? ticketCountEditText.getText().toString() : "";
    }

}