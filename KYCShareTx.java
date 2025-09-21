package com.example.kycchain.model.tx;

public class KYCShareTx extends AbstractTransaction {
    private final String fromEntity;
    private final String toEntity;
    private final String customerId;
    private final String verifiedTxId;

    public KYCShareTx(String fromEntity, String toEntity, String customerId, String verifiedTxId) {
        super("KYC_SHARE");
        this.fromEntity = fromEntity;
        this.toEntity = toEntity;
        this.customerId = customerId;
        this.verifiedTxId = verifiedTxId;
    }

    public String getFromEntity() { return fromEntity; }
    public String getToEntity() { return toEntity; }
    public String getCustomerId() { return customerId; }
    public String getVerifiedTxId() { return verifiedTxId; }

    @Override protected String canonicalPayload() {
        return fromEntity + "|" + toEntity + "|" + customerId + "|" + verifiedTxId;
    }

    @Override public String summary() {
        return "SHARE[" + getTxId() + "] " + fromEntity + " → " + toEntity + " for " + customerId;
    }
}
