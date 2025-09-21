package com.example.kycchain.ai;

import com.example.kycchain.util.CryptoUtils;
import java.util.List;
import java.util.Objects;

public final class Attestation {
    public final String modelId;
    public final String modelVersion;
    public final String inputFingerprint;
    public final double riskScore;
    public final List<String> flags;
    public final long issuedAt;
    public final String servicePubKeyB64;
    public final String signatureB64;

    public Attestation(String modelId, String modelVersion, String inputFingerprint,
                       double riskScore, List<String> flags, long issuedAt,
                       String servicePubKeyB64, String signatureB64) {
        this.modelId = modelId;
        this.modelVersion = modelVersion;
        this.inputFingerprint = inputFingerprint;
        this.riskScore = riskScore;
        this.flags = List.copyOf(flags);
        this.issuedAt = issuedAt;
        this.servicePubKeyB64 = servicePubKeyB64;
        this.signatureB64 = signatureB64;
    }

    public String canonical() {
        String joinedFlags = String.join(",", flags);
        return modelId + "|" + modelVersion + "|" + inputFingerprint + "|" +
                String.format("%.6f", riskScore) + "|" + joinedFlags + "|" + issuedAt + "|" + servicePubKeyB64;
    }

    public String hashHex() {
        return CryptoUtils.sha256Hex(canonical());
    }

    public boolean verifySignature() {
        return CryptoUtils.verifyFromBase64(canonical(), signatureB64, servicePubKeyB64);
    }

    public static final class Builder {
        private String modelId, modelVersion, inputFingerprint, servicePubKeyB64;
        private double riskScore;
        private List<String> flags = List.of();
        private long issuedAt;
        private java.security.PrivateKey signer;

        public Builder model(String id, String version) { this.modelId = id; this.modelVersion = version; return this; }
        public Builder fingerprint(String fp) { this.inputFingerprint = fp; return this; }
        public Builder risk(double r) { this.riskScore = r; return this; }
        public Builder flags(List<String> f) { this.flags = List.copyOf(f); return this; }
        public Builder issuedAt(long t) { this.issuedAt = t; return this; }
        public Builder serviceKeys(String pubKeyB64, java.security.PrivateKey priv) { this.servicePubKeyB64 = pubKeyB64; this.signer = priv; return this; }

        public Attestation buildAndSign() {
            Objects.requireNonNull(modelId);
            Objects.requireNonNull(modelVersion);
            Objects.requireNonNull(inputFingerprint);
            Objects.requireNonNull(servicePubKeyB64);
            if (issuedAt == 0) issuedAt = System.currentTimeMillis();
            String canonical = modelId + "|" + modelVersion + "|" + inputFingerprint + "|" +
                    String.format("%.6f", riskScore) + "|" + String.join(",", flags) + "|" +
                    issuedAt + "|" + servicePubKeyB64;
            String sig = CryptoUtils.signToBase64(canonical, signer);
            return new Attestation(modelId, modelVersion, inputFingerprint, riskScore, flags, issuedAt, servicePubKeyB64, sig);
        }
    }
}
