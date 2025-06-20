package com.campushub.mobile.mhs;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.campushub.mobile.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MhsKodeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mhs_kode);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        String title = getIntent().getStringExtra("title");
        TextView tvEventTitle = findViewById(R.id.textView21);
        tvEventTitle.setText(title);

        String category = getIntent().getStringExtra("category");
        TextView tvCategory = findViewById(R.id.textView22);
        tvCategory.setText(category);

        String lokasi = getIntent().getStringExtra("location");
        TextView tvLokasi = findViewById(R.id.kode_lokasi);
        tvLokasi.setText(lokasi);

        String tanggal = getIntent().getStringExtra("date");
        TextView tvTanggal = findViewById(R.id.kode_tanggal);
        tvTanggal.setText(tanggal);

        String waktu = getIntent().getStringExtra("time");
        TextView tvWaktu = findViewById(R.id.kode_waktu);
        tvWaktu.setText(waktu);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getDisplayName() != null) {
            TextView tvNama = findViewById(R.id.kode_nama);
            tvNama.setText(user.getDisplayName());
        }

        String code = getIntent().getStringExtra("code");
        if (code != null && code.length() == 4) {
            TextView code1 = findViewById(R.id.code_input1);
            TextView code2 = findViewById(R.id.code_input2);
            TextView code3 = findViewById(R.id.code_input3);
            TextView code4 = findViewById(R.id.code_input4);
            code1.setText(String.valueOf(code.charAt(0)));
            code2.setText(String.valueOf(code.charAt(1)));
            code3.setText(String.valueOf(code.charAt(2)));
            code4.setText(String.valueOf(code.charAt(3)));
        }
    }
}