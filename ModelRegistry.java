package com.example.kycchain.ai;

import java.util.HashMap;
import java.util.Map;

public class ModelRegistry {
    private final Map<String, Map<String, String>> reg = new HashMap<>();

    public void register(String modelId, String version, String pubKeyB64) {
        reg.computeIfAbsent(modelId, k -> new HashMap<>()).put(version, pubKeyB64);
    }

    public String getPubKey(String modelId, String version) {
        Map<String, String> v = reg.get(modelId);
        return v == null ? null : v.get(version);
    }

    public boolean verify(Attestation a) {
        String expected = getPubKey(a.modelId, a.modelVersion);
        if (expected == null) return false;
        if (!expected.equals(a.servicePubKeyB64)) return false;
        return a.verifySignature();
    }
}
