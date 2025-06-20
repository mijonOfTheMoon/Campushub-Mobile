package com.campushub.mobile.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.campushub.mobile.models.User;

public class UserCache {
    private static final String PREF_NAME = "user_cache";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_PHOTO_URL = "user_photo_url";
    private static final String KEY_USER_IS_EO = "user_is_eo";
    private static final String KEY_CACHE_TIMESTAMP = "cache_timestamp";
    private static final String KEY_USER_ID = "user_id";
    private static final long CACHE_VALIDITY_PERIOD = 24 * 60 * 60 * 1000;
    
    private static UserCache instance;
    private final SharedPreferences sharedPreferences;
    
    private UserCache(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized UserCache getInstance(Context context) {
        if (instance == null) {
            instance = new UserCache(context.getApplicationContext());
        }
        return instance;
    }
    
    public void cacheUserData(String userId, User user) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, user.getNamaLengkap());
        editor.putString(KEY_USER_EMAIL, user.getAlamatEmail());
        editor.putString(KEY_USER_PHONE, user.getNomorTelepon());
        editor.putString(KEY_USER_PHOTO_URL, user.getPhotoUrl());
        editor.putBoolean(KEY_USER_IS_EO, user.getIs_eo());
        editor.putLong(KEY_CACHE_TIMESTAMP, System.currentTimeMillis());
        editor.apply();
        notifyUserDataUpdated(user);
    }
    
    public User getCachedUserData() {
        if (!isCacheValid()) {
            return null;
        }
        
        String name = sharedPreferences.getString(KEY_USER_NAME, null);
        String email = sharedPreferences.getString(KEY_USER_EMAIL, null);
        String phone = sharedPreferences.getString(KEY_USER_PHONE, null);
        String photoUrl = sharedPreferences.getString(KEY_USER_PHOTO_URL, null);
        boolean isEo = sharedPreferences.getBoolean(KEY_USER_IS_EO, false);
        
        if (name != null || email != null) {
            return new User(isEo, name, email, phone, photoUrl);
        }
        
        return null;
    }
    
    public String getCachedUserId() {
        if (!isCacheValid()) {
            return null;
        }
        return sharedPreferences.getString(KEY_USER_ID, null);
    }
    
    public boolean isCacheValid() {
        long cacheTimestamp = sharedPreferences.getLong(KEY_CACHE_TIMESTAMP, 0);
        long currentTime = System.currentTimeMillis();
        return (currentTime - cacheTimestamp) < CACHE_VALIDITY_PERIOD;
    }
    
    public boolean hasCachedData() {
        return isCacheValid() && sharedPreferences.contains(KEY_USER_ID);
    }
    
    public void clearCache() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    public interface UserDataUpdateListener {
        void onUserDataUpdated(User user);
    }
    
    private UserDataUpdateListener updateListener;
    
    public void setUserDataUpdateListener(UserDataUpdateListener listener) {
        this.updateListener = listener;
    }
    
    public void notifyUserDataUpdated(User user) {
        if (updateListener != null) {
            updateListener.onUserDataUpdated(user);
        }
    }
}
