package com.campushub.mobile.models;

public class User {
    private boolean is_eo;
    private String namaLengkap;
    private String alamatEmail;
    private String nomorTelepon;
    private String photoUrl;

    public User() {
    }

    public User(boolean is_eo, String namaLengkap, String alamatEmail, String nomorTelepon, String photoUrl) {
        this.is_eo = is_eo;
        this.namaLengkap = namaLengkap;
        this.alamatEmail = alamatEmail;
        this.nomorTelepon = nomorTelepon;
        this.photoUrl = photoUrl;
    }

    public boolean getIs_eo() {
        return is_eo;
    }

    public String getNamaLengkap() {
        return namaLengkap;
    }

    public String getAlamatEmail() {
        return alamatEmail;
    }

    public String getNomorTelepon() {
        return nomorTelepon;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
