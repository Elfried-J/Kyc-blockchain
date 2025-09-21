package com.example.kycchain.model.tx;

public class KYCVerifyTx extends AbstractTransaction {
    private String bankId;
    private final String customerId;
    private final String uploadTxId;
    private final boolean verified;
    private final String notes;
    private final String modelId;
    private final String modelVersion;
    private final String attestationHash;

    public KYCVerifyTx(String bankId, String customerId, String uploadTxId, boolean verified, String notes) {
        this(bankId, customerId, uploadTxId, verified, notes, null, null, null);
    }

    public KYCVerifyTx(String bankId, String customerId, String uploadTxId, boolean verified, String notes,
                       String modelId, String modelVersion, String attestationHash) {
        super("KYC_VERIFY");
        this.bankId = bankId;
        this.customerId = customerId;
        this.uploadTxId = uploadTxId;
        this.verified = verified;
        this.notes = notes == null ? "" : notes;
        this.modelId = modelId;
        this.modelVersion = modelVersion;
        this.attestationHash = attestationHash;
    }

    public String getBankId() { return bankId; }
    public String getCustomerId() { return customerId; }
    public String getUploadTxId() { return uploadTxId; }
    public boolean isVerified() { return verified; }
    public String getNotes() { return notes; }
    public String getModelId() { return modelId; }
    public String getModelVersion() { return modelVersion; }
    public String getAttestationHash() { return attestationHash; }

    @Override
    protected String canonicalPayload() {
        String mid = modelId == null ? "" : modelId;
        String mv  = modelVersion == null ? "" : modelVersion;
        String ah  = attestationHash == null ? "" : attestationHash;
        return bankId + "|" + customerId + "|" + uploadTxId + "|" + verified + "|" + notes + "|" + mid + "|" + mv + "|" + ah;
    }

    @Override
    public String summary() {
        String ai = (modelId == null) ? "" : (" ai=" + modelId + "@" + modelVersion + " att=" + attestationHash);
        return "VERIFY[" + getTxId() + "] bank=" + bankId + " customer=" + customerId + " verified=" + verified + ai;
    }
}
