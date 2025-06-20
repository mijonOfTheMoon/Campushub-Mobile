package com.campushub.mobile;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.campushub.mobile.eo.EoActivity;
import com.campushub.mobile.mhs.MhsActivity;
import com.campushub.mobile.models.User;
import com.campushub.mobile.utils.UserCache;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SplashScreen splashScreen;
    private UserCache userCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(() -> true);

        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userCache = UserCache.getInstance(this);

        updateUI();
    }    private void updateUI() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Check if we have valid cached data
            if (userCache.hasCachedData()) {
                String cachedUserId = userCache.getCachedUserId();
                if (cachedUserId != null && cachedUserId.equals(currentUser.getUid())) {
                    // Use cached data to navigate
                    User cachedUser = userCache.getCachedUserData();
                    if (cachedUser != null) {
                        Intent intent = createNavigationIntent(cachedUser);
                        splashScreen.setKeepOnScreenCondition(() -> false);
                        startActivity(intent);
                        finish();
                        return;
                    }
                }
            }
            
            // Fetch fresh data from Firestore and cache it
            db.collection("users").document(currentUser.getUid()).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        User userData = document.toObject(User.class);
                        if (userData != null) {
                            // Cache the user data
                            userCache.cacheUserData(currentUser.getUid(), userData);
                        }
                    }
                    
                    @SuppressLint("UnsafeIntentLaunch") Intent intent = getIntent(task);
                    splashScreen.setKeepOnScreenCondition(() -> false);
                    startActivity(intent);
                    finish();
                } else {
                    Intent intent = new Intent(MainActivity.this, NoConnectionActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

                    splashScreen.setKeepOnScreenCondition(() -> false);
                    startActivity(intent);
                    finish();
                }
            });
        } else {
            // Clear cache when user is not authenticated
            userCache.clearCache();
            
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

            splashScreen.setKeepOnScreenCondition(() -> false);
            startActivity(intent);
            finish();
        }
    }

    @NonNull
    private Intent createNavigationIntent(User userData) {
        Intent intent;
        if (userData.getIs_eo()) {
            intent = new Intent(MainActivity.this, EoActivity.class);
        } else {
            intent = new Intent(MainActivity.this, MhsActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @NonNull
    private Intent getIntent(Task<DocumentSnapshot> task) {
        DocumentSnapshot document = task.getResult();
        Intent intent;

        User userData = document.toObject(User.class);
        if (userData != null && userData.getIs_eo()) {
            intent = new Intent(MainActivity.this, EoActivity.class);
        } else {
            intent = new Intent(MainActivity.this, MhsActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
}