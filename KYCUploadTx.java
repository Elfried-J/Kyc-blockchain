package com.example.kycchain.model.tx;

public class KYCUploadTx extends AbstractTransaction {
    private final String customerId;
    private final String dataHash;
    private final String dataPointer;

    public KYCUploadTx(String customerId, String dataHash, String dataPointer) {
        super("KYC_UPLOAD");
        this.customerId = customerId;
        this.dataHash = dataHash;
        this.dataPointer = dataPointer;
    }
    public String getCustomerId() { return customerId; }
    public String getDataHash() { return dataHash; }
    public String getDataPointer() { return dataPointer; }

    @Override protected String canonicalPayload() {
        return customerId + "|" + dataHash + "|" + dataPointer;
    }

    @Override public String summary() {
        return "UPLOAD[" + getTxId() + "] customer=" + customerId + " ptr=" + dataPointer;
    }
}
