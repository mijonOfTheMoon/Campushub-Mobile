package com.campushub.mobile.eo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class EoEventFormStepOneFragment extends Fragment {
    private final Calendar selectedDate = Calendar.getInstance();
    private final Calendar startTime = Calendar.getInstance();
    private final Calendar endTime = Calendar.getInstance();
    private Spinner categorySpinner;
    private EditText titleEditText;
    private EditText dateEditText;
    private EditText startTimeEditText;
    private EditText endTimeEditText;
    private EditText descriptionEditText;
    private View imageUploadLayout;
    private ImageView posterImageView;
    private ImageView uploadIconView;
    private TextView uploadImageText;
    private Uri eventImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    public EoEventFormStepOneFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                eventImageUri = result.getData().getData();
                if (eventImageUri != null) {
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
        return inflater.inflate(R.layout.fragment_eo_event_form_step_one, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        categorySpinner = view.findViewById(R.id.namaPembicara);
        titleEditText = view.findViewById(R.id.editTextTitle);
        dateEditText = view.findViewById(R.id.editTextTitle2);
        startTimeEditText = view.findViewById(R.id.editTextTitle3);
        endTimeEditText = view.findViewById(R.id.editTextEndTime);
        descriptionEditText = view.findViewById(R.id.editTextLocation);
        imageUploadLayout = view.findViewById(R.id.image_wrapper);
        uploadImageText = view.findViewById(R.id.textView6);
        posterImageView = view.findViewById(R.id.poster_image);
        uploadIconView = view.findViewById(R.id.imageView8);
        ImageView backButton = view.findViewById(R.id.back_button);

        backButton.setOnClickListener(v -> requireActivity().onBackPressed());
        descriptionEditText.setMovementMethod(new ScrollingMovementMethod());

        setupCategorySpinner();
        setupDateTimePickers();
        setupImageUpload();
        restoreSavedData();
    }

    private void restoreSavedData() {
        Bundle args = getArguments();
        if (args != null) {
            String category = args.getString("category");
            if (category != null) {
                for (int i = 0; i < categorySpinner.getAdapter().getCount(); i++) {
                    if (categorySpinner.getAdapter().getItem(i).toString().equals(category)) {
                        categorySpinner.setSelection(i);
                        break;
                    }
                }
            }

            String title = args.getString("title");
            if (title != null) {
                titleEditText.setText(title);
            }

            String eventDate = args.getString("eventDate");
            if (eventDate != null) {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    selectedDate.setTime(Objects.requireNonNull(dateFormat.parse(eventDate)));
                    updateDateDisplay();
                } catch (Exception ignored) {
                }
            }

            String startTimeStr = args.getString("startTime");
            if (startTimeStr != null) {
                try {
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    startTime.setTime(Objects.requireNonNull(timeFormat.parse(startTimeStr)));
                    updateStartTimeDisplay();
                } catch (Exception ignored) {
                }
            }

            String endTimeStr = args.getString("endTime");
            if (endTimeStr != null) {
                try {
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    endTime.setTime(Objects.requireNonNull(timeFormat.parse(endTimeStr)));
                    updateEndTimeDisplay();
                } catch (Exception ignored) {
                }
            }

            String description = args.getString("description");
            if (description != null) {
                descriptionEditText.setText(description);
            }

            String imageUriStr = args.getString("eventImageUri");
            if (imageUriStr != null) {
                eventImageUri = Uri.parse(imageUriStr);
                displaySelectedImage();
            }
        }
    }

    private void setupCategorySpinner() {
        String[] categories = getResources().getStringArray(R.array.kategori);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.item_selected, categories);
        adapter.setDropDownViewResource(R.layout.item_dropdown);
        categorySpinner.setAdapter(adapter);
    }

    private void setupDateTimePickers() {
        dateEditText.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                selectedDate.set(Calendar.YEAR, year);
                selectedDate.set(Calendar.MONTH, month);
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateDisplay();
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        startTimeEditText.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
                startTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                startTime.set(Calendar.MINUTE, minute);
                updateStartTimeDisplay();
            }, startTime.get(Calendar.HOUR_OF_DAY), startTime.get(Calendar.MINUTE), true);
            timePickerDialog.show();
        });

        endTimeEditText.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
                endTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                endTime.set(Calendar.MINUTE, minute);
                updateEndTimeDisplay();
            }, endTime.get(Calendar.HOUR_OF_DAY), endTime.get(Calendar.MINUTE), true);
            timePickerDialog.show();
        });
    }

    private void setupImageUpload() {
        imageUploadLayout.setOnClickListener(v -> openImagePicker());

        posterImageView.setOnClickListener(v -> openImagePicker());
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
        if (eventImageUri != null) {
            posterImageView.setVisibility(View.VISIBLE);
            uploadImageText.setVisibility(View.GONE);
            uploadIconView.setVisibility(View.GONE);

            Glide.with(this).load(eventImageUri).centerCrop().placeholder(R.drawable.border_upload_image).error(R.drawable.border_upload_image).into(posterImageView);
        }
    }

    private void updateDateDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        dateEditText.setText(dateFormat.format(selectedDate.getTime()));
    }

    private void updateStartTimeDisplay() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        startTimeEditText.setText(timeFormat.format(startTime.getTime()));
    }

    private void updateEndTimeDisplay() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        endTimeEditText.setText(timeFormat.format(endTime.getTime()));
    }

    public boolean validateData() {
        boolean isValid = true;

        if (eventImageUri == null) {
            Toast.makeText(getContext(), "Harap pilih poster event", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (TextUtils.isEmpty(titleEditText.getText())) {
            titleEditText.setError("Judul tidak boleh kosong");
            isValid = false;
        }

        if (TextUtils.isEmpty(dateEditText.getText())) {
            dateEditText.setError("Tanggal tidak boleh kosong");
            isValid = false;
        }

        if (TextUtils.isEmpty(startTimeEditText.getText())) {
            startTimeEditText.setError("Jam mulai tidak boleh kosong");
            isValid = false;
        }

        if (TextUtils.isEmpty(endTimeEditText.getText())) {
            endTimeEditText.setError("Jam selesai tidak boleh kosong");
            isValid = false;
        }

        if (TextUtils.isEmpty(descriptionEditText.getText())) {
            descriptionEditText.setError("Deskripsi tidak boleh kosong");
            isValid = false;
        }

        return isValid;
    }

    public Map<String, Object> getEventData() {
        Map<String, Object> data = new HashMap<>();

        data.put("category", categorySpinner.getSelectedItem().toString());
        data.put("title", titleEditText.getText().toString().trim());

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        data.put("eventDate", dateFormat.format(selectedDate.getTime()));
        data.put("startTime", timeFormat.format(startTime.getTime()));
        data.put("endTime", timeFormat.format(endTime.getTime()));

        data.put("description", descriptionEditText.getText().toString().trim());

        return data;
    }

    public Uri getEventImageUri() {
        return eventImageUri;
    }
}