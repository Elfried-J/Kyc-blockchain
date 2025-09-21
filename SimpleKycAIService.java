package com.example.kycchain.ai;

import com.example.kycchain.model.tx.KYCUploadTx;
import com.example.kycchain.util.CryptoUtils;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

public class SimpleKycAIService implements AIModelService {
    private final String modelId;
    private final String version;
    private final KeyPair serviceKeys;

    public SimpleKycAIService(String modelId, String version) {
        this.modelId = modelId;
        this.version = version;
        this.serviceKeys = CryptoUtils.generateECKeyPair();
    }

    @Override public Attestation attest(KYCUploadTx uploadTx) {
        String fp = CryptoUtils.sha256Hex(uploadTx.getCustomerId() + "|" + uploadTx.getDataHash());
        double risk = computeRisk(uploadTx);
        List<String> flags = new ArrayList<>();
        flags.add(risk < 0.2 ? "low_risk" : (risk < 0.5 ? "medium_risk" : "high_risk"));
        flags.add("upload_ptr=" + uploadTx.getDataPointer());
        return new Attestation.Builder()
                .model(modelId, version)
                .fingerprint(fp)
                .risk(risk)
                .flags(flags)
                .serviceKeys(CryptoUtils.publicKeyToBase64(serviceKeys.getPublic()), serviceKeys.getPrivate())
                .buildAndSign();
    }

    private static double computeRisk(KYCUploadTx up) {
        long seed = up.getTxId().hashCode() * 0x9E3779B97F4A7C15L;
        double base = (Math.abs(seed % 1000) / 1000.0) * 0.5 + 0.05;
        return Math.min(0.55, Math.max(0.05, base));
    }

    @Override public String getModelId() { return modelId; }
    @Override public String getModelVersion() { return version; }
    @Override public String getServicePubKeyB64() { return CryptoUtils.publicKeyToBase64(serviceKeys.getPublic()); }
}
