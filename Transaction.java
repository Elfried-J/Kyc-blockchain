package com.example.kycchain.model.tx;

public interface Transaction {
    String getType();
    long getTimestamp();
    String getSignerName();
    String getSignerPublicKeyBase64();
    String getSignatureBase64();
    String getTxId();
    String canonicalData();
    void signWith(String signerName, String signerPubKeyB64, String signatureBase64);
    boolean verifySignature();
    String summary();
}
