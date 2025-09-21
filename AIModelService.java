package com.example.kycchain.ai;

import com.example.kycchain.model.tx.KYCUploadTx;

public interface AIModelService {
    Attestation attest(KYCUploadTx uploadTx);
    String getModelId();
    String getModelVersion();
    String getServicePubKeyB64();
}
