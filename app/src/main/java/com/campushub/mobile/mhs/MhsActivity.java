package com.campushub.mobile.mhs;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.campushub.mobile.ProfileFragment;
import com.campushub.mobile.R;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;

public class MhsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mhs);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ChipNavigationBar navbar = findViewById(R.id.navbar);

        navbar.setOnItemSelectedListener(item -> {
            Fragment selected = null;

            if (item == R.id.home) {
                selected = new MhsHomeFragment();
            } else if (item == R.id.my_events) {
                selected = new MhsMyEventsFragment();
            }
            else if (item == R.id.profile) {
                selected = new ProfileFragment();
            }

            if (selected != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selected)
                        .commit();
            }
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new MhsHomeFragment())
                    .commit();
            navbar.setItemSelected(R.id.home, true);
        }
    }
}