package com.ghost.api.dto;

public class AuthData {
    private String nonce;
    private String hash;

    // No-args constructor for Gson
    public AuthData() {
    }

    public AuthData(String nonce, String hash) {
        this.nonce = nonce;
        this.hash = hash;
    }

    public String nonce() {
        return nonce;
    }

    public String hash() {
        return hash;
    }

    public String getNonce() {
        return nonce;
    }

    public String getHash() {
        return hash;
    }

    @Override
    public String toString() {
        return "AuthData{nonce='" + nonce + "', hash='" + hash + "'}";
    }
}
