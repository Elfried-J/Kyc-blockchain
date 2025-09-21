package com.example.kycchain.model;

import com.example.kycchain.util.CryptoUtils;
import java.security.KeyPair;

public class Participant {
    private final String name;
    private final String role;
    private final KeyPair keys;

    public Participant(String name, String role) {
        this.name = name;
        this.role = role;
        this.keys = CryptoUtils.generateECKeyPair();
    }
    public String getName() { return name; }
    public String getRole() { return role; }
    public KeyPair getKeys() { return keys; }
    public String publicKeyB64() { return CryptoUtils.publicKeyToBase64(keys.getPublic()); }
    public String sign(String data) { return CryptoUtils.signToBase64(data, keys.getPrivate()); }
}
