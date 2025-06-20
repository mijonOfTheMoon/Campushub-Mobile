package com.campushub.mobile.eo;

import android.os.Bundle;
import android.view.KeyEvent;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.view.View;

import com.campushub.mobile.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

import android.os.Handler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EoAttendanceActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_eo_attendance);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        eventId = getIntent().getStringExtra("eventId");

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        EditText code1 = findViewById(R.id.code_input1);
        EditText code2 = findViewById(R.id.code_input2);
        EditText code3 = findViewById(R.id.code_input3);
        EditText code4 = findViewById(R.id.code_input4);

        TextWatcher forwardWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    View next = null;
                    if (code1.hasFocus()) next = code2;
                    else if (code2.hasFocus()) next = code3;
                    else if (code3.hasFocus()) next = code4;
                    if (next != null) next.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        code1.addTextChangedListener(forwardWatcher);
        code2.addTextChangedListener(forwardWatcher);
        code3.addTextChangedListener(forwardWatcher);

        code2.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN && code2.getText().length() == 0) {
                code1.requestFocus();
                return true;
            }
            return false;
        });
        code3.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN && code3.getText().length() == 0) {
                code2.requestFocus();
                return true;
            }
            return false;
        });
        code4.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN && code4.getText().length() == 0) {
                code3.requestFocus();
                return true;
            }
            return false;
        });

        Button submit = findViewById(R.id.create_button);
        submit.setOnClickListener(v -> {
            String code = "" + code1.getText() + code2.getText() + code3.getText() + code4.getText();
            if (code.length() != 4) {
                Toast.makeText(this, "Masukkan semua 4 digit kode", Toast.LENGTH_SHORT).show();
                return;
            }
            if (eventId == null) {
                Toast.makeText(this, "Event tidak dikenal", Toast.LENGTH_SHORT).show();
                return;
            }
            db.collection("registrations").whereEqualTo("eventId", eventId).whereEqualTo("code", code).get().addOnSuccessListener((QuerySnapshot snap) -> {
                if (snap.isEmpty()) {
                    Toast.makeText(this, "Kode tidak valid", Toast.LENGTH_SHORT).show();
                    return;
                }
                DocumentSnapshot reg = snap.getDocuments().get(0);

                String status = reg.getString("status");
                if (!"Terdaftar".equalsIgnoreCase(status)) {
                    String msg = switch (status.toLowerCase()) {
                        case "dibatalkan" -> "Peserta telah membatalkan pendaftaran";
                        case "tidak hadir" -> "Peserta telah ditandai tidak hadir";
                        case "hadir" -> "Peserta sudah hadir";
                        case "waiting list" -> "Peserta berada di daftar tunggu";
                        default -> "Status pendaftaran tidak valid";
                    };
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    code1.setText("");
                    code2.setText("");
                    code3.setText("");
                    code4.setText("");
                    code1.requestFocus();
                    return;
                }
                String regId = reg.getId();

                db.collection("registrations").document(regId).update("status", "Hadir").addOnSuccessListener(a -> {

                    String userId = reg.getString("userId");
                    db.collection("users").document(userId).get().addOnSuccessListener(doc -> {

                        TextView tvName = findViewById(R.id.eo_nama);
                        String name = doc.getString("namaLengkap");
                        tvName.setText(name != null && !name.isBlank() ? name : "------");
                        TextView tvTime = findViewById(R.id.eo_waktu_presensi);
                        String now = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                        tvTime.setText(!now.isBlank() ? now : "------");
                        TextView tvEmail = findViewById(R.id.eo_email);
                        String email = doc.getString("alamatEmail");
                        tvEmail.setText(email != null && !email.isBlank() ? email : "------");
                        TextView tvPhone = findViewById(R.id.eo_phone);
                        String phone = doc.getString("nomorTelepon");
                        tvPhone.setText(phone != null && !phone.isBlank() ? phone : "------");
                        code1.setText("");
                        code2.setText("");
                        code3.setText("");
                        code4.setText("");
                        code1.requestFocus();
                        new Handler().postDelayed(() -> {
                            tvName.setText("------");
                            tvTime.setText("------");
                            tvEmail.setText("------");
                            tvPhone.setText("------");
                        }, 3000);
                    });
                });
            }).addOnFailureListener(e -> Toast.makeText(this, "Verifikasi gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }
}