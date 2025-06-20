package com.campushub.mobile;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.campushub.mobile.eo.EoActivity;
import com.campushub.mobile.mhs.MhsActivity;
import com.campushub.mobile.models.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class NoConnectionActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_connection);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupSwipeRefresh();
    }

    private void initViews() {
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::retryFetchUserData);
    }

    private void retryFetchUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get().addOnCompleteListener(task -> {
                swipeRefreshLayout.setRefreshing(false);

                if (task.isSuccessful()) {
                    @SuppressLint("UnsafeIntentLaunch") Intent intent = getIntent(task);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            });
        } else {
            swipeRefreshLayout.setRefreshing(false);

            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }

    @NonNull
    private Intent getIntent(Task<DocumentSnapshot> task) {
        DocumentSnapshot document = task.getResult();
        Intent intent;

        if (document.exists()) {
            User userData = document.toObject(User.class);
            if (userData != null && userData.getIs_eo()) {
                intent = new Intent(NoConnectionActivity.this, EoActivity.class);
            } else {
                intent = new Intent(NoConnectionActivity.this, MhsActivity.class);
            }
        } else {
            intent = new Intent(NoConnectionActivity.this, MhsActivity.class);
        }

        return intent;
    }
}
