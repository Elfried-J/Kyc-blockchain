package com.example.kycchain.model.tx;

import com.example.kycchain.util.CryptoUtils;

/**
 * Base class for all transactions.
 * - Holds signer info (name + pubkey) and signature.
 * - Computes txId as SHA-256 over (canonicalData + "|" + signatureB64).
 * - Adds canonicalDataFor(pubKeyB64) so we can sign over the exact bytes
 *   that will later be verified (including the signer public key).
 */
public abstract class AbstractTransaction implements Transaction {

    protected final String type;
    protected final long timestamp;

    protected String signerName;
    protected String signerPubKeyB64;
    protected String signatureB64;

    protected String txId;

    protected AbstractTransaction(String type) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    @Override public String getType() { return type; }
    @Override public long getTimestamp() { return timestamp; }
    @Override public String getSignerName() { return signerName; }
    @Override public String getSignerPublicKeyBase64() { return signerPubKeyB64; }
    @Override public String getSignatureBase64() { return signatureB64; }
    @Override public String getTxId() { return txId; }

    /** Must be implemented by concrete tx types. */
    protected abstract String canonicalPayload();

    /**
     * Canonical data used for verification (includes the stored signerPubKeyB64).
     */
    @Override
    public String canonicalData() {
        return type + "|" + timestamp + "|" + canonicalPayload() + "|" + signerPubKeyB64;
    }

    /**
     * Canonical data to be used at signing time, before signerPubKeyB64 is set.
     * This ensures the bytes we sign are exactly the same bytes we verify later.
     */
    public String canonicalDataFor(String pubKeyB64) {
        return type + "|" + timestamp + "|" + canonicalPayload() + "|" + pubKeyB64;
    }

    /** Compute txId binding canonical data and signature. */
    protected void finalizeId() {
        this.txId = CryptoUtils.sha256Hex(canonicalData() + "|" + signatureB64);
    }

    @Override
    public void signWith(String signerName, String signerPubKeyB64, String signatureBase64) {
        this.signerName = signerName;
        this.signerPubKeyB64 = signerPubKeyB64;
        this.signatureB64 = signatureBase64;
        finalizeId();
    }

    @Override
    public boolean verifySignature() {
        if (signatureB64 == null || signerPubKeyB64 == null) return false;
        return CryptoUtils.verifyFromBase64(canonicalData(), signatureB64, signerPubKeyB64);
    }

    @Override
    public String summary() { return getType() + "[" + getTxId() + "]"; }
}
